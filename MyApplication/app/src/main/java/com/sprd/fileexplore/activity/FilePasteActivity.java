package com.sprd.fileexplore.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sprd.fileexplore.R;
import com.sprd.fileexplore.adapter.DetailRecyclerViewAdapter;
import com.sprd.fileexplore.adapter.RecyclerItemClickListener;
import com.sprd.fileexplore.file.FileInfo;
import com.sprd.fileexplore.file.FileInfoComparator;
import com.sprd.fileexplore.file.FileInfoManager;
import com.sprd.fileexplore.service.FileManageService.OperationEventListener;
import com.sprd.fileexplore.load.ThreadPollManager;
import com.sprd.fileexplore.service.FileManageService;
import com.sprd.fileexplore.service.MountRootManagerRf;
import com.sprd.fileexplore.service.MyBinder;
import com.sprd.fileexplore.service.ProgressInfo;
import com.sprd.fileexplore.util.FileUtil;
import com.sprd.fileexplore.util.LogUtil;
import com.sprd.fileexplore.util.ToastHelper;
import com.sprd.fileexplore.view.ProgressDialogFragment;
import com.sprd.fileexplore.view.SlowHorizontalScrollView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Created by Xuehao.Jiang on 2017/7/8.
 */

public class FilePasteActivity extends Activity implements MountRootManagerRf.MountListener, View.OnClickListener {

    private static final String TAG = FilePasteActivity.class.getSimpleName();
    private static final String OPERATE_TAG = "DetailListFragment";

    private RecyclerView recyclerView;
    private TextView emptyView;
    private View navigationView;

    private SlowHorizontalScrollView mNavigationBar = null;
    private TabManager mTabManager = null;
    private MountRootManagerRf mountRootManagerRf ;
    private FileInfoManager mFileInfoManager = null;
    private FileInfoManager operateFileInfoManager =null;
    private List<FileInfo> mFileInfoList = new ArrayList<>();
    private FileInfo mSelectedFileInfo = null;
    private int mTop = -1;
    private String mCurrentPath = null;
    private int mSortType = 0;
    protected ToastHelper mToastHelper = null;

    public MyBinder binder;
    private boolean mServiceBinded = false;
    private SharedPreferences sortSpf ;

    private DetailRecyclerViewAdapter adapter;
    /** maximum tab text length */
    private static final int TAB_TET_MAX_LENGTH = 250;
    public static final int RESULT_PASTE_CODE = 1000;

