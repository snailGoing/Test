package com.sprd.fileexplore.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.sprd.fileexplore.R;
import com.sprd.fileexplore.adapter.BanViewPager;
import com.sprd.fileexplore.adapter.QuickScanAdapterSprd;
import com.sprd.fileexplore.adapter.RecyclerItemClickListener;
import com.sprd.fileexplore.controller.OverViewController;
import com.sprd.fileexplore.file.FileInfoComparator;
import com.sprd.fileexplore.file.FileInfoManager;
import com.sprd.fileexplore.service.MediaStoreHelper;
import com.sprd.fileexplore.file.FileInfo;
import com.sprd.fileexplore.file.FileType;
import com.sprd.fileexplore.load.CursorTaskThread;
import com.sprd.fileexplore.service.FileManageService;
import com.sprd.fileexplore.service.MountRootManagerRf;
import com.sprd.fileexplore.service.MyBinder;
import com.sprd.fileexplore.service.ProgressInfo;
import com.sprd.fileexplore.util.FileUtil;
import com.sprd.fileexplore.util.LogUtil;
import com.sprd.fileexplore.util.PermissionUtils;
import com.sprd.fileexplore.util.SparseBooleanArrayParcelable;
import com.sprd.fileexplore.util.ToastHelper;
import com.sprd.fileexplore.view.AlertDialogFragment;
import com.sprd.fileexplore.view.ImageViewText;
import com.sprd.fileexplore.view.ProgressDialogFragment;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 * Created by Xuehao.Jiang on 2017/4/6.
 */

public class OverViewFragment extends BaseFragment implements  View.OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor>, MountRootManagerRf.MountListener{

    private static final String TAG = OverViewFragment.class.getSimpleName();

    private static final  int MESG_LOADING_IMAGE = 100;
    private static final  int MESG_LOADING_AUDIO = 101;
    private static final  int MESG_LOADING_VIDEO= 102;
    private static final  int MESG_LOADING_DOC=103;
    private static final  int MESG_LOADING_APK=104;

    public static final String RENAME_EXTENSION_DIALOG_TAG = "rename_extension_dialog_fragment_tag";
    public static final String RENAME_DIALOG_TAG = "rename_dialog_fragment_tag";
    public static final String DELETE_DIALOG_TAG = "delete_dialog_fragment_tag";
    public static final String CLEAR_DIALOG_TAG = "clear_dialog_fragment_tag";
    public static final String FORBIDDEN_DIALOG_TAG = "forbidden_dialog_fragment_tag";
    private static final String NEW_FILE_PATH_KEY = "new_file_path_key";
    private static final String DETAIL_INFO_KEY = "detail_info_key";
    private static final String DETAIL_INFO_SIZE_KEY = "detail_info_size_key";

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_RENAME = 1;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_DELETE = 2;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_CUT = 3;
    public  static final int PERMISSIONS_REQUEST_TO_READ_EXTERNAL_STORAGE = 4;

    private ImageViewText mImageView;
    private ImageViewText mAudioView;
    private ImageViewText mVideoView;
    private ImageViewText mDocView;
    private ImageViewText mApkView;
    private int mFileType;

    private OverViewController mController;
    private CursorTaskThread mImageTaskThread;
    private CursorTaskThread mAudioTaskThread;
    private CursorTaskThread mVideoTaskThread;
    private CursorTaskThread mDocTaskThread;
    private CursorTaskThread mApkTaskThread;
    private boolean mIsMainUI = true;

    private Context mContext;
    private RecyclerView mRecyclerView;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1;
    QuickScanAdapterSprd quickScanAdapterSprd;
    private ActionMode mActionMode;
    private SparseBooleanArrayParcelable mSelectedIds;
    private int mSortType = 0;
    private SharedPreferences sortSpf ;
    private FileInfoManager mFileInfoManager = null;
    private ToastHelper mToastHelper = null;
    /*
    *   Service Binder
    * */
    public MyBinder binder;
    protected boolean mServiceBinded = false;

