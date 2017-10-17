package com.sprd.generalsecurity.network;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkInfo;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;

import com.sprd.generalsecurity.data.BlockStateProvider;

import com.sprd.generalsecurity.R;
import com.sprd.generalsecurity.utils.Contract;
import com.sprd.generalsecurity.utils.DateCycleUtils;
import com.sprd.generalsecurity.utils.DateCycleUtils.DataRestriction;
import com.sprd.generalsecurity.utils.TeleUtils;
import com.sprd.generalsecurity.utils.Formatter;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Date;

import static android.net.NetworkTemplate.buildTemplateMobileAll;


public class DataFlowService extends IntentService {
    private static final String TAG = "DataFlowService";

    private static final long REPEAT_TIME = 15 * 60 * 1000; //15 mins
    private static final long M2BITS = 1024 *1024;

    private static final String REMIND_ACTION = "com.sprd.generalsecurity.network.alert";

    public DataFlowService() {
        super("DataFlowService");
    }

    private static final String KEY_MONTH_REMIND = "key_restrict_month_reminder";
    private static final String KEY_DAY_REMIND = "key_restrict_day_reminder";

    private static final String KEY_EDIT_MONTH_USED = "key_edit_month_used";
    private static final String KEY_SEEK_BAR_PERCENT = "seek_bar_restrict";

    private static final String PREFRIX_SHARED_PREF = "sim";

    private static final String REMIND_TYPE = Contract.EXTRA_ALERT_TYPE;
    private static final int REMIND_MONTH = Contract.ALERT_TYPE_MONTH;
    private static final int REMIND_DAY = Contract.ALERT_TYPE_DAY;

    private static final String TRIGGER_INDEX = "trigger";
    private static final String WARNIN_MSG_MONTH = "msg_month";
    private static final String WARNIN_MSG_DAY = "msg_day";


    private static final int TRIGGER1 = 1;
    private static final int TRIGGER2 = 2;

    private static final String PERCENT = "percent";
    private static final String SIMCARD = "simcard";

    private static final String SIM1 = "SIM1";
    private static final String SIM2 = "SIM2";

    private static final int SIM1_INDEX = 1;
    private static final int SIM2_INDEX = 2;

    private static final int COUNT_SINGLE = 1;
    private static final int COUNT_DUAL = 2;

