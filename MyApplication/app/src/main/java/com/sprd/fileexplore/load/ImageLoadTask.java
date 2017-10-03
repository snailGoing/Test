package com.sprd.fileexplore.load;


import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import com.sprd.fileexplore.aidl.IRemoteServiceCallback;
import com.sprd.fileexplore.load.ImageCache.OnImageLoadCompleteListener;
import com.sprd.fileexplore.util.FileUtil;

public class ImageLoadTask implements Runnable {

    private static final String TAG = ImageLoadTask.class.getSimpleName();

    Context mContext = null;
    String mImageUrl = null;
    Bitmap mBitmap = null;

    int type;

    int pos = -1;
    OnImageLoadCompleteListener mListener = null;
    //IRemoteServiceCallback  mServiceListener = null;

    ThreadPollManager threadPollManager = ThreadPollManager.getInstance();
    ImageCache imageCache = ImageCache.getInstance();
    private Handler  handler;
    @SuppressWarnings("unused")
    private ImageLoadTask() {
        // TODO Auto-generated constructor stub
    }

    /*
    *  this is for locale process to call this
    * */
    public ImageLoadTask(Context context, String imageUrl, OnImageLoadCompleteListener listener,
                         Handler handler, int type, int pos) {
        this.mContext = context;
        this.mImageUrl = imageUrl;
        this.mListener = listener;
        this.type = type;
        this.pos = pos;
        this.handler = handler;
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        mBitmap = FileUtil.readBitMap(mImageUrl, type, mContext);
        threadPollManager.removeTask(mImageUrl);
        Log.d(TAG,"  run()--- >  mBitmap="+(mBitmap == null ? "null": "non-null"));
        if (null != mBitmap) {
            imageCache.put(mImageUrl, mBitmap);
            if(handler!=null){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        Log.d(TAG,"  run()--- >  complete");
                        mListener.OnImageLoadComplete(mImageUrl, true, mBitmap, pos);
                    }
                });
            }
        }
    }

}

