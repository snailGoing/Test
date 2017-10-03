package com.sprd.fileexplore.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.sprd.fileexplore.R;
import com.sprd.fileexplore.adapter.DetailRecyclerViewAdapter;
import com.sprd.fileexplore.adapter.RecyclerItemClickListener;
import com.sprd.fileexplore.file.FileInfo;
import com.sprd.fileexplore.file.FileInfoComparator;
import com.sprd.fileexplore.file.FileInfoManager;
import com.sprd.fileexplore.load.ThreadPollManager;
import com.sprd.fileexplore.service.FileManageService;
import com.sprd.fileexplore.service.MountRootManagerRf;
import com.sprd.fileexplore.service.MyBinder;
import com.sprd.fileexplore.service.ProgressInfo;
import com.sprd.fileexplore.util.FileSearch;
import com.sprd.fileexplore.util.FileUtil;
import com.sprd.fileexplore.util.LogUtil;
import com.sprd.fileexplore.util.PermissionUtils;
import com.sprd.fileexplore.util.ToastHelper;
import com.sprd.fileexplore.view.AlertDialogFragment;
import com.sprd.fileexplore.view.AlertDialogFragment.*;
import com.sprd.fileexplore.view.ProgressDialogFragment;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Created by xuehao.jiang on 2017/7/18.
 */

public class FileSearchResultActivity extends AppCompatActivity {

    private static final String TAG = FileSearchResultActivity.class.getSimpleName();
    private RecyclerView recyclerView;
    private TextView emptyView;
    private ActionMode mActionMode;

    private MountRootManagerRf mountRootManagerRf ;
    private FileInfoManager mFileInfoManager = null;

    private DetailRecyclerViewAdapter adapter;
    public MyBinder binder;
    private boolean mServiceBinded = false;

    private SharedPreferences settings ;
    private int mSortType = 0;
    private SharedPreferences sortSpf ;

    public String mSearchKey;
    public ArrayList<Integer> mSearchType;
    private List<String> mSearchLoaction = new ArrayList<String>();
    private static final String OPERATE_TAG = "DetailListFragment";
    private String mCurrentPath = null;
    private String topPath;
    private boolean inSearchResultFirstUi = true;
    protected ToastHelper mToastHelper = null;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_RENAME = 1;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_DELETE = 2;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_CUT = 3;
    public  static final int PERMISSIONS_REQUEST_TO_READ_EXTERNAL_STORAGE = 4;

