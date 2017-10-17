package com.sprd.generalsecurity.network;

import android.app.Activity;
import android.app.AppGlobals;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.IGeneralSecureManager;
import com.sprd.generalsecurity.R;
import com.sprd.generalsecurity.data.AppInfoTable;
import com.sprd.generalsecurity.data.BlockStateProvider;
import com.sprd.generalsecurity.utils.DateCycleUtils;
import com.sprd.generalsecurity.utils.TeleUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static android.net.NetworkTemplate.buildTemplateMobileAll;
import static android.net.NetworkTemplate.buildTemplateWifiWildcard;

public class DataFlowManagement extends Activity implements View.OnClickListener {

    private static String TAG = "DataFlowManagement";
    private final int MSG_SIM1_STATS_LOAD_FINISHED = 0;
    private final int MSG_SIM2_STATS_LOAD_FINISHED = 1;
    private final int MSG_WIFI_STATS_LOAD_FINISHED = 2;

    private static final String CLAUSE_SIM1_ENABLE_QUERY = "blockstate=2 or blockstate=3 "
            + "or blockstate=6 or blockstate=7";

    private static final String CLAUSE_SIM2_ENABLE_QUERY = "blockstate=4 or blockstate=5 "
            + "or blockstate=6 or blockstate=7";

    private static final String CLAUSE_WIFI_ENABLE_QUERY = "blockstate=1 or blockstate=3 "
            + "or blockstate=5 or blockstate=7";

    String[] QUERY_PRJECTION = {AppInfoTable.COLUMN_PKG_NAME, AppInfoTable.COLUMN_UID,
            AppInfoTable.COLUMN_BLOCK_STATE};
    private static String UID_SELECTION_CLAUSE = AppInfoTable.COLUMN_UID + "=?";

    private enum NetworkType {
        SIM1, SIM2, WIFI
    }

    /*
    *@param blockState:
    *  0—block all
    * 00000001—wifi only (即第0个比特表示是否禁用wifi)
    * 00000010—SIM1 mobileData only
    * 00000100—SIM2 mobileData only
    * 00000111 — no block(值为7)
    *
    */
    private static final int DISABLE_WIFI = 0x06;
    private static final int DISABLE_SIM1 = 0x05;
    private static final int DISABLE_SIM2 = 0x03;

    private static final int ENABLE_WIFI = 0x01;
    private static final int ENABLE_SIM1 = 0x02;
    private static final int ENABLE_SIM2 = 0x04;
    private static final int NO_BLOCK = 0x07;

    private static int NetworkTypeWIFI = 0;
    private static int NetworkTypeSIM1 = 1;
    private static int NetworkTypeSIM2 = 2;

    private static int INDEX_LABEL = 0;
    private static int INDEX_UID = 1;
    private static int INDEX_STATE = 2;

    private AppAdapter mAdapter;
    ArrayList<AppItem> mApps;
    // SPRD: bug 758614 ConcurrentModificationException leading to crash
    List<UidDetail> mApps_Flow;
    SparseArray<UidDetail> mKnowApps;
    private ListView mListView;
    HashMap<Integer, Integer> mAppInfoMap = new HashMap<>();

    private CheckBox mCheckSim1All;
    private CheckBox mCheckWIFIAll;
    private Spinner mFlowRankSpinner;
    private boolean mOperatingWIFIAll;
    private boolean mOperatingSim1All;
    private boolean mOperatingSim2All;
    private boolean mDualSimRelease = false;

    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;
    private int mSimCount = 0;

    private static int NO_ITEM_INFO = -1;// no app info in DB
    private int flow_rank_type = 0;

    private static boolean NetworkEnabled = true;
    private static IGeneralSecureManager mGeneralSecureManager = null;
    private INetworkStatsSession mStatsSession;
    private INetworkStatsService mStatsService;

    private int mDataCycleSim1;
    private int mDataCycleSim2;
    private int mDataCycleWIFI;

    private long mWifiUsageTotal;
    private long mSim1UsageTotal;
    private long mSim2UsageTotal;

