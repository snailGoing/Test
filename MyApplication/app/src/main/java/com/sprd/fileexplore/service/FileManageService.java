package com.sprd.fileexplore.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import android.util.Log;



/**
 * Created by Xuehao.Jiang on 2017/6/27.
 */

public class FileManageService extends Service {

    private static final String TAG = FileManageService.class.getSimpleName();
    public static final int FILE_FILTER_TYPE_UNKOWN = -1;
    public static final int FILE_FILTER_TYPE_DEFAULT = 0;
    public static final int FILE_FILTER_TYPE_FOLDER = 1;
    public static final int FILE_FILTER_TYPE_ALL = 2;
    private MountRootManagerRf mMountRootManagerRf;

    private MyBinder mBinder;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"  onCreate .... ");
        mBinder  = new MyBinder(this);
        mMountRootManagerRf = MountRootManagerRf.getInstance();
        mMountRootManagerRf.init(this);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMountRootManagerRf.onDestroy();
        Log.d(TAG,"  onDestroy .... ");
    }

    public interface OperationEventListener {
        int ERROR_CODE_NAME_VALID = 100;
        int ERROR_CODE_SUCCESS = 0;

        int ERROR_CODE_UNSUCCESS = -1;
        int ERROR_CODE_NAME_EMPTY = -2;
        int ERROR_CODE_NAME_TOO_LONG = -3;
        int ERROR_CODE_FILE_EXIST = -4;
        int ERROR_CODE_NOT_ENOUGH_SPACE = -5;
        int ERROR_CODE_DELETE_FAILS = -6;
        int ERROR_CODE_USER_CANCEL = -7;
        int ERROR_CODE_PASTE_TO_SUB = -8;
        int ERROR_CODE_UNKOWN = -9;
        int ERROR_CODE_COPY_NO_PERMISSION = -10;
        int ERROR_CODE_MKDIR_UNSUCCESS = -11;
        int ERROR_CODE_CUT_SAME_PATH = -12;
        int ERROR_CODE_BUSY = -100;
        int ERROR_CODE_DELETE_UNSUCCESS = -13;
        int ERROR_CODE_PASTE_UNSUCCESS = -14;
        int ERROR_CODE_DELETE_NO_PERMISSION = -15;
        int ERROR_CODE_COPY_GREATER_4G_TO_FAT32 = -16;

        /**
         * This method will be implemented, and called in onPreExecute of asynctask
         */
        void onTaskPrepare();

        /**
         * This method will be implemented, and called in onProgressUpdate function
         * of asynctask
         *
         * @param progressInfo information of ProgressInfo, which will be updated on UI
         */
        void onTaskProgress(ProgressInfo progressInfo);

        /**
         * This method will be implemented, and called in onPostExecute of asynctask
         *
         * @param result the result of asynctask's doInBackground()
         */
        void onTaskResult(int result);
    }

}
