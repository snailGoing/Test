package com.sprd.generalsecurity.storage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.EnvironmentEx;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.sprd.generalsecurity.R;
import com.sprd.generalsecurity.utils.ApplicationsState;
import com.sprd.generalsecurity.utils.ApplicationsState.AppEntry;
import android.text.format.Formatter;
import com.sprd.generalsecurity.utils.MemoryUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class StorageManagement extends Activity implements OnTabChangeListener,
        OnClickListener, OnItemClickListener, ApplicationsState.Callbacks {

    private static final String TAG = "StorageManagement";
    private static final String INTERNAL_STORAGE = "internal_storage";
    private static final String EXTERNAL_STORAGE = "external_storage";
    private static final int INTERNAL_STORAGE_ID = 0;
    private static final int EXTERNAL_STORAGE_ID = 1;

    private static final int SET_BUTTON_CLEAN = 1;
    private static final int SET_BUTTON_CLEAN_END = 2;
    private static final int UPDATE_CACHE_SIZE = 3;
    private static final int UPDATE_RUBBISH_SIZE = 4;
    private static final int UPDATE_TMP_SIZE = 5;
    private static final int UPDATE_APK_SIZE = 6;
    private static final int UPDATE_LARGE_FILE_SIZE = 7;
    private static final int UPDATE_FILES_PATH = 8;
    private static final int UPDATE_SIZE = 0;
    private static final int UPDATE_PATH_UI = 9;
    public static final int CACHE_ITEM = 0;
    public static final int RUBBISH_ITEM = 1;
    public static final int APK_ITEM = 2;
    public static final int TMP_ITEM = 3;
    public static final int LARGE_FILE_ITEM = 4;

    private static final int REQUEST_SIZE = 1;
    private boolean mHasSDcard;
    private boolean mIsScanEnd;

    public static final String BUNDLE_VALUE_NAME = "paths";
    public static final String BUNDLE_NAME = "detail_list";
    public static final String BUNDLE_VALUE_SIZE = "size";
    public static final String BUNDLE_KEY = "list_type";
    public static final String IS_EXTERNAL = "isExternal";

    public static final String RUBBISH_FILE1_EXT = ".log";
    public static final String RUBBISH_FILE2_EXT = ".bak";
    public static final String TMP_FILE_PREFIX = "~";
    public static final String TMP_FILE_EXT = ".tmp";
    public static final String APK_FILE_EXT = ".apk";

    public static final String LARGE_AUDIO1_FILE_EXT = ".mp3";
    public static final String LARGE_AUDIO2_FILE_EXT = ".WMA";

    public static final String FILE_TYPE_VIDEO_AVI = ".avi";
    public static final String FILE_TYPE_VIDEO_MOV = ".mov";
    public static final String FILE_TYPE_VIDEO_MP4 = ".mp4";
    public static final String FILE_TYPE_VIDEO_MKV = ".mkv";
    public static final String FILE_TYPE_VIDEO_RMVB = ".rmvb";

    private StorageManager mStorageManager;
    private Context mContext;
    private TabHost mTabHost;
    private Animation mAnim;
    private TextView mBackground;
    private Button mBt;
    private TextView mText;
    private View mInternal;
    private View mExternal;
    private PackageManager mPm;
    private FrameLayout mLayout;
    private LayoutInflater mInflater;
    private FileAdapter mInAdapter;
    private FileAdapter mExAdapter;

    private Button mInButton;
    private Button mExButton;
    private DataGroup mData = DataGroup.getInstance();

    private static final long LAEGE_FILE_FILTER_SIZE = 1024 * 1024 * 100; //100M

    private static final String SDCARD_PREFIX = "sdcard";

    private static final int CACHE_ITEM_INDEX = 0;
    private static final int RUBBISH_ITEM_INDEX = 1;
    private static final int APK_ITEM_INDEX = 2;
    private static final int TMP_ITEM_INDEX = 3;
    private static final int LARGE_ITEM_INDEX = 4;

    private static final int EXTERNAL_ACTIVITY_REQUEST_CODE_START = 10;

    private boolean isStopScan = false;
    private boolean isPressBack = false;

    private enum Tab {
        INTERNAL(INTERNAL_STORAGE, R.string.internal_storage, R.id.internal,
                R.id.internal_tv, R.id.score_label, R.id.internal_background, R.id.internal_list,
                R.id.internal_bt, true), EXTERNAL(EXTERNAL_STORAGE,
                R.string.external_storage, R.id.external, R.id.external_tv,R.id.score_label,
                R.id.external_background, R.id.external_list, R.id.external_bt,
                false);

        private final String mTag;
        private final int mLabel;
        private final int mScoreLabel;
        private final int mView;
        private final int mText;
        private final int mBackground;
        private final int mList;
        private final int mButton;
        private final boolean mWithSwitch;


        private Tab(String tag, int label, int view, int text,int score_label, int background,
                int list, int button, boolean withSwitch) {
            mTag = tag;
            mLabel = label;
            mScoreLabel = score_label;
            mView = view;
            mText = text;
            mBackground = background;
            mList = list;
            mButton = button;
            mWithSwitch = withSwitch;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate");
        isStopScan = false;
        mContext = this;
        mPm = mContext.getPackageManager();
        mStorageManager = this.getSystemService(StorageManager.class);
        mStorageManager.registerListener(mStorageListener);

        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLayout = new FrameLayout(mContext);

        setContentView(mLayout);
        refreshUi();
        mAnim = AnimationUtils.loadAnimation(mContext, R.anim.tip);

        LinearInterpolator lin = new LinearInterpolator();
        mAnim.setInterpolator(lin);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }

    private boolean hasSDcard() {
        final List<VolumeInfo> volumes = mStorageManager.getVolumes();
        for (VolumeInfo vol : volumes) {
            if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                if (vol.linkName != null && vol.linkName.startsWith(SDCARD_PREFIX) &&
                    vol.state == VolumeInfo.STATE_MOUNTED) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean getExStorageState() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    private void addTab(Tab tab) {
        TabHost.TabSpec systemSpec = mTabHost.newTabSpec(tab.mTag)
                .setIndicator(mContext.getString(tab.mLabel))
                .setContent(tab.mView);
        mTabHost.addTab(systemSpec);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
    }

    private TabSpec buildTabSpec(String tag, int resId) {
        return mTabHost.newTabSpec(tag).setIndicator(mContext.getText(resId))
                .setContent(mEmptyTabContent);
    }

    private TabContentFactory mEmptyTabContent = new TabContentFactory() {
        @Override
        public View createTabContent(String tag) {
            return new View(mTabHost.getContext());
        }
    };

    @Override
    public void onTabChanged(String tag) {
        Log.i(TAG, "tag:" + tag);
        if (tag.equals(INTERNAL_STORAGE)) {
            mExternal.setVisibility(View.GONE);
            mInternal.setVisibility(View.VISIBLE);
        } else if (tag.equals(EXTERNAL_STORAGE)) {
            mInternal.setVisibility(View.GONE);
            mExternal.setVisibility(View.VISIBLE);
        }
    }

    int datafileSize;

    @Override
    public void onClick(View view) {
        Log.i(TAG, "View" + view + " id:" + view.getId());
        final String text = ((Button) view).getText().toString().trim();
        final ListView inlist = (ListView) mInternal
                .findViewById(Tab.INTERNAL.mList);
        ListView exlist = null;
        if (mExternal != null) {
            exlist = (ListView) mExternal.findViewById(Tab.EXTERNAL.mList);
        }
        Log.i(TAG, "text" + text);
        switch (view.getId()) {
        case R.id.internal_bt:
            Log.i(TAG, "internal button");
            final TextView internal_background = (TextView) mInternal
                    .findViewById(Tab.INTERNAL.mBackground);
            if (text.equals(mContext.getText(R.string.start_scan_button))) {// Scan
                Log.i(TAG, "-----------------");
                resetData();
                ((Button) view).setText(mContext.getResources().getString(
                        R.string.stop_scan_button));
                for (int i = 0; i < inlist.getChildCount(); i++) {
                    View item = inlist.getChildAt(i);
                    Log.i(TAG,
                            "item:" + item + "  first:"
                                    + inlist.getFirstVisiblePosition()
                                    + "  last:"
                                    + inlist.getLastVisiblePosition());
                    final ProgressBar mProgressBar = (ProgressBar) item
                            .findViewById(R.id.progress);
//                    size.setText(Formatter.formatFileSize(mContext, 0, false));
                    mProgressBar.setVisibility(View.VISIBLE);
                    Drawable d = mContext.getResources().getDrawable(R.drawable.progress_small);
                    mProgressBar.setIndeterminateDrawable(d);
                }
                //Start
                scanInCached();// Cache
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            File file_inter = EnvironmentEx.getInternalStoragePath();
                            Log.i(TAG, "Start--------------------------------");
                            Log.i(TAG, "file:" + file_inter.getName() + " ;path:"
                                    + file_inter.getAbsolutePath());
                            scanFiles(file_inter.listFiles(), mInhandler);
                            if(hasSDcard()){
                                File file_ext = EnvironmentEx.getExternalStoragePath();
                                scanFiles(file_ext.listFiles(), mInhandler);
                            }
                            Log.i(TAG, "End-------------------------------");
                        } catch (Exception e) {
                            // e.printStackTrace();
                            Log.i(TAG, "InterruptedException 1");
                        } finally {
                            while (true) {
                                if (mIsScanEnd ) {
                                    if(!isStopScan &&!isPressBack){
                                        mInhandler.sendEmptyMessage(SET_BUTTON_CLEAN);
                                        mIsScanEnd = false;
                                        break;
                                    }
                                }
                                try{
                                   Thread.sleep(100);
                                } catch (InterruptedException e) {
                                }
                                /* @} */
                            }
                        }
                    }

                }.start();
            } else if (text.equals(mContext
                    .getText(R.string.clean_memory_button))) {
                Log.i(TAG, "-----------------");
                Log.i(TAG, "click clean button");
                ((Button) view).setText(mContext.getResources().getString(
                        R.string.doing_clean_memory_button));

                for (int i = 0; i < inlist.getChildCount(); i++) {
                    View item = inlist.getChildAt(i);
                    Log.i(TAG,
                            "item:" + item + "  first:"
                                    + inlist.getFirstVisiblePosition()
                                    + "  last:"
                                    + inlist.getLastVisiblePosition());
                }
                Log.i(TAG, "clean Button>>");

                // delete !cache file
                new Thread() {
                    @Override
                    public void run() {
                        deleteLargeFiles(mInhandler);
                        deleteFiles(mData.mInRubbishMap, RUBBISH_ITEM,
                                UPDATE_RUBBISH_SIZE, mInhandler);
                        deleteFiles(mData.mInTmpMap, TMP_ITEM, UPDATE_TMP_SIZE,
                                mInhandler);
                        deleteFiles(mData.mInApkMap, APK_ITEM, UPDATE_APK_SIZE,
                                mInhandler);

                        notifyMediaScanDir(mContext, EnvironmentEx.getInternalStoragePath());

                        if (mData.mInCacheMap == null || mData.mInCacheMap.size() <= 0) {
                            mInhandler.sendEmptyMessage(SET_BUTTON_CLEAN_END);
                        }
                    }
                }.start();

                // delete cache
                if (mClearCacheObserver == null) {
                    mClearCacheObserver = new ClearCacheObserver();
                }
                for (int i = 0; i < mData.mInCacheMap.size(); i++) {
                    Log.i(TAG, " i:" + i);
                    File file = new File(mData.mInCacheMap.keyAt(i));
                    String packageName = file.getName();
                    Log.i(TAG, " i:" + i);
                    Log.i(TAG, i + ": " + packageName);
                    mPm.deleteApplicationCacheFiles(packageName,
                            mClearCacheObserver);
                }
            } else if (text.equals(mContext.getText(R.string.clear_memory))){
                this.finish();
            } else if(text.equals(mContext.getText(R.string.stop_scan_button))){
                isStopScan = true;
                mInhandler.sendEmptyMessage(SET_BUTTON_CLEAN);
            }
            break;
        }
    }

    private void deleteLargeFiles(Handler handler) {
        ArrayMap<String, Long> map = handler == mExhandler ? mData.mExLargeMap : mData.mLargeMap;
        if (mInhandler == handler) {
            mUniqueLargeFileSize = 0;
            mData.mUniqueLargeFileSize = 0;
        } else {
            mExUniqueLargeFileSize = 0;
            mData.mExUniqueLargeFileSize = 0;
        }
        for (int i = 0; i < map.size(); i++) {
            File file = new File(map.keyAt(i));
            long size = map.valueAt(i);
            file.delete();
            if (handler == mExhandler) {
                mData.mExLargeFileCategorySize -= size;
                mExLargeFileSize = mData.mExLargeFileCategorySize;
            } else {
                mData.mLargeFileCategorySize -= size;
                mLargeFileSize = mData.mLargeFileCategorySize;
            }
            handler.sendEmptyMessage(UPDATE_LARGE_FILE_SIZE);
        }
        map.clear();
    }

    private void deleteFiles(ArrayMap<String,Long> map, int type, int what,
            Handler handler) {
        if (handler == mExhandler) {
            for (int i = 0; i < map.size(); i++) {
                File file = new File(map.keyAt(i));
                long size = map.valueAt(i);
                if (type != CACHE_ITEM) {
                    file.delete();
                    switch (type) {
                        case RUBBISH_ITEM:
                            mExRubbishSize -= size;
                            Log.i(TAG, "mExRubbishSize:" + mExRubbishSize
                                    + "   fileSize:" + size);
                            break;
                        case APK_ITEM:
                            mExApkSize -= size;
                            mData.mAPKCategorySize = mExApkSize;
                            Log.i(TAG, "mExApkSize:" + mExApkSize
                                    + "   fileSize:" + size);
                            break;
                        case TMP_ITEM:
                            mExTmpSize -= size;
                            Log.i(TAG, "mExTmpSize:" + mExTmpSize
                                    + "   fileSize:" + size);
                            break;
                        default:
                            break;
                    }

                    mData.mExLargeFileCategorySize = getLargeFileSize(false);
                    mExLargeFileSize = mData.mExLargeFileCategorySize;
                    Log.i(TAG, "what:" + what);
                    handler.sendEmptyMessage(what);
                    handler.sendEmptyMessage(UPDATE_LARGE_FILE_SIZE);
                } else {
                    if (dirDelete(new File(map.keyAt(i)))) {
                        mExCacheSize -= size;
                    }
                    Log.i(TAG, "mExCacheSize:" + mExCacheSize + "   fileSize:"
                            + size);
                    map.removeAt(i);
                    handler.sendEmptyMessage(what);
                }
            }

            map.clear();
        } else {
            for (int i = 0; i < map.size(); i++) {
                File file = new File(map.keyAt(i));
                long size = map.valueAt(i);
                file.delete();
                switch (type) {
                    case RUBBISH_ITEM:
                        mInRubbishSize -= size;
                        Log.i(TAG, "mInRubbishSize:" + mInRubbishSize
                                + "   fileSize:" + size);
                        break;
                    case APK_ITEM:
                        mInApkSize -= size;
                        Log.i(TAG, "mInApkSize:" + mInApkSize + "   fileSize:"
                                + size + ":" + file.getName());
                        mData.mAPKCategorySize = mInApkSize;
                        break;
                    case TMP_ITEM:
                        mInTmpSize -= size;
                        Log.i(TAG, "mInTmpSize:" + mInTmpSize + "   fileSize:"
                                + size);
                        break;
                    default:
                        break;
                }
                mLargeFileSize = getLargeFileSize(true);
                handler.sendEmptyMessage(what);
                handler.sendEmptyMessage(UPDATE_LARGE_FILE_SIZE);
            }

            map.clear();
        }
    }

    private boolean dirDelete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!dirDelete(f)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }


    public static void notifyMediaScanDir(Context context, File dir) {
        Log.i(TAG,"send broadcast to scan dir = " + dir);
        String path = dir.getPath();
        Intent intent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_DIR");
        Bundle bundle = new Bundle();
        bundle.putString("scan_dir_path", path);
        intent.putExtras(bundle);
        context.sendBroadcast(intent);
    }

    long mInCacheSize;
    long mInRubbishSize;
    long mInApkSize;
    long mInTmpSize;
    long mExCacheSize;
    long mExRubbishSize;
    long mExApkSize;
    long mExTmpSize;
    long mLargeFileSize;
    long mUniqueLargeFileSize;
    long mExLargeFileSize;
    long mExUniqueLargeFileSize; //large file size that not contained in APK, tmp category.

    ApplicationsState mState;
    AppEntry mAppEntry;
    protected ApplicationsState.Session mSession;

    private void scanInCached() {
        ApplicationsState.sInstance = null;
        mState = ApplicationsState.getInstance(getApplication());
        mSession = mState.newSession(this);
        mSession.resume();
    }

    public void onRunningStateChanged(boolean running) {
        Log.e(TAG, "onRunningStateChanged");
    }

    public void onPackageListChanged() {
        Log.e(TAG, "onPackageListChanged");
    }

    public void onRebuildComplete(ArrayList<AppEntry> apps) {
        Log.e(TAG, "onRebuildComplete");
    }

    public void onPackageIconChanged() {
        Log.e(TAG, "onPackageIconChanged");
    }

    public void onPackageSizeChanged(String packageName) {

        if (callBack) {
            Log.i(TAG,
                    "..........onPackageSizeChanged.................................");
            return;
        }

        if (mState == null) {
            mState = ApplicationsState.getInstance(getApplication());
            mSession = mState.newSession(this);
            mSession.resume();
        }

        mAppEntry = mState.getEntry(packageName, UserHandle.myUserId());

        Log.e(TAG, "----------entry:" + packageName +"\t "+(mAppEntry.cacheSize+mAppEntry.externalCacheSize)+"  " + mAppEntry + ":" + isChange);
        if (isChange) { // clear
            ArrayList<String> list = new ArrayList<String> ();
            for (int i = 0; i < mData.mInCacheMap.size(); i++) {
                String name = new File(mData.mInCacheMap.keyAt(i)).getName();
                AppEntry appEntry = mState.getEntry(name, UserHandle.myUserId());
                Log.i(TAG, " i:" + i+"\tappEntry:"+name+"\tsize:"+(mAppEntry.cacheSize + mAppEntry.externalCacheSize)+" \t"+mData.mInCacheMap.valueAt(i));
                if (mAppEntry.cacheSize + mAppEntry.externalCacheSize <= 0
                        || (mAppEntry.cacheSize == 4096 * 3 && mAppEntry.externalCacheSize == 0)){
                mInCacheSize -= mData.mInCacheMap.valueAt(i);
                list.add(mData.mInCacheMap.keyAt(i));
                mInhandler.sendEmptyMessage(UPDATE_CACHE_SIZE);
                }
            }
            for (int i=0; i < list.size();i++) {
                Log.i(TAG, "remove "+i+":\t"+list.get(i));
                mData.mInCacheMap.remove(list.get(i));
            }
            mData.mInCacheMap.clear();
            mInhandler.sendEmptyMessage(SET_BUTTON_CLEAN_END);
            isChange = false;
            return;
        }
        // Scan
        if (mAppEntry != null) {
            boolean flag = false;
            if ((mAppEntry.info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                flag = true;
            } else if ((mAppEntry.info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                flag = true;
            } else if (mAppEntry.hasLauncherEntry) {
                flag = true;
            }

            if (flag) {
                if (mAppEntry.cacheSize + mAppEntry.externalCacheSize > 0
                        && !(mAppEntry.cacheSize == 4096 * 3 && mAppEntry.externalCacheSize == 0)) {
                    Log.e(TAG, "\t\t----------add:" + packageName
                            + mAppEntry.cacheSize + " ex:"
                            + mAppEntry.externalCacheSize);
                    /* SPRD: Bug 674774 keep mc10086 from killing and cleaning @{ */
                    if (MemoryUtils.isPersistentServiceProcess(packageName)) {
                        return;
                    }
                    /* @} */
                    mData.mRubbish_cach1_ext.add(new FileDetailModel((Environment.getDataDirectory()
                            .getAbsoluteFile()
                            + File.separator
                            + "data"
                            + File.separator + packageName), (mAppEntry.cacheSize
                            + mAppEntry.externalCacheSize)));
                    mInCacheSize += mAppEntry.cacheSize
                            + mAppEntry.externalCacheSize;
                    Log.i(TAG, "mInRubbishMap \t\tfile:" + packageName+":\t"+mInCacheSize);
                    mInhandler.sendEmptyMessage(UPDATE_CACHE_SIZE);

                }
            }
        }

        Log.e("TAG", "onPack:" + packageName);
    }

    public void onAllSizesComputed() {
        Log.e(TAG, "onAllSizesComputed--------------------:");
        if (callBack) {
            Log.i(TAG,"..........onAllSizesComputed.................................");
            return;
        }
        // Clear
        if (isChange) {
            Log.i(TAG,"..........onAllSizeComputed......................");
            mInhandler.sendEmptyMessage(SET_BUTTON_CLEAN_END);
            isChange = false;
            return;
        }
        mIsScanEnd = true;
    }

    public void onLauncherInfoChanged() {

    }

    public void onLoadEntriesCompleted() {

    }

    private void scanFiles(File[] files, Handler handler) {
        if (files != null) {
            for (File file : files) {
                if (isStopScan) {
                    break;
                }
                Log.i(TAG, file.getName() + ":\t" + file.getAbsolutePath());
                Message UpdatePathUi = mInhandler.obtainMessage(UPDATE_PATH_UI, file);
                mInhandler.sendMessage(UpdatePathUi);
                if (file.isDirectory()) {
                    if (file.getName().equals("cache")
                            | file.getName().equals("code_cache")) {
                        Log.i(TAG, "file.path:" + file.getAbsolutePath());
                        long size = scanDirSize(file);
                        Log.i(TAG, "\t\tsize:" + size);
                        if (size > 0) {
                            mData.mInCacheMap.put((file.getAbsolutePath()), size);
                            mInCacheSize += size;
                            mData.mRubbish_cach2_ext.add(new FileDetailModel(file.getAbsolutePath(), size));
                            mInhandler.sendEmptyMessage(UPDATE_CACHE_SIZE);
                        }
                        continue;
                    }
                    scanFiles(file.listFiles(), handler);
                } else {
                    String name = file.getName();
                    final String size = Formatter.formatFileSize(mContext, file.length());
                    if (name.endsWith(RUBBISH_FILE1_EXT)) {
                        mInRubbishSize += file.length();
                        mData.mRubbishCategorySize = mInRubbishSize;
                        mData.mInRubbishMap.put(file.getAbsolutePath(), file.length());
                        mData.mRubbish_log_ext.add(new FileDetailModel(file.getAbsolutePath(), file.length()));
                        mInhandler.sendEmptyMessage(UPDATE_RUBBISH_SIZE);
                    } else if (name.endsWith(RUBBISH_FILE2_EXT)) {
                        mInRubbishSize += file.length();
                        mData.mRubbishCategorySize = mInRubbishSize;
                        mData.mInRubbishMap.put(file.getAbsolutePath(), file.length());
                        mData.mRubbish_bak_ext.add(new FileDetailModel(file.getAbsolutePath(), file.length()));
                        mInhandler.sendEmptyMessage(UPDATE_RUBBISH_SIZE);
                    } else if (name.endsWith(APK_FILE_EXT)) {
                        Log.i(TAG, "\t\tfile:" + file.getAbsolutePath()
                                .substring(0, 5) + "\t\t" + Environment.getDataDirectory()
                                .getAbsolutePath());
                        if (file.getAbsolutePath()
                                .substring(0, 5)
                                .equals(Environment.getDataDirectory()
                                        .getAbsolutePath())) {
                            continue;
                        }
                        mInApkSize += file.length();
                        mData.mAPKCategorySize = mInApkSize;
                        mData.mInApkMap.put(file.getAbsolutePath(), file.length());
                        mData.mRubbish_apk_ext.add(new FileDetailModel(file.getAbsolutePath(), file.length()));
                        mInhandler.sendEmptyMessage(UPDATE_APK_SIZE);
                    } else if (name.endsWith(TMP_FILE_EXT) || name.startsWith(TMP_FILE_PREFIX)) {
                        mInTmpSize += file.length();
                        mData.mTempCategorySize = mInTmpSize;
                        mData.mInTmpMap.put(file.getAbsolutePath(), file.length());
                        mData.mRubbish_tmp_ext.add(new FileDetailModel(file.getAbsolutePath(), file.length()));
                        mInhandler.sendEmptyMessage(UPDATE_TMP_SIZE);
                    }
                    largeFileCheck(file, true);
                }
            }
            Log.i(TAG, "getFinal:" + mInApkSize + ":" + mInTmpSize + ":" + ":" + mLargeFileSize);
        }
    }

    public static boolean isLargeFileUnique(File f) {
        String fileName = f.getName();
        if (fileName.endsWith(StorageManagement.RUBBISH_FILE1_EXT) ||
            fileName.endsWith(StorageManagement.RUBBISH_FILE2_EXT) ||
            fileName.startsWith(TMP_FILE_PREFIX) ||
            fileName.endsWith(StorageManagement.TMP_FILE_EXT) ||
            fileName.endsWith(StorageManagement.APK_FILE_EXT) ) {
            Log.d(TAG, "large file not unique:" + f.getAbsolutePath());
            return false;
        }
        Log.d(TAG, " unique large file:" + f.getAbsolutePath() + ":" + f.length());
        return true;
    }

    private void largeFileCheck(File f, boolean isInternal) {
        /**
         * SPRD: Bug726136 large file should not include temp files
         * @{
         */
        if (f.length() > LAEGE_FILE_FILTER_SIZE && isLargeFileUnique(f)) {
            Log.e(TAG, "got large file:" + f.getAbsolutePath());
            mLargeFileSize += f.length();
            mData.mLargeMap.put(f.getAbsolutePath(), f.length());
            if (f.getName().endsWith(LARGE_AUDIO1_FILE_EXT) || f.getName().endsWith(LARGE_AUDIO2_FILE_EXT)) {
                mData.mRubbish_large_audio_ext.add(new FileDetailModel(f.getAbsolutePath(), f.length()));
            } else if (f.getName().endsWith(FILE_TYPE_VIDEO_AVI) || f.getName().endsWith(FILE_TYPE_VIDEO_MOV)
                    || f.getName().endsWith(FILE_TYPE_VIDEO_MP4) || f.getName().endsWith(FILE_TYPE_VIDEO_MKV)
                    || f.getName().endsWith(FILE_TYPE_VIDEO_RMVB)) {
                mData.mRubbish_large_video_ext.add(new FileDetailModel(f.getAbsolutePath(), f.length()));
            } else {
                mData.mRubbish_large_ext.add(new FileDetailModel(f.getAbsolutePath(), f.length()));
            }
            DataGroup.getInstance().mLargeFileCategorySize = mLargeFileSize;
            mInhandler.sendEmptyMessage(UPDATE_LARGE_FILE_SIZE);
            mData.mUniqueLargeFileSize += f.length();
            mInhandler.sendEmptyMessage(UPDATE_SIZE);
        }
        /**
         * @}
         */
    }

    private long getLargeFileSize(boolean isInternal) {
        long size = 0;

        ArrayMap<String,Long> map = isInternal ? mData.mLargeMap : mData.mExLargeMap;

        for (Iterator<Map.Entry<String, Long>> it =map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Long> entry = it.next();
            String key = entry.getKey();

            File f = new File(key);
            if (f.exists()) {
                size += f.length();
            } else {
                it.remove();
            }
        }
        return size;
    }

    private long scanDirSize(File dir) {
        File[] fileList = dir.listFiles();
        int size = 0;
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isDirectory()) {
                    size += scanDirSize(file);
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }

    private Button mExternalButton;
    private TextView mExternalBackground;
    private TextView mExternalSizeView;
    private ListView mExternalListView;
    private View mExternalCacheItem;
    private View mExternalRubbishItem;
    private View mExternalApkItem;
    private View mExternalTmpItem;
    private View mExternalLargeItem;

    private void initExternalViews() {
        mExternalButton = (Button) mExternal
                    .findViewById(Tab.EXTERNAL.mButton);
        mExternalBackground = (TextView) mExternal
                    .findViewById(Tab.EXTERNAL.mBackground);
        mExternalSizeView = (TextView) mExternal
                    .findViewById(Tab.EXTERNAL.mText);
        mExternalListView = (ListView) mExternal
                    .findViewById(Tab.EXTERNAL.mList);

        mExternalCacheItem = mExternalListView.getChildAt(CACHE_ITEM_INDEX);
        mExternalRubbishItem = mExternalListView.getChildAt(RUBBISH_ITEM_INDEX);
        mExternalApkItem = mExternalListView.getChildAt(APK_ITEM_INDEX);
        mExternalTmpItem = mExternalListView.getChildAt(TMP_ITEM_INDEX);
        mExternalLargeItem = mExternalListView.getChildAt(LARGE_ITEM_INDEX);
    }

    Handler mExhandler = new Handler() {
        public void handleMessage(Message msg) {
            long orgSize = 0;
            if (mExternalRubbishItem == null) {
                initExternalViews();
            }
            switch (msg.what) {
            default:
            case UPDATE_APK_SIZE:
                removeMessages(UPDATE_APK_SIZE);
                sendEmptyMessage(UPDATE_SIZE);
                break;
            case UPDATE_TMP_SIZE:
                removeMessages(UPDATE_TMP_SIZE);
                sendEmptyMessage(UPDATE_SIZE);
                break;
            case UPDATE_RUBBISH_SIZE:
                removeMessages(UPDATE_RUBBISH_SIZE);
                sendEmptyMessage(UPDATE_SIZE);
                break;
            case UPDATE_CACHE_SIZE:
                removeMessages(UPDATE_CACHE_SIZE);
                sendEmptyMessage(UPDATE_SIZE);
                break;

            case UPDATE_LARGE_FILE_SIZE:
                 removeMessages(UPDATE_LARGE_FILE_SIZE);
                 Log.e(TAG, "EXhandler update:" + Formatter.formatFileSize(mContext, mExLargeFileSize));
                sendEmptyMessage(UPDATE_SIZE);
                break;
            case UPDATE_SIZE:
                removeMessages(UPDATE_SIZE);
                Log.i(TAG, "update.....***." + mExCacheSize + mExRubbishSize
                        + mExTmpSize + mExApkSize + ":" + mData.mExUniqueLargeFileSize + ":" + mLargeFileSize);
                orgSize = mExCacheSize + mExRubbishSize + mExTmpSize
                        + mExApkSize + mData.mExUniqueLargeFileSize;
                orgSize = (orgSize <= 0) ? 0 : orgSize;

                mExternalSizeView.setText(Formatter.formatFileSize(mContext, orgSize));
                break;
            case SET_BUTTON_CLEAN:
                removeMessages(SET_BUTTON_CLEAN);
                mExternalButton.setText(mContext.getResources().getString(
                        R.string.clean_memory_button));
                mExternalBackground.clearAnimation();
                mExternalBackground.setVisibility(View.GONE);

                orgSize = mExCacheSize + mExRubbishSize + mExTmpSize
                        + mExApkSize + mData.mExUniqueLargeFileSize;;
                Log.e(TAG, "EXhandler set button:" + orgSize);
                if (orgSize <= 0) {
                    mExternalButton.setText(mContext.getResources().getString(R.string.end_clean_memory_button));
                }

                for (int i = 0; i < mExternalListView.getChildCount(); i++) {
                    View item = mExternalListView.getChildAt(i);
                }
                break;
            case SET_BUTTON_CLEAN_END:
                removeMessages(SET_BUTTON_CLEAN_END);
                mExternalBackground.clearAnimation();
                mExternalBackground.setVisibility(View.GONE);
                mExternalButton.setText(mContext.getResources().getString(
                        R.string.end_clean_memory_button));
                break;
            }
        }
    };

    // Views for internal tab.
    private Button mInternalButton;
    private TextView mInternalBackground;
    private TextView mInternalSizeView;
    private TextView mInternalScore_LableView;
    private ListView mInternalListView;
    private View mInternalCacheItem;
    private View mInternalRubbishItem;
    private View mInternalApkItem;
    private View mInternalTmpItem;
    private View mInternalLargeItem;

    void initInternalViews() {
        mInternalButton =  (Button) mInternal.findViewById(Tab.INTERNAL.mButton);
        mInternalBackground = (TextView) mInternal.findViewById(Tab.INTERNAL.mBackground);
        mInternalSizeView = (TextView) mInternal.findViewById(Tab.INTERNAL.mText);
        mInternalScore_LableView= (TextView) mInternal.findViewById(Tab.INTERNAL.mScoreLabel);
        mInternalListView = (ListView) mInternal.findViewById(Tab.INTERNAL.mList);

        mInternalCacheItem = mInternalListView.getChildAt(CACHE_ITEM_INDEX);
        mInternalRubbishItem = mInternalListView.getChildAt(RUBBISH_ITEM_INDEX);
        mInternalApkItem = mInternalListView.getChildAt(APK_ITEM_INDEX);
        mInternalTmpItem = mInternalListView.getChildAt(TMP_ITEM_INDEX);
        mInternalLargeItem = mInternalListView.getChildAt(LARGE_ITEM_INDEX);
    }

    private boolean flag;
    Handler mInhandler = new Handler() {

        public void handleMessage(Message msg) {
            long orgSize = 0;
            if (mInternalRubbishItem == null) {
                initInternalViews();
            }
            File file = (File) msg.obj;
            switch (msg.what) {
            default:
            case UPDATE_PATH_UI:
                if(file != null){
                    mInternalBackground.setText(mContext.getResources().getString(R.string.doing_scan_memory_button)+file.getAbsolutePath());
                }
                break;
            case UPDATE_APK_SIZE:
                removeMessages(UPDATE_APK_SIZE);
                mInApkSize = DataGroup.getInstance().mAPKCategorySize;
                Log.i(TAG, "mInApkSize=" + mInApkSize);
                if (mInternalApkItem != null) {
                    final ProgressBar mProGressBar = (ProgressBar) mInternalApkItem
                            .findViewById(R.id.progress);
                    mProGressBar.setVisibility(View.GONE);
                    final ImageView mImg = (ImageView) mInternalApkItem
                            .findViewById(R.id.img);
                    mImg.setVisibility(View.VISIBLE);
                    mImg.setImageDrawable(mContext.getResources().getDrawable(R.drawable.finish_optimize));
                }
                sendEmptyMessage(UPDATE_SIZE);
                break;
            case UPDATE_TMP_SIZE:
                removeMessages(UPDATE_TMP_SIZE);
                if (mInternalTmpItem != null) {
                    final ProgressBar mProGressBar = (ProgressBar) mInternalTmpItem
                            .findViewById(R.id.progress);
                    mProGressBar.setVisibility(View.GONE);
                    final ImageView mImg = (ImageView) mInternalTmpItem
                            .findViewById(R.id.img);
                    mImg.setVisibility(View.VISIBLE);
                    mImg.setImageDrawable(mContext.getResources().getDrawable(R.drawable.finish_optimize));
                }
                sendEmptyMessage(UPDATE_SIZE);
                break;
            case UPDATE_RUBBISH_SIZE:
                removeMessages(UPDATE_RUBBISH_SIZE);
                Log.i(TAG, "mInRubbishSize=" + mInRubbishSize);
                if (mInternalRubbishItem != null) {
                    final ProgressBar mProGressBar = (ProgressBar) mInternalRubbishItem
                            .findViewById(R.id.progress);
                    mProGressBar.setVisibility(View.GONE);
                    final ImageView mImg = (ImageView) mInternalRubbishItem
                            .findViewById(R.id.img);
                    mImg.setVisibility(View.VISIBLE);
                    mImg.setImageDrawable(mContext.getResources().getDrawable(R.drawable.finish_optimize));
                }
                sendEmptyMessage(UPDATE_SIZE);
                break;
            case UPDATE_CACHE_SIZE:
                removeMessages(UPDATE_CACHE_SIZE);
                Log.i(TAG, "update cache......" + mInCacheSize);
                sendEmptyMessage(UPDATE_SIZE);
                break;
            case UPDATE_LARGE_FILE_SIZE:
                removeMessages(UPDATE_LARGE_FILE_SIZE);
                Log.e(TAG, "inhandler update:" + Formatter.formatFileSize(mContext, mLargeFileSize));
                if (mInternalLargeItem != null) {
                    final ProgressBar mProGressBar = (ProgressBar) mInternalLargeItem
                            .findViewById(R.id.progress);
                    mProGressBar.setVisibility(View.GONE);
                    final ImageView mImg = (ImageView) mInternalLargeItem
                            .findViewById(R.id.img);
                    mImg.setVisibility(View.VISIBLE);
                    mImg.setImageDrawable(mContext.getResources().getDrawable(R.drawable.finish_optimize));
                }
                sendEmptyMessage(UPDATE_SIZE);
                break;
            case UPDATE_SIZE:
                removeMessages(UPDATE_SIZE);
                Log.i(TAG, "update......" + mInCacheSize + mInRubbishSize
                       + mInTmpSize + mInApkSize);
                orgSize = mInCacheSize + mInRubbishSize + mInTmpSize
                        + mInApkSize + mData.mUniqueLargeFileSize;
                String size_lable = Formatter.formatFileSize(mContext, orgSize);
                String[] strSize_lable = size_lable.split(" ");
                if(strSize_lable!= null && strSize_lable.length >1){
                    mInternalSizeView.setText(strSize_lable[0]);
                    mInternalScore_LableView.setText(strSize_lable[1]);

                }
                break;
            case SET_BUTTON_CLEAN:
                removeMessages(SET_BUTTON_CLEAN);
                orgSize = mInCacheSize + mInRubbishSize + mInTmpSize
                        + mInApkSize + mData.mUniqueLargeFileSize;
                Log.e(TAG, "cln button2:" + orgSize);
                for (int i = 0; i < mInternalListView.getChildCount(); i++) {
                    View item = mInternalListView.getChildAt(i);
                    final ProgressBar mProGressBar = (ProgressBar) item
                            .findViewById(R.id.progress);
                    mProGressBar.setVisibility(View.GONE);
                    final ImageView mImg = (ImageView) item
                            .findViewById(R.id.img);
                    mImg.setVisibility(View.VISIBLE);
                    mImg.setImageDrawable(mContext.getResources().getDrawable(R.drawable.finish_optimize));
                }
                if (orgSize <= 0) {
                    mInternalButton.setText(mContext.getResources().
                                                  getString(R.string.clear_memory));
                    // SPRD: Bug728805 hide path info when finish clearing rubbish
                    mInternalBackground.setVisibility(View.INVISIBLE);
                } else {
                    startUpdateActivity();
                }
                break;
            case SET_BUTTON_CLEAN_END:
                removeMessages(SET_BUTTON_CLEAN_END);
                if (mSession != null) {
                    mSession.release();
                }
                mInternalButton.setText(mContext.getResources().getString(
                        R.string.end_clean_memory_button));
                break;
            }
        }
    };

    private final int mNameIds[] = { R.string.cache_file,
            R.string.rubbish_file, R.string.invalid_file, R.string.temp_file, R.string.large_file };

    private class FileAdapter extends BaseAdapter {
        class ViewHolder {
            TextView name;
        }

        private LayoutInflater mInflater;

        public FileAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
            Log.i(TAG, "mInflater:" + mInflater);
        }

        @Override
        public int getCount() {
            return mNameIds != null ? mNameIds.length : 0;
        }

        @Override
        public Object getItem(int position) {
            return mNameIds[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = new ViewHolder();
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.storage_type_item,
                        null);
                holder.name = (TextView) convertView.findViewById(R.id.name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.name.setText(mNameIds[position]);
            Log.i(TAG, position+"\t convertView:" + convertView);
            return convertView;
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {}

    private ClearCacheObserver mClearCacheObserver;

    class ClearCacheObserver extends IPackageDataObserver.Stub {
        public void onRemoveCompleted(final String packageName,
                final boolean succeeded) {
            Log.i(TAG, "clear cache>" + packageName + "  :" + succeeded);
            final Message msg = mHandler.obtainMessage(REQUEST_SIZE);
            AppEntry appEntry = mState.getEntry(packageName,
                    UserHandle.myUserId());
            Log.i(TAG, "appEntry:  " + appEntry.cacheSize + "   ex:"
                    + appEntry.externalCacheSize);
            msg.obj = packageName;
            mHandler.sendMessage(msg);
        }
    }

    boolean isChange;
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case REQUEST_SIZE:
                // Refresh size info
                removeMessages(REQUEST_SIZE);
                isChange = true;
                callBack = false;
                Log.i(TAG, "refresh >" + msg.obj.toString());
                String packageName = msg.obj.toString();
                mState.requestSize(packageName, UserHandle.myUserId());
                Log.i(TAG, "refresh end");
                break;
            }
        }
    };

    @Override
    protected void onPause() {
        Log.i(TAG, " onPause()");
        super.onPause();
    }
    @Override
    public void onBackPressed() {
        isPressBack = true;
        super.onBackPressed();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIsScanEnd = true; //SPRD: added for #602621
        if (mState != null) {
            if (mState.mThread != null && mState.mThread.getLooper() != null) {
                mState.mThread.getLooper().quit();
            }
            mState = null;
        }

        if (mStorageManager != null && mStorageListener != null) {
            try {
                mStorageManager.unregisterListener(mStorageListener);
            } catch (Exception e) {
                Log.i(TAG, "unregisterListener... exception");
            }
        }
    }

    private boolean callBack;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "------Step5. onActivityResult  delete finished---------");
        /**
         * SPRD: Bug 705510 intent is null after sdcard state changed
         * @{
         */
        long size = 0;
        if (data != null) {
            size = data.getLongExtra("size", 0);
        }
        /**
         * @}
         */

        if  (requestCode == CACHE_ITEM) {
                mInCacheSize = size;
                mInhandler.sendEmptyMessage(UPDATE_CACHE_SIZE);
        }

        checkAfterLargeFileDel(requestCode);
        if (mHasSDcard) {
           if (mTabHost != null) {
               mTabHost.setCurrentTab((requestCode >= EXTERNAL_ACTIVITY_REQUEST_CODE_START)
                                        ? EXTERNAL_STORAGE_ID : INTERNAL_STORAGE_ID);
           }
        }
    }

    void checkAfterLargeFileDel(int requestCode) {
        DataGroup dataGroup = DataGroup.getInstance();
        if (requestCode < EXTERNAL_ACTIVITY_REQUEST_CODE_START) {
            mLargeFileSize = dataGroup.mLargeFileCategorySize;
            mInhandler.sendEmptyMessage(UPDATE_LARGE_FILE_SIZE);

            mInApkSize = dataGroup.mAPKCategorySize;
            mInhandler.sendEmptyMessage(UPDATE_APK_SIZE);

            mInTmpSize = dataGroup.mTempCategorySize;
            mInhandler.sendEmptyMessage(UPDATE_TMP_SIZE);

            mInRubbishSize = dataGroup.mRubbishCategorySize;
            mInhandler.sendEmptyMessage(UPDATE_RUBBISH_SIZE);

            dataGroup.mFileUpdateBits = 0;
        } else {
            mExLargeFileSize = dataGroup.mExLargeFileCategorySize;
            mExhandler.sendEmptyMessage(UPDATE_LARGE_FILE_SIZE);

            mExApkSize = dataGroup.mExAPKCategorySize;
            mExhandler.sendEmptyMessage(UPDATE_APK_SIZE);

            mExTmpSize = dataGroup.mExTempCategorySize;
            mExhandler.sendEmptyMessage(UPDATE_TMP_SIZE);

            mExRubbishSize = dataGroup.mExRubbishCategorySize;
            mExhandler.sendEmptyMessage(UPDATE_RUBBISH_SIZE);

            mExCacheSize = dataGroup.mExCacheFileSize;
            mExhandler.sendEmptyMessage(UPDATE_CACHE_SIZE);

            dataGroup.mFileUpdateBits = 0;
        }
    }

    StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState,
                int newState) {
            Log.i(TAG, "vol state:" + vol + "volchange  oldState:" + oldState
                    + "    newState:" + newState);

            if (vol.linkName != null && vol.linkName.startsWith(SDCARD_PREFIX)) {
                if ((oldState == VolumeInfo.STATE_MOUNTED && newState == VolumeInfo.STATE_EJECTING)) {
                    Toast.makeText(mContext, mContext.getResources().getString(R.string.sdcard_state_removed),
                        Toast.LENGTH_SHORT).show();
                    /** SPRD: Bug 705510 intent is null after sdcard state changed */
                    //refreshUi();
                    finish();
                } else if (oldState == VolumeInfo.STATE_CHECKING
                        && newState == VolumeInfo.STATE_MOUNTED) {
                    Toast.makeText(mContext, mContext.getResources().getString(R.string.sdcard_state_inserted),
                        Toast.LENGTH_SHORT).show();
                    refreshUi();
                }
            }
        }
    };
    private void startUpdateActivity(){
        Intent intent = new Intent(StorageManagement.this,
                StorageClearManagement.class);
        startActivity(intent);
        finish();
    }

    private void refreshUi() {
        Log.i(TAG, "refreshUi().........");
        mInAdapter = new FileAdapter(mContext);
            Log.i(TAG, "***************  refresh INTERNAL  *************");
            String text = "";
            resetData();
            Log.i(TAG, "mState=" + mState + "    mSession=" + mSession);
            mLayout.removeAllViews();
            mInternal = mInflater.inflate(R.layout.manage_storage_2, null);
            mInButton = ((Button) mInternal.findViewById(R.id.internal_bt));
            mInButton.setOnClickListener(this);

            ListView internalList = (ListView) mInternal
                    .findViewById(R.id.internal_list);
            internalList.setAdapter(mInAdapter);
            mLayout.addView(mInternal);
    }

    private void resetData() {
        mData.mInCacheMap.clear();
        mData.mInRubbishMap.clear();
        mData.mInApkMap.clear();
        mData.mInTmpMap.clear();
        mData.mLargeMap.clear();

        mData.mExCacheMap.clear();
        mData.mExRubbishMap.clear();
        mData.mExApkMap.clear();
        mData.mExTmpMap.clear();
        mData.mExLargeMap.clear();

        mData.mRubbish_bak_ext.clear();
        mData.mRubbish_log_ext.clear();
        mData.mRubbish_tmp_prefix.clear();
        mData.mRubbish_tmp_ext.clear();
        mData.mRubbish_apk_ext.clear();
        mData.mRubbish_cach1_ext.clear();
        mData.mRubbish_cach2_ext.clear();
        mData.mRubbish_large_ext.clear();
        mData.mRubbish_large_video_ext.clear();
        mData.mRubbish_large_audio_ext.clear();

        // SPRD: Bug730263 large file is cleared, but not reset the value
        mData.mUniqueLargeFileSize = 0;
        mInCacheSize = 0;
        mInRubbishSize = 0;
        mInApkSize = 0;
        mInTmpSize = 0;
        mLargeFileSize = 0;
        mUniqueLargeFileSize = 0;

        mExCacheSize = 0;
        mExRubbishSize = 0;
        mExApkSize = 0;
        mExTmpSize = 0;
        mExLargeFileSize = 0;
        mExUniqueLargeFileSize = 0;

        callBack = false;

        Log.i(TAG, "mSession=" + mSession);
        if (mSession != null) {

            if (mState != null) {
                Log.i(TAG,
                        "sessions before:" + mState.mSessions == null ? "mState.mSessions is null"
                                : "" + mState.mSessions.size()
                                + "\n\t\tmState.mSessions="
                                + mState.mSessions);
            }
            mSession.release();
            if (mState != null) {
                Log.i(TAG,
                        "sessions after:" + mState.mSessions == null ? "mState.mSessions is null"
                                : "" + mState.mSessions.size()
                                + "\n\t\tmState.mSessions="
                                + mState.mSessions);
            }
        }
        if (mState != null) {
            if (mState.mThread != null && mState.mThread.getLooper() != null) {
                mState.mThread.getLooper().quit();
            }
            mState = null;
        }
    }
}
