package com.sprd.generalsecurity.network;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.KeyguardManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.sprd.generalsecurity.R;
import com.sprd.generalsecurity.utils.Contract;
import com.sprd.generalsecurity.utils.DateCycleUtils;
import android.text.format.Formatter;
import com.sprd.generalsecurity.utils.TeleUtils;
import com.sprd.generalsecurity.utils.TeleUtils.NetworkType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.System;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static android.net.NetworkTemplate.buildTemplateMobileAll;
import static android.net.NetworkTemplate.buildTemplateWifiWildcard;

public class ScreenStateReceiver extends BroadcastReceiver {
    private static String TAG = "ScreenStateReceiver";
    private Context mContext;

    private static SCREEN_STATE lastMode = SCREEN_STATE.NONE;

    private enum SCREEN_STATE {
        NONE, SCREEN_OFF, SCREEN_ON, USER_PRESENT
    }

    private SharedPreferences mSharedPref;

    private long mBytesUsedInKeyguard;

    private NetworkStats mStatsPrevious;
    private NetworkStats mStatsCurrent;
    private long mStartTime = -1;
    private long mEndTime;

    private static final String NETWORK_STAT_PATH = "/proc/uid_stat/";

    private DataUsageQuery mQuery;

    private void getDataUsageBeforeLock() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                mQuery.getDataUsage(true);
            }
        });

        t.start();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TeleUtils.getCurrentNetworkType(context) == NetworkType.DISCONNECTED) {
            return;
        }

        String action = intent.getAction();

        if (mQuery == null) {
            File f = new File(NETWORK_STAT_PATH);
            if (!f.exists()) {
                mQuery = new DataUsageQueryNetwork();
                Log.d(TAG, "DataUsageQueryNetwork");
            } else {
                mQuery = new DataUsageQueryStatsFile();
                Log.d(TAG, "DataUsageQueryStatsFile");
            }

            //for phone boot time, lastMode maybe NONE.
            if (lastMode == SCREEN_STATE.NONE) {
                //get the data usage before lock screen
                getDataUsageBeforeLock();
            }
        }

        if (mSharedPref == null) {
            mSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        }
        KeyguardManager keyManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);

        if (Intent.ACTION_SCREEN_ON.equals(action)) {
            KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager.isKeyguardLocked()) {
                FloatKeyView.getInstance(context).hide();
            } else {
                //keyguard on, should show speed if user set
                if (getSpeedSetting(context)) {
                    FloatKeyView.getInstance(context).startRealSpeed();
                    FloatKeyView.getInstance(context).show();
                }
            }
            if (lastMode == SCREEN_STATE.NONE) {
                mStartTime = System.currentTimeMillis();
            }
            lastMode = SCREEN_STATE.SCREEN_ON;
        } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            if (lastMode == SCREEN_STATE.USER_PRESENT || lastMode == SCREEN_STATE.NONE) {
                lastMode = SCREEN_STATE.SCREEN_OFF;
                mStartTime = System.currentTimeMillis();
                getDataUsageBeforeLock();
            }

            //stop real speed view when screen off.
            FloatKeyView.getInstance(context).hide();
            FloatKeyView.getInstance(context).stopRealSpeed();
        } else if (Intent.ACTION_USER_PRESENT.equals(action) && !keyManager.isKeyguardLocked()) {
            if (lastMode == SCREEN_STATE.NONE) {
                mStartTime = System.currentTimeMillis();
            }
            lastMode = SCREEN_STATE.USER_PRESENT;
            mEndTime = System.currentTimeMillis();
            if (getSpeedSetting(context)) {
                FloatKeyView.getInstance(context).startRealSpeed();
                FloatKeyView.getInstance(context).show();
            }

            if (mSharedPref.getBoolean(Contract.KEY_LOCK_DATA_SWITCH, false)) {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mQuery.getDataUsage(false);
                        mBytesUsedInKeyguard = mQuery.generateUsageReport();
                        notifyLockScreenDataUsed();
                    }
                });

                t.start();
            }
        }
    }

    boolean getSpeedSetting(Context context) {
        if (mSharedPref == null) {
            mSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        }
        if (mSharedPref.getBoolean(Contract.KEY_REAL_SPEED_SWITCH, false)) {
            return true;
        } else {
            return false;
        }
    }

    public void registerScreenReceiver(Context context) {
        mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        context.registerReceiver(this, filter);
    }

    public void unRegisterScreenReceiver(Context context) {
        context.unregisterReceiver(this);
    }

    abstract class DataUsageQuery {
        abstract void getDataUsage(boolean beforeLock);
        abstract long generateUsageReport();
    }

    class QueryDataResult {
        long dataUsed;
        SparseArray<Long> entries;

        QueryDataResult(long usedData, SparseArray<Long> entriesArray) {
            dataUsed = usedData;
            entries = entriesArray;
        }
    }

    class DataUsageQueryNetwork extends DataUsageQuery {
        private static final int TIME_DELTA = 1000 * 3600 * 2;

        void getDataUsage(boolean beforeLock){
            mStartTime = (mStartTime != -1) ? mStartTime :
                        (System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime());
            if (beforeLock) {
                mStatsPrevious = startNetworkStatsLoader(mStartTime - TIME_DELTA, mStartTime);
            } else {
                mStatsCurrent = startNetworkStatsLoader(mStartTime - TIME_DELTA, mEndTime);
            }
        }

        private QueryDataResult collopseData(NetworkStats stats) {
            if (stats == null) {
                return null;
            }
            NetworkStats.Entry entry = null;
            long dataUsed = 0;
            SparseArray<Long> sparse = new SparseArray<Long>();
            for (int i = 0; i < stats.size(); i++) {
                entry = stats.getValues(i, entry);
                if (entry.uid == android.os.Process.ROOT_UID) {
                    //convert uid 0 to SYSTEM_UID, so the data usage
                    //are counted as SYSTEM_UID
                    entry.uid = android.os.Process.SYSTEM_UID;
                }
                dataUsed += (entry.rxBytes + entry.txBytes);
                if (sparse.get(entry.uid, -1L) != -1L) {
                    sparse.put(entry.uid, sparse.get(entry.uid, -1L) + (entry.rxBytes + entry.txBytes));
                } else {
                    sparse.put(entry.uid, (entry.rxBytes + entry.txBytes));
                }
            }

            return new QueryDataResult(dataUsed, sparse);
        }

        private void addDataEntryToReport(int uid, long used) {
            AppUsageInfo reportInfoItem = new AppUsageInfo();
            reportInfoItem.uid = uid;
            reportInfoItem.bytesUsed = used;
            mReportList.add(reportInfoItem);
        }

        long generateUsageReport() {
            mReportList.clear();
            QueryDataResult resultPrevious = collopseData(mStatsPrevious);
            QueryDataResult resultCurrent = collopseData(mStatsCurrent);
            if (resultCurrent == null) {
                return 0;
            }

            long dataUsedFinal = 0;

            if (resultCurrent != null && resultPrevious == null) {
                SparseArray<Long> entryArray = resultCurrent.entries;
                if (entryArray != null) {
                    for (int i = 0; i < entryArray.size(); i++) {
                        int uid = entryArray.keyAt(i);
                        addDataEntryToReport(uid, entryArray.valueAt(i));
                        dataUsedFinal +=  entryArray.valueAt(i);
                    }
                }
                return dataUsedFinal;
            }

            if (resultCurrent != null && resultPrevious != null &&
                resultCurrent.dataUsed != resultPrevious.dataUsed) {
                //generate report data
                SparseArray<Long> entryArray = resultCurrent.entries;
                SparseArray<Long> entryArrayPrevious = resultPrevious.entries;
                if (entryArray != null) {
                    for (int i = 0; i < entryArray.size(); i++) {
                        int uid = entryArray.keyAt(i);
                        if (entryArrayPrevious.get(uid, -1L) == -1L) {
                            addDataEntryToReport(uid, entryArray.valueAt(i));
                            dataUsedFinal +=  entryArray.valueAt(i);
                        } else {
                            if (entryArrayPrevious != null && entryArrayPrevious.size() > i) {
                                long dataDiff = entryArray.valueAt(i) - entryArrayPrevious.get(uid, 0L);
                                if (dataDiff > 0) {
                                    addDataEntryToReport(uid, dataDiff);
                                    dataUsedFinal +=  dataDiff;
                                }
                            }
                        }
                    }
                }
            }

            return dataUsedFinal;
        }

        private NetworkStats startNetworkStatsLoader(long start, long end) {
            NetworkTemplate template = null;

            NetworkType type = TeleUtils.getCurrentNetworkType(mContext);
            final int SINGLE_SIM_COUNT = 1;
            switch (type) {
                case SIM1:
                case SIM2:
                    if (TeleUtils.getSimCount(mContext) == SINGLE_SIM_COUNT) {
                        template = buildTemplateMobileAll(TeleUtils.getActiveSubscriberId(mContext));
                    } else {
                        template = buildTemplateMobileAll(TeleUtils.getActiveSubscriberId(mContext,
                                                              TeleUtils.getPrimarySlot(mContext)));
                    }
                    break;
                case WIFI:
                    template = buildTemplateWifiWildcard();
                    break;
                case DISCONNECTED:
                    Log.e(TAG, "Disconnected");
                    return null;
                default:
                    return null;
            }

            INetworkStatsSession statsSession;
            INetworkStatsService statsService;
            statsService = INetworkStatsService.Stub.asInterface(
                    ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
            try {
                statsSession = statsService.openSession();
                statsService.forceUpdate();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }

            try {
                NetworkStats stats = statsSession.getSummaryForAllUid(template, start, end, false);
                return stats;
            } catch (RemoteException e) {
                Log.e(TAG, "exception:" + e.toString());
                return null;
            }
        }
    }

    class DataUsageQueryStatsFile extends DataUsageQuery {
        void getDataUsage(boolean beforeLock){
            if (beforeLock) {
                getLockScreenDataUsage(mUsageList);
            } else {
                getLockScreenDataUsage(mLatestUsageList);
            }
        }

        private Long getTotalBytesManual(String localUid) throws IOException{
            File uidFileDir = new File("/proc/uid_stat/"+ localUid);
            File uidActualFileReceived = new File(uidFileDir,"tcp_rcv");
            File uidActualFileSent = new File(uidFileDir,"tcp_snd");

            String textReceived = "0";
            String textSent = "0";
            BufferedReader brReceived = null, brSent = null;

            try {
                brReceived = new BufferedReader(new FileReader(uidActualFileReceived));
                brSent = new BufferedReader(new FileReader(uidActualFileSent));
                String receivedLine;
                String sentLine;

                if ((receivedLine = brReceived.readLine()) != null) {
                    textReceived = receivedLine;
                }
                if ((sentLine = brSent.readLine()) != null) {
                    textSent = sentLine;
                }
            } finally {
                brReceived.close();
                brSent.close();
            }

            return Long.valueOf(textReceived).longValue() + Long.valueOf(textSent).longValue();
        }

        private void getLockScreenDataUsage(ArrayList<AppUsageInfo> list) {
            File dir = new File(NETWORK_STAT_PATH);
            String[] children = dir.list();
            list.clear();

            try {
                 for (String path: children) {
                    long bytes = getTotalBytesManual(path);
                    AppUsageInfo info = new AppUsageInfo();
                    info.uid = Integer.valueOf(path);
                    info.bytesUsed = bytes;
                    list.add(info);
                }
            } catch(IOException e) {
                Log.e(TAG, "exception happened:" + e);
            }
        }

        long generateUsageReport() {
            long dataUsed = 0;
            mReportList.clear();
            for (AppUsageInfo info : mLatestUsageList) {
                for (AppUsageInfo infoPrevious: mUsageList) {
                    if (infoPrevious.uid == info.uid) {
                        //uid matched, compute the usage used in lock period
                        long used = info.bytesUsed - infoPrevious.bytesUsed;
                        if (used > 0) {
                            AppUsageInfo reportInfoItem = new AppUsageInfo();
                            reportInfoItem.uid = info.uid;
                            reportInfoItem.bytesUsed = used;
                            mReportList.add(reportInfoItem);
                            dataUsed += used;
                        }
                        break;
                    }
                }
            }
            Log.d(TAG, "final report used:" + dataUsed);

            return dataUsed;
        }
    }

    void notifyLockScreenDataUsed() {
        NetworkType type = TeleUtils.getCurrentNetworkType(mContext);
        //intent to start DataFlowMainEntry when notification clicked.
        Intent resultIntent = new Intent(mContext, LockPeriodFlowActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        resultIntent.putExtra(LockPeriodFlowActivity.LOCKSCREEN_NETWORK_TYPE, type.ordinal());
        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext, 0,
                                             resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        String msg = String.format(mContext.getResources().getString(
                                R.string.data_used_lockscreen_message),
                                Formatter.formatFileSize(mContext, mBytesUsedInKeyguard));
        Notification notification = new Notification.Builder(mContext)
                .setContentTitle(mContext.getResources().getString(
                                                       R.string.data_used_lockscreen_title))
                .setAutoCancel(true)
                .setContentText(msg)
                .setContentIntent(resultPendingIntent)
                .setSmallIcon(R.drawable.lock_flow)
                .setStyle(new Notification.BigTextStyle().bigText(msg))
                .build();

        NotificationManager nm = (NotificationManager) mContext.getSystemService(
                                                                 Context.NOTIFICATION_SERVICE);
        nm.notify(1, notification);
    }

    private static ArrayList<AppUsageInfo> mUsageList = new ArrayList<AppUsageInfo>();
    private static ArrayList<AppUsageInfo> mLatestUsageList = new ArrayList<AppUsageInfo>();

    public class AppUsageInfo {
        int uid;
        String appName;
        long bytesUsed;
    }

    public static ArrayList<AppUsageInfo> mReportList = new ArrayList<AppUsageInfo>();

    public static void clearDataLists() {
        mReportList.clear();
        mUsageList.clear();
        mLatestUsageList.clear();
    }
}
