package com.sprd.generalsecurity.network;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import com.sprd.generalsecurity.utils.Contract;

public class NetworkStateReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkStateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        Log.e(TAG, "receiver:" + activeNetwork + ":" + intent + "\n time:" + System.currentTimeMillis());
        Intent it = new Intent(context, DataFlowService.class);
        context.startService(it);

        //stop real speed view if network disconnected.
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPref.getBoolean(Contract.KEY_REAL_SPEED_SWITCH, false)) {
            realSpeedSetting(activeNetwork, context);
        }
    }

    private void realSpeedSetting(NetworkInfo info, Context context) {
        if (info != null && info.isConnectedOrConnecting()) {
            FloatKeyView.getInstance(context).startRealSpeed();
        } else {
            FloatKeyView.getInstance(context).stopRealSpeed();
            Log.e(TAG, "stopped real speed");
        }
    }
}