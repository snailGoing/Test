package com.sprd.generalsecurity.network;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class FloatViewService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FloatKeyView v = FloatKeyView.getInstance(this);
        v.addToWindow();
    }

    @Override
    public void onDestroy() {
        FloatKeyView v = FloatKeyView.getInstance(this);
        v.removeFromWindow();
        super.onDestroy();
    }
}
