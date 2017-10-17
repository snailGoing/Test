package com.sprd.generalsecurity.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sprd.generalsecurity.utils.Contract;
import com.sprd.generalsecurity.utils.TeleUtils;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiverGS";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPref.getBoolean(Contract.KEY_REAL_SPEED_SWITCH, false)) {
            context.startService(new Intent(context, FloatViewService.class));
        }

        if (sharedPref.getBoolean(Contract.KEY_LOCK_DATA_SWITCH, false) ||
                    sharedPref.getBoolean(Contract.KEY_REAL_SPEED_SWITCH, false)) {
            Log.d(TAG, "keyguard_data_switch on:" );
            Intent it = new Intent(context, ScreenStateService.class);
            context.startService(it);
        } else {
            Log.d(TAG, "keyguard_data_switch false");
        }
    }
}