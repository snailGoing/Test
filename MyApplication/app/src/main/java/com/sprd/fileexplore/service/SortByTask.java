package com.sprd.fileexplore.service;

import android.util.Log;

import com.sprd.fileexplore.file.FileInfo;
import com.sprd.fileexplore.file.FileInfoManager;
import com.sprd.fileexplore.service.FileManageService.OperationEventListener;

/**
 * Created by Xuehao.Jiang on 2017/7/7.
 */

public class SortByTask extends BaseAsyncTask {


    private int sortType;
    /**
     * Constructor of BaseAsyncTask
     *
     * @param fileInfoManager a instance of FileInfoManager, which manages information of files in
     * @param listener        a instance of OperationEventListener, which is a interface doing things
     * @param sortType        a sort type ,sortType
     */
    public SortByTask(FileInfoManager fileInfoManager, OperationEventListener listener,int sortType){
        super(fileInfoManager,listener);
        this.sortType  = sortType;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        // this will do sort task
        mFileInfoManager.sort(sortType);
        return OperationEventListener.ERROR_CODE_SUCCESS;
    }
}
