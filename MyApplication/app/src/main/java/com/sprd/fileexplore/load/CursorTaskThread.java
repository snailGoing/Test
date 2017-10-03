package com.sprd.fileexplore.load;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import com.sprd.fileexplore.file.FileInfo;
import com.sprd.fileexplore.file.FileInfoComparator;
import com.sprd.fileexplore.file.FileType;
import com.sprd.fileexplore.util.FileUtil;
import com.sprd.fileexplore.util.LogUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Xuehao.Jiang on 2017/4/7.
 */

public class CursorTaskThread  extends HandlerThread implements Callback {

    private static final String TASK_THREAD = "CursorTaskThread : FileExplore ...";
    /**
     *  mCurThreadHandler  means this current thread
     *  mParThreadHandler  means parent thread or other thread
     * */
    private Handler mCurThreadHandler;
    private Handler mParThreadHandler;
    private ArrayList<FileInfo> mFileListData = new ArrayList<FileInfo>();
    private ArrayList<FileInfo> mAcceptList = new ArrayList<FileInfo>();
    private ArrayList<FileInfo> mWorkingList= new ArrayList<FileInfo>();
    private List<FileInfo> mTempWorkingList;
    private int mPathColumnIndex;
    private int mMimeTypeCoclumnIndex;
    private  View mCurrentView;
    private int mLoaderId =-1;
    private SharedPreferences sortSpf ;

    public Cursor getCursor() {
        return mCursor;
    }

    public void setCursor(Cursor mCursor) {
        this.mCursor = mCursor;
    }

    private Cursor mCursor;

    private static final int MESSAGE_OBTAIN_CURSOR_DATA = 0;
    private static final int MESSAGE_SORT_CURSOR_DATA = 1;
    private static final int MESSAGE_REQUEST_CURSOR_DATA = 2;
    public static final int MESSAGE_REQUEST_CURSOR_DONE = 3;
    public static final int MESSAGE_REFRESH = 4;

    public View getCurrentView() {
        return mCurrentView;
    }

    public void setCurrentView(View mCurrentView) {
        this.mCurrentView = mCurrentView;
    }

    public int getLoaderId() {
        return mLoaderId;
    }

    public void setLoaderId(int mLoaderId) {
        this.mLoaderId = mLoaderId;
    }

    public CursorTaskThread(Handler mParentThreadHandler, int loaderId,SharedPreferences sp){
        super(TASK_THREAD);
        this.mLoaderId = loaderId;
        this.mParThreadHandler =  mParentThreadHandler;
        sortSpf = sp;
    }

    @Override
    public boolean handleMessage(Message msg) {

        if (msg==null) return  true;
        switch (msg.what) {
            case MESSAGE_OBTAIN_CURSOR_DATA:
                obtainCursorFileInfoList((Cursor)msg.obj);
                break;
            case MESSAGE_SORT_CURSOR_DATA:
                sortCursorFileInfoList();
                break;
            // to do some  consumed time things
            //
        }
        return true;
    }

    public void requestCursorData(Cursor mCursor) {
        if (mCurThreadHandler == null) {
            mCurThreadHandler = new Handler(getLooper(), this);
        }
        this.mCursor = mCursor;
        mCurThreadHandler.sendMessage(mCurThreadHandler.obtainMessage( MESSAGE_OBTAIN_CURSOR_DATA, mCursor));
    }
    public void obtainCursorFileInfoList(Cursor cursor){
        mAcceptList.clear();
        if (cursor != null && cursor.isClosed()) {
            //mParThreadHandler.sendEmptyMessage(MESSAGE_REFRESH);
            return;
        }
        synchronized (mAcceptList) {
            if (cursor != null && cursor.getCount()> 0) {
                mPathColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                mMimeTypeCoclumnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);
                while (cursor.moveToNext()) {
                    String path = cursor.getString(mPathColumnIndex);
                    String mimeType = cursor.getString(mMimeTypeCoclumnIndex);
                    FileInfo file = new FileInfo(path);
                    boolean isVideo= FileType.getInstance().isVideoFileType(path);
                    boolean isAudio= FileType.getInstance().isAudioFileType(path);
                    if(isVideo){
                        file.setFileIcon(FileType.FILE_TYPE_VIDEO_DEFAULT);
                    }else if(isAudio){
                        file.setFileIcon(FileType.FILE_TYPE_AUDIO_DEFAULT);
                    }else{
                        file.setFileIcon(FileType.getInstance().getFileType(new File(path)));
                    }

                    file.setMimeTpye(mimeType);
                    mAcceptList.add(file);
                }
            }
            mWorkingList.clear();
            mWorkingList = (ArrayList<FileInfo>) mAcceptList.clone();
            mCurThreadHandler.sendEmptyMessage(MESSAGE_SORT_CURSOR_DATA);
        }
    }

    public void sortCursorFileInfoList(){
        ArrayList<FileInfo> invalidFiles = new ArrayList<FileInfo>();
        synchronized (mWorkingList) {
            boolean isShowHide = false ;
            for (FileInfo f: mWorkingList) {
                if (!FileUtil.isValid(f, isShowHide)) {
                    if (null != f) {
                        invalidFiles.add(f);
                    }
                }
            }
            if (!invalidFiles.isEmpty()) {
                for(FileInfo file:invalidFiles) {
                    mWorkingList.remove(file);
                }
            }
            mTempWorkingList = (ArrayList<FileInfo>) mWorkingList.clone();
            mFileListData.clear();
            for (FileInfo f : mTempWorkingList) {
                if (f.isFolder()) {
                    continue;
                }
                mFileListData.add(f);
            }

            int sortType = sortSpf.getInt(FileInfoComparator.SORT_KEY,FileInfoComparator.SORT_BY_NAME);
            Collections.sort(mFileListData, FileInfoComparator.getInstance(sortType));
            LogUtil.d(" mFileListData size = "+mFileListData.size() +" sortType="+sortType);
            mParThreadHandler.sendMessage(mParThreadHandler.obtainMessage(MESSAGE_REQUEST_CURSOR_DONE,mLoaderId));
        }
    }
    public ArrayList<FileInfo> getFileInfoList(){
        return  mFileListData;
    }

}
