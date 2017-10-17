package com.sprd.generalsecurity.optimize;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settingslib.BatteryInfo;
import com.sprd.generalsecurity.R;
import com.sprd.generalsecurity.network.SummaryForAllUidLoader;
import com.sprd.generalsecurity.utils.Contract;
import com.sprd.generalsecurity.utils.DateCycleUtils;
import com.sprd.generalsecurity.utils.TeleUtils;

import java.util.ArrayList;
import java.util.List;

import static android.net.NetworkTemplate.buildTemplateMobileAll;

/**
 * Created by SPREADTRUM\chris.wang on 17-5-10.
 */

public class DefaultFragment extends Fragment {

    private static final String ACTION_NETWORK_MANAGEMENT = "com.sprd.generalsecurity.network.dataflowmainentry";
    private static final String ACTION_MEMORY_MANAGEMENT = "com.sprd.generalsecurity.memory.MemoryManagement";
    private static final String ACTION_BATTERY_MANAGEMENT = "android.intent.action.POWER_USAGE_SUMMARY";
    //private static final String ACTION_BATTERY_MANAGEMENT = "com.sprd.generalsecurity.battery.BatteryManagement";
    private static final String ACTION_STORAGE_MANAGEMENT = "com.sprd.generalsecurity.storage.StorageManagement";
    private final int mNameIds[] = {R.string.storage, R.string.flow, R.string.battery, R.string.memory};
    private final int mIconIds[] = {R.drawable.clean_garbage, R.drawable.flow_management_new, R.drawable.battery_new, R.drawable.memory_new};
    private static final String TAG = "GeneralSecurity DefaultFragment";

    private static final String STATE_SIM_USED = "sim_used";
    private static final String STATE_SIM_USED_SET = "sim_used_set";
    private static final String STATE_SIM_REMAINED = "sim_remained";
    private static final String STATE_SIM_TOTAL = "sim_total";
    /**
     * SPRD: Bug705090 double sim cards, the remain data size compute error
     * @{
     */
    private static final int SIM1_INDEX = 0;
    private static final int SIM2_INDEX = 1;
    private static final int COUNT_SINGLE = 1;
    private static final int COUNT_DUAL = 2;
    private static final String SIM1 = "sim1";
    private static final String SIM2 = "sim2";
    /**
     * @}
     */
    private static final int M2BITS = 1024 * 1024;
    private final int STORAGE_ITEM = 0;
    private final int NETWORK_ITEM = 1;
    private final int BATTERY_ITEM = 2;
    private final int MEMORY_ITEM = 3;
    private List<EntryItem> mEntryList = new ArrayList<EntryItem>();
    private Context mContext;
    private View mRootView;
    private ListView mListView;
    private DefaultAdapter mAdapter;
    private int mPrimaryCard = 0;
    private static int mSimCount;
    private float mSimDataTotal;
    private float mSimDataUsed;