    private Intent intent ;
    private final String ACTION_COPY_PASTE ="android.intent.action.COPY_PASTE";
    private final String ACTION_CUT_PASTE ="android.intent.action.CUT_PASTE";
    SharedPreferences settings ;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_paste);
        setTitle(R.string.paste_name);

        settings = getDefaultSharedPreferences(this);
        mToastHelper = new ToastHelper(this);
        intent = getIntent();
        String action = intent.getAction();
        Log.d(TAG," action ="+action);

        sortSpf = getSharedPreferences(FileInfoComparator.SORT_KEY, 0);
        mSortType = sortSpf.getInt(FileInfoComparator.SORT_KEY,FileInfoComparator.SORT_BY_NAME);

        recyclerView = (RecyclerView) findViewById(R.id.paste_recyclerview);
        emptyView = (TextView) findViewById(R.id.empty_paste);
        navigationView = findViewById(R.id.bar_background);
        setDirNavigation(navigationView);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
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
        adapter = new DetailRecyclerViewAdapter(this,itemClickListener);
        recyclerView.setAdapter(adapter);

        mountRootManagerRf  = MountRootManagerRf.getInstance();
        mountRootManagerRf.registerMountListener(this);

        bindService(new Intent(getApplicationContext(), FileManageService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG," onResume");
        reloadFileInfos();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(binder!=null  && mServiceBinded){
            Log.d(TAG," onDestroy()  : unbindService success!");
            unbindService(mServiceConnection);
            mServiceBinded = false;
        }else{
            Log.d(TAG," onDestroy()  : unbindService fail !");
        }
        if(mountRootManagerRf!=null){
            mountRootManagerRf.unregisterMountListener(this);
        }

    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        if (binder != null && binder.isBusy(TAG)) {
            Log.i(TAG, "onBackPressed, service is busy. ");
            return  ;
        }
        if (mCurrentPath != null && !mountRootManagerRf.isRootPath(mCurrentPath)) {
            FileInfoManager.NavigationRecord navRecord = mFileInfoManager.getPrevNavigation();
            String prevPath = null;
            if(navRecord!=null){
                prevPath = navRecord.getRecordPath();
                mSelectedFileInfo = navRecord.getSelectedFile();
                mTop = navRecord.getTop();
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
            return ;
        }
        Log.d(TAG, "onBackPressed   end --> mCurrentPath="+mCurrentPath);
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_paste, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem pasteMenu = menu.findItem(R.id.menu_paste);
        MenuItem cancelMenu = menu.findItem(R.id.menu_cancel);
        String action = intent.getAction();

        if(ACTION_COPY_PASTE.equals(action)||ACTION_CUT_PASTE.equals(action)){
            pasteMenu.setVisible(true);
            cancelMenu.setVisible(true);
        }else{
            pasteMenu.setVisible(false);
            cancelMenu.setVisible(false);
        }
        if(MountRootManagerRf.getInstance().isRootPath(mCurrentPath)){
            pasteMenu.setEnabled(false);
        }else{
            pasteMenu.setEnabled(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_paste:
                menuPaste();
                break;
            case R.id.menu_cancel:
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
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
            operateFileInfoManager = binder.initFileInfoManager(OPERATE_TAG);

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

    private RecyclerItemClickListener itemClickListener = new RecyclerItemClickListener(){

        @Override
        public void onItemClick(View v, int position) {
            LogUtil.d(TAG, "onItemClick " + position);
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

            if (selecteItemFileInfo.isFolder()) {
                int top = v.getTop();
                Log.d(TAG, "onItemClickExe, fromTop = " + top);
                showDirectoryFileInfo(selecteItemFileInfo.getPath());
            } else {
                FileUtil.startUpFileByIntent(FilePasteActivity.this,selecteItemFileInfo,binder);
            }
        }

        @Override
        public void onItemLongClick(View v, int position) {}
    };
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
    protected void showDirectoryFileInfo(String path) {
        Log.d(TAG, "showDirectoryFileInfo,path = " + path);
        if ( isFinishing()) {
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
    public void onClick(View v) {
        if (binder.isBusy(this.getClass().getName())) {
            Log.d(TAG, "onClick(), service is busy.");
            return;
        }
        int id = v.getId();
        Log.d(TAG, "onClick() id=" + id);
        mTabManager.updateNavigationBar(id);
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
                setEmptyViewShow(true);
            } else{
                setEmptyViewShow(false);
            }
            adapter.refreshAdapter(mFileInfoManager);
            onPathChanged();
        }

    }
    private void onPathChanged() {
        Log.d(TAG, "onPathChanged");
        if (mTabManager != null) {
            mTabManager.refreshTab(mCurrentPath);
        }
        invalidateOptionsMenu();
    }
    private class TabManager {
        private final List<String> mTabNameList = new ArrayList<String>();
        protected LinearLayout mTabsHolder = null;
        private String mCurFilePath = null;
        private final Button mBlankTab;
        private LinearLayout.LayoutParams mBlanckBtnParam = null;

        private TabManager(View v) {
            mTabsHolder = (LinearLayout) v.findViewById(R.id.tabs_holder);
            mBlankTab = new Button(FilePasteActivity.this);
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
                viewLikeBtn = new ImageButton(FilePasteActivity.this);
                mlp = new LinearLayout.LayoutParams(new ViewGroup.MarginLayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT));
                mlp.setMargins(0, 0, 0, 0);
                viewLikeBtn.setLayoutParams(mlp);
            } else {
                Button button = new Button(FilePasteActivity.this);
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
            viewLikeBtn.setOnClickListener(FilePasteActivity.this);
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
    protected String initCurrentFileInfo() {
        return mountRootManagerRf.getRootPath();
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
    public void setEmptyViewShow(boolean isShow){
        if(isShow){
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }else{
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    private int getViewDirection() {
        return mNavigationBar.getParent().getParent().getLayoutDirection();
    }
    private void addToNavigationList(String path, FileInfo selectedFileInfo, int top) {
        mFileInfoManager.addToNavigationList(new FileInfoManager.NavigationRecord(path, selectedFileInfo, top));
    }


    public void menuPaste(){
        String action = intent.getAction();
        if(ACTION_CUT_PASTE.equals(action)||ACTION_COPY_PASTE.equals(action)){

            MountRootManagerRf mountRootManagerRf = MountRootManagerRf.getInstance();
            List<FileInfo> lists = mountRootManagerRf.getOperateFileInfos();

            int type= intent.getIntExtra("paste_type",FileInfoManager.PASTE_MODE_UNKOWN);
            int size= lists.size();
            Log.d(TAG,"   type="+ type+"  size= "+size);
            if(size>0){
                int i=0;
                for(FileInfo f: lists){
                    Log.d(TAG,"  f["+i+"] ="+ f.getPath());
                    i++;
                }
            }
            mFileInfoManager.savePasteList(type,lists);

            List<FileInfo> tarLists= mFileInfoManager.getPasteList();
            Log.d(TAG,"   tarLists   size= "+tarLists.size() +" mCurrentPath="+mCurrentPath);
            if(binder!=null){
                binder.pasteFiles(TAG,tarLists ,
                        mCurrentPath, mFileInfoManager.getPasteType(), new HeavyOperationListener(
                                R.string.pasting));
            }
            mountRootManagerRf.clearOperateFileInfos();
        }
    }

    protected class HeavyOperationListener implements FileManageService.OperationEventListener,
            View.OnClickListener {
        int mTitle = R.string.deleting;

        private boolean mPermissionToast = false;
        private boolean mOperationToast = false;
        public static final String HEAVY_DIALOG_TAG = "HeavyDialogFragment";

        public HeavyOperationListener(int titleID) {
            mTitle = titleID;
        }

        @Override
        public void onTaskPrepare() {
            if (!FilePasteActivity.this.isFinishing()) {
                ProgressDialogFragment heavyDialogFragment = ProgressDialogFragment.newInstance(
                        ProgressDialog.STYLE_HORIZONTAL, mTitle, R.string.wait, R.string.cancel);
                heavyDialogFragment.setCancelListener(this);
                heavyDialogFragment.setViewDirection(getViewDirection());
                heavyDialogFragment.show(getFragmentManager(), HEAVY_DIALOG_TAG);
                boolean ret = getFragmentManager().executePendingTransactions();
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
                    mFileInfoManager.updateFileInfoList(mCurrentPath, mSortType);
                    adapter.notifyDataSetChanged();
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

            if(errorType == 0){
                Log.d(TAG, "HeavyOperationListener,onTaskResult result = " + errorType);
                setResult(RESULT_PASTE_CODE,new Intent().putExtra("path",mCurrentPath));
                finish();
            }else{
                setResult(-100,new Intent().putExtra("path",""));
                finish();
            }
        }

        @Override
        public void onClick(View v) {
            if (binder != null) {
                Log.i(TAG, "onClick cancel");
                binder.cancel(TAG);
            }
        }
    }
}