    private int mPrimaryCard;

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getBooleanExtra(Contract.EXTRA_SIM_STATE, false)) {
            checkSimStateChange();
            return;
        }

        //Check network state
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ns = cm.getActiveNetworkInfo();

        boolean isConnected = ns != null && ns.isConnectedOrConnecting();

        mPrimaryCard = TeleUtils.getPrimarySlot(this);
        if (ns != null) {
            Log.d(TAG, "pri card:" + mPrimaryCard + ":" + ns.getState());
        }

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getService(this, 0, new Intent(this, DataFlowService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        boolean remindDisabled = ifAllRemindDisabled();
        Log.d(TAG, "isconn:" + isConnected + ":" + remindDisabled);
        if (isConnected && !remindDisabled) {
            if (ns.getType() == ConnectivityManager.TYPE_MOBILE) {
                //TODO: check mobile network only
                notifyUserForMonthIfNeeded(mPrimaryCard);
                am.set(AlarmManager.RTC, System.currentTimeMillis() + REPEAT_TIME, pi);
                Log.d(TAG, "start alarm");
            }
        } else {
            //network disconnected
            am.cancel(pi);
        }
    }

    /**
     * Check if all remind disabled
     * @return true: all remind disabled
     *         false: some remind is enabled
     */
    private boolean ifAllRemindDisabled() {
        int simCount = TeleUtils.getSimCount(this);
        if (simCount == COUNT_SINGLE) {
            SharedPreferences pref = this.getSharedPreferences(PREFRIX_SHARED_PREF + mPrimaryCard,
                    Context.MODE_PRIVATE);

            boolean monthRemindEnabled = pref.getBoolean(KEY_MONTH_REMIND, false);
            boolean dayRemindEnabled = pref.getBoolean(KEY_DAY_REMIND, false);

            if (monthRemindEnabled || dayRemindEnabled) {
                return false;
            } else {
                return true;
            }
        } else if (simCount == COUNT_DUAL) {
            SharedPreferences pref1 = this.getSharedPreferences(PREFRIX_SHARED_PREF + 1, Context.MODE_PRIVATE);
            boolean monthRemindEnabled1 = pref1.getBoolean(KEY_MONTH_REMIND, false);
            boolean dayRemindEnabled1 = pref1.getBoolean(KEY_DAY_REMIND, false);

            if (monthRemindEnabled1 || dayRemindEnabled1) {
                return false;
            }

            SharedPreferences pref2 = this.getSharedPreferences(PREFRIX_SHARED_PREF + 2, Context.MODE_PRIVATE);
            boolean monthRemindEnabled2 = pref2.getBoolean(KEY_MONTH_REMIND, false);
            boolean dayRemindEnabled2 = pref2.getBoolean(KEY_DAY_REMIND, false);
            if (monthRemindEnabled2 || dayRemindEnabled2) {
                return false;
            } else {
                return true;
            }
        } else {
            //do nothing
        }

        return true;
    }

   // Query network usage since startTime to current time.
    private long queryNetworkDataUsed(INetworkStatsSession statsSession, long startTime, int simId) {
        NetworkTemplate template;
        NetworkStats stats;

        if (TeleUtils.getSimCount(this) == COUNT_SINGLE) {
            template = buildTemplateMobileAll(TeleUtils.getActiveSubscriberId(this));
        } else {
            template = buildTemplateMobileAll(TeleUtils.getActiveSubscriberId(this, simId));
        }

        try {
            stats = statsSession.getSummaryForAllUid(template, startTime,
                    System.currentTimeMillis(), false);
        } catch (RemoteException e) {
            e.printStackTrace();
            return 0;
        }

        NetworkStats.Entry entry = null;
        long dataUsed = 0;
        int size = (stats != null) ? stats.size() : 0;
        for (int i = 0; i < size; i++) {
            entry = stats.getValues(i, entry);
            dataUsed += (entry.rxBytes + entry.txBytes);
        }

        return dataUsed;
    }

    private void notifyUser(String msg) {
        Intent it = new Intent(REMIND_ACTION);
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        it.putExtra(REMIND_TYPE, REMIND_MONTH);
        it.putExtra(WARNIN_MSG_MONTH, msg);
        startActivity(it);

        //intent to start DataFlowMainEntry when notification clicked.
        Intent resultIntent = new Intent(this, DataFlowMainEntry.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
                resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        // Month reminder notificatin
        Notification notification = new Notification.Builder(this)
                .setContentTitle(getResources().getString(R.string.data_flow_management))
                .setContentText(msg)
                .setContentIntent(resultPendingIntent)
                .setSmallIcon(R.drawable.month_remind)
                .setAutoCancel(true)
                .setStyle(new Notification.BigTextStyle().bigText(msg))
                .build();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(0, notification);
    }

    private void checkMonthDataAndNotify(int sim, INetworkStatsSession statsSession,
                                         int simCount, DataRestriction dt) {
        SharedPreferences pref = this.getSharedPreferences(PREFRIX_SHARED_PREF + sim,
                Context.MODE_PRIVATE);
        boolean monthRemindEnabled = pref.getBoolean(KEY_MONTH_REMIND, false);
        long dataUsedSetByUser = (long) Float.parseFloat(
                pref.getString(KEY_EDIT_MONTH_USED, "0")) * M2BITS;

        final int PERCENT_START = 50;
        final int PERCENT_FULL = 100;
        float percent = (float) (pref.getInt(KEY_SEEK_BAR_PERCENT, 0) + PERCENT_START) / PERCENT_FULL;

        if (monthRemindEnabled) {
            long delta = pref.getLong(Contract.KEY_DATA_USED_QUERY_DELTA, 0);
            long dataUsed = queryNetworkDataUsed(statsSession,
                    DateCycleUtils.getMonthCycleStart(), sim) - delta;
            long remindDiffTriggerWarn = pref.getLong(Contract
                    .KEY_MONTH_REMIND_TIME_TRIGGER_WARN, 0) -
                    DateCycleUtils.getInstance().getDayCycleStart();
            long remindDiffTriggerOver = pref.getLong(Contract
                    .KEY_MONTH_REMIND_TIME_TRIGGER_OVER, 0) -
                    DateCycleUtils.getInstance().getDayCycleStart();
            Date date = new Date(pref.getLong(Contract.KEY_MONTH_REMIND_TIME_TRIGGER_WARN, 0));
            if (remindDiffTriggerWarn < 0 && dt.monthRestriction > 0 &&
                    dataUsed >= dt.monthRestriction * percent && dataUsed < dt.monthRestriction) {
                //trigger 1
                String msg = "";

                if (simCount == COUNT_SINGLE) {
                    /*if phone has only one sim card*/
                    msg = String.format(getResources().getString(R.string.warning_msg),
                            "", (int) (percent * PERCENT_FULL));
                } else if (simCount == COUNT_DUAL) {
                     /*if phone has two sim cards*/
                    if (sim == SIM1_INDEX) {
                        msg = String.format(getResources().getString(R.string.warning_msg),
                                SIM1, (int) (percent * PERCENT_FULL));
                    } else if (sim == SIM2_INDEX) {
                        msg = String.format(getResources().getString(R.string.warning_msg),
                                SIM2, (int) (percent * PERCENT_FULL));
                    }
                } else {
                    //do nothing
                }
                notifyUser(msg);

                pref.edit().putLong(Contract.KEY_MONTH_REMIND_TIME_TRIGGER_WARN,
                        System.currentTimeMillis()).commit();
                Log.d(TAG, "remind display1:" + sim);
            } else if (remindDiffTriggerOver < 0 && dt.monthRestriction > 0
                    && dataUsed >= dt.monthRestriction) {
                //trigger 2
                String msg = "";
                if (simCount == COUNT_SINGLE) {
                    /*if phone has only one sim card*/
                    msg = String.format(getResources().
                            getString(R.string.warning_msg_month_SIM), "");
                } else if (simCount == COUNT_DUAL) {
                     /*if phone has two sim cards*/
                    if (sim == SIM1_INDEX) {
                        msg = String.format(getResources().
                                getString(R.string.warning_msg_month_SIM), SIM1);
                    } else if (sim == SIM2_INDEX) {
                        msg = String.format(getResources().
                                getString(R.string.warning_msg_month_SIM), SIM2);
                    }
                } else {
                    //do nothing
                }

                notifyUser(msg);
                pref.edit().putLong(Contract.KEY_MONTH_REMIND_TIME_TRIGGER_OVER,
                        System.currentTimeMillis()).commit();
                Log.d(TAG, "remind display2:" + sim);
            } else {
                //do nothing
            }
        }
    }

    private void checkDayDataAndNotify(int sim, INetworkStatsSession statsSession,
                                       int simCount, DataRestriction dt) {
        long dataUsed = queryNetworkDataUsed(statsSession,
                DateCycleUtils.getDayCycleStart(), sim);
        SharedPreferences pref = this.getSharedPreferences(PREFRIX_SHARED_PREF + sim,
                Context.MODE_PRIVATE);

        boolean dayRemindEnabled = pref.getBoolean(KEY_DAY_REMIND, false);
        if (dayRemindEnabled) {
            // get data used ,
            long remindDayDiffTrigger = pref.getLong(Contract.KEY_DAY_REMIND_TIME_TRIGGER, 0) -
                    DateCycleUtils.getInstance().getDayCycleStart();
            if (remindDayDiffTrigger < 0 && dt.dayRestriction > 0) {
                //trigger 1
                if (dataUsed >= dt.dayRestriction) {
                    String msgDaySIM = "";
                    if (simCount == COUNT_SINGLE) {
                    /*if phone has only one sim card*/
                        msgDaySIM = String.format(getResources().
                                getString(R.string.warning_msg_day_SIM), "");
                    } else if (simCount == COUNT_DUAL) {
                        /*if phone has two sim cards*/
                        if (sim == SIM1_INDEX) {
                            msgDaySIM = String.format(getResources().
                                    getString(R.string.warning_msg_day_SIM), SIM1);
                        } else {
                            msgDaySIM = String.format(getResources().
                                    getString(R.string.warning_msg_day_SIM), SIM2);
                        }
                    } else {
                        // do nothing
                    }

                    notifyUser(msgDaySIM);
                    pref.edit().putLong(Contract.KEY_DAY_REMIND_TIME_TRIGGER,
                            System.currentTimeMillis()).commit();
                    Log.d(TAG, "remind display3:" + sim);
                }
            }
        }
    }

    private void checkMonthDataRemainedAndNotify(int sim, INetworkStatsSession statsSession,
                                                 int simCount, DataRestriction dt) {


        SharedPreferences pref = this.getSharedPreferences(PREFRIX_SHARED_PREF + sim,
                Context.MODE_PRIVATE);
        boolean monthRemindEnabled = pref.getBoolean(KEY_MONTH_REMIND, false);
        if (monthRemindEnabled) {
            // get data used ,
            long remindDiffTriggerWarn = pref.getLong(Contract
                    .KEY_MONTH_REMIND_TIME_TRIGGER_WARN, 0) -
                    DateCycleUtils.getInstance().getDayCycleStart();
            long remindDiffTriggerOver = pref.getLong(Contract
                    .KEY_MONTH_REMIND_TIME_TRIGGER_OVER, 0) -
                    DateCycleUtils.getInstance().getDayCycleStart();
            long delta = pref.getLong(Contract.KEY_DATA_USED_QUERY_DELTA, 0);
            long dataUsed = queryNetworkDataUsed(statsSession,
                    DateCycleUtils.getMonthCycleStart(), sim) - delta;
            if (remindDiffTriggerWarn < 0 && dt.monthRestriction > 0 &&
                    (dt.monthRestriction - dataUsed) <= dt.monthleftRestriction && dataUsed < dt.monthRestriction) {
                //trigger 1
                String msg = "";

                if (simCount == COUNT_SINGLE) {
                    /*if phone has only one sim card*/
                    msg = String.format(getResources().getString(R.string.warning_remain_msg),
                            "", (int) (dt.monthleftRestriction));
                } else if (simCount == COUNT_DUAL) {
                     /*if phone has two sim cards*/
                    if (sim == SIM1_INDEX) {
                        msg = String.format(getResources().getString(R.string.warning_remain_msg),
                                SIM1);
                    } else if (sim == SIM2_INDEX) {
                        msg = String.format(getResources().getString(R.string.warning_remain_msg),
                                SIM2);
                    }
                } else {
                    //do nothing
                }
                notifyUser(msg);

                pref.edit().putLong(Contract.KEY_MONTH_REMIND_TIME_TRIGGER_WARN,
                        System.currentTimeMillis()).commit();
                Log.d(TAG, "remind display1:" + sim);
            } else if (remindDiffTriggerOver < 0 && dt.monthRestriction > 0
                    && dataUsed >= dt.monthRestriction) {
                //trigger 2
                String msg = "";
                if (simCount == COUNT_SINGLE) {
                    /*if phone has only one sim card*/
                    msg = String.format(getResources().
                            getString(R.string.warning_msg_month_SIM), "");
                } else if (simCount == COUNT_DUAL) {
                     /*if phone has two sim cards*/
                    if (sim == SIM1_INDEX) {
                        msg = String.format(getResources().
                                getString(R.string.warning_msg_month_SIM), SIM1);
                    } else if (sim == SIM2_INDEX) {
                        msg = String.format(getResources().
                                getString(R.string.warning_msg_month_SIM), SIM2);
                    }
                } else {
                    //do nothing
                }

                notifyUser(msg);
                pref.edit().putLong(Contract.KEY_MONTH_REMIND_TIME_TRIGGER_OVER,
                        System.currentTimeMillis()).commit();
                Log.d(TAG, "remind display2:" + sim);
            }
        }
    }
    /**
     * Remind user once a day if needed.
     * @param dataUsed
     * @param sim
     */
    private void notifyUserForMonthIfNeeded(int sim) {
        int simCount = TeleUtils.getSimCount(this);
        INetworkStatsSession statsSession;

        INetworkStatsService statsService = INetworkStatsService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        try {
            statsSession = statsService.openSession();
            statsService.forceUpdate();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        NetworkTemplate template = null;
        NetworkStats mStats;

        //get month restrict quota
        DataRestriction dt = DateCycleUtils.getInstance().getDataFlowRestriction(this, sim);
        checkMonthDataRemainedAndNotify(sim, statsSession, simCount, dt);
        checkDayDataAndNotify(sim, statsSession, simCount, dt);
    }

    private void checkSimStateChange() {
        Log.e(TAG, "checkSim:");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //get current sim count
        int simCount = TeleUtils.getSimCount(this);
        if (simCount == 1) {
            String storedNum, currentNum;
            if (TeleUtils.getPrimaryCard(this) == 0) {
                //sim 1 is primary
                storedNum = sharedPreferences.getString("sim1_num", null);
                currentNum = TeleUtils.getSimNumber(this, 0);
                Log.e(TAG, "single Num:" + storedNum + ":" + currentNum);
            } else {
                //sim2 is primary
                storedNum = sharedPreferences.getString("sim2_num", null);
                currentNum = TeleUtils.getSimNumber(this, 1);
                Log.e(TAG, "single Num:" + storedNum + ":" + currentNum);
            }

            if (storedNum != null && currentNum != null
                && !storedNum.equalsIgnoreCase(currentNum) ){
                Log.e(TAG, "prompt user to reset");
                startSimAlert();
            }
            return;
        } else {
            String storedNum = sharedPreferences.getString("sim1_num", null);
            String currentNum = TeleUtils.getSimNumber(this, 0);
            Log.e(TAG, "Num:" + storedNum + ":" + currentNum);

            if (storedNum != null && currentNum != null
                    && !storedNum.equalsIgnoreCase(currentNum)){
                Log.e(TAG, "prompt user to reset 2");
                startSimAlert();
                return;
            }
            String storedNum2 = sharedPreferences.getString("sim2_num", null);
            String currentNum2 = TeleUtils.getSimNumber(this, 1);
            Log.e(TAG, "Num2:" + storedNum2 + ":" + currentNum2);
            if (storedNum2 != null && currentNum2 != null
                    && !storedNum2.equalsIgnoreCase(currentNum2) ){
                Log.e(TAG, "prompt user to reset 3");
                startSimAlert();
            }
        }
    }

    private void startSimAlert() {
        Intent it = new Intent(REMIND_ACTION);
        it.putExtra(Contract.EXTRA_SIM_PROMPT, true);
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(it);
    }
}
