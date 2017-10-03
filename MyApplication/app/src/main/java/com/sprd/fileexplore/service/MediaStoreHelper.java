/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.sprd.fileexplore.service;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteFullException;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;


import com.sprd.fileexplore.file.FileType;
import com.sprd.fileexplore.util.SystemPropertiesInvoke;

import java.io.File;
import java.util.List;

public final class MediaStoreHelper {

    private static final String TAG = "MediaStoreHelper";
    private final Context mContext;
    private BaseAsyncTask mBaseAsyncTask;
    private String mDstFolder;
    private static final int SCAN_FOLDER_NUM = 20;
    /**
     * Constructor of MediaStoreHelper
     *
     * @param context
     *            the Application context
     */
    public MediaStoreHelper(Context context) {
        mContext = context;
    }

    public MediaStoreHelper(Context context, BaseAsyncTask baseAsyncTask) {
        mContext = context;
        mBaseAsyncTask = baseAsyncTask;
    }

    public void updateInMediaStore(String newPath, String oldPath) {
        Log.d(TAG, "updateInMediaStore,newPath = " + newPath + ",oldPath = " + oldPath);
        if (mContext != null && !TextUtils.isEmpty(newPath) && !TextUtils.isEmpty(oldPath)) {

            Uri uri = MediaStore.Files.getContentUri("external");

            String where = MediaStore.Files.FileColumns.DATA + "=?";
            String[] whereArgs = new String[] { oldPath };

            ContentResolver cr = mContext.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Files.FileColumns.DATA, newPath);
            whereArgs = new String[] { oldPath };

            try {
                Log.d(TAG, "updateInMediaStore,update.");
                cr.update(uri, values, where, whereArgs);
                if( new File(newPath).isDirectory()){
                    scanDir(mContext,new File(newPath).getParentFile());
                }else{
                    scanPathforMediaStore(newPath);
                }
            } catch (UnsupportedOperationException e) {
                Log.e(TAG, "Error, database is closed!!!");
            } catch (NullPointerException e) {
                Log.e(TAG, "Error, NullPointerException:" + e + ",update db may failed!!!");
            } catch (SQLiteFullException e) {
                Log.e(TAG, "Error, database or disk is full!!!" + e);
                if (mBaseAsyncTask != null) {
                    mBaseAsyncTask.cancel(true);
                }
            }

        }
    }

    /**
     * scan Path for new file or folder in MediaStore
     *
     * @param path
     *            the scan path,  file
     */
    public void scanPathforMediaStore(String path) {
        Log.d(TAG, "scanPathforMediaStore.path =" + path);
        if (mContext != null && !TextUtils.isEmpty(path)) {
            String[] paths = { path };
            Log.d(TAG, "scanPathforMediaStore,scan file .");
            MediaScannerConnection.scanFile(mContext, paths, null, null);
        }
    }

    public void scanPathforMediaStore(List<String> scanPaths) {
        Log.d(TAG, "scanPathforMediaStore,scanPaths.");
        int length = scanPaths.size();
        if (mContext != null && length > 0) {
            String[] paths;
            if (mDstFolder != null && length > SCAN_FOLDER_NUM) {
                paths = new String[] { mDstFolder };
            } else {
                paths = new String[length];
                scanPaths.toArray(paths);
            }

            Log.d(TAG, "scanPathforMediaStore, scanFiles.");
            MediaScannerConnection.scanFile(mContext, paths, null, null);
        }
    }

    /**
     * delete the record in MediaStore
     *
     * @param paths
     *            the delete file or folder in MediaStore
     */
    public void deleteFileInMediaStore(List<String> paths) {
        Log.d(TAG, "deleteFileInMediaStore.");
        Uri uri = MediaStore.Files.getContentUri("external");
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("?");
        for (int i = 0; i < paths.size() - 1; i++) {
            whereClause.append(",?");
        }
        String where = MediaStore.Files.FileColumns.DATA + " IN(" + whereClause.toString() + ")";
        // notice that there is a blank before "IN(".
        if (mContext != null && !paths.isEmpty()) {
            ContentResolver cr = mContext.getContentResolver();
            String[] whereArgs = new String[paths.size()];
            paths.toArray(whereArgs);
            Log.d(TAG, "deleteFileInMediaStore,delete.");
            try {
                cr.delete(uri, where, whereArgs);
            } catch (SQLiteFullException e) {
                Log.e(TAG, "Error, database or disk is full!!!" + e);
                if (mBaseAsyncTask != null) {
                    mBaseAsyncTask.cancel(true);
                }
            } catch (UnsupportedOperationException e) {
                Log.e(TAG, "Error, database is closed!!!");
                if (mBaseAsyncTask != null) {
                    mBaseAsyncTask.cancel(true);
                }
            }
        }
    }

    /**
     * delete the record in MediaStore
     *
     * @param path
     *            the delete file or folder in MediaStore
     */
    public void deleteFileInMediaStore(String path) {
        Log.d(TAG, "deleteFileInMediaStore,path =" + path);
        if (TextUtils.isEmpty(path)) {
            return;
        }
        Uri uri = MediaStore.Files.getContentUri("external");
        String where = MediaStore.Files.FileColumns.DATA + "=?";
        String[] whereArgs = new String[] { path };
        if (mContext != null) {
            ContentResolver cr = mContext.getContentResolver();
            Log.d(TAG, "deleteFileInMediaStore,delete.");
            try {
                if (true) {
                    cr.delete(uri, where, whereArgs);
                } else {
                    try {
                        cr.delete(uri, where, whereArgs);
                    } catch (UnsupportedOperationException e) {
                        Log.e(TAG, "Error, database is closed!!!");
                        if (mBaseAsyncTask != null) {
                            mBaseAsyncTask.cancel(true);
                        }
                    }
                }
            } catch (SQLiteFullException e) {
                Log.e(TAG, "Error, database or disk is full!!!" + e);
                if (mBaseAsyncTask != null) {
                    mBaseAsyncTask.cancel(true);
                }
            }
        }
    }
    public static void scanDir(Context context, File dir) {
        Log.i(TAG,"send broadcast to scan dir = " + dir);
        String path = dir.getPath();
        Intent intent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_DIR");
        Bundle bundle = new Bundle();
        bundle.putString("scan_dir_path", path);
        intent.putExtras(bundle);
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1){
           // intent.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        }
        context.sendBroadcast(intent);
    }
    /**
     * Set dstfolder so when scan files size more than SCAN_FOLDER_NUM use folder
     * path to make scanner scan this folder directly.
     *
     * @param dstFolder
     */
    public void setDstFolder(String dstFolder) {
        mDstFolder = dstFolder;
    }
    
    /**
    *  The following is defined for DataBase
    *  @author Xuehao.Jiang
    *  created at 2017/7/25 15:30
    */
    public static final Uri AUDIO_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    public static final Uri IMAGE_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    public static final Uri VIDEO_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    public static final Uri DOC_URI = MediaStore.Files.getContentUri("external");
    public static final Uri APK_URI = MediaStore.Files.getContentUri("external");
    public static final boolean DRMSWITCH = "true".equals(SystemPropertiesInvoke.getString("drm.service.enabled"));

    public  static  boolean isSupportDrm(){
        return DRMSWITCH;
    }
    public static final String DOC_SELECTION = MediaStore.Files.FileColumns.DATA + " like '%.txt' or " /*+  MediaStore.Files.FileColumns.DATA + " like '%.log' or "*/
            + MediaStore.Files.FileColumns.DATA + " like '%.doc' or " + MediaStore.Files.FileColumns.DATA + " like '%.pdf' or "
            + MediaStore.Files.FileColumns.DATA + " like '%.ppt' or " + MediaStore.Files.FileColumns.DATA + " like '%.pptx' or "
            + MediaStore.Files.FileColumns.DATA + " like '%.xls' or " + MediaStore.Files.FileColumns.DATA + " like '%.xlsx' or "
            +( isSupportDrm()  ?  (MediaStore.Files.FileColumns.DATA + " like '%.dcf' and " + MediaStore.Files.FileColumns.MIME_TYPE + " is 'text/plain' or ") : "")
            + MediaStore.Files.FileColumns.DATA + " like '%.docx'" ;
    public static  final String APK_SELECTION = ( isSupportDrm() ?
            ( MediaStore.Files.FileColumns.DATA + " like '%.dcf' and " + MediaStore.Files.FileColumns.MIME_TYPE + " is 'application/vnd.android.package-archive' or ") : "")
            + MediaStore.Files.FileColumns.DATA + " like '%.apk' ";

    public static final String[] DATA_PROJECTION = new String[]{MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.TITLE, MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.SIZE};


    public static  final int IMAGE_LOADER_ID = FileType.FILE_TYPE_IMAGE *10;
    public static  final int AUDIO_LOADER_ID = FileType.FILE_TYPE_AUDIO_DEFAULT *10;
    public static  final int VIDEO_LOADER_ID = FileType.FILE_TYPE_VIDEO_DEFAULT*10;
    public static  final int DOC_LOADER_ID = FileType.FILE_TYPE_DOC *10;
    public static  final int APK_LOADER_ID = FileType.FILE_TYPE_APK *10;


}