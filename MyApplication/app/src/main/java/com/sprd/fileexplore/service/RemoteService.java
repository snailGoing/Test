package com.sprd.fileexplore.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;


import android.os.RemoteException;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sprd.fileexplore.aidl.IRemoteService;
import com.sprd.fileexplore.aidl.IRemoteServiceCallback;
import com.sprd.fileexplore.load.ImageCache;

/**
 * Created by Xuehao.Jiang on 2017/7/4.
 */

public class RemoteService extends Service {


    private static final String TAG = RemoteService.class.getSimpleName();
    private ImageCache imageCache = ImageCache.getInstance();
    private Handler handler;
    private Context context;
    private RemoteServiceBinder binder;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "  RemoteService: --->  onCreate()");
        context = this;
        handler = new Handler();
        binder = new RemoteServiceBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "  RemoteService: --->  onDestroy()");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        Log.d(TAG,"   onBind:  intent="+ intent);
        return binder;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d(TAG,"   onStart:  intent="+ intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"   onStartCommand:  intent="+ intent);
        return super.onStartCommand(intent, flags, startId);

    }

    public class RemoteServiceBinder extends IRemoteService.Stub{
        @Override
        public void loadImagesWithPath(String filePath, int imageType, IRemoteServiceCallback cb,
                                       int priority) throws RemoteException {
            Log.d(TAG,"  RemoteServiceBinder---> loadImageBitmap");
        }
    }
}
