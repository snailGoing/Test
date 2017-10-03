package com.sprd.fileexplore.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sprd.fileexplore.R;
import com.sprd.fileexplore.activity.FilePasteActivity;
import com.sprd.fileexplore.adapter.BanViewPager;
import com.sprd.fileexplore.adapter.DetailRecyclerViewAdapter;
import com.sprd.fileexplore.adapter.RecyclerItemClickListener;
import com.sprd.fileexplore.file.FileInfo;
import com.sprd.fileexplore.file.FileInfoComparator;
import com.sprd.fileexplore.file.FileInfoManager;
import com.sprd.fileexplore.file.FileInfoManager.NavigationRecord;
import com.sprd.fileexplore.load.ThreadPollManager;
import com.sprd.fileexplore.service.FileManageService;
import com.sprd.fileexplore.service.FileManageService.OperationEventListener;
import com.sprd.fileexplore.service.MountRootManagerRf;
import com.sprd.fileexplore.service.MyBinder;
import com.sprd.fileexplore.service.ProgressInfo;
import com.sprd.fileexplore.util.FileUtil;
import com.sprd.fileexplore.util.LogUtil;
import com.sprd.fileexplore.util.PermissionUtils;
import com.sprd.fileexplore.util.ToastHelper;
import com.sprd.fileexplore.view.AlertDialogFragment;
import com.sprd.fileexplore.view.AlertDialogFragment.*;
import com.sprd.fileexplore.view.AlertDialogFragment.EditTextDialogFragment.EditTextDoneListener;
import com.sprd.fileexplore.view.ProgressDialogFragment;
import com.sprd.fileexplore.view.SlowHorizontalScrollView;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Created by Xuehao.Jiang on 2017/4/6.
 */

public class DetailListFragment extends BaseFragment implements MountRootManagerRf.MountListener, View.OnClickListener {

    private static  final String TAG = DetailListFragment.class.getSimpleName();


    private Context mContext;
    private TabLayout tabLayout;
    private BanViewPager viewPager;
    private Handler mHandler;
    private DetailRecyclerViewAdapter adapter;
    private View mRecyclerViewContainer;
    private RecyclerView recyclerView;
    private TextView path;
    private View emptyView;
    private View mStandByView;
    private View dirContainer;
    private View navigationContainer;
    private SlowHorizontalScrollView mNavigationBar = null;
    private TabManager mTabManager = null;
    /** maximum tab text length */
    private static final int TAB_TET_MAX_LENGTH = 250;
    public static final String CREATE_FOLDER_DIALOG_TAG = "CreateFolderDialog";
    private int mTop = -1;


    private ActionMode mActionMode;
    private MountRootManagerRf mountRootManagerRf ;


    protected FileInfoManager mFileInfoManager = null;
    protected String mCurrentPath = null;
    protected int mSortType = 0;
    protected FileInfo mSelectedFileInfo = null;

    protected ToastHelper mToastHelper = null;
    private int first =0;

    private Dialog mDialog;

