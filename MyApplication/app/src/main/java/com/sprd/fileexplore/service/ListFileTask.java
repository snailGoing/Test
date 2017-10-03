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

import android.content.Context;
import android.util.Log;


import com.sprd.fileexplore.file.FileInfo;
import com.sprd.fileexplore.file.FileInfoManager;
import com.sprd.fileexplore.file.FileType;
import com.sprd.fileexplore.service.FileManageService.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


class ListFileTask extends BaseAsyncTask {
    private static final String TAG = "ListFileTask";
    private final String mPath;
    private final int mFilterType;
    private Context mContext;
    private static final int FIRST_NEED_PROGRESS = 250;
    private static final int NEXT_NEED_PROGRESS = 200;

    /**
     * Constructor for ListFileTask, construct a ListFileTask with certain
     * parameters
     *
     * @param fileInfoManager a instance of FileInfoManager, which manages
     *            information of files in FileInfoManager.
     * @param operationEvent a instance of OperationEventListener, which is a
     *            interface doing things before/in/after the task.
     * @param path ListView will list files included in this path.
     * @param filterType to determine which files will be listed.
     */
    public ListFileTask(Context context, FileInfoManager fileInfoManager,
            OperationEventListener operationEvent, String path, int filterType) {
        super(fileInfoManager, operationEvent);
        mContext = context;
        mPath = path;
        mFilterType = filterType;
    }

    @Override
    protected Integer doInBackground(Void... params) {

        synchronized (mContext.getApplicationContext()) {

            File[] files = null;
            int total = 0;
            int progress = 0;
            long startLoadTime = System.currentTimeMillis();
            Log.d(TAG, "doInBackground path = " + mPath);
            final boolean isRootPath = MountRootManagerRf.getInstance().isRootPath(mPath);
            if (isRootPath) {
                MountRootManagerRf.getInstance().updateMountPointSpaceInfo();
            }

            File dir = new File(mPath);
            if (dir.exists()) {
                files = dir.listFiles();
                if (files == null) {
                    Log.w(TAG, "doInBackground,directory is null");

                    return OperationEventListener.ERROR_CODE_UNSUCCESS;
                }
            } else {
                Log.w(TAG, "doInBackground,directory is not exist.");

                return OperationEventListener.ERROR_CODE_UNSUCCESS;
            }
            total = files.length;
            long loadTime = 0;
            int nextUpdateTime = FIRST_NEED_PROGRESS;
            Log.d(TAG, "doInBackground, total = " + total);
            for (int i = 0; i < files.length; i++) {
                if (isCancelled()) {
                    Log.w(TAG, " doInBackground,calcel.");

                    return OperationEventListener.ERROR_CODE_UNSUCCESS;
                }

                if (mFilterType == FileManageService.FILE_FILTER_TYPE_DEFAULT) {
                    if (files[i].getName().startsWith(".")) {
                        Log.i(TAG, " doInBackground,start with.,contine.");
                        continue;
                    }
                }

                if (mFilterType == FileManageService.FILE_FILTER_TYPE_FOLDER) {
                    if (!files[i].isDirectory()) {
                        Log.i(TAG, " doInBackground,is not directory,continue..");
                        continue;
                    }
                }
                FileInfo file = new FileInfo(files[i]);
                String path = file.getPath();
                boolean isVideo= FileType.getInstance().isVideoFileType(path);
                boolean isAudio= FileType.getInstance().isAudioFileType(path);
                if(isVideo){
                    file.setFileIcon(FileType.FILE_TYPE_VIDEO_DEFAULT);
                }else if(isAudio){
                    file.setFileIcon(FileType.FILE_TYPE_AUDIO_DEFAULT);
                }else{
                    file.setFileIcon(FileType.getInstance().getFileType(new File(path)));
                }

                mFileInfoManager.addItem(file);
                loadTime = System.currentTimeMillis() - startLoadTime;
                progress++;

                if (loadTime > nextUpdateTime) {
                    startLoadTime = System.currentTimeMillis();
                    nextUpdateTime = NEXT_NEED_PROGRESS;
                    Log.d(TAG, "doInBackground,pulish progress.");
                    publishProgress(new ProgressInfo("", progress, total, progress, total));

                }
            }
            Log.d(TAG, "doInBackground ERROR_CODE_SUCCESS");

            return OperationEventListener.ERROR_CODE_SUCCESS;
        }
    }
}
