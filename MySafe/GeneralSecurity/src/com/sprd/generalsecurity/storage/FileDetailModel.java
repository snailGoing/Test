package com.sprd.generalsecurity.storage;

/**
 * Created by SPREADTRUM\bo.yan on 17-5-15.
 */

public class FileDetailModel {
    private String filePath;
    private long fileSize;


    public FileDetailModel(){}

    public FileDetailModel(String path, long size){
        this.filePath = path;
        this.fileSize = size;
    }

    public void setFilePath(String path){
        this.filePath = path;
    }

    public void setFileSize(long size){
        this.fileSize = size;
    }

    public String getFilePath(){
        return this.filePath;
    }

    public long getFileSize(){
        return this.fileSize;
    }

}