    /*
    *   Service Binder
    * */
    public MyBinder binder;
    protected boolean mServiceBinded = false;
    private SharedPreferences sortSpf ;

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
    SharedPreferences settings ;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"  onCreate()  ... ");

        Bundle bd = savedInstanceState;
        if(bd != null){
            mCurrentPath = bd.getString(KEY_PATH);
        }else{
            Log.d(TAG,"  onCreate: bundle ==null");
        }
        setHasOptionsMenu(true);
        mContext = getActivity();

        sortSpf = mContext
                .getSharedPreferences(FileInfoComparator.SORT_KEY, 0);
        settings = getDefaultSharedPreferences(mContext);

        mountRootManagerRf  = MountRootManagerRf.getInstance();
        mountRootManagerRf.registerMountListener(this);
        mHandler = new Handler(getActivity().getMainLooper());
        mToastHelper = new ToastHelper(mContext);

        /*
        *
        *  bind service to operate some tasks
        * */
        mContext.bindService(new Intent(mContext.getApplicationContext(), FileManageService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE);

    }

    public void setDirNavigation(View view){
        // set up a sliding navigation bar for navigation view
        mNavigationBar = (SlowHorizontalScrollView) view.findViewById(R.id.navigation_bar);
        if (mNavigationBar != null) {
            mNavigationBar.setVerticalScrollBarEnabled(false);
            mNavigationBar.setHorizontalScrollBarEnabled(false);
            mTabManager = new TabManager(mNavigationBar);
        }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.detail_list_fragment, container, false);

        dirContainer = view.findViewById(R.id.dir_container);
        mRecyclerViewContainer = dirContainer.findViewById(R.id.detailed_recyclerview_container);
        navigationContainer = dirContainer.findViewById(R.id.bar_background);

        recyclerView = (RecyclerView)view.findViewById(R.id.detailed_recyclerview);
        path= (TextView)view.findViewById(R.id.detailed_path_text);
        emptyView=view.findViewById(R.id.empty_detailed);
        mStandByView  = view.findViewById(R.id.file_explore_sorting_standby_layout);

        recyclerView.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        setDirNavigation(dirContainer);
        adapter = new DetailRecyclerViewAdapter(mContext,this.itemClickListener,this );
        if(recyclerView!=null){
            Log.d(TAG, "   setAdapter ..." );
            recyclerView.setAdapter(adapter);
        }
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(adapter!=null&& adapter.imageLoader!=null){
                    switch (newState){
                        case 0:
                            adapter.imageLoader.resume();
                            break;
                        case 1:   // 1 means screnn is scrolling with touch by hand or hand in touch with screen
                        case 2:   // 2 means because of auto scrolling by operating
                            adapter.imageLoader.pause();
                            break;
                    }
                }
                scrollStateChanged(recyclerView,newState);

            }
        });
        return view;
    }

    public void scrollStateChanged(RecyclerView recyclerView, int newState) {
        if (null == recyclerView) {
            return;
        }
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager linearManager = (LinearLayoutManager) layoutManager;

            int endPos = linearManager.findLastVisibleItemPosition();
            int startPos = linearManager.findFirstVisibleItemPosition();
            Log.d(TAG," scrollStateChanged--->   startPos= "+startPos+" endPos= "+endPos);
            ArrayList<String> newTaskUrl = new ArrayList<String>();

            int len = adapter.getItemCount();
            int pos = -1;
            FileInfo file = null;
            for (pos = startPos; pos <= endPos && pos < len; pos++) {//Bug229761
                file = adapter.getCurrentPosFileInfo(pos);
                if (file != null) {
                    newTaskUrl.add(file.getPath());
                }
            }

            if (0 == newState) {
                adapter.setScrolling(false);
                adapter.notifyDataSetChanged();

            } else {
                adapter.setScrolling(true);
            }
            ThreadPollManager.getInstance().updateWorkQueue(newTaskUrl,adapter.mScrolling);
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
            Log.d( TAG,"onServiceConnected");
            binder = (MyBinder)service;
            mServiceBinded = true;

            Log.d(TAG, "onServiceConnected   start.." );
            binder = (MyBinder)service;

            Log.d(TAG, "onServiceConnected   mFileInfoManager -->"+TAG );
            mFileInfoManager = binder.initFileInfoManager(TAG);
            if(mCurrentPath==null){
                mCurrentPath = initCurrentFileInfo();
            }
            if (mCurrentPath != null) {
                mTabManager.refreshTab(mCurrentPath);
                // this obtain fileinfo list by servie
                reloadFileInfos();
            }
            adapter.refreshAdapter(mFileInfoManager);
            Log.d(TAG, "onServiceConnected   end ..." );
        }
    };
    private RecyclerItemClickListener itemClickListener = new RecyclerItemClickListener(){

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
            String path = adapter.getCurrentPosFileInfo(position).getPath();
            if(mountRootManagerRf != null && mountRootManagerRf.isMountPoint(path)){
                return;
            }
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

        if (position >= adapter.getItemCount() || position < 0) {
            Log.e(TAG, "onItemClickExe,events error,mFileInfoList.size(): "
                    + adapter.getItemCount());
            return;
        }
        FileInfo selecteItemFileInfo = (FileInfo) adapter.getCurrentPosFileInfo(position);

        if (selecteItemFileInfo.isFolder()) {
            int top = v.getTop();
            Log.d(TAG, "onItemClickExe, fromTop = " + top);
            showDirectoryFileInfo(selecteItemFileInfo.getPath());
        } else {
            FileUtil.startUpFileByIntent(mContext,selecteItemFileInfo,binder);
        }
    }
    /**
    *  true: means pass key event to parent activty; false : means fragment to handle it
    *  @author Xuehao.Jiang
    *  created at 2017/6/30 22:06
    */
    @Override
    public boolean onBackPressed() {
        Log.d(TAG, "onBackPressed");
        if (binder != null && binder.isBusy(TAG)) {
            Log.i(TAG, "onBackPressed, service is busy. ");
            return false ;
        }
        if (mCurrentPath != null && !mountRootManagerRf.isRootPath(mCurrentPath)) {
            NavigationRecord navRecord = mFileInfoManager.getPrevNavigation();
            String prevPath = null;
            if(navRecord!=null){
                prevPath = navRecord.getRecordPath();
                mSelectedFileInfo = navRecord.getSelectedFile();
                mTop = navRecord.getTop();
//                if (prevPath != null) {
//                    mTabManager.showPrevNavigationView(prevPath);
//                    Log.d(TAG, "sonBackPressed,prevPath = " + prevPath);
//                    return false;
//                }
            }
            if(!mountRootManagerRf.isMountPoint(mCurrentPath) ){
                Log.d(TAG, "onBackPressed    1");
                prevPath = new File(mCurrentPath).getParent();
            }else{
                Log.d(TAG, "onBackPressed    2");
                prevPath = initCurrentFileInfo();
            }
            Log.d(TAG, "onBackPressed    prevPath="+prevPath);
            showDirectoryFileInfo(prevPath);
            return false;
        }
        Log.d(TAG, "onBackPressed   end --> mCurrentPath="+mCurrentPath);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean isShowHide = settings.getBoolean("display_hide_file",false);
        int showType = FileManageService.FILE_FILTER_TYPE_UNKOWN;
        if(binder!=null){
            showType = binder.getListType(TAG);
            Log.d(TAG,"onResume()...mCurrentPath="+mCurrentPath +"  isShowHide="+ isShowHide
                    +"  showType="+showType);
            if((isShowHide && showType == FileManageService.FILE_FILTER_TYPE_DEFAULT )
                    ||(!isShowHide && showType == FileManageService.FILE_FILTER_TYPE_ALL)){
                showDirectoryFileInfo(mCurrentPath);
            }else{
                reloadFileInfos();
            }
        }
    }

    private void reloadFileInfos() {
        Log.d(TAG, "reloadFileInfos ... mCurrentPath="+mCurrentPath +" binder="+(binder==null ? "null" :"non-null"));
        if (binder != null && !binder.isBusy(TAG)) {
            Log.d(TAG, "reloadFileInfos ... binder no busy, to go!");
            if (mountRootManagerRf!=null && mountRootManagerRf.isRootPath(mCurrentPath) ||
                    (mFileInfoManager != null && mFileInfoManager.isPathModified(mCurrentPath))) {
                showDirectoryFileInfo(mCurrentPath);
            } else if (mFileInfoManager != null && adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }else{
            Log.d(TAG, "reloadFileInfos ... binder  busy, to leave!");
        }
    }
    /**
     * This method gets all files/folders from a directory and displays them in
     * the list view
     *
     * @param path
     *            the directory path
     */
    public void showDirectoryFileInfo(String path) {
        Log.d(TAG, "showDirectoryFileInfo,path = " + path);
        if (getActivity().isFinishing()) {
            Log.i(TAG, "showDirectoryFileInfo,isFinishing: true, do not loading again");
            return;
        }
        mCurrentPath = path;
        if (binder != null) {
            boolean isShowHide = settings.getBoolean("display_hide_file",false);
            Log.d(TAG, "showDirectoryFileInfo,path   listFiles  to start... isShowHide="+isShowHide );
            binder.setListType(
                    !isShowHide ? FileManageService.FILE_FILTER_TYPE_DEFAULT
                            : FileManageService.FILE_FILTER_TYPE_ALL, TAG);
            binder.listFiles(TAG, mCurrentPath, new RecyclerViewListener());
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        if(binder!=null && mContext!=null && mServiceBinded){
            Log.d(TAG," onDestroy()  : unbindService success!");
            mContext.unbindService(mServiceConnection);
            mServiceBinded = false;
        }else{
            Log.d(TAG," onDestroy()  : unbindService fail !");
        }
        if(mountRootManagerRf!=null){
            mountRootManagerRf.unregisterMountListener(this);
        }

    }

    public DetailListFragment setTabLayout(TabLayout mTabLayout){
        Log.d(TAG, "setTabLayout");
        this.tabLayout = mTabLayout;
        return this;
    }


    public DetailListFragment setViewPager(BanViewPager mViewPager){
        Log.d(TAG, "setTabLayout");
        this.viewPager = mViewPager;
        return this;
    }
    //List item select method
    private void onListItemSelect(int position) {
        adapter.toggleSelection(position);//Toggle the selection
        setActionMode(adapter.getSelectedCount());
    }

    private void setActionMode(int countSelected) {
        boolean hasCheckedItems = countSelected > 0;//Check if any items are already selected or not

        if (hasCheckedItems && mActionMode == null)
            // there are some selected items, start the actionMode
            mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);

        else if (!hasCheckedItems && mActionMode != null){
            // there no selected items, finish the actionMode
            // mActionMode.finish();
        }
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
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback(){
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {


            if(tabLayout!=null) {
                tabLayout.setVisibility(View.GONE);
                navigationContainer.setVisibility(View.GONE);
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
                case R.id.menu_clear:   menuClear();    break;
                case R.id.menu_rename:  menuRename();   break;
                case R.id.menu_detail:  menuDetails();  break;
                case R.id.menu_share:   menuShare();    break;
                default:    Log.d(TAG, "default: "+ item.getItemId());  break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.removeSelection();
            setNullToActionMode();
            if(tabLayout!=null) tabLayout.setVisibility(View.VISIBLE);
            if(viewPager!=null) viewPager.setNoScroll(false);
            if(navigationContainer!=null) navigationContainer.setVisibility(View.VISIBLE);
        }
    };
    public void refreActionModeMenu(ActionMode mode, Menu menu){

        if(menu==null) return;
        MenuItem menuDelete = menu.findItem(R.id.menu_delete);
        MenuItem menuCopy = menu.findItem(R.id.menu_copy);
        MenuItem menuCut = menu.findItem(R.id.menu_cut);

        MenuItem menuSelet = menu.findItem(R.id.menu_select);
        MenuItem menuClear = menu.findItem(R.id.menu_clear);
        MenuItem menuRename = menu.findItem(R.id.menu_rename);
        MenuItem menuDetail = menu.findItem(R.id.menu_detail);
        MenuItem menuShare = menu.findItem(R.id.menu_share);

        int sum = adapter.getItemCount();
        int selectCount = adapter.getSelectedCount();

        Log.d(TAG,"   ActionMode onPrepareActionMode--> sum= "+sum +" selectCount= "+selectCount);
        if(selectCount == 0){
            menuDelete.setEnabled(false).setIcon(R.mipmap.menu_trash_disable);
            menuCopy.setEnabled(false).setIcon(R.mipmap.menu_copy_disable);
            menuCut.setEnabled(false).setIcon(R.mipmap.menu_cut_disable);

            menuClear.setEnabled(false);
            menuRename.setEnabled(false);
            menuDetail.setEnabled(false);
            menuShare.setEnabled(false);
        }else {
            menuDelete.setEnabled(true).setIcon(R.mipmap.menu_trash);
            menuCopy.setEnabled(true).setIcon(R.mipmap.menu_copy);
            menuCut.setEnabled(true).setIcon(R.mipmap.menu_cut);
            boolean hasFolder = false;
            for(FileInfo f: adapter.getCheckedFileInfoItemsList()){
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
                menuClear.setEnabled(false);
                menuRename.setEnabled(false);
                menuDetail.setEnabled(false);
            }else{
                FileInfo selected = adapter.getCheckedFileInfoItemsList().get(0);
                if(selected.isFolder()){
                    menuClear.setEnabled(true);
                }else{
                    menuClear.setEnabled(false);
                }
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
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult, requestCode:" + requestCode);
        if(permissions == null || permissions.length == 0 ||
                grantResults == null || grantResults.length == 0){
            Log.d(TAG, "onRequestPermissionsResult, Permission or grant res null");
            return;
        }
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_RENAME:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(isResumed()){
                        //showRenameDialog();
                    } else{
                        // showRenameOnResume = true;
                    }
                } else {
                    if(!PermissionUtils.showWriteRational((Activity) mContext)){
                        mToastHelper.showToast(R.string.error_permissions);
                    }
                }
                break;
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_DELETE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(isResumed()){
                        showDeleteDialog();
                    } else{
                        //showDeleteOnResume = true;
                    }
                } else {
                    if(!PermissionUtils.showWriteRational((Activity)mContext)){
                        mToastHelper.showToast( R.string.error_permissions);
                    }
                }
                break;
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_CUT:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mFileInfoManager.savePasteList(FileInfoManager.PASTE_MODE_CUT, adapter
                            .getCheckedFileInfoItemsList());
                    if (mActionMode != null) {
                        mActionMode.finish();
                    }
                } else {
                    if(!PermissionUtils.showWriteRational((Activity)mContext)){
                        mToastHelper.showToast( R.string.error_permissions);
                    }
                }
                break;
            case PERMISSIONS_REQUEST_TO_READ_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent();
//                    intent.setClass( mContext, FileManagerSearchActivity.class);
//                    intent.putExtra(FileManagerSearchActivity.CURRENT_PATH, mCurrentPath);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    if(!PermissionUtils.showReadRational((Activity)mContext)){
                        mToastHelper.showToast( R.string.error_permissions);
                    }
                }
                break;
            default:
                //do nothing
        }

    }

    protected String initCurrentFileInfo() {
        return mountRootManagerRf.getRootPath();
    }

    @Override
    public void notifyStorageChanged(String path, String oldState, String newState) {
        String changedPath = path;
        String newSt = newState;
        Log.d(TAG,"notifyStorageChanged: changedPath= "+changedPath +"  newState="+ newSt);

        if(mountRootManagerRf.isMountPoint(changedPath)){
            if(MountRootManagerRf.STATUS_EJECTED.equals(newState)||MountRootManagerRf.STATUS_UNMOUNTED.equals(newState)
                    || MountRootManagerRf.STATUS_BAD_REMOVAL.equals(newState)){
                // case 1: non root,but this scanning storage is ejected
                if(mCurrentPath !=null && mCurrentPath.startsWith(path) || mountRootManagerRf.isRootPath(mCurrentPath)){
                    // refresh root datas
                    mCurrentPath = initCurrentFileInfo();
                    showDirectoryFileInfo(mCurrentPath);
                }
            }else if(MountRootManagerRf.STATUS_MOUNTED.equals(newState)){
                // if current displaying storage root
                if(mCurrentPath!=null && mountRootManagerRf.isRootPath(mCurrentPath)){
                    showDirectoryFileInfo(mCurrentPath);
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (binder.isBusy(this.getClass().getName())) {
            Log.d(TAG, "onClick(), service is busy.");
            return;
        }
        int id = v.getId();
        Log.d(TAG, "onClick() id=" + id);
        mTabManager.updateNavigationBar(id);
    }


    private int restoreSelectedPosition() {
        if (mSelectedFileInfo == null) {
            return -1;
        } else {
            int curSelectedItemPosition = adapter.getPosition(mSelectedFileInfo);
            mSelectedFileInfo = null;
            return curSelectedItemPosition;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle bd = new Bundle();
        bd.putString(KEY_PATH,mCurrentPath);
    }
    private static  final String KEY_PATH= "key_path";
    public void setEmptyViewShow(boolean isShow){
        if(isShow){
            mRecyclerViewContainer.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }else{
            emptyView.setVisibility(View.GONE);
            mRecyclerViewContainer.setVisibility(View.VISIBLE);
        }
    }

    private class TabManager {
        private final List<String> mTabNameList = new ArrayList<String>();
        protected LinearLayout mTabsHolder = null;
        private String mCurFilePath = null;
        private final Button mBlankTab;
        private LinearLayout.LayoutParams mBlanckBtnParam = null;

        private TabManager(View v) {
            mTabsHolder = (LinearLayout) v.findViewById(R.id.tabs_holder);
            mBlankTab = new Button(mContext);
            mBlankTab.setBackgroundDrawable(getResources().getDrawable(R.mipmap.fm_blank_tab));//fm_blank_tab
            mBlanckBtnParam = new LinearLayout.LayoutParams(
                    new ViewGroup.MarginLayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT));

            mBlankTab.setLayoutParams(mBlanckBtnParam);
            mTabsHolder.addView(mBlankTab);
        }

        private void refreshTab(String initFileInfo) {

            int count = mTabsHolder.getChildCount();
            Log.d(TAG, "refreshTab,initFileInfo = " + initFileInfo +" before remove:  mTabsHolder count="+ count);
            mTabsHolder.removeViews(0, count);
            int m = mTabsHolder.getChildCount();
            Log.d(TAG, "refreshTab,  after remove : mTabsHolder count = " +m  );

            mTabNameList.clear();

            if (getViewDirection() == ViewGroup.LAYOUT_DIRECTION_LTR) {
                mBlanckBtnParam.setMargins((int) getResources().getDimension(R.dimen.tab_margin_left), 0,
                        (int) getResources().getDimension(R.dimen.tab_margin_right), 0);
            } else if (getViewDirection() == ViewGroup.LAYOUT_DIRECTION_RTL) {
                mBlanckBtnParam.setMargins((int) getResources().getDimension(R.dimen.tab_margin_right), 0,
                        (int) getResources().getDimension(R.dimen.tab_margin_left), 0);
            }
            mBlankTab.setLayoutParams(mBlanckBtnParam);

            mCurFilePath = initFileInfo;
            if (mCurFilePath != null) {
                addTab(MountRootManagerRf.HOME);
                if (!mountRootManagerRf.isRootPath(mCurFilePath)) {
                    String path = mountRootManagerRf.getDescriptionPath(mCurFilePath);
                    String[] result = path.split(MountRootManagerRf.SEPARATOR);
                    for (String string : result) {
                        addTab(string);
                    }

                    if (getViewDirection() == ViewGroup.LAYOUT_DIRECTION_LTR) {
                        // scroll to right with slow-slide animation
                        startActionBarScroll();
                    } else if (getViewDirection() == ViewGroup.LAYOUT_DIRECTION_RTL) {
                        // scroll horizontal view to the right
                        mNavigationBar.startHorizontalScroll(-mNavigationBar.getScrollX(),
                                -mNavigationBar.getRight());
                    }
                }
            }
            updateHomeButton();
        }

        private void startActionBarScroll() {
            // scroll to right with slow-slide animation
            // To pass the Launch performance test, avoid the scroll
            // animation when launch.
            int tabHostCount = mTabsHolder.getChildCount();
            int navigationBarCount = mNavigationBar.getChildCount();
            if ((tabHostCount > 2) && (navigationBarCount >= 1)) {
                View view = mNavigationBar.getChildAt(navigationBarCount - 1);
                if (null == view) {
                    Log.d(TAG, "startActionBarScroll, navigationbar child is null");
                    return;
                }
                int width = view.getRight();
                mNavigationBar.startHorizontalScroll(mNavigationBar.getScrollX(), width
                        - mNavigationBar.getScrollX());
            }
        }

        private void updateHomeButton() {
            ImageButton homeBtn = (ImageButton) mTabsHolder.getChildAt(0);
            if (homeBtn == null) {
                Log.w(TAG, "HomeBtm is null,return.");
                return;
            }
            Resources resources = getResources();
            Log.d(TAG,"   updateHomeButton: mTabsHolder count= "+ mTabsHolder.getChildCount());
            if (mTabsHolder.getChildCount() == 2) { // two tabs: home tab +
                // blank
                // tab
                homeBtn.setBackgroundDrawable(resources
                        .getDrawable(R.drawable.custom_home_ninepatch_tab));
                homeBtn.setImageDrawable(resources.getDrawable(R.mipmap.ic_home_text));
                homeBtn.setPadding((int) resources.getDimension(R.dimen.home_btn_padding), 0,
                        (int) resources.getDimension(R.dimen.home_btn_padding), 0);
            } else {
                homeBtn.setBackgroundDrawable(resources
                        .getDrawable(R.drawable.custom_home_ninepatch_tab));
                homeBtn.setImageDrawable(resources.getDrawable(R.mipmap.ic_home));
            }
        }

        /**
         * This method updates the navigation view to the previous view when
         * back button is pressed
         *
         * @param newPath
         *            the previous showed directory in the navigation history
         */
        private void showPrevNavigationView(String newPath) {

            refreshTab(newPath);
            showDirectoryFileInfo(newPath);
        }

        /**
         * This method creates tabs on the navigation bar
         *
         * @param text
         *            the name of the tab
         */
        private void addTab(String text) {

            LinearLayout.LayoutParams mlp = null;
            Log.d(TAG,"addTab  text ="+ text+"  removeView  ---> mBlankTab");
            // need to clear blank Button( In the end of mTabsHolder ,and then add another new path Button  and blank Button )
            mTabsHolder.removeView(mBlankTab);
            View viewLikeBtn = null;
            if (mTabNameList.isEmpty()) {
                viewLikeBtn = new ImageButton(mContext);
                mlp = new LinearLayout.LayoutParams(new ViewGroup.MarginLayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT));
                mlp.setMargins(0, 0, 0, 0);
                viewLikeBtn.setLayoutParams(mlp);
            } else {
                Button button = new Button(mContext);
                button.setTextColor(Color.BLACK);

                button.setBackgroundResource(R.drawable.custom_tab);
                button.setMaxWidth(TAB_TET_MAX_LENGTH);
                FileUtil.fadeOutLongString(((TextView) button));
                button.setText(text);

                mlp = new LinearLayout.LayoutParams(new ViewGroup.MarginLayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT));

                if (getViewDirection() == ViewGroup.LAYOUT_DIRECTION_LTR) {
                    mlp.setMargins((int) getResources().getDimension(R.dimen.tab_margin_left), 0, 0, 0);
                } else if (getViewDirection() == ViewGroup.LAYOUT_DIRECTION_RTL) {
                    mlp.setMargins(0, 0, (int) getResources().getDimension(R.dimen.tab_margin_left), 0);
                }
                button.setLayoutParams(mlp);
                viewLikeBtn = button;
            }
            viewLikeBtn.setOnClickListener(DetailListFragment.this);
            viewLikeBtn.setId(mTabNameList.size());
            Log.d(TAG,"addTab    addView  ---> viewLikeBtn  ");
            mTabsHolder.addView(viewLikeBtn);
            mTabNameList.add(text);

            // add blank tab to the tab holder end
            Log.d(TAG,"addTab    addView  ---> mBlankTab  ");
            mTabsHolder.addView(mBlankTab);
            viewLikeBtn.setOutlineProvider(null);
        }
        /**
         * The method updates the navigation bar
         *
         * @param id
         *            the tab id that was clicked
         */
        private void updateNavigationBar(int id) {
            Log.d(TAG, "updateNavigationBar,id = " + id +"  nameList size="+  mTabNameList.size());
            int j =0;
            for(String str: mTabNameList){
                Log.d(TAG," updateNavigationBar: name["+j+"]= "+ str);
                j++;
            }
            // click current button do not response
            if (id < mTabNameList.size() - 1) {
                String oldPath = mCurFilePath;
                int count = mTabNameList.size() - id;
                mTabsHolder.removeViews(id + 1, count);

                for (int i = 1; i < count; i++) {
                    // update mTabNameList
                    mTabNameList.remove(mTabNameList.size() - 1);
                }
                mTabsHolder.addView(mBlankTab);

                if (id == 0) {
                    mCurFilePath = mountRootManagerRf.getRootPath();
                } else {
                    // get mount point path
                    String mntPointPath = mountRootManagerRf.getRealMountPointPath(mCurFilePath);
                    Log.d(TAG, "mntPointPath: " + mntPointPath + " for mCurFilepath: " + mCurFilePath);
                    String path = mCurFilePath.substring(mntPointPath.length() + 1);
                    StringBuilder sb = new StringBuilder(mntPointPath);
                    String[] pathParts = path.split(MountRootManagerRf.SEPARATOR);
                    // id=0,1 is for Home button and mnt point button, so from id = 2 to get other parts of path
                    for (int i = 2; i <= id; i++) {
                        sb.append(MountRootManagerRf.SEPARATOR);
                        sb.append(pathParts[i - 2]);
                    }
                    mCurFilePath = sb.toString();
                    Log.d(TAG, "to enter file path: " + mCurFilePath);
                }
                int top = -1;
                int pos = -1;
                FileInfo selectedFileInfo = null;
                if (adapter.getItemCount() > 0) {
                        pos = 0;
                        selectedFileInfo = adapter.getCurrentPosFileInfo(pos);
                        top = 0;
                        Log.d(TAG, "updateNavigationBar, pos: " + pos + " top: " + top);
                        addToNavigationList(oldPath, selectedFileInfo, top);

                } else {
                    addToNavigationList(oldPath, null, top);
                }
                showDirectoryFileInfo(mCurFilePath);
                updateHomeButton();
            }
        }

    }

    private int getViewDirection() {
        return mNavigationBar.getParent().getParent().getLayoutDirection();
    }

    /**
     * This method add a path into navigation history list
     *
     * @param path
     *            the path that should be added
     */
    private void addToNavigationList(String path, FileInfo selectedFileInfo, int top) {
        mFileInfoManager.addToNavigationList(new NavigationRecord(path, selectedFileInfo, top));
    }

    /**
     * This method clear navigation history list
     */
    private void clearNavigationList() {
        if(mFileInfoManager != null){
            mFileInfoManager.clearNavigationList();
        }
    }
    private void onPathChanged() {
        Log.d(TAG, "onPathChanged");
        if (mTabManager != null) {
            mTabManager.refreshTab(mCurrentPath);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_detail_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // this will update menus item
        MenuItem newFolder= menu.findItem(R.id.menu_new_folder);
        MenuItem sortBy= menu.findItem(R.id.menu_sort_by);
        if(newFolder != null && sortBy != null){
            boolean isRoot =MountRootManagerRf.getInstance().isRootPath(mCurrentPath);
            Log.d(TAG,"onPrepareOptionsMenu--->  mCurrentPath =  " +mCurrentPath +" isRoot="+isRoot);
            if(mCurrentPath!=null && isRoot){
                newFolder.setVisible(false);
                sortBy.setVisible(false);
            }else if(mCurrentPath!=null ){
                newFolder.setVisible(true);
                sortBy.setVisible(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_folder:
                createFolder();
                break;
            case R.id.menu_sort_by:
                showSortDialog();
                break;
            default:
                return super.onOptionsItemSelected(item);

        }
        return true;
    }


    private void createFolder(){
        Log.d(TAG," createFolder");
        if (mIsAlertDialogShowing) {
            Log.d(TAG, "Another Dialog showing, return!~~");
            return;
        }
        if (isResumed()) {
            mIsAlertDialogShowing =true;
            EditDialogFragmentBuilder builder = new EditDialogFragmentBuilder();
            builder.setDefault("", 0).setDoneTitle(R.string.ok).setCancelTitle(R.string.cancel)
                    .setTitle(R.string.new_folder);
            EditTextDialogFragment createFolderDialogFragment = builder.create();
            createFolderDialogFragment.setOnEditTextDoneListener(new CreateFolderListener());
            createFolderDialogFragment.setOnDialogDismissListener(onDialogDismissListener);
            try {
                createFolderDialogFragment.show(((Activity)mContext).getFragmentManager(), CREATE_FOLDER_DIALOG_TAG);
                boolean ret = ((Activity)mContext).getFragmentManager().executePendingTransactions();
                Log.d(TAG, "executing pending transactions result: " + ret);
            } catch (IllegalStateException e) {
                Log.d(TAG, "call show dialog after onSaveInstanceState " + e);
                if (createFolderDialogFragment != null) {
                    createFolderDialogFragment.dismissAllowingStateLoss();
                }
            }
        }
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
                                    recyclerView.requestFocus();
                                    return;
                                }
                                type = FileInfoComparator.SORT_BY_NAME;
                                break;
                            case 1:
                                if(sortType ==  FileInfoComparator.SORT_BY_TYPE){
                                    dialog.dismiss();
                                    recyclerView.requestFocus();
                                    return;
                                }
                                type = FileInfoComparator.SORT_BY_TYPE;
                                break;
                            case 2:
                                if(sortType ==  FileInfoComparator.SORT_BY_TIME){
                                    dialog.dismiss();
                                    recyclerView.requestFocus();
                                    return;
                                }
                                type = FileInfoComparator.SORT_BY_TIME;
                                break;
                            case 3:
                                if(sortType ==  FileInfoComparator.SORT_BY_SIZE){
                                    dialog.dismiss();
                                    recyclerView.requestFocus();
                                    return;
                                }
                                type = FileInfoComparator.SORT_BY_SIZE;
                                break;
                        }
                        if(adapter == null){
                            return;
                        }
                        sortSpf.edit().putInt(FileInfoComparator.SORT_KEY, type).commit();
                        Log.d(TAG," showSortDialog:  set new mSortBy= "+type);
                        binder.sortBy(TAG,type,new SortOperationListener());
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
                                    recyclerView.requestFocus();
                                    return;
                                }
                                type = FileInfoComparator.SORT_BY_NAME_DESC;
                                break;
                            case 1:
                                if(sortType ==  FileInfoComparator.SORT_BY_TYPE_DESC){
                                    dialog.dismiss();
                                    recyclerView.requestFocus();
                                    return;
                                }
                                type = FileInfoComparator.SORT_BY_TYPE_DESC;
                                break;
                            case 2:
                                if(sortType ==  FileInfoComparator.SORT_BY_TIME_DESC){
                                    dialog.dismiss();
                                    recyclerView.requestFocus();
                                    return;
                                }
                                type = FileInfoComparator.SORT_BY_TIME_DESC;
                                break;
                            case 3:
                                if(sortType ==  FileInfoComparator.SORT_BY_SIZE_DESC){
                                    dialog.dismiss();
                                    recyclerView.requestFocus();
                                    return;
                                }
                                type = FileInfoComparator.SORT_BY_SIZE_DESC;
                                break;
                        }
                        if(adapter == null){
                            return;
                        }
                        sortSpf.edit().putInt(FileInfoComparator.SORT_KEY, type).commit();
                        Log.d(TAG," showSortDialog:  set new mSortBy= "+type);
                        binder.sortBy(TAG,type,new SortOperationListener());
                    }
                });

        mDialog = sortTypeDialog.show();
    }
    private void showDeleteDialog() {
        Log.d(TAG, "show DeleteDialog...");
        if (mIsAlertDialogShowing) {
            Log.d(TAG, "Another Dialog is exist, return!~~");
            return;
        }
        int alertMsgId = R.string.alert_delete_multiple;
        if (adapter.getSelectedCount() == 1) {
            alertMsgId = R.string.alert_delete_single;
        } else {
            alertMsgId = R.string.alert_delete_multiple;
        }

        if (isResumed()) {
            mIsAlertDialogShowing = true;
            AlertDialogFragmentBuilder builder = new AlertDialogFragmentBuilder();
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
        FileInfo fileInfo = adapter.getFirstCheckedFileInfoItem();
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
                EditDialogFragmentBuilder builder = new EditDialogFragmentBuilder();
                builder.setDefault(name, selection).setDoneTitle(R.string.done).setCancelTitle(
                        R.string.cancel).setTitle(R.string.rename);
                EditTextDialogFragment renameDialogFragment = builder.create();
                renameDialogFragment.setOnEditTextDoneListener(new RenameDoneListener(fileInfo));
                renameDialogFragment.setOnDialogDismissListener(onDialogDismissListener);
                renameDialogFragment.show(((Activity)mContext).getFragmentManager(), RENAME_DIALOG_TAG);
                boolean ret = ((Activity)mContext).getFragmentManager().executePendingTransactions();
                Log.d(TAG, "executing pending transactions result: " + ret);
            }
        }
    }
    private void showClearDialog(){
        Log.d(TAG, "show ClearDialog...");
        if (mIsAlertDialogShowing) {
            Log.d(TAG, "Another Dialog showing, return!~~");
            return;
        }
        FileInfo fileInfo = adapter.getFirstCheckedFileInfoItem();

        if (fileInfo != null && fileInfo.isFolder()) {

            int alertMsgId = R.string.alert_clear_folder;
            if (isResumed()) {
                mIsAlertDialogShowing = true;
                AlertDialogFragmentBuilder builder = new AlertDialogFragmentBuilder();
                AlertDialogFragment deleteDialogFragment = builder.setMessage(alertMsgId).setDoneTitle(
                        R.string.ok).setCancelTitle(R.string.cancel).setIcon(
                        R.mipmap.ic_dialog_alert_holo_light).setTitle(R.string.clear).create();
                deleteDialogFragment.setOnDoneListener(new ClearListener());
                deleteDialogFragment.setOnDialogDismissListener(onDialogDismissListener);
                deleteDialogFragment.show(((Activity)mContext).getFragmentManager(), CLEAR_DIALOG_TAG);
                boolean ret = ((Activity)mContext).getFragmentManager().executePendingTransactions();
                Log.d(TAG, "executing pending transactions result: " + ret);
            }
        }else{
            Log.d(TAG, "showClearDialog can clear file  ,only for folder, return!~~");
            return;
        }
    }

    protected void showRenameExtensionDialog(FileInfo srcfileInfo, final String newFilePath){
        Log.d(TAG, "show RenameExtensionDialog...");

        AlertDialogFragmentBuilder builder = new AlertDialogFragmentBuilder();
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

    private OnDialogDismissListener  onDialogDismissListener=  new OnDialogDismissListener(){

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
                    FileInfo fileInfo = mFileInfoManager.updateOneFileInfoList(mCurrentPath,mSortType);
                    adapter.refreshAdapter(mFileInfoManager);
                    if (fileInfo != null) {
                        int postion = adapter.getPosition(fileInfo);
                        Log.d(TAG, "LightOperation postion = " + postion);
                        recyclerView.getLayoutManager().scrollToPosition(postion);
                    }else{
                        Log.d(TAG, "LightOperation fileInfo = =null" );
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
                    if(adapter!=null){
                        adapter.refreshAdapter(mFileInfoManager);
                        Log.d(TAG," SortOperationListener done");
                    }
                    break;
            }
        }
    }
    private class RecyclerViewListener implements FileManageService.OperationEventListener{
        public static final String LIST_DIALOG_TAG = "RecyclerViewDialogFragment";
        @Override
        public void onTaskPrepare() {

        }

        @Override
        public void onTaskProgress(ProgressInfo progressInfo) {

            Log.d(TAG, "  onTaskProgress    progressInfo ="+progressInfo);
        }

        @Override
        public void onTaskResult(int result) {

            if(sortSpf!=null){
                mSortType = sortSpf.getInt(FileInfoComparator.SORT_KEY,FileInfoComparator.SORT_BY_NAME);
            }
            mFileInfoManager.loadFileInfoList(mCurrentPath,mSortType);

            int size =mFileInfoManager.getShowFileList().size();
            Log.d(TAG, " RecyclerViewListener,   TaskResult result = " + result +"file sum size="+size);

            adapter.refreshAdapter(mFileInfoManager);
            int selectedItemPosition = restoreSelectedPosition();
            // this can restore old position showed in UI
            onPathChanged();
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
                case OperationEventListener.ERROR_CODE_COPY_NO_PERMISSION:
                    if (!mPermissionToast) {
                        mToastHelper.showToast(R.string.copy_deny);
                        mPermissionToast = true;
                    }
                    break;
                case OperationEventListener.ERROR_CODE_DELETE_NO_PERMISSION:
                    if (!mPermissionToast) {
                        mToastHelper.showToast(R.string.delete_deny);
                        mPermissionToast = true;
                    }
                    break;
                case OperationEventListener.ERROR_CODE_DELETE_UNSUCCESS:
                    if (!mOperationToast) {
                        mToastHelper.showToast(R.string.some_delete_fail);
                        mOperationToast = true;
                    }
                    break;
                case OperationEventListener.ERROR_CODE_PASTE_UNSUCCESS:
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
                mFileInfoManager.updateFileInfoList(mCurrentPath, mSortType);
                adapter.refreshAdapter(mFileInfoManager);
                break;
        }
        ProgressDialogFragment heavyDialogFragment = (ProgressDialogFragment) ((Activity)mContext).getFragmentManager()
                .findFragmentByTag(HEAVY_DIALOG_TAG);
        if (heavyDialogFragment != null) {
            heavyDialogFragment.dismissAllowingStateLoss();
        }
        if (mFileInfoManager.getPasteType() == FileInfoManager.PASTE_MODE_CUT) {
            mFileInfoManager.clearPasteList();
            adapter.notifyDataSetChanged();
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
                AlertDialogFragmentBuilder builder = new AlertDialogFragmentBuilder();
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
    /**
    *  Those listeners are to request service to do actually works
    *  @author Xuehao.Jiang
    *  created at 2017/7/8 11:08
    */
    private final class CreateFolderListener implements EditTextDoneListener {
        public void onClick(String text) {
            if (binder != null) {
                String dstPath = mCurrentPath + MountRootManagerRf.SEPARATOR + text;
                binder.createFolder(TAG, dstPath,
                        new LightOperationListener(text));
            }
        }
    }
    protected class RenameDoneListener implements EditTextDoneListener{
        FileInfo mSrcfileInfo;

        public RenameDoneListener(FileInfo srcFile) {
            mSrcfileInfo = srcFile;
        }

        @Override
        public void onClick(String text) {
            String newFilePath = mCurrentPath + MountRootManagerRf.SEPARATOR + text;
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
                        adapter.getCheckedFileInfoItemsList(), new HeavyOperationListener(
                                R.string.deleting));
            }
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }
    }
    private class ClearListener implements DialogInterface.OnClickListener{

        @Override
        public void onClick(DialogInterface dialog, int which) {
            Log.d(TAG, "onClick() method for alertClearDialog, OK button");
            AlertDialogFragment clearFragment = (AlertDialogFragment) ((Activity)mContext).getFragmentManager().
                    findFragmentByTag(CLEAR_DIALOG_TAG);
            if (null != clearFragment) {
                clearFragment.dismissAllowingStateLoss();
            }
            if (binder != null) {
                binder.clearFolder(TAG,
                        adapter.getFirstCheckedFileInfoItem(), new HeavyOperationListener(
                                R.string.clearing));
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



    /**
    *  Following are actionMode menus' operatation
    *  @author Xuehao.Jiang
    *  created at 2017/7/8 11:11
    */
    public void menuSelectOrCancelAll(){
        if(adapter!=null &&mActionMode!=null ){
            adapter.selectOrCancellAll();
            mActionMode.invalidate();
            mActionMode.setTitle(String.valueOf(adapter.getSelectedCount()));
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
        int size = adapter.getCheckedFileInfoItemsList().size();
        if(size>0){
            for(FileInfo fi:adapter.getCheckedFileInfoItemsList()){
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
        int size = adapter.getCheckedFileInfoItemsList().size();
        if(size>0){
            for(FileInfo fi:adapter.getCheckedFileInfoItemsList()){
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
    public void menuClear(){
        showClearDialog();
    }
    public void menuRename(){
        showRenameDialog();
    }
    public void menuDetails(){
        binder.getDetailInfo(TAG,
                adapter.getCheckedFileInfoItemsList().get(0), new DetailInfoListener(
                        adapter.getCheckedFileInfoItemsList().get(0)));
    }
    public void menuShare(){

        List<FileInfo> files = adapter.getCheckedFileInfoItemsList();
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
}
