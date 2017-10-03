package com.sprd.fileexplore.file;

import android.os.FileObserver;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;


/**
 * Created by Xuehao.Jiang on 2017/4/19.
 */

public class PathObserver extends FileObserver {


    private static final int FOLDER_DELETE = 1073742336;
    private static final int FOLDER_MOVED_FROM = 1073741888;
    private static final int MAX_CHECK_TIME = 200;
    public static final int MSG_PATH_TOP_REFRESH = 200;

    private FileInfo mCurrentPath = null;
    private ArrayList<FileInfo> mAddChangeList = new ArrayList<FileInfo>();
    private ArrayList<FileInfo> mDeleteChangeList = new ArrayList<FileInfo>();

    private Handler mHandler;
    private Runnable mUpdateRunnable;
    private Object object = new Object();




    private static  final  String TAG = PathObserver.class.getSimpleName();

    public PathObserver(String path, int mask) {
        super(path, mask);
    }

    public PathObserver(FileInfo path, int mask,Handler handler,Runnable runnable){
        this(path.getPath(),FileObserver.DELETE | FileObserver.CREATE
                | FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO | FileObserver.MOVED_FROM);
        mCurrentPath = new FileInfo(path);
        mHandler = handler;
        mUpdateRunnable = runnable;
    }
    @Override
    public void onEvent(int event, String path) {
        Log.d(TAG, "onEvent:event = " + event + ", path is " + path);
        if(mCurrentPath!=null && mCurrentPath.isExist()){
            FileInfo fileInfo = new FileInfo(mCurrentPath.getPath()+ File.separator+path);
            // Make sure that the file is deleted before refreshing UI
            if (event == FileObserver.DELETE || event == FOLDER_DELETE || event == FileObserver.MOVED_FROM
                    || event == FOLDER_MOVED_FROM) {
                long startCheckTime = System.currentTimeMillis();
                long currentTime = 0;
                while (fileInfo.isExist()) {
                    currentTime = System.currentTimeMillis();
                    if (currentTime - startCheckTime > MAX_CHECK_TIME) {
                        Log.d(TAG, "onEvent: Timeout, the refresh may have an error!");
                        break;
                    }
                }

            }
         synchronized (object) {
             String obPath= mCurrentPath.getPath() + File.separator + path;
             FileInfo mf= new FileInfo(obPath);
             Log.d(TAG, "onEvent: ,  obPath ="+obPath);
            if(fileInfo.isExist()){
                mAddChangeList.add(mf);
            }else{
                mDeleteChangeList.add(fileInfo);
            }

            mHandler.removeCallbacks(mUpdateRunnable);
            // notify to update FileInfo list datas
            mHandler.postDelayed(mUpdateRunnable, 500);
         }
        }else{
            mCurrentPath=null;
            stopObserve();
        }
    }

    public void stopObserve() {
        Log.d(TAG, "stopObserve");
        this.stopWatching();
        synchronized(object){
            mAddChangeList.clear();
            mDeleteChangeList.clear();
        }
    }
    public void startObserve(FileInfo target) {
        Log.d(TAG, "startObserver: Start watching " + target.getPath());
        mCurrentPath = target;
        this.startWatching();
    }

    public ArrayList<FileInfo> getAddChangeList() {
        return mAddChangeList;
    }

    public ArrayList<FileInfo> getDeleteChangeList() {
        return mDeleteChangeList;
    }

}
