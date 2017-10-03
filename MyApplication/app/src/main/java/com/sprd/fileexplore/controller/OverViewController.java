package com.sprd.fileexplore.controller;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.sprd.fileexplore.R;
import com.sprd.fileexplore.adapter.QuickScanAdapterSprd;
import com.sprd.fileexplore.file.FileInfo;
import com.sprd.fileexplore.file.FileType;
import com.sprd.fileexplore.load.ThreadPollManager;
import com.sprd.fileexplore.util.FileUtil;
import com.sprd.fileexplore.util.LogUtil;
import com.sprd.fileexplore.view.CircleProgressBar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xuehao.Jiang on 2017/4/10.
 */

public class OverViewController {

    private Menu menu;
    MenuItem sortItem;
    MenuItem selectItem;
    MenuItem installItem;
    MenuItem settingItem;

    // category view
    private View mCategoryViewContainer;
    private View internalView;
    private CircleProgressBar internalCircleBar;
    private TextView internalTextView;

    private View internalExternalView;
    private CircleProgressBar iternalExternalCircleBar;
    private TextView internalExternalTextView;

    private CircleProgressBar phoneCircleBar;
    private  TextView phoneTextView;

    // detail recycle view
    private View mOverViewContainer;
    private TextView mPath;
    private RecyclerView mRecyclerView;
    private View mLoadingView;
    private View mEmptyView;
    private QuickScanAdapterSprd mRecycleViewAdapter;

    private Context mContext;
    private List<FileInfo> mCurImageFileInfo = new ArrayList<FileInfo>();
    private List<FileInfo> mCurAudioFileInfo = new ArrayList<FileInfo>();
    private List<FileInfo> mCurVideoFileInfo = new ArrayList<FileInfo>();
    private List<FileInfo> mCurDocFileInfo = new ArrayList<FileInfo>();
    private List<FileInfo> mCurApkFileInfo = new ArrayList<FileInfo>();

    /* 只有第一次启动应用时，doc和apk数据量大时，查询较慢，进入到具体分类后，该数据可能还未获得，需要显示“正在加载”;
 *  其他情况有数据，不需要显示正在加载
 */
    private boolean isImageHasUpdated; // if true, means image datas have ever changed from cursorLoader, so no need "loading" display
    private boolean isAudioHasUpdated; // if true, means audio datas have ever changed from cursorLoader, so no need "loading" display
    private boolean isVideoHasUpdated; // if true, means video datas have ever changed from cursorLoader, so no need "loading" display
    private boolean isDocHasUpdated;   // if true, means doc datas have ever changed from cursorLoader, so no need "loading" display
    private boolean isApkHasUpdated;   // if true, means apk datas have ever changed from cursorLoader, so no need "loading" display

    boolean isUpdateImage; // if true ,means category item has been clicked and enter image list UI, or false
    boolean isUpdateAudio; // if true ,means category item has been clicked and enter audio list UI, or false
    boolean isUpdateVideo; // if true ,means category item has been clicked and enter video list UI, or false
    boolean isUpdateDoc;   // if true ,means category item has been clicked and enter doc list UI, or false
    boolean isUpdateApk;   // if true ,means category item has been clicked and enter apk list UI, or false

    public boolean isRootStorageUI() {
        return isRootStorageUI;
    }

    public void setRootStorageUI(boolean rootStorageUI) {
        isRootStorageUI = rootStorageUI;
    }

    private boolean isRootStorageUI = false;