    public static final String RENAME_EXTENSION_DIALOG_TAG = "rename_extension_dialog_fragment_tag";
    public static final String RENAME_DIALOG_TAG = "rename_dialog_fragment_tag";
    public static final String DELETE_DIALOG_TAG = "delete_dialog_fragment_tag";
    public static final String CLEAR_DIALOG_TAG = "clear_dialog_fragment_tag";
    public static final String FORBIDDEN_DIALOG_TAG = "forbidden_dialog_fragment_tag";
    private static final String NEW_FILE_PATH_KEY = "new_file_path_key";
    private static final String DETAIL_INFO_KEY = "detail_info_key";
    private static final String DETAIL_INFO_SIZE_KEY = "detail_info_size_key";


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
            // this will obtain pasted target files
            search();
            Log.d(TAG, "onServiceConnected   end ..." );
        }
    };

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        if (binder != null && binder.isBusy(TAG)) {
            Log.i(TAG, "onBackPressed, service is busy. ");
            return  ;
        }

        if (topPath!=null && mCurrentPath != null) {
            if(topPath.equals(mCurrentPath)&& !inSearchResultFirstUi){
                search();
                inSearchResultFirstUi =true;
                return;
            }else if(!inSearchResultFirstUi){
                String parent = new File(mCurrentPath).getParent();
                showDirectoryFileInfo(parent);
                return;
            }
        }
        super.onBackPressed();
    }

    private void search(){
        Log.d(TAG," search ");
        Intent data = getIntent();
        if (null != data) {
            Bundle bun = data.getBundleExtra(FileSearch.SEARCH_ATTACH);
            if (bun != null) {
                mSearchKey = bun.getString(FileSearch.SEARCH_KEY);
                mSearchType = bun.getIntegerArrayList(FileSearch.SEARCH_TYPE);
                String searchLocation = bun.getString(FileSearch.SEARCH_LOCATION);
                getSearchLoaction(searchLocation);
                if (mSearchKey != null && !mSearchKey.isEmpty()
                        && mSearchLoaction.size()>0 ) {
                    binder.search(TAG , mSearchKey, mSearchLoaction, mSearchType,new SearchTaskResultListener());
                }
            }
        } else {
            finish();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_search_result);

        settings = getDefaultSharedPreferences(this);
        sortSpf = getSharedPreferences(FileInfoComparator.SORT_KEY, 0);
        mSortType = sortSpf.getInt(FileInfoComparator.SORT_KEY,FileInfoComparator.SORT_BY_NAME);

        recyclerView = (RecyclerView) findViewById(R.id.search_recyclerview);
        emptyView = (TextView) findViewById(R.id.search_empty);
        adapter = new DetailRecyclerViewAdapter(this,itemClickListener);

        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
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

        mountRootManagerRf = MountRootManagerRf.getInstance();
        mToastHelper = new ToastHelper(this);
        bindService(new Intent(getApplicationContext(), FileManageService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE);
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
            for (pos = startPos; pos <= endPos && pos < len; pos++) {
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
    private void showEmptyView(boolean show){
        if(show){
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }else{
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mFileInfoManager!=null){
            mFileInfoManager.clearSearchList();
        }
        if(binder!=null  && mServiceBinded){
            Log.d(TAG," onDestroy()  : unbindService success!");
            unbindService(mServiceConnection);
            mServiceBinded = false;
        }else{
            Log.d(TAG," onDestroy()  : unbindService fail !");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


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
    public void onItemClickExe(View v, int position){
        Log.d(TAG, "onItemClickExe, position = " + position);
        if (binder != null && binder.isBusy(TAG)) {
            Log.d(TAG, "onItemClick, service is busy,return. ");
            return;
        }
        Log.d(TAG, "onItemClick,Selected position: " + position);

        if (position >= adapter.getItemCount() || position < 0) {
            Log.e(TAG, "onItemClick,events error,mFileInfoList.size(): "
                    + adapter.getItemCount());
            return;
        }
        FileInfo selecteItemFileInfo = (FileInfo) adapter.getCurrentPosFileInfo(position);
        if(selecteItemFileInfo.isFolder()){
            showDirectoryFileInfo(selecteItemFileInfo.getPath());
        }else
            FileUtil.startUpFileByIntent(FileSearchResultActivity.this,selecteItemFileInfo,binder);
    }
    protected void showDirectoryFileInfo(String path) {
        Log.d(TAG, "showDirectoryFileInfo,path = " + path);
        if ( isFinishing()) {
            Log.i(TAG, "showDirectoryFileInfo,isFinishing: true, do not loading again");
            return;
        }
        if(inSearchResultFirstUi){
            inSearchResultFirstUi = false;
            topPath = path;
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
            Log.d(TAG, " RecyclerViewListener,   TaskResult result = " + result +"   file sum size="+size);

            if(size == 0){
                showEmptyView(true);
            } else{
                showEmptyView(false);
            }
            adapter.refreshAdapter(mFileInfoManager);

        }

    }
    public void getSearchLoaction(String  path) {
        if (mSearchLoaction == null)
            return;
        mSearchLoaction.clear();
        if("key_no_path".equals(path)){

        }else if("key_all_path".equals(path)){
            List<FileInfo> lists = mountRootManagerRf.getMountPointFileInfo();
            int size = lists.size();
            if(size>0){
                for(FileInfo fi:lists){
                    mSearchLoaction.add(fi.getPath());
                }
            }
        }else{
            mSearchLoaction.add(path);
        }
    }

    private class SearchTaskResultListener implements FileManageService.OperationEventListener {

        @Override
        public void onTaskPrepare() {

        }

        @Override
        public void onTaskProgress(ProgressInfo progressInfo) {

        }

        @Override
        public void onTaskResult(int result) {

            mFileInfoManager.obtainSearchList();
            int size = mFileInfoManager.getShowFileList().size();
            if(sortSpf!=null){
                mSortType = sortSpf.getInt(FileInfoComparator.SORT_KEY,FileInfoComparator.SORT_BY_NAME);
            }
            mFileInfoManager.sort(mSortType);
            Log.d(TAG," SearchTaskResultListener:   onTaskResult --> result="+result +" size="+size);
            if(size>0){
                showEmptyView(false);
            } else{
                showEmptyView(true);
            }
            adapter.refreshAdapter(mFileInfoManager);

        }
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
            mActionMode =   startSupportActionMode(mActionModeCallback);

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

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback(){
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
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
            menuShare.setEnabled(true);
            if( selectCount > 1){
                menuClear.setEnabled(false);
                menuRename.setEnabled(false);
                menuDetail.setEnabled(false);
            }else{
                menuClear.setEnabled(true);
                menuRename.setEnabled(true);
                menuDetail.setEnabled(true);
            }
        }
        if(sum == selectCount){
            menuSelet.setTitle(getString(R.string.menu_cancel_all));
        }else{
            menuSelet.setTitle(getString(R.string.menu_select_all));
        }
    }
    //Set action mode null after use
    public void setNullToActionMode() {
        if (mActionMode != null)
            mActionMode = null;
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
                    .append(fileInfo.isFolder()? ( getString(R.string.folder)): getString(R.string.file))
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
            if (!isFinishing()) {
                AlertDialogFragment.AlertDialogFragmentBuilder builder = new AlertDialogFragment.AlertDialogFragmentBuilder();
                AlertDialogFragment detailFragment = builder.setCancelTitle(R.string.ok).setLayout(
                        R.layout.dialog_details).setTitle(R.string.details).create();

                detailFragment.setDismissListener(this);
                detailFragment.show( getFragmentManager(), DETAIL_DIALOG_TAG);
                boolean ret =  getFragmentManager().executePendingTransactions();
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
            AlertDialogFragment detailFragment = (AlertDialogFragment) getFragmentManager()
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
    protected class RenameDoneListener implements EditTextDialogFragment.EditTextDoneListener {
        FileInfo mSrcfileInfo;

        public RenameDoneListener(FileInfo srcFile) {
            mSrcfileInfo = srcFile;
        }

        @Override
        public void onClick(String text) {
            String newFilePath = mCurrentPath + MountRootManagerRf.SEPARATOR + text;
            if(inSearchResultFirstUi){
                newFilePath = mSrcfileInfo.getFile().getParent()+ MountRootManagerRf.SEPARATOR + text;
            }

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
                    Log.d(TAG,"  rename  :  newFilePath="+newFilePath+"  ");
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
            AlertDialogFragment deleteFragment = (AlertDialogFragment) getFragmentManager().
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
            AlertDialogFragment clearFragment = (AlertDialogFragment)  getFragmentManager().
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
                    if(inSearchResultFirstUi){
                        mFileInfoManager.updateSearchList(mSortType);
                        adapter.refreshAdapter(mFileInfoManager);

                    }else{
                        FileInfo fileInfo = mFileInfoManager.updateOneFileInfoList(mCurrentPath,mSortType);
                        adapter.refreshAdapter(mFileInfoManager);
                        if (fileInfo != null) {
                            int postion = adapter.getPosition(fileInfo);
                            Log.d(TAG, "LightOperation postion = " + postion);
                            recyclerView.getLayoutManager().scrollToPosition(postion);
                        }else{
                            Log.d(TAG, "LightOperation fileInfo = =null" );
                        }
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
            if (!isFinishing()) {
                ProgressDialogFragment heavyDialogFragment = ProgressDialogFragment.newInstance(
                        ProgressDialog.STYLE_HORIZONTAL, mTitle, R.string.wait, R.string.cancel);
                heavyDialogFragment.setCancelListener(this);
                heavyDialogFragment.setViewDirection(getWindow().getDecorView().getLayoutDirection());
                heavyDialogFragment.show( getFragmentManager(), HEAVY_DIALOG_TAG);
                boolean ret =  getFragmentManager().executePendingTransactions();
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
                ProgressDialogFragment heavyDialogFragment = (ProgressDialogFragment) getFragmentManager()
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
                    Log.d(TAG,"  HeavyOperationListener,onTaskResult  inSearchResultFirstUi="+inSearchResultFirstUi);
                    if(!inSearchResultFirstUi){
                        mFileInfoManager.updateFileInfoList(mCurrentPath, mSortType);
                        adapter.refreshAdapter(mFileInfoManager);
                    }else{
                        mFileInfoManager.updateSearchList(mSortType);
                        adapter.refreshAdapter(mFileInfoManager);
                    }
                    break;
            }
            ProgressDialogFragment heavyDialogFragment = (ProgressDialogFragment) getFragmentManager()
                    .findFragmentByTag(HEAVY_DIALOG_TAG);
            if (heavyDialogFragment != null) {
                heavyDialogFragment.dismissAllowingStateLoss();
            }
            if (mFileInfoManager.getPasteType() == FileInfoManager.PASTE_MODE_CUT) {
                mFileInfoManager.clearPasteList();
                adapter.notifyDataSetChanged();
            }
            invalidateOptionsMenu();
        }

        @Override
        public void onClick(View v) {
            if (binder != null) {
                Log.i(this.getClass().getName(), "onClick cancel");
                binder.cancel(TAG);
            }
        }
    }
    private OnDialogDismissListener  onDialogDismissListener=  new OnDialogDismissListener(){

        @Override
        public void onDialogDismiss() {
            Log.d(TAG, "dialog dismissed...");
            mIsAlertDialogShowing = false;
        }
    };
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

        if (!isFinishing()) {
            mIsAlertDialogShowing = true;
            AlertDialogFragmentBuilder builder = new AlertDialogFragmentBuilder();
            AlertDialogFragment deleteDialogFragment = builder.setMessage(alertMsgId).setDoneTitle(
                    R.string.ok).setCancelTitle(R.string.cancel).setIcon(
                    R.mipmap.ic_dialog_alert_holo_light).setTitle(R.string.delete).create();
            deleteDialogFragment.setOnDoneListener(new DeleteListener());
            deleteDialogFragment.setOnDialogDismissListener(onDialogDismissListener);
            deleteDialogFragment.show( getFragmentManager(), DELETE_DIALOG_TAG);
            boolean ret =  getFragmentManager().executePendingTransactions();
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
            if (!isFinishing()) {
                mIsAlertDialogShowing = true;
                EditDialogFragmentBuilder builder = new EditDialogFragmentBuilder();
                builder.setDefault(name, selection).setDoneTitle(R.string.done).setCancelTitle(
                        R.string.cancel).setTitle(R.string.rename);
                EditTextDialogFragment renameDialogFragment = builder.create();
                renameDialogFragment.setOnEditTextDoneListener(new RenameDoneListener(fileInfo));
                renameDialogFragment.setOnDialogDismissListener(onDialogDismissListener);
                renameDialogFragment.show( getFragmentManager(), RENAME_DIALOG_TAG);
                boolean ret =  getFragmentManager().executePendingTransactions();
                Log.d(TAG, "executing pending transactions result: " + ret);
            }
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
        renameExtensionDialogFragment.show( getFragmentManager(), RENAME_EXTENSION_DIALOG_TAG);
        boolean ret =  getFragmentManager().executePendingTransactions();
        Log.d(TAG, "executing pending transactions result: " + ret);
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
            if (!isFinishing()) {
                mIsAlertDialogShowing = true;
                AlertDialogFragmentBuilder builder = new AlertDialogFragmentBuilder();
                AlertDialogFragment deleteDialogFragment = builder.setMessage(alertMsgId).setDoneTitle(
                        R.string.ok).setCancelTitle(R.string.cancel).setIcon(
                        R.mipmap.ic_dialog_alert_holo_light).setTitle(R.string.clear).create();
                deleteDialogFragment.setOnDoneListener(new ClearListener());
                deleteDialogFragment.setOnDialogDismissListener(onDialogDismissListener);
                deleteDialogFragment.show( getFragmentManager(), CLEAR_DIALOG_TAG);
                boolean ret =  getFragmentManager().executePendingTransactions();
                Log.d(TAG, "executing pending transactions result: " + ret);
            }
        }else{
            Log.d(TAG, "showClearDialog can clear file  ,only for folder, return!~~");
            return;
        }
    }
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
        if(!PermissionUtils.hasStorageWritePermission(this)){
            PermissionUtils.requestPermission((Activity) this,
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
        FileUtil.startUpMultiShare(this,datas,binder);
        mActionMode.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if( resultCode == FilePasteActivity.RESULT_PASTE_CODE){
            search();
        }
    }
}