    private RecyclerItemClickListener recyclerItemClickListener = new RecyclerItemClickListener() {
        @Override
        public void onItemClick(View v, int position) {
            LogUtil.d(TAG, "onItemClick " + position);
            if (mActionMode != null){
                onListItemSelect(position);
            }else{
                // regular to click
                onItemClickExe(v,position);
            }
        }

        @Override
        public void onItemLongClick(View v, int position) {
            onListItemSelect(position);
        }
    };
    /**
     *  this fun for some item clicked
     *  @author Xuehao.Jiang
     *  created at 2017/6/30 21:41
     */
    public void onItemClickExe(View v, int position){
        Log.d(TAG, "onItemClickExe, position = " + position);
        if (binder != null && binder.isBusy(TAG)) {
            Log.d(TAG, "onItemClickExe, service is busy,return. ");
            return;
        }
        Log.d(TAG, "onItemClickExe,Selected position: " + position);
        if (position >= quickScanAdapterSprd.getItemCount() || position < 0) {
            Log.e(TAG, "onItemClickExe,events error,mFileInfoList.size(): "
                    + quickScanAdapterSprd.getItemCount());
            return;
        }
        FileInfo selecteItemFileInfo = (FileInfo) quickScanAdapterSprd.getCurrentPosFileInfo(position);

        FileUtil.startUpFileByIntent(mContext,selecteItemFileInfo,binder);

    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mContext = getActivity();
        sortSpf = mContext
                .getSharedPreferences(FileInfoComparator.SORT_KEY, 0);
        mToastHelper = new ToastHelper(mContext);
        if (ContextCompat.checkSelfPermission( mContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }else{
            initLoader(mContext);
        }

        mountRootManagerRf  = MountRootManagerRf.getInstance();
        mountRootManagerRf.registerMountListener(this);

        /**
        *
        *  bind service to operate some tasks
        * */
        mContext.bindService(new Intent(mContext, FileManageService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==0&& (grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
            initLoader(mContext);
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder.disconnected(this.getClass().getName());
            mServiceBinded = false;
            Log.d(TAG,"onServiceDisconnected");
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d( TAG,"onServiceConnected :  name=  "+name);
            binder = (MyBinder)service;
            mServiceBinded = true;
            Log.d(TAG, "onServiceConnected   mFileInfoManager -->"+TAG );
            mFileInfoManager = binder.initFileInfoManager(TAG);
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.over_view_fragment, container,
                false);
        quickScanAdapterSprd = new QuickScanAdapterSprd(mContext,recyclerItemClickListener, mHandler,this);
        mController = new OverViewController(view,mContext,quickScanAdapterSprd);
        mRecyclerView = (RecyclerView)view.findViewById(R.id.recycle_view);
        mImageView = (ImageViewText) view.findViewById(R.id.quickscan_image_layout);
        mAudioView = (ImageViewText) view.findViewById(R.id.quickscan_audio_layout);
        mVideoView = (ImageViewText) view.findViewById(R.id.quickscan_video_layout);
        mDocView = (ImageViewText) view.findViewById(R.id.quickscan_doc_layout);
        mApkView = (ImageViewText) view.findViewById(R.id.quickscan_apk_layout);

        mImageView.setOnClickListener(this);
        mAudioView.setOnClickListener(this);
        mVideoView.setOnClickListener(this);
        mDocView.setOnClickListener(this);
        mApkView.setOnClickListener(this);
        LogUtil.d("OverView: onCreateView-------> ");
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshRoots();

        LogUtil.d(" onResume: --->");
    }

    public void refreshRoots(){
        boolean Sdstatus = mountRootManagerRf.getSdMountPointStatue();
        FileInfo sd  = mountRootManagerRf.getSdFileInfo();
        FileInfo phone  = mountRootManagerRf.getPhoneFileInfo();

        if(Sdstatus){
            mController.showInternalView(false);
            mController.setStorageStatus( sd,phone,mContext);
        }else{
            mController.showInternalView(true);
            mController.setStorageStatus( sd,phone,mContext);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.quickscan_audio_layout:
                mFileType = FileType.FILE_TYPE_AUDIO_DEFAULT;
                mController.updateUI(mFileType);
                mIsMainUI = false;
                break;
            case R.id.quickscan_image_layout:
                mFileType = FileType.FILE_TYPE_IMAGE;
                mController.updateUI(mFileType);
                mIsMainUI = false;
                break;
            case R.id.quickscan_video_layout:
                mFileType = FileType.FILE_TYPE_VIDEO_DEFAULT;
                mController.updateUI(mFileType);
                mIsMainUI = false;
                break;
            case R.id.quickscan_doc_layout:
                mFileType = FileType.FILE_TYPE_DOC;
                mController.updateUI(mFileType);
                mIsMainUI = false;
                break;
            case R.id.quickscan_apk_layout:
                mFileType = FileType.FILE_TYPE_APK;
                mController.updateUI(mFileType);
                mIsMainUI = false;
                break;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = null;
        String selection = null;
        switch (id) {
            case MediaStoreHelper.IMAGE_LOADER_ID:
                uri = MediaStoreHelper.IMAGE_URI;
                break;
            case MediaStoreHelper.AUDIO_LOADER_ID:
                uri = MediaStoreHelper.AUDIO_URI;
                break;
            case MediaStoreHelper.VIDEO_LOADER_ID:
                uri = MediaStoreHelper.VIDEO_URI;
                break;
            case MediaStoreHelper.DOC_LOADER_ID:
                uri = MediaStoreHelper.DOC_URI;
                selection = MediaStoreHelper.DOC_SELECTION;
                break;
            case MediaStoreHelper.APK_LOADER_ID:
                uri = MediaStoreHelper.APK_URI;
                selection = MediaStoreHelper.APK_SELECTION;
                break;
        }
//        if (ContextCompat.checkSelfPermission( mContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
//            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);

        return new CursorLoader(mContext, uri,MediaStoreHelper.DATA_PROJECTION , selection,null,null){
            @Override
            public Cursor loadInBackground() {
                int loaderId = getId();
                switch(loaderId){
                    case MediaStoreHelper.IMAGE_LOADER_ID:
                        LogUtil.d(" Loader----> loadInBackground  iamge---->changed");
                        mHandler.sendEmptyMessage(MESG_LOADING_IMAGE);
                        break;
                    case MediaStoreHelper.AUDIO_LOADER_ID:
                        mHandler.sendEmptyMessage(MESG_LOADING_AUDIO);
                        break;
                    case MediaStoreHelper.VIDEO_LOADER_ID:
                        mHandler.sendEmptyMessage(MESG_LOADING_VIDEO);
                        break;
                    case MediaStoreHelper.DOC_LOADER_ID:
                        mHandler.sendEmptyMessage(MESG_LOADING_DOC);
                        break;
                    case MediaStoreHelper.APK_LOADER_ID:
                        mHandler.sendEmptyMessage(MESG_LOADING_APK);
                        break;

                }
                return super.loadInBackground();
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        int loaderId = loader.getId();
        setSummary(loaderId,data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public void  initLoader(Context mContext){
        ((Activity)mContext).getLoaderManager().initLoader(MediaStoreHelper.IMAGE_LOADER_ID  , null, this);
        ((Activity)mContext).getLoaderManager().initLoader(MediaStoreHelper.AUDIO_LOADER_ID  , null, this);
        ((Activity)mContext).getLoaderManager().initLoader(MediaStoreHelper.VIDEO_LOADER_ID , null, this);
        ((Activity)mContext).getLoaderManager().initLoader(MediaStoreHelper.DOC_LOADER_ID  , null, this);
        ((Activity)mContext).getLoaderManager().initLoader(MediaStoreHelper.APK_LOADER_ID , null, this);
    }

    public  void setSummary(int id,Cursor data){
        int count = 0;
        switch (id) {
            case MediaStoreHelper.IMAGE_LOADER_ID:
                if(data!=null){
                    count = data.getCount();
                    mImageView.setSummary(count);
                    if( mImageTaskThread == null){
                        mImageTaskThread = new CursorTaskThread(mHandler,id,sortSpf);
                        mImageTaskThread.start();
                    }
                    mImageTaskThread.requestCursorData(data);
                }
                break;
            case MediaStoreHelper.AUDIO_LOADER_ID:
                if(data!=null){
                    count = data.getCount();
                    mAudioView.setSummary(count);
                    if( mAudioTaskThread == null){
                        mAudioTaskThread = new CursorTaskThread(mHandler,id,sortSpf);
                        mAudioTaskThread.start();
                    }
                    mAudioTaskThread.requestCursorData(data);
                }
                break;
            case MediaStoreHelper.VIDEO_LOADER_ID:
                if(data!=null){
                    count = data.getCount();
                    mVideoView.setSummary(count);
                    if( mVideoTaskThread == null){
                        mVideoTaskThread =  new CursorTaskThread(mHandler,id,sortSpf);
                        mVideoTaskThread.start();
                    }
                    mVideoTaskThread.requestCursorData(data);
                }

                break;
            case MediaStoreHelper.DOC_LOADER_ID:
                if(data!=null){
                    //count = data.getCount();
                    count = FileUtil.validNoHiddenCount(data);
                    data.moveToPosition(-1);
                    mDocView.setSummary(count);
                    if( mDocTaskThread == null){
                        mDocTaskThread = new CursorTaskThread(mHandler,id,sortSpf);
                        mDocTaskThread.start();
                    }
                    mDocTaskThread.requestCursorData(data);
                }
                break;
            case MediaStoreHelper.APK_LOADER_ID:
                if(data!=null){
                    //count = data.getCount();
                    count = FileUtil.validNoHiddenCount(data);
                    data.moveToPosition(-1);
                    mApkView.setSummary(count);
                    if( mApkTaskThread == null){
                        mApkTaskThread =  new CursorTaskThread(mHandler,id,sortSpf);
                        mApkTaskThread.start();
                    }
                    mApkTaskThread.requestCursorData(data);
                }
                break;
        }
    }
    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case CursorTaskThread.MESSAGE_REQUEST_CURSOR_DONE:
                    int loaderId = (int)msg.obj;
                    notifyRecycleViewData(loaderId);
                    break;
                case CursorTaskThread.MESSAGE_REFRESH:
                    break;

            }
        }
    };

    public void notifyRecycleViewData(int loaderId ){
        switch (loaderId) {
            case MediaStoreHelper.IMAGE_LOADER_ID:
                mController.setImageFileInfoList(mImageTaskThread.getFileInfoList());
                break;
            case MediaStoreHelper.AUDIO_LOADER_ID:
                mController.setAudioFileInfoList(mAudioTaskThread.getFileInfoList());
                break;
            case MediaStoreHelper.VIDEO_LOADER_ID:
                mController.setVideoFileInfoList(mVideoTaskThread.getFileInfoList());
                break;
            case MediaStoreHelper.DOC_LOADER_ID:
                mController.setDocFileInfoList(mDocTaskThread.getFileInfoList());
                break;
            case MediaStoreHelper.APK_LOADER_ID:
                mController.setApkFileInfoList(mApkTaskThread.getFileInfoList());
                break;
        }

    }
    @Override
    public boolean onBackPressed() {

        if (!mIsMainUI) {
            // if in over view list ui, when press back button, return to category ui
            mIsMainUI = true;
            mController.setOverViewCategoryShow(true);
            mController.setInstallAllMenuShow(false);
            mController.setSelectSortMenuShow(false);
            mController.showSettingMenu(true);
            mController.resetUpdateStatus();
            return false;
        } else {
            // if in  category ui, handle it regularly
            return true;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.over_view, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
       super.onPrepareOptionsMenu(menu);
        mController.setMenu(menu);
        Log.d(TAG,"   onPrepareOptionsMenu");
        mController.refreshMenu();


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_install_all:

                return true;
            case R.id.menu_select_more:
                mActionMode =((AppCompatActivity)mContext).startSupportActionMode(mActionModeCallback);
                setActionMode(quickScanAdapterSprd.getSelectedCount());
                return true;
            case R.id.menu_sort_by:
                showSortDialog();
                return true;
            case R.id.menu_style_by:

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mountRootManagerRf.unregisterMountListener(this);

        if(binder!=null && mContext!=null && mServiceBinded){
            Log.d(TAG," onDestroy()  : unbindService success!");
            mContext.unbindService(mServiceConnection);
            mServiceBinded = false;
        }else{
            Log.d(TAG," onDestroy()  : unbindService fail !");
        }
    }

    private TabLayout tabLayout;
    public OverViewFragment setTabLayout(TabLayout mTabLayout){
        Log.d(TAG, "setTabLayout");
        this.tabLayout = mTabLayout;
        return this;
    }

    private BanViewPager viewPager;
    public OverViewFragment setViewPager(BanViewPager mViewPager){
        Log.d(TAG, "setTabLayout");
        this.viewPager = mViewPager;
        return this;
    }
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback(){
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {

            if(tabLayout!=null) {
                tabLayout.setVisibility(View.GONE);

                Log.d(TAG, "onCreateActionMode  tabLayout gone");
            }else{
                Log.d(TAG, "onCreateActionMode tabLayout null");
            }
            if(viewPager!=null) viewPager.setNoScroll(true);
            mode.getMenuInflater().inflate(R.menu.mode_dir, menu);//Inflate the menu over action mode

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            refreActionModeMenu( mode, menu);
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            switch (item.getItemId()) {
                case R.id.menu_delete:  menuDelete();   break;
                case R.id.menu_copy:    menuCopy();     break;
                case R.id.menu_cut:     menuCut();      break;
                case R.id.menu_select:  menuSelectOrCancelAll();    break;
                case R.id.menu_rename:  menuRename();   break;
                case R.id.menu_detail:  menuDetails();  break;
                case R.id.menu_share:   menuShare();    break;
                default:    Log.d(TAG, "default: "+ item.getItemId());  break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            quickScanAdapterSprd.removeSelection();
            setNullToActionMode();
            if(tabLayout!=null) tabLayout.setVisibility(View.VISIBLE);
            if(viewPager!=null) viewPager.setNoScroll(false);
        }
    };
    public void refreActionModeMenu(ActionMode mode, Menu menu){

        if(menu==null) return;
        MenuItem menuDelete = menu.findItem(R.id.menu_delete);
        MenuItem menuCopy = menu.findItem(R.id.menu_copy);
        MenuItem menuCut = menu.findItem(R.id.menu_cut);
        menu.findItem(R.id.menu_clear).setVisible(false);

        MenuItem menuSelet = menu.findItem(R.id.menu_select);
        MenuItem menuRename = menu.findItem(R.id.menu_rename);
        MenuItem menuDetail = menu.findItem(R.id.menu_detail);
        MenuItem menuShare = menu.findItem(R.id.menu_share);

        int sum = quickScanAdapterSprd.getItemCount();
        int selectCount = quickScanAdapterSprd.getSelectedCount();

        Log.d(TAG,"   ActionMode onPrepareActionMode--> sum= "+sum +" selectCount= "+selectCount);
        if(selectCount == 0){
            menuDelete.setEnabled(false).setIcon(R.mipmap.menu_trash_disable);
            menuCopy.setEnabled(false).setIcon(R.mipmap.menu_copy_disable);
            menuCut.setEnabled(false).setIcon(R.mipmap.menu_cut_disable);

            menuRename.setEnabled(false);
            menuDetail.setEnabled(false);
            menuShare.setEnabled(false);
        }else {
            menuDelete.setEnabled(true).setIcon(R.mipmap.menu_trash);
            menuCopy.setEnabled(true).setIcon(R.mipmap.menu_copy);
            menuCut.setEnabled(true).setIcon(R.mipmap.menu_cut);
            boolean hasFolder = false;
            for(FileInfo f: quickScanAdapterSprd.getCheckedFileInfoItemsList()){
                if(f.isFolder()){
                    hasFolder = true;
                    break;
                }
            }
            if(hasFolder){
                menuShare.setEnabled(false);
            }else{
                menuShare.setEnabled(true);
            }
            if( selectCount > 1){
                menuRename.setEnabled(false);
                menuDetail.setEnabled(false);
            }else{
                FileInfo selected = quickScanAdapterSprd.getCheckedFileInfoItemsList().get(0);
                menuRename.setEnabled(true);
                menuDetail.setEnabled(true);
            }
        }
        if(sum == selectCount){
            menuSelet.setTitle(mContext.getString(R.string.menu_cancel_all));
        }else{
            menuSelet.setTitle(mContext.getString(R.string.menu_select_all));
        }
    }
    //List item select method
    private void onListItemSelect(int position) {
        quickScanAdapterSprd.toggleSelection(position);//Toggle the selection
        setActionMode(quickScanAdapterSprd.getSelectedCount());
    }

    private void setActionMode(int countSelected) {
        boolean hasCheckedItems = countSelected > 0;//Check if any items are already selected or not

        if (hasCheckedItems && mActionMode == null){
            // there are some selected items, start the actionMode
            mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);
        }
//        else if (!hasCheckedItems && mActionMode != null){
//            mActionMode.finish();
//        }
        if (mActionMode != null){
            //set action mode title on item selection
            mActionMode.setTitle(String.valueOf(countSelected));
            mActionMode.invalidate();
        }
    }

    //Set action mode null after use
    public void setNullToActionMode() {
        if (mActionMode != null)
            mActionMode = null;
    }

    @Override
    public void notifyStorageChanged(String path, String oldState, String newState) {

        String changedPath = path;
        String newSt = newState;
        Log.d(TAG,"notifyStorageChanged: changedPath= "+changedPath +"  newState="+ newSt);
        if(mountRootManagerRf.isMountPoint(changedPath) || "local_changed".equals(newState)){
            refreshRoots();
        }
    }
    private MountRootManagerRf mountRootManagerRf ;
    private void showDeleteDialog() {
        Log.d(TAG, "show DeleteDialog...");
        if (mIsAlertDialogShowing) {
            Log.d(TAG, "Another Dialog is exist, return!~~");
            return;
        }
        int alertMsgId = R.string.alert_delete_multiple;
        if (  quickScanAdapterSprd.getSelectedCount() == 1) {
            alertMsgId = R.string.alert_delete_single;
        } else {
            alertMsgId = R.string.alert_delete_multiple;
        }

        if (isResumed()) {
            mIsAlertDialogShowing = true;
            AlertDialogFragment.AlertDialogFragmentBuilder builder = new AlertDialogFragment.AlertDialogFragmentBuilder();
            AlertDialogFragment deleteDialogFragment = builder.setMessage(alertMsgId).setDoneTitle(
                    R.string.ok).setCancelTitle(R.string.cancel).setIcon(
                    R.mipmap.ic_dialog_alert_holo_light).setTitle(R.string.delete).create();
            deleteDialogFragment.setOnDoneListener(new DeleteListener());
            deleteDialogFragment.setOnDialogDismissListener(onDialogDismissListener);
            deleteDialogFragment.show(((Activity)mContext).getFragmentManager(), DELETE_DIALOG_TAG);
            boolean ret = ((Activity)mContext).getFragmentManager().executePendingTransactions();
            Log.d(TAG, "executing pending transactions result: " + ret);
        }
    }
    private void showRenameDialog(){
        Log.d(TAG, "show RenameDialog...");
        if (mIsAlertDialogShowing) {
            Log.d(TAG, "Another Dialog showing, return!~~");
            return;
        }
        FileInfo fileInfo = quickScanAdapterSprd.getFirstCheckedFileInfoItem();
        int selection = 0;
        if (fileInfo != null) {
            String name = fileInfo.getName();
            String fileExtension = FileUtil.getFileExtension(name);
            selection = name.length();
            if (!fileInfo.isFolder() && fileExtension != null) {
                selection = selection - fileExtension.length() - 1;
            }
            if (isResumed()) {
                mIsAlertDialogShowing = true;
                AlertDialogFragment.EditDialogFragmentBuilder builder = new AlertDialogFragment.EditDialogFragmentBuilder();
                builder.setDefault(name, selection).setDoneTitle(R.string.done).setCancelTitle(
                        R.string.cancel).setTitle(R.string.rename);
                AlertDialogFragment.EditTextDialogFragment renameDialogFragment = builder.create();
                renameDialogFragment.setOnEditTextDoneListener(new RenameDoneListener(fileInfo));
                renameDialogFragment.setOnDialogDismissListener(onDialogDismissListener);
                renameDialogFragment.show(((Activity)mContext).getFragmentManager(), RENAME_DIALOG_TAG);
                boolean ret = ((Activity)mContext).getFragmentManager().executePendingTransactions();
                Log.d(TAG, "executing pending transactions result: " + ret);
            }
        }
    }

    protected void showRenameExtensionDialog(FileInfo srcfileInfo, final String newFilePath){
        Log.d(TAG, "show RenameExtensionDialog...");

        AlertDialogFragment.AlertDialogFragmentBuilder builder = new AlertDialogFragment.AlertDialogFragmentBuilder();
        AlertDialogFragment renameExtensionDialogFragment = builder.setTitle(
                R.string.confirm_rename).setIcon(R.mipmap.ic_dialog_alert_holo_light).setMessage(
                R.string.msg_rename_ext).setCancelTitle(R.string.cancel).setDoneTitle(R.string.ok)
                .create();
        renameExtensionDialogFragment.getArguments().putString(NEW_FILE_PATH_KEY, newFilePath);
        renameExtensionDialogFragment.setOnDoneListener(new RenameExtensionListener(srcfileInfo,
                newFilePath));
        renameExtensionDialogFragment.show(((Activity)mContext).getFragmentManager(), RENAME_EXTENSION_DIALOG_TAG);
        boolean ret = ((Activity)mContext).getFragmentManager().executePendingTransactions();
        Log.d(TAG, "executing pending transactions result: " + ret);
    }

    private AlertDialogFragment.OnDialogDismissListener onDialogDismissListener=  new AlertDialogFragment.OnDialogDismissListener(){

        @Override
        public void onDialogDismiss() {
            Log.d(TAG, "dialog dismissed...");
            mIsAlertDialogShowing = false;
        }
    };

    /**
     *  Those listeners are to update  main UI when request service to do actually works done.
     *  @author Xuehao.Jiang
     *  created at 2017/7/8 11:09
     */
    private class LightOperationListener implements FileManageService.OperationEventListener {

        String mDstName = null;

        LightOperationListener(String dstName) {
            mDstName = dstName;
        }

        @Override
        public void onTaskResult(int errorType) {
            Log.i(TAG, "LightOperationListener,TaskResult result = " + errorType);
            switch (errorType) {
                case ERROR_CODE_SUCCESS:
                case ERROR_CODE_USER_CANCEL:
                    if(sortSpf!=null){
                        mSortType = sortSpf.getInt(FileInfoComparator.SORT_KEY,FileInfoComparator.SORT_BY_NAME);
                    }
                    break;
                case ERROR_CODE_FILE_EXIST:
                    if (mDstName != null) {
                        mToastHelper.showToast(getResources().getString(R.string.already_exists,
                                mDstName));
                    }
                    break;
                case ERROR_CODE_NAME_EMPTY:
                    mToastHelper.showToast(R.string.invalid_empty_name);
                    break;
                case ERROR_CODE_NAME_TOO_LONG:
                    mToastHelper.showToast(R.string.file_name_too_long);
                    break;
                case ERROR_CODE_NOT_ENOUGH_SPACE:
                    mToastHelper.showToast(R.string.insufficient_memory);
                    break;
                case ERROR_CODE_UNSUCCESS:
                    mToastHelper.showToast(R.string.operation_fail);
                    break;
                default:
                    Log.e(TAG, "wrong errorType for LightOperationListener");
                    break;
            }
        }

        @Override
        public void onTaskPrepare() {
            return;
        }

        @Override
        public void onTaskProgress(ProgressInfo progressInfo) {
            return;
        }
    }
    private class SortOperationListener implements FileManageService.OperationEventListener {

        @Override
        public void onTaskPrepare() {}
        @Override
        public void onTaskProgress(ProgressInfo progressInfo) {}
        @Override
        public void onTaskResult(int result) {
            switch (result){
                case ERROR_CODE_SUCCESS:
                case ERROR_CODE_USER_CANCEL:
                    break;
            }
        }
    }

    private class HeavyOperationListener implements FileManageService.OperationEventListener, View.OnClickListener {
        int mTitle = R.string.deleting;

        private boolean mPermissionToast = false;
        private boolean mOperationToast = false;
        public static final String HEAVY_DIALOG_TAG = "HeavyDialogFragment";

        public HeavyOperationListener(int titleID) {
            mTitle = titleID;
        }

        @Override
        public void onTaskPrepare() {
            // beforeTime = System.currentTimeMillis();
            if (isResumed()) {
                ProgressDialogFragment heavyDialogFragment = ProgressDialogFragment.newInstance(
                        ProgressDialog.STYLE_HORIZONTAL, mTitle, R.string.wait, R.string.cancel);
                heavyDialogFragment.setCancelListener(this);
                heavyDialogFragment.setViewDirection(getViewDirection());
                heavyDialogFragment.show(((Activity)mContext).getFragmentManager(), HEAVY_DIALOG_TAG);
                boolean ret = ((Activity)mContext).getFragmentManager().executePendingTransactions();
                Log.d(TAG, "executing pending transactions result: " + ret);
            } else {
                Log.d(TAG, "HeavyOperationListener onTaskResult activity is not resumed.");
            }
        }

        @Override
        public void onTaskProgress(ProgressInfo progressInfo) {
            if (progressInfo.isFailInfo()) {
                switch (progressInfo.getErrorCode()) {
                    case FileManageService.OperationEventListener.ERROR_CODE_COPY_NO_PERMISSION:
                        if (!mPermissionToast) {
                            mToastHelper.showToast(R.string.copy_deny);
                            mPermissionToast = true;
                        }
                        break;
                    case FileManageService.OperationEventListener.ERROR_CODE_DELETE_NO_PERMISSION:
                        if (!mPermissionToast) {
                            mToastHelper.showToast(R.string.delete_deny);
                            mPermissionToast = true;
                        }
                        break;
                    case FileManageService.OperationEventListener.ERROR_CODE_DELETE_UNSUCCESS:
                        if (!mOperationToast) {
                            mToastHelper.showToast(R.string.some_delete_fail);
                            mOperationToast = true;
                        }
                        break;
                    case FileManageService.OperationEventListener.ERROR_CODE_PASTE_UNSUCCESS:
                        if (!mOperationToast) {
                            mToastHelper.showToast(R.string.some_paste_fail);
                            mOperationToast = true;
                        }
                        break;
                    default:
                        if (!mPermissionToast) {
                            mToastHelper.showToast(R.string.operation_fail);
                            mPermissionToast = true;
                        }
                        break;
                }

            } else {
                ProgressDialogFragment heavyDialogFragment = (ProgressDialogFragment) ((Activity)mContext).getFragmentManager()
                        .findFragmentByTag(HEAVY_DIALOG_TAG);
                if (heavyDialogFragment != null) {
                    heavyDialogFragment.setProgress(progressInfo);
                }
            }
        }

        @Override
        public void onTaskResult(int errorType) {
            Log.d(TAG, "HeavyOperationListener,onTaskResult result = " + errorType);
            switch (errorType) {
                case ERROR_CODE_PASTE_TO_SUB:
                    mToastHelper.showToast(R.string.paste_sub_folder);
                    break;
                case ERROR_CODE_CUT_SAME_PATH:
                    mToastHelper.showToast(R.string.paste_same_folder);
                    break;
                case ERROR_CODE_NOT_ENOUGH_SPACE:
                    mToastHelper.showToast(R.string.insufficient_memory);
                    break;
                case ERROR_CODE_DELETE_FAILS:
                    mToastHelper.showToast(R.string.delete_fail);
                    break;
                case ERROR_CODE_COPY_NO_PERMISSION:
                    mToastHelper.showToast(R.string.copy_deny);
                    break;
                case ERROR_CODE_COPY_GREATER_4G_TO_FAT32:
                    mToastHelper.showToast(R.string.operation_fail);
                    break;
                default:
                    if(sortSpf!=null){
                        mSortType = sortSpf.getInt(FileInfoComparator.SORT_KEY,FileInfoComparator.SORT_BY_NAME);
                    }
                    break;
            }
            ProgressDialogFragment heavyDialogFragment = (ProgressDialogFragment) ((Activity)mContext).getFragmentManager()
                    .findFragmentByTag(HEAVY_DIALOG_TAG);
            if (heavyDialogFragment != null) {
                heavyDialogFragment.dismissAllowingStateLoss();
            }
            ((Activity)mContext).invalidateOptionsMenu();
        }

        @Override
        public void onClick(View v) {
            if (binder != null) {
                Log.i(this.getClass().getName(), "onClick cancel");
                binder.cancel(TAG);
            }
        }
    }
    protected class DetailInfoListener implements FileManageService.OperationEventListener,
            DialogInterface.OnDismissListener{
        public static final String DETAIL_DIALOG_TAG = "detaildialogtag";
        private TextView mDetailsText;
        private final String mName;
        private String mType;
        private String mPath;
        private String mSize;
        private final String mModifiedTime;
        private final String mPermission;
        private final StringBuilder mStringBuilder = new StringBuilder();
        private long mFileLength = -1;

        public DetailInfoListener(FileInfo fileInfo) {
            mStringBuilder.setLength(0);
            mName = mStringBuilder.append(getString(R.string.name)).append(": ").append(
                    fileInfo.getName()).append("\n").toString();

            mStringBuilder.setLength(0);
            mType = mStringBuilder.append(getString(R.string.type)).append(": ")
                    .append(fileInfo.isFolder()? (mContext.getString(R.string.folder)):
                            mContext.getString(R.string.file))
                    .append(" \n").toString();

            mStringBuilder.setLength(0);
            mPath = mStringBuilder.append(getString(R.string.path)).append(": ").append(
                    fileInfo.getPath()).append("\n").toString();

            mStringBuilder.setLength(0);
            mSize = mStringBuilder.append(getString(R.string.size)).append(": ").append(
                    FileUtil.sizeToString(0)).append(" \n").toString();

            long time = fileInfo.getLastModifiedTime();

            mStringBuilder.setLength(0);
            mModifiedTime = mStringBuilder.append(getString(R.string.modified_time)).append(": ")
                    .append(DateFormat.getDateInstance().format(new Date(time))).append("\n")
                    .toString();
            mStringBuilder.setLength(0);
            mPermission = getPermission(fileInfo.getFile());

            Log.d(TAG, "DetailInfoListener():  mName ="+mName+"  mSize="+mSize+"  mModifiedTime="+mModifiedTime+" mPermission="+mPermission);
        }

        public String getDetailInfo(long size) {
            mSize = getString(R.string.size) + ": "
                    + FileUtil.sizeToString(size) + " \n";
            StringBuilder builder = new StringBuilder();
            builder.append(mName).append(mType).append(mPath).append(mSize).append(mModifiedTime)
                    .append(mPermission);
            return builder.toString();
        }

        private void appendPermission(boolean hasPermission, int title) {
            mStringBuilder.append(getString(title) + ": ");
            if (hasPermission) {
                mStringBuilder.append(getString(R.string.yes));
            } else {
                mStringBuilder.append(getString(R.string.no));
            }
        }

        private String getPermission(File file) {
            appendPermission(file.canRead(), R.string.readable);
            mStringBuilder.append("\n");
            appendPermission(file.canWrite(), R.string.writable);
            mStringBuilder.append("\n");
            appendPermission(file.canExecute(), R.string.executable);

            return mStringBuilder.toString();
        }

        @Override
        public void onTaskPrepare() {
            if (isResumed()) {
                AlertDialogFragment.AlertDialogFragmentBuilder builder = new AlertDialogFragment.AlertDialogFragmentBuilder();
                AlertDialogFragment detailFragment = builder.setCancelTitle(R.string.ok).setLayout(
                        R.layout.dialog_details).setTitle(R.string.details).create();

                detailFragment.setDismissListener(this);
                detailFragment.show(((Activity)mContext).getFragmentManager(), DETAIL_DIALOG_TAG);
                boolean ret = ((Activity)mContext).getFragmentManager().executePendingTransactions();
                Log.d(TAG, "executing pending transactions result: " + ret);
                if (detailFragment.getDialog() != null) {
                    mDetailsText = (TextView) detailFragment.getDialog()
                            .findViewById(R.id.details_text);

                    mStringBuilder.setLength(0);
                    if (mDetailsText != null) {
                        mDetailsText.setText(mStringBuilder.append(mName).append(mType).append(mPath).append(mSize).append(
                                mModifiedTime).append(mPermission).toString());
                        mDetailsText.setMovementMethod(ScrollingMovementMethod.getInstance());
                    }
                }
            } else {
                Log.e(TAG, "onTaskPrepare activity is not resumed");
            }
        }

        @Override
        public void onTaskProgress(ProgressInfo progressInfo) {
            mFileLength = progressInfo.getTotal();
            mSize = getString(R.string.size) + ": "
                    + FileUtil.sizeToString(progressInfo.getTotal()) + " \n";
            if (mDetailsText != null) {
                mStringBuilder.setLength(0);
                mStringBuilder.append(mName).append(mType).append(mPath).append(mSize).append(mModifiedTime)
                        .append(mPermission);
                mDetailsText.setText(mStringBuilder.toString());
            }

        }

        @Override
        public void onTaskResult(int result) {
            Log.d(TAG, "DetailInfoListener onTaskResult." + mFileLength +" str="+mStringBuilder.toString());
            if (mFileLength == -1) {
                mFileLength = 0;
            }
            AlertDialogFragment detailFragment = (AlertDialogFragment)((Activity)mContext). getFragmentManager()
                    .findFragmentByTag(DETAIL_DIALOG_TAG);
            if (detailFragment != null) {

                detailFragment.getArguments().putString(DETAIL_INFO_KEY, mStringBuilder.toString());
                detailFragment.getArguments().putLong(DETAIL_INFO_SIZE_KEY, mFileLength);
            } else {
                // this case may happen in case of this operation already canceled.
                Log.d(TAG, "get detail fragment is null...");
            }

            return;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            if (binder != null) {
                Log.d(this.getClass().getName(), "onDismiss");
                binder.cancel(TAG);
            }
        }
    }

    protected class RenameDoneListener implements AlertDialogFragment.EditTextDialogFragment.EditTextDoneListener {
        FileInfo mSrcfileInfo;

        public RenameDoneListener(FileInfo srcFile) {
            mSrcfileInfo = srcFile;
        }

        @Override
        public void onClick(String text) {
            String newFilePath = mSrcfileInfo.getFileParentPath() + MountRootManagerRf.SEPARATOR + text;
            if (null == mSrcfileInfo) {
                Log.w(TAG, "mSrcfileInfo is null.");
                return;
            }
            if (FileUtil.isExtensionChange(newFilePath, mSrcfileInfo.getPath())) {
                showRenameExtensionDialog(mSrcfileInfo, newFilePath);
            } else {
                if (binder != null) {
                    if (mActionMode != null) {
                        mActionMode.finish();
                    }
                    binder.rename(TAG,
                            mSrcfileInfo, new FileInfo(newFilePath), new LightOperationListener(
                                    FileUtil.getFileName(newFilePath)));
                }
            }

        }
    }
    private class DeleteListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int id) {
            Log.d(TAG, "onClick() method for alertDeleteDialog, OK button");
            AlertDialogFragment deleteFragment = (AlertDialogFragment) ((Activity)mContext).getFragmentManager().
                    findFragmentByTag(DELETE_DIALOG_TAG);
            if (null != deleteFragment) {
                deleteFragment.dismissAllowingStateLoss();
            }
            if (binder != null) {
                binder.deleteFiles(TAG,
                        quickScanAdapterSprd.getCheckedFileInfoItemsList(), new HeavyOperationListener(
                                R.string.deleting));
            }
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }
    }

    private class RenameExtensionListener implements DialogInterface.OnClickListener{
        private final String mNewFilePath;
        private final FileInfo mSrcFile;

        public RenameExtensionListener(FileInfo fileInfo, String newFilePath) {
            mNewFilePath = newFilePath;
            mSrcFile = fileInfo;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (binder != null) {
                if (mActionMode != null) {
                    mActionMode.finish();
                }
                binder.rename(TAG, mSrcFile,
                        new FileInfo(mNewFilePath), new LightOperationListener(FileUtil.getFileName(mNewFilePath)));
            }
        }

    }

    private int location =-1;
    private boolean mIsAlertDialogShowing =false;
    private int getViewDirection() {
        return  getActivity().getWindow().getDecorView().getLayoutDirection();
    }


    /**
     *  Following are actionMode menus' operatation
     *  @author Xuehao.Jiang
     *  created at 2017/7/8 11:11
     */
    public void menuSelectOrCancelAll(){
        if(quickScanAdapterSprd!=null &&mActionMode!=null ){
            quickScanAdapterSprd.selectOrCancellAll();
            mActionMode.invalidate();
            mActionMode.setTitle(String.valueOf(quickScanAdapterSprd.getSelectedCount()));
        }
    }
    public void menuDelete(){
        if(!PermissionUtils.hasStorageWritePermission(mContext)){
            PermissionUtils.requestPermission((Activity) mContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_DELETE);

        } else{
            showDeleteDialog();
        }
    }
    public void menuCopy(){
        ArrayList<FileInfo> targets = new ArrayList<>();
        int size = quickScanAdapterSprd.getCheckedFileInfoItemsList().size();
        if(size>0){
            for(FileInfo fi:quickScanAdapterSprd.getCheckedFileInfoItemsList()){
                targets.add(fi);
            }
        }
        MountRootManagerRf.getInstance().setOperateFileInfos(targets);

        Intent intent = new Intent( );
        intent.setAction("android.intent.action.COPY_PASTE");
        intent.putExtra("paste_type",FileInfoManager.PASTE_MODE_COPY );

        startActivityForResult(intent,FileInfoManager.PASTE_MODE_COPY);
        if(mActionMode!=null){
            mActionMode.finish();
        }
    }
    public void menuCut(){
        ArrayList<FileInfo> targets = new ArrayList<>();
        int size = quickScanAdapterSprd.getCheckedFileInfoItemsList().size();
        if(size>0){
            for(FileInfo fi:quickScanAdapterSprd.getCheckedFileInfoItemsList()){
                targets.add(fi);
            }
        }
        MountRootManagerRf.getInstance().setOperateFileInfos(targets);

        Intent intent = new Intent( );
        intent.setAction("android.intent.action.CUT_PASTE");
        intent.putExtra("paste_type",FileInfoManager.PASTE_MODE_CUT );

        startActivityForResult(intent,FileInfoManager.PASTE_MODE_CUT);
        if(mActionMode!=null){
            mActionMode.finish();
        }
    }

    public void menuRename(){
        showRenameDialog();
    }
    public void menuDetails(){
        binder.getDetailInfo(TAG,
                quickScanAdapterSprd.getCheckedFileInfoItemsList().get(0), new  DetailInfoListener(
                        quickScanAdapterSprd.getCheckedFileInfoItemsList().get(0)));
    }
    public void menuShare(){

        List<FileInfo> files = quickScanAdapterSprd.getCheckedFileInfoItemsList();
        int size = files.size();
        List<FileInfo> datas = new ArrayList<FileInfo>();
        Log.d(TAG," menuShare:----> size="+size);
        for(int i=0; i< size;i++){
            if(!files.get(i).isFolder()){
                datas.add(files.get(i));
            }
        }
        FileUtil.startUpMultiShare(mContext,datas,binder);
        mActionMode.finish();
    }

    public void menusInstallAll(){

    }
    private void showSortDialog(){
        AlertDialog.Builder sortTypeDialog = new AlertDialog.Builder(
                mContext);
        sortTypeDialog.setTitle(R.string.menu_sort_type);

        final int sortType = sortSpf.getInt(FileInfoComparator.SORT_KEY,FileInfoComparator.SORT_BY_NAME);
        int selectItem = FileInfoComparator.getSelectItemByType(sortType);
        // if you not click any item, so we also  need init location
        location = selectItem;
        Log.d(TAG," showSortDialog:  sortType= "+sortType +" init  location="+location);

        sortTypeDialog.setSingleChoiceItems(R.array.sort_type, selectItem,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        location = whichButton;
                        Log.d(TAG," showSortDialog:  setSingleChoiceItems   location= "+location );
                    }
                });
        sortTypeDialog.setNegativeButton(R.string.sort_by_asc,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        int type=  sortType;
                        switch (location) {
                            case 0:
                                if(sortType ==  FileInfoComparator.SORT_BY_NAME){
                                    dialog.dismiss();
                                    return;
                                }
                                type = FileInfoComparator.SORT_BY_NAME;
                                break;
                            case 1:
                                if(sortType ==  FileInfoComparator.SORT_BY_TYPE){
                                    dialog.dismiss();
                                    return;
                                }
                                type = FileInfoComparator.SORT_BY_TYPE;
                                break;
                            case 2:
                                if(sortType ==  FileInfoComparator.SORT_BY_TIME){
                                    dialog.dismiss();
                                    return;
                                }
                                type = FileInfoComparator.SORT_BY_TIME;
                                break;
                            case 3:
                                if(sortType ==  FileInfoComparator.SORT_BY_SIZE){
                                    dialog.dismiss();
                                    return;
                                }
                                type = FileInfoComparator.SORT_BY_SIZE;
                                break;
                        }
                        if(quickScanAdapterSprd == null){
                            return;
                        }
                        sortSpf.edit().putInt(FileInfoComparator.SORT_KEY, type).commit();
                        Log.d(TAG," showSortDialog:  set new mSortBy= "+type);
                        Collections.sort(quickScanAdapterSprd.getFileInfoList(), FileInfoComparator.getInstance(sortType));
                        quickScanAdapterSprd.notifyDataSetChanged();
                    }
                });
        sortTypeDialog.setPositiveButton(R.string.sort_by_desc,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        int type=  sortType;
                        switch (location) {
                            case 0:
                                if(sortType ==  FileInfoComparator.SORT_BY_NAME_DESC){
                                    dialog.dismiss();
                                    return;
                                }
                                type = FileInfoComparator.SORT_BY_NAME_DESC;
                                break;
                            case 1:
                                if(sortType ==  FileInfoComparator.SORT_BY_TYPE_DESC){
                                    dialog.dismiss();
                                    return;
                                }
                                type = FileInfoComparator.SORT_BY_TYPE_DESC;
                                break;
                            case 2:
                                if(sortType ==  FileInfoComparator.SORT_BY_TIME_DESC){
                                    dialog.dismiss();
                                    return;
                                }
                                type = FileInfoComparator.SORT_BY_TIME_DESC;
                                break;
                            case 3:
                                if(sortType ==  FileInfoComparator.SORT_BY_SIZE_DESC){
                                    dialog.dismiss();
                                    return;
                                }
                                type = FileInfoComparator.SORT_BY_SIZE_DESC;
                                break;
                        }
                        if(quickScanAdapterSprd == null){
                            return;
                        }
                        sortSpf.edit().putInt(FileInfoComparator.SORT_KEY, type).commit();
                        Log.d(TAG," showSortDialog:  set new mSortBy= "+type);
                        Collections.sort(quickScanAdapterSprd.getFileInfoList(), FileInfoComparator.getInstance(sortType));
                        quickScanAdapterSprd.notifyDataSetChanged();
                    }
                });

         sortTypeDialog.show();
    }
}