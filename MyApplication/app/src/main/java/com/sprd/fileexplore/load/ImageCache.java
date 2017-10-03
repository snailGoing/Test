package com.sprd.fileexplore.load;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;

import com.sprd.fileexplore.aidl.IRemoteServiceCallback;
import com.sprd.fileexplore.service.RemoteService;

public class ImageCache {

    private static final String TAG ="ImageCache";
    private static ImageCache instance = new ImageCache();
    private ThreadPollManager threadPollManager = ThreadPollManager.getInstance();
    private static final int INITIAL_CAPACITY = 25;
    private  ConcurrentHashMap<String, SoftReference<Bitmap>> sSoftImageMap = new ConcurrentHashMap<String, SoftReference<Bitmap>>();
    private  LinkedHashMap<String, Bitmap> sImageMap = new LinkedHashMap<String, Bitmap>(
            INITIAL_CAPACITY / 2, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Entry<String, Bitmap> eldest) {
            if (size() > INITIAL_CAPACITY) {
                sSoftImageMap.put(eldest.getKey(), new SoftReference<Bitmap>(
                        eldest.getValue()));
                return true;
            }
            return false;
        };
    };
    public static ImageCache getInstance(){
        if(instance==null){
            instance = new ImageCache();
        }
        return instance;
    }
    private ImageCache() {
        // TODO Auto-generated constructor stub

    }
    public  void put(String fileUrl, Bitmap bitmap) {
        if (isEmptyOrWhitespace(fileUrl) || bitmap == null) {
            return;
        }
        synchronized (sImageMap) {
            if (sImageMap.get(fileUrl) == null) {
                sImageMap.put(fileUrl, bitmap);
            }
        }
    }

    public  boolean remove(String fileUrl) {
        synchronized (sImageMap) {
            if (sImageMap != null ){
                sImageMap.remove(fileUrl);
                sSoftImageMap.remove(fileUrl);
                return true;
            }
            return false;
        }
    }

    public  Bitmap get(String fileUrl) {
        Log.d(TAG," get file ="+ fileUrl);
        synchronized (sImageMap) {
            Bitmap bitmap = (Bitmap) sImageMap.get(fileUrl);
            if (bitmap != null) {
                // put the map to the first, so it will be deleted last
                // do it by removeEldestEntry()
                Log.d(TAG," get file  success!");
                return bitmap;
            }
            Log.d(TAG," get file  fail 1 !");
            SoftReference<Bitmap> sBitmap = sSoftImageMap.get(fileUrl);
            if (sBitmap != null) {
                bitmap = sBitmap.get();
                if (bitmap == null) {
                    sSoftImageMap.remove(fileUrl);
                } else {
                    return bitmap;
                }
            }
            Log.d(TAG," get file  fail 2 !");
            return null;
        }
    }

    public  boolean isEmptyOrWhitespace(String s) {
        s = makeSafe(s);
        for (int i = 0, n = s.length(); i < n; i++) {
          if (!Character.isWhitespace(s.charAt(i))) {
            return false;
          }
        }
        return true;
      }


      public  String makeSafe(String s) {
        return (s == null) ? "" : s;
      }

    public  void loadImageBitmap(Context context, String imageUrl,
                                       OnImageLoadCompleteListener  listener, Handler handler, int type , int pos) {

        ImageLoadTask task = new ImageLoadTask(context, imageUrl, listener, handler, type, pos);
        threadPollManager.submitTask(imageUrl, task);
        Log.e(TAG, "execute a ImageLoadTask !");


    }

    public interface OnImageLoadCompleteListener {
        public void OnImageLoadComplete(String fileUrl, boolean success,
                                        Bitmap bitmap,int pos);
    }

//    public  void loadImageBitmap(Context context, String imageUrl, int imageType, int priority,
//                                 IRemoteServiceCallback cb, Handler mHandler) {
//        ImageLoadTask task = new ImageLoadTask(context, imageUrl, cb, imageType, priority,mHandler);
//        threadPollManager.submitTask(imageUrl, task);
//        Log.d(TAG, "execute a ImageLoadTask !");
//    }


}

