package com.sprd.generalsecurity.utils;

public class Contract {
    public static final String EXTRA_SIM_PROMPT = "sim_prompt";
    public static final String EXTRA_ALERT_TYPE = "type";
    public static final String EXTRA_SIM_STATE = "simstate";
    public static final String EXTRA_SIM_ID = "sim";

    //lock screen data notification
    public static final String KEY_LOCK_DATA_SWITCH = "keyguard_data_switch";
    public static final String KEY_REAL_SPEED_SWITCH = "networkspeed_switch";

    // data used set by user
    public static final String KEY_DATA_USED_ORIGINAL = "key_user_set_used_orignal";

    //delta between user set and query result
    public static final String KEY_DATA_USED_QUERY_DELTA = "data_query_delta";

    public static final int ALERT_TYPE_MONTH = 1;
    public static final int ALERT_TYPE_DAY = 2;

    public static final String KEY_CURRENT_MONTH = "current_month";
    public static final String KEY_MONTH_TOTAL = "key_edit_month_total";
    public static final String KEY_MONTH_USED = "key_edit_month_used";

    public static final String KEY_MONTH_REMIND_TIME_TRIGGER_OVER = "sim_month_remind_time_trigger_over";

    public static final String KEY_MONTH_REMIND_TIME_TRIGGER_WARN = "sim_month_remind_time_trigger_warn";

    public static final String KEY_DAY_REMIND_TIME_TRIGGER = "sim_day_remind_time_trigger";

    public static final int MAX_SCORE = 100;
    public static final int MIN_SCORE = 60;
    public static final int SIM1_INDEX = 0;
    public static final int SIM2_INDEX = 1;
    public static final String SIM1 = "sim1";
    public static final String SIM2 = "sim2";
    public static final int FINISH_SCAN = 0;
    public static final int FINISH_OPTIMIZE = 1;

}