    private long mSimBitsUsed;
    private long mSimBitsRemained = -1;
    private INetworkStatsSession mStatsSession;
    private INetworkStatsService mStatsService;
    private NetworkTemplate mTemplate;
    private BatteryStatsHelper mStatsHelper;
    private String mBatterySummery;
    /**
     * SPRD: Bug705090 double sim cards, the remain data size compute error
     * @{
     */
    private float mSim1DataTotal;
    private float mSim1DataUsed;
    private float mSim2DataTotal;
    private float mSim2DataUsed;
    private long mSim1BitsUsed;
    private long mSim1BitsRemained;
    private long mSim2BitsUsed;
    private long mSim2BitsRemained;
    /**
     * @}
     */

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.default_list, null);
        mListView = (ListView) mRootView.findViewById(R.id.list);
        mEntryList = getData(mNameIds, mIconIds);
        mAdapter = new DefaultAdapter(mContext);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                switch (position) {
                    case STORAGE_ITEM:
                        Intent storageIntent = new Intent(ACTION_STORAGE_MANAGEMENT, null);
                        mContext.startActivity(storageIntent);
                        break;
                    case NETWORK_ITEM:
                        Intent networkIntent = new Intent(ACTION_NETWORK_MANAGEMENT, null);
                        mContext.startActivity(networkIntent);
                        break;
                    case BATTERY_ITEM:
                        Intent batteryIntent = new Intent(ACTION_BATTERY_MANAGEMENT, null);
                        mContext.startActivity(batteryIntent);
                        break;
                    case MEMORY_ITEM:
                        Intent memoryIntent = new Intent(ACTION_MEMORY_MANAGEMENT, null);
                        mContext.startActivity(memoryIntent);
                        break;
                }

            }
        });
        mStatsService = INetworkStatsService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        try {
            mStatsSession = mStatsService.openSession();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        mStatsHelper = new BatteryStatsHelper(mContext);
        mStatsHelper.create(savedInstanceState);
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mContext.registerReceiver(mBatteryInfoReceiver, new IntentFilter(
                Intent.ACTION_BATTERY_CHANGED));
        mSimCount = TeleUtils.getSimCount(mContext);
        mPrimaryCard = TeleUtils.getPrimarySlot(mContext) - 1;
        if (mSimCount >= 1) {
            startNetworkStatsLoader();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        mContext.unregisterReceiver(mBatteryInfoReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SIM_USED, mSimBitsUsed);
        outState.putFloat(STATE_SIM_USED_SET, mSimDataUsed);
        outState.putLong(STATE_SIM_REMAINED, mSimBitsRemained);
        outState.putFloat(STATE_SIM_TOTAL, mSimDataTotal);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    class ViewHolder {
        public ImageView icon;
        public TextView name;
        public TextView description;
        public ImageView arrow;
    }

    private class DefaultAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public DefaultAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mEntryList.size();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.entry_item, null);
                holder = new ViewHolder();
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                holder.name = (TextView) convertView.findViewById(R.id.name);
                holder.description = (TextView) convertView.findViewById(R.id.description);
                holder.arrow = (ImageView) convertView.findViewById(R.id.arrow);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.icon.setImageDrawable(getResources().getDrawable(mEntryList.get(position).icon));
            holder.name.setText(mEntryList.get(position).name);
            holder.arrow.setImageDrawable(getResources().getDrawable(R.drawable.arrow));
            /**
             * SPRD: Bug705090 double sim cards, the remain data size compute error
             * @{
             */
            if (position == NETWORK_ITEM) {
                String strRemained = getResources().getString(R.string.month_data_remained);
                if (mSimCount == COUNT_SINGLE) {
                    holder.description.setText(String.format(strRemained,
                            Formatter.formatFileSize(mContext, mSim1BitsRemained)));
                } else if (mSimCount == COUNT_DUAL) {
                    long value = mPrimaryCard == SIM1_INDEX ? mSim1BitsRemained : mSim2BitsRemained;
                    holder.description.setText(String.format(strRemained,
                            Formatter.formatFileSize(mContext, value)));
                } else {
                    holder.description.setText(null);
                }
            }
            /**
             * @}
             */

            if (position == BATTERY_ITEM) {
                holder.description.setText(mBatterySummery);
            }

            if (TextUtils.isEmpty(holder.description.getText())) {
                holder.description.setVisibility(View.GONE);
            } else {
                holder.description.setVisibility(View.VISIBLE);
            }
            return convertView;
        }

        @Override
        public Object getItem(int position) {
            return mEntryList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }

    private class EntryItem {
        public int icon;
        public int name;
    }

    private List<EntryItem> getData(int[] nameIds, int[] iconIds) {
        List<EntryItem> listItem = new ArrayList<EntryItem>();
        for (int i = 0; i < nameIds.length; i++) {
            EntryItem item = new EntryItem();
            item.icon = iconIds[i];
            item.name = nameIds[i];
            listItem.add(item);
        }

        return listItem;
    }

    void startNetworkStatsLoader() {
        long start = DateCycleUtils.getMonthCycleStart();
        try {
            mStatsService.forceUpdate();
        } catch (RemoteException e) {
            Log.e(TAG, "exception caught:" + e);
        }
        /**
         * SPRD: Bug705090 double sim cards, the remain data size compute error
         * @{
         */
        if (mSimCount == COUNT_SINGLE) {
            //single card inserted, need to check the primary card num
            mTemplate = buildTemplateMobileAll(TeleUtils.getActiveSubscriberId(mContext));

            // kick off loader for sim1 detailed stats
            getLoaderManager().restartLoader(0,
                    SummaryForAllUidLoader.buildArgs(mTemplate, start,
                            System.currentTimeMillis()), mSummaryCallbacks);
            return;
        }

        //dual card case
        if (mSimCount == COUNT_DUAL) {
            for (int i = 1; i <= COUNT_DUAL; i++) {
                mTemplate = buildTemplateMobileAll(
                        TeleUtils.getActiveSubscriberId(mContext, i));
                getLoaderManager().restartLoader(i - 1,
                        SummaryForAllUidLoader.buildArgs(mTemplate, start,
                                System.currentTimeMillis()), mSummaryCallbacks);
            }
        }
        /**
         * @}
         */
    }

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                BatteryInfo.getBatteryInfo(mContext, new BatteryInfo.Callback() {
                    @Override
                    public void onBatteryInfoLoaded(BatteryInfo info) {
                        mBatterySummery = info.mChargeLabelString;
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    };

    private final LoaderManager.LoaderCallbacks<NetworkStats> mSummaryCallbacks = new LoaderManager.LoaderCallbacks<
            NetworkStats>() {
        @Override
        public Loader<NetworkStats> onCreateLoader(int id, Bundle args) {
            return new SummaryForAllUidLoader(mContext, mStatsSession, args);
        }

        @Override
        public void onLoadFinished(Loader<NetworkStats> loader, NetworkStats data) {
            updateDataUsed(loader.getId(), data);
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onLoaderReset(Loader<NetworkStats> loader) {

        }
    };

    /**
     * SPRD: Bug705090 double sim cards, the remain data size compute error
     * @{
     */
    private void updateDataUsed(int id, NetworkStats data) {
        if (data == null) {
            return;
        }
        if (mPrimaryCard < 0) {
            return;
        }
        long totalBytes = 0;
        NetworkStats.Entry entry = null;
        for (int i = 0; i < data.size(); i++) {
            entry = data.getValues(i, entry);
            totalBytes += (entry.rxBytes + entry.txBytes);
            Log.d(TAG, "--Got +" + entry.uid + ":" + (entry.rxBytes + entry.txBytes));
        }
        if (id == SIM1_INDEX) { //SIM1
            SharedPreferences pref;
            if (mSimCount == COUNT_SINGLE) {
                if (mPrimaryCard == SIM1_INDEX) {
                    pref = mContext.getSharedPreferences(SIM1, Context.MODE_PRIVATE);
                } else {
                    pref = mContext.getSharedPreferences(SIM2, Context.MODE_PRIVATE);
                }
            } else { // dual sim release
                pref = mContext.getSharedPreferences(SIM1, Context.MODE_PRIVATE);
            }

            mSim1DataTotal = Float.parseFloat(pref.getString(Contract.KEY_MONTH_TOTAL, "0"));
            mSim1DataUsed = Float.parseFloat(pref.getString(Contract.KEY_DATA_USED_ORIGINAL, "0"));
            if (mSim1DataTotal == 0) {
                return;
            }

            long delta = pref.getLong(Contract.KEY_DATA_USED_QUERY_DELTA, 0);
            Log.d(TAG, "mSim1DataTotal:" + mSim1DataTotal + ":" + delta + ":" + mPrimaryCard);

            mSim1BitsUsed = (long) (totalBytes - delta);
            if (mSim1BitsUsed > (long) (mSim1DataUsed * M2BITS)) {
                mSim1DataUsed = (float) (mSim1BitsUsed / M2BITS);
            }

            mSim1BitsRemained = (long) (mSim1DataTotal * M2BITS) - mSim1BitsUsed;
            if (mSim1BitsRemained < 0) {
                mSim1BitsRemained = 0;
            }
        } else if (id == SIM2_INDEX) {
            SharedPreferences pref = mContext.getSharedPreferences(SIM2, Context.MODE_PRIVATE);
            mSim2DataTotal = Float.parseFloat(pref.getString(Contract.KEY_MONTH_TOTAL, "0"));
            mSim2DataUsed = Float.parseFloat(pref.getString(Contract.KEY_DATA_USED_ORIGINAL, "0"));
            if (mSim2DataTotal == 0) {
                return;
            }
            long delta = pref.getLong(Contract.KEY_DATA_USED_QUERY_DELTA, 0);
            mSim2BitsUsed = (long) (totalBytes - delta);
            if (mSim2BitsUsed > (long) (mSim2DataUsed * M2BITS)) {
                mSim2DataUsed = (float) (mSim2BitsUsed / M2BITS);
            }
            mSim2BitsRemained = (long) (mSim2DataTotal * M2BITS) - mSim2BitsUsed;
            if (mSim2BitsRemained < 0) {
                mSim2BitsRemained = 0;
            }
        }
    }
    /**
     * @}
     */
}
