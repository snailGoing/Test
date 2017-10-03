package com.sprd.fileexplore.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

public class PermissionUtils {
    private static final String TAG = "PermissionUtil";
    public static boolean hasStorageWritePermission(Context ctx) {


        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            return true;
        }else{
            return (ctx.checkSelfPermission(Manifest.permission.
                    WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        }

    }

    public static boolean hasStorageReadPermission(Context ctx) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            return true;
        }else{
            return (ctx.checkSelfPermission(Manifest.permission.
                    READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        }
    }

    public static void requestPermission(Activity ctx, String permission, int requestCode){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            return ;
        }else{
            ctx.requestPermissions(new String[]{permission}, requestCode);
        }
    }

    public static boolean showWriteRational(Activity ctx){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            return true;
        }else{
            return ctx.shouldShowRequestPermissionRationale(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

    }

    public static boolean showReadRational(Activity ctx){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            return true;
        }else{
            return ctx.shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }
}
