package com.sprd.fileexplore.util;


import android.util.Log;

/**
 * Created by Xuehao.Jiang on 2017/4/6.
 */

public class LogUtil {

    private static final String TAG = "FileExploreLog";
    private static final Boolean DEBUG_INFO_FLAG = true;

    public static void i(String string) {
        if (DEBUG_INFO_FLAG) {
            Log.i(TAG, string);
        }
    }

    public static void d(String string) {
        if (DEBUG_INFO_FLAG) {
            Log.d(TAG, string);
        }
    }
    public static void d(String tag,  String string) {
        if (DEBUG_INFO_FLAG) {
            Log.d(tag, string);
        }
    }
    public static void d(String string,Exception e) {
        if (DEBUG_INFO_FLAG) {
            Log.d(TAG, string,e);
        }
    }
}