    public OverViewController(View layout, Context context,QuickScanAdapterSprd quickScanAdapterSprd){

        mContext= context;

        mCategoryViewContainer = layout.findViewById(R.id.over_view_category);

        mOverViewContainer = layout.findViewById(R.id.over_view_list);
        mPath = (TextView)mOverViewContainer.findViewById(R.id.detailed_path);
        mLoadingView = mOverViewContainer.findViewById(R.id.loading_recycle_view);
        mEmptyView = mOverViewContainer.findViewById(R.id.empty_recycle_view);
        mRecyclerView = (RecyclerView)mOverViewContainer.findViewById(R.id.recycle_view);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));

        //  new GridLayoutManager(context,columNum);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
       // mRecyclerView.setLayoutManager(new GridLayoutManager(mContext,2));
        mRecycleViewAdapter = quickScanAdapterSprd; //new QuickScanAdapterSprd(mContext, recyclerItemClickListener);
        mRecyclerView.setAdapter(mRecycleViewAdapter);


        phoneCircleBar = (CircleProgressBar) mCategoryViewContainer.findViewById(R.id.internal_storage_bar);
        phoneTextView = (TextView) mCategoryViewContainer.findViewById(R.id.progress_bar_text);

        internalView = mCategoryViewContainer.findViewById(R.id.internal_pro_bar);
        internalExternalView = mCategoryViewContainer.findViewById(R.id.internal_external_bar);

        internalCircleBar = (CircleProgressBar) mCategoryViewContainer.findViewById(R.id.internal_bar);
        internalTextView= (TextView) mCategoryViewContainer.findViewById(R.id.progress_bar_text1);
        iternalExternalCircleBar= (CircleProgressBar) mCategoryViewContainer.findViewById(R.id.external_bar);
        internalExternalTextView= (TextView) mCategoryViewContainer.findViewById(R.id.progress_bar_text2);

        phoneCircleBar.setOnClickListener(mOnclickListener);
        internalCircleBar.setOnClickListener(mOnclickListener);


        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(mRecycleViewAdapter!=null&& mRecycleViewAdapter.imageLoader!=null){
                    switch (newState){
                        case 0:
                            mRecycleViewAdapter.imageLoader.resume();
                            break;
                        case 1:   // 1 means screnn is scrolling with touch by hand or hand in touch with screen
                        case 2:   // 2 means because of auto scrolling by operating
                            mRecycleViewAdapter.imageLoader.pause();
                            break;
                    }
                }
                scrollStateChanged(recyclerView,newState);

            }
        });

    }
    public void scrollStateChanged(RecyclerView recyclerView, int newState) {
        if (null == recyclerView || mRecycleViewAdapter == null) {
            return;
        }
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager linearManager = (LinearLayoutManager) layoutManager;
            int endPos = linearManager.findLastVisibleItemPosition();
            int startPos = linearManager.findFirstVisibleItemPosition();
            Log.d("xuehao"," scrollStateChanged--->   startPos= "+startPos+" endPos= "+endPos);
            ArrayList<String> newTaskUrl = new ArrayList<String>();

            int len = mRecycleViewAdapter.getItemCount();
            int pos = -1;
            FileInfo file = null;
            for (pos = startPos; pos <= endPos && pos < len; pos++) {
                file = mRecycleViewAdapter.getCurrentPosFileInfo(pos);
                if (file != null) {
                    newTaskUrl.add(file.getPath());
                }
            }
            /*
            *  firstly,need to update scroll state and notify ThreadPoolManager to remove unused task
             */
            if (0 == newState) {
                mRecycleViewAdapter.setScrolling(false);

                mRecycleViewAdapter.notifyDataSetChanged();


            } else {
                mRecycleViewAdapter.setScrolling(true);
            }
            ThreadPollManager.getInstance().updateWorkQueue(newTaskUrl,mRecycleViewAdapter.mScrolling);
        }

    }
    private View.OnClickListener mOnclickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
             Intent mIntent = new Intent( );
            ComponentName cm = new ComponentName("com.android.settings", "com.android.settings.Settings$StorageSettingsActivity");
            mIntent.setComponent(cm);
            mIntent.setAction(Intent.ACTION_VIEW);
            if(mContext!= null) mContext.startActivity(mIntent);
        }
    };
    public void setOverViewCategoryShow(boolean isShow){
        if(isShow){
            mCategoryViewContainer.setVisibility(View.VISIBLE);
            mOverViewContainer.setVisibility(View.GONE);
        }else{
            mCategoryViewContainer.setVisibility(View.GONE);
            mOverViewContainer.setVisibility(View.VISIBLE);
        }
    }

    public void setmPath(String path){
        if(mPath!=null){
            mPath.setVisibility(View.VISIBLE);
            mPath.setText(path);
        }
    }
    public void showLoadView(){
            mLoadingView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.GONE);
            //mPath.setVisibility(View.GONE);
    }

    public void showEmptyView(){
        mLoadingView.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
        //mPath.setVisibility(View.GONE);
    }
    public void showRecycleView(){
        mLoadingView.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
        mPath.setVisibility(View.VISIBLE);
    }


    public void setImageFileInfoList(List<FileInfo> mFileInfo){
        mCurImageFileInfo = mFileInfo;
        isImageHasUpdated = true;
        LogUtil.d(" setImageFileInfoList: isUpdateImage="+isUpdateImage);
        updateOverViewUi(isUpdateImage,mFileInfo );
    }
    public void setAudioFileInfoList(List<FileInfo> mFileInfo){
        mCurAudioFileInfo = mFileInfo;
        isAudioHasUpdated =true;
        updateOverViewUi(isUpdateAudio,mFileInfo );
    }
    public void setVideoFileInfoList(List<FileInfo> mFileInfo){
        mCurVideoFileInfo = mFileInfo;
        isVideoHasUpdated =true;
        updateOverViewUi(isUpdateVideo,mFileInfo );
    }
    public void setDocFileInfoList(List<FileInfo> mFileInfo){
        mCurDocFileInfo = mFileInfo;
        isDocHasUpdated =true;
        updateOverViewUi(isUpdateDoc,mFileInfo );
    }
    public void setApkFileInfoList(List<FileInfo> mFileInfo){
        mCurApkFileInfo = mFileInfo;
        isApkHasUpdated =true;
        if(isUpdateApk){
            if(mFileInfo!=null && mFileInfo.size()>0){
                showRecycleView();
                showInstallAllEnabled();
                showSelectSortEnabled();
                mRecycleViewAdapter.setFileInfoList(mFileInfo);
                mRecycleViewAdapter.notifyDataSetChanged();
            }else{
                showInstallAllDisabled();
                showSelectSortDisabled();
                showEmptyView();
            }
        }
    }

    public void updateOverViewUi(boolean isUpdate, List<FileInfo> mFileInfo){
        if(isUpdate){
            if(mFileInfo!=null && mFileInfo.size()>0){
                showRecycleView();
                showSelectSortEnabled();
                mRecycleViewAdapter.setFileInfoList(mFileInfo);
                mRecycleViewAdapter.notifyDataSetChanged();
            }else{
                showSelectSortDisabled();
                showEmptyView();
            }
        }
    }

    public  void setUpdateImage(){
        if(!isImageHasUpdated){
            showLoadView();
            showSelectSortDisabled();
        }else if(mCurImageFileInfo.size()==0){
            showEmptyView();
            showSelectSortDisabled();

        }else{
            showSelectSortEnabled();
            showRecycleView();
            mRecycleViewAdapter.setFileInfoList(mCurImageFileInfo);
            mRecycleViewAdapter.notifyDataSetChanged();
        }

        isUpdateImage = true;
        LogUtil.d(" setUpdateImage: isUpdateImage="+isUpdateImage);
        isUpdateAudio = false;
        isUpdateVideo = false;
        isUpdateDoc = false;
        isUpdateApk = false;
    }
    public void setUpdateAudio(){
        if(!isAudioHasUpdated){
            showLoadView();
            showSelectSortDisabled();
        }else if(mCurAudioFileInfo.size()==0){
            showEmptyView();
            showSelectSortDisabled();
        }else{
            showRecycleView();
            showSelectSortEnabled();
            mRecycleViewAdapter.setFileInfoList(mCurAudioFileInfo);
            mRecycleViewAdapter.notifyDataSetChanged();
        }

        isUpdateImage = false;
        isUpdateAudio = true;
        isUpdateVideo = false;
        isUpdateDoc = false;
        isUpdateApk = false;
    }
    public  void setUpdateVideo(){
        if(!isVideoHasUpdated){
            showLoadView();
            showSelectSortDisabled();
        }else if(mCurVideoFileInfo.size()==0){
            showEmptyView();
            showSelectSortDisabled();
        }else{
            showSelectSortEnabled();
            showRecycleView();
            mRecycleViewAdapter.setFileInfoList(mCurVideoFileInfo);
            mRecycleViewAdapter.notifyDataSetChanged();
        }

        isUpdateImage = false;
        isUpdateAudio = false;
        isUpdateVideo = true;
        isUpdateDoc = false;
        isUpdateApk = false;
    }
    public  void setUpdateDoc(){
        LogUtil.d(" setUpdateDoc : isDocHasUpdated="+isDocHasUpdated+"   size="+ mCurDocFileInfo.size() );
        if(!isDocHasUpdated){
            showLoadView();
            showSelectSortDisabled();
        }else if(mCurDocFileInfo.size()==0){
            showEmptyView();
            showSelectSortDisabled();
        }else{
            showSelectSortEnabled();
            showRecycleView();
            mRecycleViewAdapter.setFileInfoList(mCurDocFileInfo);
            mRecycleViewAdapter.notifyDataSetChanged();
        }
        isUpdateImage = false;
        isUpdateAudio = false;
        isUpdateVideo = false;
        isUpdateDoc = true;
        isUpdateApk = false;
    }
    public  void setUpdateApk(){
        if(!isApkHasUpdated){
            showLoadView();
            showSelectSortDisabled();
        }else if(mCurApkFileInfo.size()==0){
            showEmptyView();
            showInstallAllDisabled();
            showSelectSortDisabled();
        }else{
            showInstallAllEnabled();
            showSelectSortEnabled();
            showRecycleView();
            mRecycleViewAdapter.setFileInfoList(mCurApkFileInfo);
            mRecycleViewAdapter.notifyDataSetChanged();
        }
        isUpdateImage = false;
        isUpdateAudio = false;
        isUpdateVideo = false;
        isUpdateDoc = false;
        isUpdateApk = true;
    }

    public Menu getMenu() {
        return menu;
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
        sortItem = menu.findItem(R.id.menu_sort_by);
        selectItem = menu.findItem(R.id.menu_select_more);
        installItem = menu.findItem(R.id.menu_install_all);
        settingItem = menu.findItem(R.id.menu_settings);
    }

    public void setSelectSortMenuShow(boolean isShow){
        if(isShow){
            if(selectItem!=null) selectItem.setVisible(true);
            if(sortItem!=null) sortItem.setVisible(true);
        }else{
            if(selectItem!=null) selectItem.setVisible(false);
            if(sortItem!=null) sortItem.setVisible(false);
        }
    }

    public void setInstallAllMenuShow(boolean isShow){
        if(isShow){
            if(installItem!= null) installItem.setVisible(true);
        }else{
            if(installItem!= null) installItem.setVisible(false);
        }
    }
    public  void setSelectMoreMenuEnabled(boolean isEnabled){
        if(isEnabled){
            if(selectItem!=null) selectItem.setEnabled(true);
        }else{
            if(selectItem!=null) selectItem.setEnabled(false);
        }
    }
    public void setSortByMenuEnabled(boolean isEnabled){
        if(isEnabled){
            if(sortItem!=null) sortItem.setEnabled(true);
        }else{
            if(sortItem!=null) sortItem.setEnabled(false);
        }
    }
    public void setInstallAllMenuEnabled(boolean isEnabled){
        if(isEnabled){
            if(installItem!=null) installItem.setEnabled(true);
        }else{
            if(installItem!=null) installItem.setEnabled(false);
        }
    }

    public void showSelectSortEnabled(){
        setSelectSortMenuShow(true);
        setSelectMoreMenuEnabled(true);
        setSortByMenuEnabled(true);
    }
    public void showSelectSortDisabled(){
        setSelectSortMenuShow(true);
        setSelectMoreMenuEnabled(false);
        setSortByMenuEnabled(false);
    }
    public void showInstallAllEnabled(){
        setInstallAllMenuShow(true);
        setInstallAllMenuEnabled(true);
    }
    public void showInstallAllDisabled(){
        setInstallAllMenuShow(true);
        setInstallAllMenuEnabled(false);
    }

    public void showSettingMenu(boolean isShow){
        if(isShow ){
            if(settingItem!=null) settingItem.setVisible(isShow);
        }else{
            if(settingItem!=null) settingItem.setVisible(isShow);
        }
    }

    public void refreshMenu(){

        LogUtil.d(" isUpdateImage:  "+ isUpdateImage+"  size= "+ mCurImageFileInfo.size() );
        LogUtil.d(" isUpdateAudio: "+ isUpdateAudio+"  size= "+ mCurAudioFileInfo.size() );
        LogUtil.d(" isUpdateVideo: "+ isUpdateVideo+"  size= "+ mCurVideoFileInfo.size() );
        LogUtil.d(" isUpdateDoc: "+ isUpdateDoc+"  size= "+ mCurDocFileInfo.size() );
        LogUtil.d(" isUpdateApk: "+ isUpdateApk+"  size= "+ mCurApkFileInfo.size() );


        LogUtil.d("selectItem "+ (selectItem==null ? "null":"no-null") );
        LogUtil.d("sortItem "+ (sortItem==null ? "null":"no-null") );
        LogUtil.d("settingItem "+ (settingItem==null ? "null":"no-null") );
        LogUtil.d("installItem "+ (installItem==null ? "null":"no-null") );
        if(isUpdateImage){
            if(mCurImageFileInfo.size()>0){
                showSelectSortEnabled();
            }else{
                showSelectSortDisabled();
            }
            showSettingMenu(false);
        }else if(isUpdateVideo){
            if(mCurVideoFileInfo.size()>0){
                showSelectSortEnabled();
            }else{
                showSelectSortDisabled();
            }
            showSettingMenu(false);
        }else if(isUpdateDoc){
            if(mCurDocFileInfo.size()>0){
                showSelectSortEnabled();
            }else{
                showSelectSortDisabled();
            }
            showSettingMenu(false);
        }else if(isUpdateAudio){
            if(mCurAudioFileInfo.size()>0){
                showSelectSortEnabled();
            }else{
                showSelectSortDisabled();
            }
            showSettingMenu(false);
        }else if(isUpdateApk){
            if(mCurApkFileInfo.size()>0){
                showSelectSortEnabled();
                showInstallAllEnabled();
            }else{
                showInstallAllDisabled();
                showSelectSortDisabled();
            }
            showSettingMenu(false);
        }
//        LogUtil.d("  invalidateOptionsMenu--------> before select  "+selectItem.isVisible()+" setting : "+ settingItem.isVisible());
//        ((Activity)mContext).invalidateOptionsMenu();
//        LogUtil.d("  invalidateOptionsMenu--------> after select "+selectItem.isVisible()+" setting : "+ settingItem.isVisible());
    }
    public void resetUpdateStatus(){
        isUpdateImage = false;
        isUpdateVideo =false;
        isUpdateDoc =false;
        isUpdateAudio = false;
        isUpdateApk =false;
    }

    public  void showInternalView(boolean isShow){
        if(isShow){
            internalView.setVisibility(View.VISIBLE);
            internalExternalView.setVisibility(View.GONE);
        }else{
            internalView.setVisibility(View.GONE);
            internalExternalView.setVisibility(View.VISIBLE);
        }
    }

    public void setStorageStatus(FileInfo sd,FileInfo phone,  Context mContext ){

        int internalProgress = 0;
        int externalProgress = 0;
        String avali1="";
        String total1="";
        String avalia2="";
        String total2="";
        String phoneName="";
        String sdName="";
        if(phone!=null){
            internalProgress= FileUtil.getUsedPercent(phone.getFile());
            avali1 = FileUtil.getUsedSize(mContext,phone.getFile());
            total1 = FileUtil.getTotalSize(mContext,phone.getFile());
            phoneName = phone.getName();
        }
        if(sd != null){
            externalProgress = FileUtil.getUsedPercent(sd.getFile());
            avalia2 = FileUtil.getUsedSize(mContext,sd.getFile());
            total2 = FileUtil.getTotalSize(mContext,sd.getFile());
            sdName = sd.getName();
        }

        phoneCircleBar.setProgress(internalProgress);
        phoneCircleBar.setProgressTextSig(phoneName);
        phoneTextView.setText(avali1+"/"+total1);

        internalCircleBar.setProgress(internalProgress);
        internalCircleBar.setProgressTextSig(phoneName);
        internalTextView.setText(avali1+"/"+total1);

        iternalExternalCircleBar.setProgress(externalProgress);
        iternalExternalCircleBar.setProgressTextSig(sdName);
        internalExternalTextView.setText(avalia2+"/"+total2);
    }

    public void updateUI(int type){
        String str ="";
        if(mContext != null ){
            str = mContext.getString(R.string.file_category_title);
        }
        switch (type){
            case FileType.FILE_TYPE_AUDIO_DEFAULT:
                setOverViewCategoryShow(false);
                setUpdateAudio();
                showSettingMenu(false);
                setmPath( str +" / "+ mContext.getString(R.string.quickscan_audio));
                break;
            case  FileType.FILE_TYPE_IMAGE:
                setOverViewCategoryShow(false);
                setUpdateImage();
                showSettingMenu(false);
                setmPath(str + " / "+mContext.getString(R.string.quickscan_image));
                break;
            case FileType.FILE_TYPE_VIDEO_DEFAULT:
                setOverViewCategoryShow(false);
                setUpdateVideo();
                showSettingMenu(false);
                setmPath(str+"/ "+ mContext.getString(R.string.quickscan_video));
                break;
            case FileType.FILE_TYPE_DOC:
                setOverViewCategoryShow(false);
                setUpdateDoc();
                showSettingMenu(false);
                setmPath(str+"/ "+ mContext.getString(R.string.quickscan_doc));
                break;
            case FileType.FILE_TYPE_APK:
                 setOverViewCategoryShow(false);
                 setUpdateApk();
                 showSettingMenu(false);
                setmPath(str+"/ "+ mContext.getString(R.string.quickscan_app));
                break;

        }
    }
}
