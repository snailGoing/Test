package com.sprd.fileexplore.adapter;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.sprd.fileexplore.file.FileInfo;
import com.sprd.fileexplore.fragment.BaseFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xuehao.Jiang on 2017/4/8.
 */

public class QuickScanAdapterSprd extends  BaseRecyclerViewAdapter {

    public QuickScanAdapterSprd(Context context, RecyclerItemClickListener itemClickListener){
        super(context,itemClickListener);
    }
    public QuickScanAdapterSprd(Context context, RecyclerItemClickListener itemClickListener, Handler handler, BaseFragment fragment){
        super(context,itemClickListener,handler,fragment);
    }

    /**
     *  This method gets the first item checked.
     *  @author Xuehao.Jiang
     *  created at 2017/7/8 10:56
     */
    public FileInfo getFirstCheckedFileInfoItem(){
        List<FileInfo> list = getCheckedFileInfoItemsList();
        int size =list .size();
        if(size>0){
            return list.get(0);
        }
        return null;
    }
    /**
     *  This method return all the files selected by us
     *  @author Xuehao.Jiang
     *  created at 2017/7/7 22:05
     */
    public  List<FileInfo> getCheckedFileInfoItemsList(){

        List<FileInfo> fileInfoCheckedList = new ArrayList<FileInfo>();
        int count = getItemCount();
        for(int i=0; i < count; i++){
            boolean isContain = mSelectedItemsIds.get(i);
            if(isContain){
                fileInfoCheckedList.add(mFileInfoList.get(i));
            }
        }
        Log.d(TAG,"getCheckedFileInfoItemsList: --> size="+fileInfoCheckedList.size()
                +" save checked size= "+ mSelectedItemsIds.size());
        return fileInfoCheckedList;
    }
    public int getPosition(FileInfo fileInfo) {
        return mFileInfoList.indexOf(fileInfo);
    }
    public void selectOrCancellAll(){
        int count = getItemCount();
        int selectCount = getSelectedCount();

        if( count >0 && selectCount < count){
            mSelectedItemsIds.clear();
            for(int i=0;i<count;i++){
                mSelectedItemsIds.put(i,true);
            }
        }else if(count >0  && selectCount == count){
            mSelectedItemsIds.clear();
        }
        notifyDataSetChanged();
    }
}
