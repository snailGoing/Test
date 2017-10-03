package com.sprd.fileexplore.service;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.sprd.fileexplore.file.FileInfo;
import com.sprd.fileexplore.file.FileInfoManager;
import com.sprd.fileexplore.file.FileType;
import com.sprd.fileexplore.service.FileManageService.OperationEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xuehao.Jiang on 2017/7/22.
 */

public class MultiSearchTask extends BaseAsyncTask {


    private static final String TAG = "MultiSearchTask";
    private String mSearchName;
    private List<String> mPaths = new ArrayList<>();
    private final ContentResolver mContentResolver;
    private ArrayList<Integer> mSearchType;

    public MultiSearchTask(FileInfoManager fileInfoManager, OperationEventListener operationEvent,
                           String searchName,  List<String> path,ArrayList<Integer> mSearchType,
                           Context context) {
        super(fileInfoManager, operationEvent);
        mContentResolver = context.getContentResolver();
        mPaths = path;
        mSearchName = searchName;
        this.mSearchType = mSearchType;
    }

    @Override
    protected Integer doInBackground(Void... params) {

        int ret = OperationEventListener.ERROR_CODE_SUCCESS;

        Uri uri = MediaStore.Files.getContentUri("external");
        String[] projection = { MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.MIME_TYPE };

        String selection = "(" + MediaStore.Files.FileColumns.DATA
                + " like '%" + mSearchName + "%' )";

        if (mPaths.size() == 1) {
            selection += " AND " + MediaStore.Files.FileColumns.DATA
                    + " like '" + mPaths.get(0) + "%'";
        }
        selection += " COLLATE NOCASE";

        Cursor cursor = null;
        Log.d(TAG," doInBackground ----> selection="+selection);
        try{
            cursor = mContentResolver.query(uri, projection, selection, null, null);
        }catch(Exception e){
            Log.e(TAG,"query database unkown exception");
        }
        if (cursor == null) {
            Log.d(TAG, "doInBackground, cursor is null.");
            return OperationEventListener.ERROR_CODE_UNSUCCESS;
        }
        if (cursor.getCount() == 0) {
            cursor.close();
            ret = OperationEventListener.ERROR_CODE_SUCCESS;
            return ret;
        }
        mSearchName = mSearchName.toLowerCase();
        int count =0;
        while (cursor.moveToNext()) {
            String filePath = cursor.getString(0);
            String mimeType = cursor.getString(1);

            FileInfo file = new FileInfo(filePath);

            int fileType = FileType.getInstance().getBasicFileType(mimeType,file);
            String path = file.getPath();

            if (mSearchType != null && !mSearchType.isEmpty()) {
                 if (mSearchType.contains(fileType)) {
                    if ((file.getName().toLowerCase().contains(mSearchName))) {

                        if( mimeType!=null && mimeType.startsWith("video/")){
                            file.setFileIcon(FileType.FILE_TYPE_VIDEO_DEFAULT);
                        }else if(mimeType!=null && mimeType.startsWith("audio/")){
                            file.setFileIcon(FileType.FILE_TYPE_AUDIO_DEFAULT);
                        }else{
                            file.setFileIcon(FileType.getInstance().getFileType(new File(path)));
                        }

                        mFileInfoManager.addItem(file);
                        count++;
                    }
                }
            }
        }
        Log.d(TAG,"   count="+count);
        cursor.close();
        ret = OperationEventListener.ERROR_CODE_SUCCESS;
        return ret;
    }
}