    SubscriptionInfo mSubInfo1, mSubInfo2;
    private NetworkTemplate mTemplate;
    private int mPrimaryCard;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "handleMessage:" + msg.what);
            switch (msg.what) {
                case MSG_SIM1_STATS_LOAD_FINISHED:
                    if (!mDualSimRelease) {
                        updateNetworkUsageStat(NetworkType.WIFI, mDataCycleWIFI);
                    } else {
                        updateNetworkUsageStatForDualSim(NetworkTypeSIM2);
                    }
                    break;
                case MSG_SIM2_STATS_LOAD_FINISHED:
                    updateNetworkUsageStat(NetworkType.WIFI, mDataCycleWIFI);
                    break;
                case MSG_WIFI_STATS_LOAD_FINISHED:
                    Collections.sort(new ArrayList<UidDetail>(mApps_Flow));
                    // SPRD: Bug707100 the dataflow rank is not regular and the wifi data is 0
                    reloadAppData();
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                    Log.d(TAG, "notifyDataSetChanged");
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * @ param packageUid: uid of package
     * @ param networkType:
     * 0 — TYPE_WIFI
     * 1 — TYPE_SIM1_MOBILE_DATA
     * 2 — TYPE_SIM2_MOBILE_DATA
     * @ param allowed:
     * true — allowed on the networkType
     * false — not allowed on the networkType
     */
    static void setPackageBlockState(int packageUid, int networkType, boolean allowed) {
        Log.e(TAG, "setPackageBlockState uid=" + packageUid + ":type=" + networkType + ":allowed=" + allowed);
        if (NetworkEnabled) {
            if (mGeneralSecureManager == null) {
                Log.d(TAG, "mGeneralSecureManager is null");
            } else {
                try {
                    mGeneralSecureManager.setPackageBlockState(packageUid, networkType, allowed);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static void deleteBlockPackage(int packageUid) {
        Log.e(TAG, "deleteBlockPackage uid=" + packageUid);
        if (NetworkEnabled) {
            if (mGeneralSecureManager == null) {
                Log.d(TAG, "mGeneralSecureManager is null");
            } else {
                try {
                    mGeneralSecureManager.deleteBlockPackage(packageUid);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mStatsService = INetworkStatsService.Stub.asInterface(ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        try {
            mStatsSession = mStatsService.openSession();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        // SPRD: bug 758614 ConcurrentModificationException leading to crash
        mApps_Flow = new CopyOnWriteArrayList<UidDetail>();
        mKnowApps = new SparseArray<UidDetail>();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String v = sharedPref.getString(DateCycleUtils.KEY_DATA_CYCLE_SIM1, "0");
        mDataCycleSim1 = Integer.parseInt(v);

        v = sharedPref.getString(DateCycleUtils.KEY_DATA_CYCLE_SIM2, "0");
        mDataCycleSim2 = Integer.parseInt(v);
        if (TeleUtils.getPrimarySlot(this) == this.NetworkTypeSIM1) {
            mDataCycleWIFI = mDataCycleSim1;
        } else {
            mDataCycleWIFI = mDataCycleSim2;
        }

        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mSimCount = TeleUtils.getSimCount(this);

        //
        // enableTestMode();
        //

        if (mSimCount == 0 || mSimCount == 1) {
            mDualSimRelease = false;
        } else {
            mDualSimRelease = true;
        }
        setContentView(R.layout.manage_app);
        Log.e(TAG, "oncreate: dual" + mDualSimRelease);
        mListView = (ListView) findViewById(android.R.id.list);
        mCheckSim1All = (CheckBox) findViewById(R.id.checkBoxAllSim1);
        mCheckWIFIAll = (CheckBox) findViewById(R.id.checkBoxAllWIFI);
        mCheckSim1All.setOnClickListener(this);
        mCheckWIFIAll.setOnClickListener(this);
        mFlowRankSpinner = (Spinner) findViewById(R.id.flow_rank);
        mFlowRankSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                flow_rank_type = position;
                if (mApps != null) {
                    Collections.sort(mApps);
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //disable the checkbox if no sim card inserted
        if (mSimCount == 0) {
            mCheckSim1All.setEnabled(false);
        }

        if (NetworkEnabled) {
            if (mGeneralSecureManager == null) {
                IBinder netBinder = ServiceManager.getService("generalsecure");
                if (netBinder == null) {
                    Log.d(TAG, "generalsecure fail");
                }
                mGeneralSecureManager = IGeneralSecureManager.Stub.asInterface(netBinder);
                if (mGeneralSecureManager == null) {
                    Log.d(TAG, "mGeneralSecureManager is null");
                }
            }
        }
    }


    private void initStat() {
        mKnowApps.clear();
        mApps_Flow.clear();
        mSim1UsageTotal = 0;
        mWifiUsageTotal = 0;
        mSim2UsageTotal = 0;
        Log.e(TAG, "data clear");

    }

    private static String getActiveSubscriberId(Context context, int subId) {
        final TelephonyManager tele = TelephonyManager.from(context);
        String retVal = tele.getSubscriberId(subId);
        Log.d(TAG, "getActiveSubscriberId=" + retVal + " subId=" + subId);
        return retVal;
    }

    private static String getActiveSubscriberId(Context context) {
        final TelephonyManager tele = TelephonyManager.from(context);
        final String actualSubscriberId = tele.getSubscriberId();
        String TEST_SUBSCRIBER_PROP = "test.subscriberid";
        String retVal = SystemProperties.get(TEST_SUBSCRIBER_PROP, actualSubscriberId);
        Log.d(TAG, "getActiveSubscriberId111=" + retVal + " actualSubscriberId=" + actualSubscriberId);
        return retVal;
    }

    private long getCycleStart(int cycleType) {
        long t = 0;
        switch (cycleType) {
            case DateCycleUtils.CYCLE_MONTH:
                t = DateCycleUtils.getMonthCycleStart();
                break;
            case DateCycleUtils.CYCLE_WEEK:
                t = DateCycleUtils.getWeekCycleStart();
                break;
            case DateCycleUtils.CYCLE_DAY:
                t = DateCycleUtils.getDayCycleStart();
                break;
            default:
                break;
        }

        return t;
    }

    private void updateNetworkUsageStatForDualSim(int slotIndex) {
        NetworkTemplate template = buildTemplateMobileAll(TeleUtils.getActiveSubscriberId(this, slotIndex));
        long start = getCycleStart(slotIndex == NetworkTypeSIM1 ? mDataCycleSim1 : mDataCycleSim2);
        int id = (slotIndex == NetworkTypeSIM1 ? NetworkTypeSIM1 : NetworkTypeSIM2);
        getLoaderManager().restartLoader(id, SummaryForAllUidLoader.buildArgs(template, start, System.currentTimeMillis()), mSummaryCallbacks);
    }

    void updateNetworkUsageStat(NetworkType networkType, int cycleType) {
        long t = getCycleStart(cycleType);

        if (networkType == NetworkType.WIFI) {
            mTemplate = buildTemplateWifiWildcard();
            getLoaderManager().restartLoader(NetworkTypeWIFI,
                    SummaryForAllUidLoader.buildArgs(mTemplate, t, System.currentTimeMillis()), mSummaryCallbacks);

        } else if (networkType == NetworkType.SIM1) {
            Log.e(TAG, "load sim1:" + mDualSimRelease);
            //mobile
            if (mDualSimRelease) {
                mTemplate = buildTemplateMobileAll(getActiveSubscriberId(this, NetworkTypeSIM1));
                getLoaderManager().restartLoader(NetworkTypeSIM1,
                        SummaryForAllUidLoader.buildArgs(mTemplate, t, System.currentTimeMillis()), mSummaryCallbacks);
            } else {
                String subscriberId = getActiveSubscriberId(this);
                mTemplate = buildTemplateMobileAll(subscriberId);

                getLoaderManager().restartLoader(NetworkTypeSIM1,
                        SummaryForAllUidLoader.buildArgs(mTemplate, t, System.currentTimeMillis()), mSummaryCallbacks);
            }
        } else if (networkType == NetworkType.SIM2) {
            //mobile
            mTemplate = buildTemplateMobileAll(getActiveSubscriberId(this, NetworkTypeSIM2));
            getLoaderManager().restartLoader(NetworkTypeSIM2,
                    SummaryForAllUidLoader.buildArgs(mTemplate, t, System.currentTimeMillis()), mSummaryCallbacks);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPrimaryCard = TeleUtils.getPrimaryCard(this);
        initStat();

        try {
            mStatsService.forceUpdate();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        if (mDualSimRelease) {
            mSubscriptionManager = SubscriptionManager.from(this);
            updateNetworkUsageStatForDualSim(NetworkTypeSIM1);
        } else {
            updateNetworkUsageStat(NetworkType.SIM1, mDataCycleSim1);
        }
    }


    private void reloadAppData() {
        AppLoadTask t = new AppLoadTask();
        t.execute();
    }

    private void appLoadFinished() {
        if (mAdapter == null) {
            mAdapter = new AppAdapter(this);
        }

        Collections.sort(mApps);

        Log.e(TAG, "release:" + mDualSimRelease + ":" + mPrimaryCard);
        if (mPrimaryCard == 0) {
            if (queryIfAllAppsChecked(NetworkType.SIM1)) {
                mCheckSim1All.setChecked(true);
            } else {
                mCheckSim1All.setChecked(false);
            }
        } else {
            if (queryIfAllAppsChecked(NetworkType.SIM2)) {
                mCheckSim1All.setChecked(true);
            } else {
                mCheckSim1All.setChecked(false);
            }
        }

        if (queryIfAllAppsChecked(NetworkType.WIFI)) {
            mCheckWIFIAll.setChecked(true);
        } else {
            mCheckWIFIAll.setChecked(false);
        }

        // SPRD: Bug730374 closed all apps dataflow,open only dataflow for Browser,it can not download file
        if (mSimCount == 0) {
            mCheckSim1All.setEnabled(false);
            mCheckSim1All.setChecked(false);
        } else {
            mCheckSim1All.setEnabled(true);
            mCheckWIFIAll.setEnabled(true);
        }
        mListView.setAdapter(mAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(final View v) {
        if (v == mCheckSim1All) {
            mCheckWIFIAll.setEnabled(false);
            if (mOperatingSim1All) {
                Log.e(TAG, "blocking all sim1, rtn");
                return;
            }
            mOperatingSim1All = true;
            promptUserOperation();
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

                    int networkType = mPrimaryCard + 1;
                    Log.e(TAG, "networktype:" + networkType);

                    if (((CheckBox) v).isChecked()) {
                        if (networkType == NetworkTypeSIM1) {
                            unBlockAllAppSim1Data();
                        } else {
                            unBlockAllAppSim2Data();
                        }
                        reloadAppData();
                    } else {
                        if (networkType == NetworkTypeSIM1) {
                            blockAllAppSim1Data();
                        } else {
                            blockAllAppSim2Data();
                        }
                        reloadAppData();
                    }
                    mOperatingSim1All = false;
                }
            });
            t.start();

            return;
        } else if (v == mCheckWIFIAll) {
            mCheckSim1All.setEnabled(false);
            if (mOperatingWIFIAll) {
                Log.e(TAG, "blocking all wifi, rtn");
                return;
            }
            mOperatingWIFIAll = true;
            promptUserOperation();
            Log.e(TAG, "blocking all wifi===================" + mOperatingWIFIAll);
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    if (((CheckBox) v).isChecked()) {
                        unBlockAllAppWIFI();
                        reloadAppData();
                    } else {
                        blockAllAppWIFI();
                        reloadAppData();
                    }
                    mOperatingWIFIAll = false;
                    Log.e(TAG, "blocking all wifi~~~~~~~~~~~~~" + mOperatingWIFIAll);
                }
            });
            t.start();

            return;
        }

        AppItem it = (AppItem) v.getTag();

        if (v.getId() == R.id.sim1) {
            int networkType = mPrimaryCard + 1;
            Log.d(TAG, "sim1:" + ((CheckBox) v).isChecked() + " : " + it.label + " : " + it.uid);

            //1. get current state
            int state = queryAppItemState(it.uid);
            if (((CheckBox) v).isChecked()) {
                setPackageBlockState(it.uid, networkType, true);
                if (mPrimaryCard == 0) {
                    updateAppInfoItem(it.uid, state | ENABLE_SIM1);
                    if (queryIfAllAppsChecked(NetworkType.SIM1)) {
                        mCheckSim1All.setChecked(true);
                    }
                } else {
                    //sim2 is primary card
                    updateAppInfoItem(it.uid, state | ENABLE_SIM2);
                    if (queryIfAllAppsChecked(NetworkType.SIM2)) {
                        mCheckSim1All.setChecked(true);
                    }
                }
            } else {
                mCheckSim1All.setChecked(false);
                //disable sim1
                setPackageBlockState(it.uid, networkType, false);
                //item exists in DB, update it.
                if (mPrimaryCard == 0) {
                    updateAppInfoItem(it.uid, state & DISABLE_SIM1);
                } else {
                    updateAppInfoItem(it.uid, state & DISABLE_SIM2);
                }
            }

        } else if (v.getId() == R.id.wifi) {
            Log.e(TAG, "onclick wifi:" + ((CheckBox) v).isChecked() + ":" + it.label + ":" + it.uid);
            int state = queryAppItemState(it.uid);
            if (((CheckBox) v).isChecked()) {
                //enable WIFI
                setPackageBlockState(it.uid, 0, true);
                updateAppInfoItem(it.uid, state | ENABLE_WIFI);

                //check WIFI-all button state
                if (queryIfAllAppsChecked(NetworkType.WIFI)) {
                    mCheckWIFIAll.setChecked(true);
                }
            } else {
                mCheckWIFIAll.setChecked(false);
                //disable WIFI
                setPackageBlockState(it.uid, 0, false);
                //item exists in DB, update it.
                updateAppInfoItem(it.uid, state & DISABLE_WIFI);
            }
        }
    }

    private void promptUserOperation() {
        String prompt = getResources().getString(R.string.operating_wait);
        Toast toast = Toast.makeText(this, prompt, Toast.LENGTH_SHORT);
        toast.show();
    }

    void insertAppInfoItem(AppItem item, int state) {
//        Log.e(TAG, "block sim---db---" + ((CheckBox) v).isChecked() + ":" + it.uid);
        ContentValues values = new ContentValues();
        values.put(AppInfoTable.COLUMN_UID, item.uid);
        values.put(AppInfoTable.COLUMN_PKG_NAME, item.label);
        values.put(AppInfoTable.COLUMN_BLOCK_STATE, state);
        getContentResolver().insert(BlockStateProvider.CONTENT_URI, values);
    }

    private int updateAppInfoItem(int uid, int state) {
        mAppInfoMap.put(uid, state);

        String[] selArgs = {""};
        selArgs[0] = Integer.toString(uid);
        ContentValues values = new ContentValues();
        values.put(AppInfoTable.COLUMN_BLOCK_STATE, state);
        Log.e(TAG, "up state=" + state);
        return getContentResolver().update(BlockStateProvider.CONTENT_URI, values, UID_SELECTION_CLAUSE, selArgs);
    }

    private void blockAllAppSim1Data() {
        if (mApps != null) {
            for (AppItem it : mApps) {
                int state = queryAppItemState(it.uid);
                try {
                    setPackageBlockState(it.uid, NetworkTypeSIM1, false);
                    Thread.sleep(50);
                } catch (Exception e) {

                }
                updateAppInfoItem(it.uid, state & DISABLE_SIM1);
            }
        }
    }

    private void blockAllAppSim2Data() {
        if (mApps != null) {
            for (AppItem it : mApps) {
                int state = queryAppItemState(it.uid);
                try {
                    setPackageBlockState(it.uid, NetworkTypeSIM2, false);
                    Thread.sleep(50);
                } catch (Exception e) {

                }
                updateAppInfoItem(it.uid, state & DISABLE_SIM2);
            }
        }
    }

    private void unBlockAllAppSim1Data() {
        if (mApps != null) {
            for (AppItem it : mApps) {
                int state = queryAppItemState(it.uid);
                updateAppInfoItem(it.uid, state | ENABLE_SIM1);
                try {
                    setPackageBlockState(it.uid, NetworkTypeSIM1, true); //enable sim1
                    Thread.sleep(50);
                } catch (Exception e) {

                }
            }
        }
    }

    private void unBlockAllAppSim2Data() {
        if (mApps != null) {
            for (AppItem it : mApps) {
                int state = queryAppItemState(it.uid);
                try {
                    setPackageBlockState(it.uid, NetworkTypeSIM2, true);
                    Thread.sleep(50);
                } catch (Exception e) {

                }
                updateAppInfoItem(it.uid, state | ENABLE_SIM2);
            }
        }
    }

    private synchronized void blockAllAppWIFI() {
        if (mApps != null) {
            for (AppItem it : mApps) {
                int state = queryAppItemState(it.uid);
                try {
                    setPackageBlockState(it.uid, NetworkTypeWIFI, false);
                    Thread.sleep(50);
                } catch (Exception e) {

                }

                updateAppInfoItem(it.uid, state & DISABLE_WIFI);
            }
        }
    }

    private synchronized void unBlockAllAppWIFI() {
        if (mApps != null) {
            for (AppItem it : mApps) {
                try {
                    setPackageBlockState(it.uid, NetworkTypeWIFI, true);
                    Thread.sleep(50);
                } catch (Exception e) {

                }
                int state = queryAppItemState(it.uid);
                updateAppInfoItem(it.uid, state | ENABLE_WIFI);
            }
        }
    }

    //TODO:
    //all button, not in list
    //cache appinfo, cache unchecked item.

    private int queryAppItemState(int uid) {
        String[] selArgs = {""};
        selArgs[0] = Integer.toString(uid);
        Cursor cursor = getContentResolver().query(BlockStateProvider.CONTENT_URI,
                QUERY_PRJECTION, "uid=?", selArgs, null);
        try {
            if (cursor == null || cursor.getCount() == 0) {
                return NO_ITEM_INFO;
            } else {
                cursor.moveToFirst();
                return cursor.getInt(INDEX_STATE);
            }
        } finally {
            cursor.close();
        }
    }

    private void queryAllAppState() {
        Cursor cursor = getContentResolver().query(BlockStateProvider.CONTENT_URI,
                QUERY_PRJECTION, null, null, null);
        try {
            if (cursor == null || cursor.getCount() == 0) {
                //DB init, No info in DB, so all apps are enabled
                Log.e(TAG, "no item found");
                //inset app info as enabled
                for (AppItem item : mApps) {
                    insertAppInfoItem(item, NO_BLOCK);
                    mAppInfoMap.put(item.uid, NO_BLOCK);
                }
                return;
            } else {
                while (cursor.moveToNext()) {
                    mAppInfoMap.put(cursor.getInt(INDEX_UID), cursor.getInt(INDEX_STATE));
                }
            }

//            Iterator it = mAppInfoMap.entrySet().iterator();
//            while(it.hasNext()) {
//                Map.Entry<Integer, Integer> entry = (Map.Entry<Integer, Integer>)it.next();
//                Log.e(TAG, "map got:" + entry.getKey() + ":" + entry.getValue());
//            }
        } finally {
            cursor.close();
        }
    }

    private boolean queryIfAllAppsChecked(NetworkType type) {
        Cursor cursor = null;
        cursor = getContentResolver().query(BlockStateProvider.CONTENT_URI,
                QUERY_PRJECTION, null, null, null);

        switch (type) {
            case SIM1:
                cursor = getContentResolver().query(BlockStateProvider.CONTENT_URI,
                        QUERY_PRJECTION, CLAUSE_SIM1_ENABLE_QUERY, null, null);
                break;
            case SIM2:
                cursor = getContentResolver().query(BlockStateProvider.CONTENT_URI,
                        QUERY_PRJECTION, CLAUSE_SIM2_ENABLE_QUERY, null, null);
                break;
            case WIFI:
                cursor = getContentResolver().query(BlockStateProvider.CONTENT_URI,
                        QUERY_PRJECTION, CLAUSE_WIFI_ENABLE_QUERY, null, null);
                break;
            default:
                break;
        }

        try {
            if (cursor != null) {
                Log.e(TAG, "cccount=" + cursor.getCount() + ":" + mApps.size());
                if (cursor.getCount() == mApps.size()) {
                    return true;
                }
            }
            return false;
        } finally {
            cursor.close();
        }
    }

    static private void deleteAppInfoItem(Context context, int uid) {
        String[] selArgs = {""};
        selArgs[0] = Integer.toString(uid);
        context.getContentResolver().delete(BlockStateProvider.CONTENT_URI,
                UID_SELECTION_CLAUSE, selArgs);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
    }

    /**
     * Query apps which have internet permissions.
     * New installed apps will be updated to DB.
     */
    class AppLoadTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            // mAppInfoMap.clear();
            mApps = getLauncherApp();//getAppIdsOfInternet();
            Log.e(TAG, "got size:" + mApps.size());
            // SPRD: Bug730374 closed all apps dataflow,open only dataflow for Browser,it can not download file
            updateAppsFlow(mApps, mApps_Flow);
            queryAllAppState();
            for (AppItem item : mApps) {
                //add new installed app into DB and map
                if (mAppInfoMap.get(item.uid) == null) {
                    Log.e(TAG, "no item:" + item.uid + ":" + item.label);
                    insertAppInfoItem(item, NO_BLOCK);
                    mAppInfoMap.put(item.uid, NO_BLOCK);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            appLoadFinished();
        }
    }

    /**
     * SPRD: Bug730374 closed all apps dataflow,open only dataflow for Browser,
     * it can not download file
     * SPRD: bug 758614 ConcurrentModificationException leading to crash
     * @{
     */
    public void updateAppsFlow(ArrayList<AppItem> mApps, List<UidDetail> mApps_Flow) {
        if (mApps != null && mApps_Flow != null) {
            for (AppItem a : mApps) {
                for (UidDetail u : mApps_Flow) {
                    if (a.label.equals(u.label)) {
                        a.usageSum = u.usageSum;
                        a.wifiusageSum = u.usageWifi;
                    }
                }
            }
        }
    }

    /**
     * @}
     */

    class AppAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public AppAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mApps.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        class ViewHolder {
            ImageView img;
            TextView tv;
            TextView data_flow_sum;
            TextView wifi_flow_sum;
            CheckBox checkBoxSim1;
            CheckBox checkBoxWifi;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = new ViewHolder();
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.app_item, null);
                holder.img = (ImageView) convertView.findViewById(R.id.icon);
                holder.tv = (TextView) convertView.findViewById(R.id.title);
                holder.checkBoxSim1 = (CheckBox) convertView.findViewById(R.id.sim1);
                holder.checkBoxWifi = (CheckBox) convertView.findViewById(R.id.wifi);
                holder.data_flow_sum = (TextView) convertView.findViewById(R.id.flow);
                holder.wifi_flow_sum = (TextView) convertView.findViewById(R.id.wifi_flow);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.img.setVisibility(View.VISIBLE);
            holder.img.setImageDrawable(mApps.get(position).icon);
            holder.tv.setText(mApps.get(position).label);
            holder.checkBoxSim1.setOnClickListener(DataFlowManagement.this);
            holder.checkBoxSim1.setTag(mApps.get(position));
            holder.checkBoxWifi.setOnClickListener(DataFlowManagement.this);
            holder.checkBoxWifi.setTag(mApps.get(position));
            holder.data_flow_sum.setText(String.format(getResources().getString(R.string.data_flow1),
                    Formatter.formatFileSize(DataFlowManagement.this, mApps.get(position).usageSum)));
            holder.wifi_flow_sum.setText(String.format(getResources().getString(R.string.wifi_flow1),
                    Formatter.formatFileSize(DataFlowManagement.this, mApps.get(position).wifiusageSum)));
            if (flow_rank_type == 0) {
                holder.data_flow_sum.setVisibility(View.VISIBLE);
                holder.wifi_flow_sum.setVisibility(View.GONE);
            } else {
                holder.data_flow_sum.setVisibility(View.GONE);
                holder.wifi_flow_sum.setVisibility(View.VISIBLE);
            }

            //set network checkbox state
            if (mAppInfoMap.get(mApps.get(position).uid) != null) {
                //checkbox all state
                if (mCheckWIFIAll.isChecked()) {
                    holder.checkBoxWifi.setChecked(true);
                } else {
                    if ((ENABLE_WIFI & mAppInfoMap.get(mApps.get(position).uid)) == ENABLE_WIFI) {
                        Log.e(TAG, "wifi check:" + mApps.get(position).label);
                        holder.checkBoxWifi.setChecked(true);
                    } else {
                        Log.e(TAG, "wifi uncheck:" + mApps.get(position).label);
                        holder.checkBoxWifi.setChecked(false);
                    }
                }
                // SPRD: Bug 704578 set the sim checkbox enabled and gray when there is no sim card
                if (mSimCount == 0) {
                    holder.checkBoxSim1.setEnabled(false);
                    holder.checkBoxSim1.setChecked(false);
                } else {
                    if (mCheckSim1All.isChecked()) {
                        //sim1 all checkbox checked, set checkbox directly.
                        Log.e(TAG, "mCheckSim1All checked");
                        holder.checkBoxSim1.setChecked(true);
                    } else {
                        if (mPrimaryCard == 0) {
                            if ((ENABLE_SIM1 & mAppInfoMap.get(mApps.get(position).uid)) == ENABLE_SIM1) {
                                Log.e(TAG, "sim1 check");
                                holder.checkBoxSim1.setChecked(true);
                            } else {
                                Log.e(TAG, "sim1 UNcheck");
                                holder.checkBoxSim1.setChecked(false);
                            }
                        } else {
                            if ((ENABLE_SIM2 & mAppInfoMap.get(mApps.get(position).uid)) == ENABLE_SIM2) {
                                Log.e(TAG, "sim2 check");
                                holder.checkBoxSim1.setChecked(true);
                            } else {
                                Log.e(TAG, "sim2 UNcheck");
                                holder.checkBoxSim1.setChecked(false);
                            }
                        }
                    }
                }
            } else {
                holder.checkBoxSim1.setChecked(false);
                holder.checkBoxWifi.setChecked(false);
            }

            Log.e(TAG, "uid=" + mApps.get(position).uid + ":" + mAppInfoMap.get(mApps.get(position).uid)
                    + ":" + (position) + ":" + mApps.get(position).label);

            return convertView;
        }
    }

    class AppItem implements Comparable<AppItem> {
        public String label;
        public int uid;
        public Drawable icon;
        private final Collator sCollator = Collator.getInstance();
        public long usageSum;
        public long wifiusageSum;

        @Override
        public int compareTo(AppItem another) {
            /**
             * SPRD: Bug750748 app list not sort as name
             * @{
             */
            int result;
            if (flow_rank_type == 0) {
                result = (int) (another.usageSum - usageSum);
            } else {
                result = (int) (another.wifiusageSum - wifiusageSum);
            }
            if (result == 0) {
                result = Collator.getInstance().compare(label, another.label);
            }
            return result;
            /**
             * @}
             */
        }
    }

    private ArrayList<AppItem> getLauncherApp() {
        PackageManager pm = getPackageManager();
        Intent it = new Intent(Intent.ACTION_MAIN, null);
        it.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = pm.queryIntentActivities(it, 0);
        Log.e(TAG, "size============" + infos.size());
        ApplicationInfo applicationInfo;
        ArrayList<AppItem> appsGot = new ArrayList<>();
        HashSet<String> appList = new HashSet<>();

        String title;
        int uid;
        for (ResolveInfo info : infos) {
            PackageInfo pkgInfo = null;
            try {
                pkgInfo = pm.getPackageInfo(info.activityInfo.packageName, PackageManager.GET_PERMISSIONS);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            String[] requestedPermissions = pkgInfo.requestedPermissions;
            if (requestedPermissions == null) {
                continue;
            }

            //Filter out apps requested INTERNET permission.
            for (String per : requestedPermissions) {
                if (TextUtils.equals(per, android.Manifest.permission.INTERNET)) {
                    try {
                        appList.add(info.activityInfo.packageName);
                        break;
                    } catch (Exception e) {
                        Log.e(TAG, "error:" + e);
                    }
                }
            }
        }

        //contrsuct result list
        for (String pkgName : appList) {
            Log.e(TAG, "------------------------||" + pkgName);
            AppItem item = new AppItem();
            try {
                applicationInfo = pm.getApplicationInfo(pkgName, 0);
                item.label = (String) ((applicationInfo != null) ? applicationInfo.loadLabel(pm) : "???");
                item.uid = pm.getPackageUid(pkgName, UserHandle.myUserId());
                item.icon = applicationInfo.loadIcon(pm);
                appsGot.add(item);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        return appsGot;
    }

    public static class AppPackageListener extends BroadcastReceiver {
        public AppPackageListener() {
            super();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "onRec:" + intent);
            if (intent == null || intent.getAction() == null) {
                return;
            }
            debugIntent(context, intent);
            if (intent.getAction().equalsIgnoreCase(Intent.ACTION_PACKAGE_REMOVED)) {
                deleteAppInfoItem(context, (int) intent.getExtras().get(Intent.EXTRA_UID));
                deleteBlockPackage((int) intent.getExtras().get(Intent.EXTRA_UID));
            } else if (intent.getAction().equalsIgnoreCase(Intent.ACTION_PACKAGE_ADDED)) {
                // do nothing
            }
        }

        private void debugIntent(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                for (String key : extras.keySet()) {
                    Log.e(TAG, "key[" + key + "]:" + extras.get(key));
                }
            } else {
                Log.e(TAG, "no extras");
            }
        }
    }

    private void enableTestMode() {
        mSimCount = 2;
    }

    class UidDetail implements Comparable<UidDetail> {
        public CharSequence label;
        public CharSequence contentDescription;
        public CharSequence[] detailLabels;
        public CharSequence[] detailContentDescriptions;
        public Drawable icon;
        public long usageWifi;
        public long usageSim1;
        public long usageSim2;
        public long usageSum;

        @Override
        public int compareTo(UidDetail another) {
            return (int) (another.usageSum - usageSum);
        }
    }

    private int collopseUid(int uid) {
        if (!UserHandle.isApp(uid) && (uid != TrafficStats.UID_REMOVED)
                && (uid != TrafficStats.UID_TETHERING)) {
            uid = android.os.Process.SYSTEM_UID;
        }
        return uid;
    }

    private UidDetail buildUidDetail(int uid) {
        final Resources res = getResources();
        final PackageManager pm = getPackageManager();

        final UidDetail detail = new UidDetail();
        detail.label = pm.getNameForUid(uid);
        detail.icon = pm.getDefaultActivityIcon();

        // handle special case labels
        switch (uid) {
            case android.os.Process.SYSTEM_UID:
                detail.label = getResources().getString(R.string.android_os);
                detail.icon = pm.getDefaultActivityIcon();

                return detail;
            case TrafficStats.UID_REMOVED:
                detail.label = getResources().getString(R.string.app_removed);
                detail.icon = pm.getDefaultActivityIcon();

                return detail;
            case TrafficStats.UID_TETHERING:
                detail.label = getResources().getString(R.string.tether_settings_title_all);
                detail.icon = pm.getDefaultActivityIcon();

                return detail;
            default:
                break;
        }

        final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);

        // otherwise fall back to using packagemanager labels
        final String[] packageNames = pm.getPackagesForUid(uid);
        final int length = packageNames != null ? packageNames.length : 0;
        try {
            final int userId = UserHandle.getUserId(uid);
            UserHandle userHandle = new UserHandle(userId);
            if (userHandle.isApp(uid) == false) {
//                return null;
            }
            IPackageManager ipm = AppGlobals.getPackageManager();

            if (length == 1) {
                final ApplicationInfo info = ipm.getApplicationInfo(packageNames[0],
                        0 /* no flags */, userId);
                if (info != null) {
                    detail.label = info.loadLabel(pm).toString();
                    detail.icon = um.getBadgedIconForUser(info.loadIcon(pm),
                            new UserHandle(userId));
                }
            } else if (length > 1) {
                detail.detailLabels = new CharSequence[length];
                detail.detailContentDescriptions = new CharSequence[length];
                for (int i = 0; i < length; i++) {
                    final String packageName = packageNames[i];
                    final PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
                    final ApplicationInfo appInfo = ipm.getApplicationInfo(packageName, 0 /* no flags */, userId);

                    if (appInfo != null) {
                        detail.detailLabels[i] = appInfo.loadLabel(pm).toString();
                        detail.detailContentDescriptions[i] = um.getBadgedLabelForUser(
                                detail.detailLabels[i], userHandle);
                        if (packageInfo.sharedUserLabel != 0) {
                            detail.label = pm.getText(packageName, packageInfo.sharedUserLabel,
                                    packageInfo.applicationInfo).toString();
                            detail.icon = um.getBadgedIconForUser(appInfo.loadIcon(pm), userHandle);
                        }
                    }
                }
            }
            detail.contentDescription = um.getBadgedLabelForUser(detail.label, userHandle);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Error while building UI detail for uid " + uid, e);
        } catch (RemoteException e) {
            Log.w(TAG, "Error while building UI detail for uid " + uid, e);
        }

        if (TextUtils.isEmpty(detail.label)) {
            detail.label = Integer.toString(uid);
        }

        return detail;
    }

    private final LoaderCallbacks<NetworkStats> mSummaryCallbacks = new LoaderCallbacks<NetworkStats>() {

        @Override
        public Loader<NetworkStats> onCreateLoader(int id, Bundle args) {
            return new SummaryForAllUidLoader(DataFlowManagement.this, mStatsSession, args);
        }

        @Override
        public void onLoaderReset(Loader<NetworkStats> loader) {

        }

        @Override
        public void onLoadFinished(Loader<NetworkStats> loader, NetworkStats data) {
            NetworkStats.Entry entry = null;

            Log.e(TAG, "onLoadFinished : begin data handling==::::::::::");
            if (loader.getId() == NetworkTypeWIFI) {
                mWifiUsageTotal = 0;

                for (UidDetail app : mApps_Flow) {
                    app.usageWifi = 0;
                }

                final int size = data != null ? data.size() : 0;
                for (int i = 0; i < size; i++) {
                    entry = data.getValues(i, entry);

                    // Decide how to collapse items together
                    final int uid = collopseUid(entry.uid);
                    UidDetail dt = mKnowApps.get(uid);

                    if (dt == null) {

                        dt = buildUidDetail(uid);
                        if (dt != null) {
                            mKnowApps.put(uid, dt);
                            mApps_Flow.add(dt);
                        }
                    }
                    if (dt == null)
                        continue;
                    dt.usageWifi += (entry.rxBytes + entry.txBytes);
                    dt.usageSum = dt.usageSim1 + dt.usageSim2;
                    mWifiUsageTotal += (entry.rxBytes + entry.txBytes);
                }

                mHandler.sendEmptyMessage(MSG_WIFI_STATS_LOAD_FINISHED);
            } else if (loader.getId() == NetworkTypeSIM1) {
                ArrayList<Integer> uidList = new ArrayList<Integer>();
                mSim1UsageTotal = 0;
                for (UidDetail app : mApps_Flow) {
                    app.usageSim1 = 0;
                }

                final int size = data != null ? data.size() : 0;
                Log.e(TAG, "callback sim1:" + size);
                for (int i = 0; i < size; i++) {
                    entry = data.getValues(i, entry);

                    // Decide how to collapse items together
                    final int uid = collopseUid(entry.uid);
                    uidList.add(uid);
                    UidDetail dt = mKnowApps.get(uid);
                    if (dt == null) {
                        dt = buildUidDetail(uid);
                        if (dt != null) {
                            mKnowApps.put(uid, dt);
                            mApps_Flow.add(dt);
                        }
                    }
                    if (dt == null)
                        continue;
                    dt.usageSim1 += (entry.rxBytes + entry.txBytes);
                    mSim1UsageTotal += (entry.rxBytes + entry.txBytes);
                    dt.usageSum = dt.usageSim1 + dt.usageSim2;
                }
                mHandler.sendEmptyMessage(MSG_SIM1_STATS_LOAD_FINISHED);
            } else {
                mSim2UsageTotal = 0;
                for (UidDetail app : mApps_Flow) {
                    app.usageSim2 = 0;
                }

                final int size = data != null ? data.size() : 0;
                for (int i = 0; i < size; i++) {
                    entry = data.getValues(i, entry);
                    // Decide how to collapse items together
                    final int uid = collopseUid(entry.uid);
                    UidDetail dt = mKnowApps.get(uid);
                    if (dt == null) {
                        dt = buildUidDetail(uid);
                        if (dt != null) {
                            mKnowApps.put(uid, dt);
                            mApps_Flow.add(dt);
                        }
                    }
                    if (dt == null)
                        continue;
                    dt.usageSim2 += (entry.rxBytes + entry.txBytes);
                    mSim2UsageTotal += (entry.rxBytes + entry.txBytes);
                    dt.usageSum = dt.usageSim1 + dt.usageSim2;
                }
                mHandler.sendEmptyMessage(MSG_SIM2_STATS_LOAD_FINISHED);
            }
            Log.d(TAG, "onLoadFinished : end data handling==================");
        }
    };
}