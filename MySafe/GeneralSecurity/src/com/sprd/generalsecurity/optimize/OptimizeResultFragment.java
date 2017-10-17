package com.sprd.generalsecurity.optimize;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.EnvironmentEx;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sprd.generalsecurity.GeneralSecurityManagement;
import com.sprd.generalsecurity.R;
import com.sprd.generalsecurity.storage.DataGroup;
import com.sprd.generalsecurity.utils.ApplicationsState;
import com.sprd.generalsecurity.utils.Contract;
import com.sprd.generalsecurity.utils.RunningState;
import com.sprd.generalsecurity.utils.TeleUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OptimizeResultFragment extends Fragment {
    private final int mNameIds[] = {R.string.rubbish_file_scan, R.string.running_process, R.string.data_control};
    private final int mIconIds[] = {R.drawable.garbage_optimize, R.drawable.cache_optimize, R.drawable.data_optimize};
    private static final String TAG = "GeneralSecurity OptimizeResultFragment";
    public static final String RUBBISH_FILE2_EXT = ".bak";
    public static final String TMP_FILE_PREFIX = "~";
    public static final String TMP_FILE_EXT = ".tmp";
    public static final String APK_FILE_EXT = ".apk";
    private static final int REQUEST_SIZE = 0;

    public static final int FINISH_RUBBISH_SCAN = 1;
    public static final int FINISH_APPS_CACHE_SCAN = 2;
    private static final int FINISH_RUBBISH_OPTIMIZE = 3;
    private static final int FINISH_APPS_CACHE_OPTIMIZE = 4;
    private static final int UPDATE_DATA_SET = 5;
    private static final int UPDATE_SCORE = 6;
    private static final int UPDATE_PATH = 7;

    public static final int RUBBISH_OPTIMIZE_ITEM = 0;
    public static final int MEMORY_OPTIMIZE_ITEM = 1;
    public static final int DATA_SET_OPTIMIZE_ITEM = 2;

    public static final int CACHE_ITEM = 0;
    public static final int RUBBISH_ITEM = 1;
    public static final int APK_ITEM = 2;
    public static final int TMP_ITEM = 3;

    public static final int FLAG_DATA_SET = 0;
    public static final int DATA_SET_SCORE = 3;
    private static final String SDCARD_PREFIX = "sdcard";
    private static final long MB = 1024 * 1024;

    private List<EntryItem> mItemData = new ArrayList<EntryItem>();
    private Context mContext;
    private View mRootView;
    private OptimizeResultAdapter mOptimizeAdapter;
    private ListView mOptimizeList;
    private ApplicationsState mApplicationsState;
    private ApplicationsState.Session mSession;
    private long mInCacheSize;
    private long mInRubbishSize;
    private long mInApkSize;
    private long mInTmpSize;
    private long mExCacheSize;
    private long mExRubbishSize;
    private long mExApkSize;
    private long mExTmpSize;
    private float mBeforeRubbishScore = 0;
    private float mBeforeMemoryhScore = 0;

    private long mScanRubbishSize = 0; // the rubbish size after scan
    private long mScanAppCacheSize = 0; // the app cache size after scan

    private DataGroup mData = DataGroup.getInstance();
    private boolean mAllSizesComputedEnd; // mark scan is end
    private boolean mRubbishScanEnd; // mark rubbish scan is end
    private boolean mAppsCacheScanEnd; // mark apps cache scan is end
    private boolean mRubbishOptimizeEnd; // mark rubbish scan is end
    private boolean mAppsCacheOptimizeEnd; // mark apps cache scan is end
    private StorageManager mStorageManager;
    private final ApplicationsState.Callbacks mCallbacks = new ApplicationsStateCallbacks();
    private ApplicationsState.AppEntry mAppEntry;
    private boolean mIsChange; // flag to delete Application Cache Files
    private ClearCacheObserver mClearCacheObserver;
    private Listener mListener; // to refresh main UI
    private int mScore; // the score display in main ui
    private PackageManager mPm;
    private RunningState mRunningState; // to get running apps
    private ArrayList<RunningState.MergedItem> mBackgroundItems; // running apps
    private ArrayList<RunningState.MergedItem> mOldBackgroundItems; //save running apps after scan
    private ActivityManager mAm;
    private int mPrimarySim = 0;
    private float mSimDataTotal;
    private Boolean mDataSetFlag = true;
    private Thread mRubbishThread = null; // thread to scan rubbish
    private Thread mAppsCacheThread = null; // thread to get apps cache
    private Boolean mNeed = false; // true: scan and optimize, false: just scan

    /**
     * Callbacks for refresh main UI.
     */
    public interface Listener {
        void onRefreshMainUi(int score, int mode, Boolean isScanOrOptimizeEnd);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mListener = (Listener) activity;
        Log.i(TAG, "onAttach mNeed = " + mNeed);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStorageManager = mContext.getSystemService(StorageManager.class);
        mStorageManager.registerListener(mStorageListener);
        mScore = Contract.MAX_SCORE;
        mPm = mContext.getPackageManager();
        mAm = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        mRunningState = RunningState.getInstance(mContext);
        mBackgroundItems = new ArrayList<RunningState.MergedItem>();
        mOldBackgroundItems = new ArrayList<RunningState.MergedItem>();
        resetData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.default_list, null);
        mOptimizeList = (ListView) mRootView.findViewById(R.id.list);
        mItemData = getItemData(mNameIds, mIconIds);
        mOptimizeAdapter = new OptimizeResultAdapter(mContext);
        mOptimizeList.setAdapter(mOptimizeAdapter);
        if (getActivity() != null) {
            mNeed = ((GeneralSecurityManagement) getActivity()).mNeedOptimize;
        }
        ScanAppsCache();
        ScanRubbish();
        CheckDataFlowSet(!mNeed);
        return mRootView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAllSizesComputedEnd = true;
        mData.destroy();
        if (mApplicationsState != null) {
            if (mApplicationsState.mThread != null && mApplicationsState.mThread.getLooper() != null) {
                mApplicationsState.mThread.getLooper().quit();
            }
            mApplicationsState = null;
        }
        if (mSession != null) {
            mSession.release();
        }
        if (mStorageManager != null) {
            try {
                mStorageManager.unregisterListener(mStorageListener);
            } catch (Exception e) {
                Log.d(TAG, "unregisterListener... exception");
            }
        }
        mBackgroundItems.clear();
        mOldBackgroundItems.clear();
    }

    private void resetData() {
        mInCacheSize = 0;
        mInRubbishSize = 0;
        mInApkSize = 0;
        mInTmpSize = 0;

        mExCacheSize = 0;
        mExRubbishSize = 0;
        mExApkSize = 0;
        mExTmpSize = 0;

        mData.mRubbishCategorySize = 0;
        mData.mTempCategorySize = 0;
        mData.mAPKCategorySize = 0;
        mData.mExRubbishCategorySize = 0;
        mData.mExTempCategorySize = 0;
        mData.mExAPKCategorySize = 0;

        mData.mInRubbishMap.clear();
        mData.mInTmpMap.clear();
        mData.mInApkMap.clear();
        mData.mExRubbishMap.clear();
        mData.mExTmpMap.clear();
        mData.mExApkMap.clear();
        mData.mInCacheMap.clear();
        mData.mExCacheMap.clear();
    }

    /**
     * Check if data plan has set to avoid overusing your data.
     *
     * @param update whether to refresh the score text .
     * @return {@code true} data plan set, {@code false} otherwise.
     */
    public Boolean CheckDataFlowSet(Boolean update) {
        if (TeleUtils.getSimCount(mContext) == 0) {
            return false;
        }
        mPrimarySim = TeleUtils.getPrimarySlot(mContext) - 1;
        Log.i(TAG, "mPrimarySim = " + mPrimarySim);
        SharedPreferences pref;
        if (mPrimarySim == Contract.SIM1_INDEX) {
            pref = mContext.getSharedPreferences(Contract.SIM1, Context.MODE_PRIVATE);
        } else if (mPrimarySim == Contract.SIM2_INDEX) {
            pref = mContext.getSharedPreferences(Contract.SIM2, Context.MODE_PRIVATE);
        } else {
            return false;
        }
        mSimDataTotal = Float.parseFloat(pref.getString(Contract.KEY_MONTH_TOTAL, "0"));
        if (mSimDataTotal == 0 && mDataSetFlag) {
            mScore -= DATA_SET_SCORE;
            mDataSetFlag = false;
        } else if (mSimDataTotal != 0 && !mDataSetFlag) {
            mDataSetFlag = true;
            mScore += DATA_SET_SCORE;
        }
        Log.i(TAG, "mSimDataTotal = " + mSimDataTotal + "mDataSetFlag = " + mDataSetFlag
                + "mScore = " + mScore);
        if (update && mListener != null) {
            mListener.onRefreshMainUi(mScore, UPDATE_DATA_SET, false);
        }
        return mSimDataTotal == 0 ? false : true;
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
                } else if (oldState == VolumeInfo.STATE_CHECKING
                        && newState == VolumeInfo.STATE_MOUNTED) {
                    Toast.makeText(mContext, mContext.getResources().getString(R.string.sdcard_state_inserted),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private class EntryItem {
        public int icon;
        public int name;
    }

    private List<EntryItem> getItemData(int[] nameIds, int[] iconIds) {
        List<EntryItem> listItem = new ArrayList<EntryItem>();
        for (int i = 0; i < nameIds.length; i++) {
            EntryItem item = new EntryItem();
            item.icon = iconIds[i];
            item.name = nameIds[i];
            listItem.add(item);
        }
        return listItem;
    }

    class ResultViewHolder {
        public ImageView icon;
        public TextView name;
        public TextView description;
        public ProgressBar progress;
        public ImageView done;
        public ImageView set;
    }

    private class OptimizeResultAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public OptimizeResultAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mItemData.size();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ResultViewHolder holder;
            Log.v("OptimizeResultAdapter", "getView " + position + " " + convertView);

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.optimize_result_list_item, null);
                holder = new ResultViewHolder();
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                holder.name = (TextView) convertView.findViewById(R.id.name);
                holder.description = (TextView) convertView.findViewById(R.id.description);
                holder.progress = (ProgressBar) convertView.findViewById(R.id.progress);
                holder.done = (ImageView) convertView.findViewById(R.id.done);
                holder.set = (ImageView) convertView.findViewById(R.id.set);
                convertView.setTag(holder);
            } else {
                holder = (ResultViewHolder) convertView.getTag();
            }

            holder.icon.setImageDrawable(getResources().getDrawable(mItemData.get(position).icon));
            holder.name.setText(mItemData.get(position).name);
            holder.description.setText(R.string.prepare_optimize);
            holder.progress.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress_small));
            holder.set.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent it = new Intent("com.sprd.generalsecurity.network.dataflowsetting");
                    it.putExtra(Contract.EXTRA_SIM_ID, TeleUtils.getPrimarySlot(mContext));
                    startActivityForResult(it, FLAG_DATA_SET);
                }
            });
            if (TeleUtils.getSimCount(mContext) == 0 && position == DATA_SET_OPTIMIZE_ITEM) {
                convertView.setVisibility(View.GONE);
            } else {
                convertView.setVisibility(View.VISIBLE);
            }
            return convertView;
        }

        @Override
        public Object getItem(int position) {
            return mItemData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FLAG_DATA_SET) {
            refreshListView(DATA_SET_OPTIMIZE_ITEM, null, CheckDataFlowSet(true));
        }
    }

    private void scanInCached(Activity activity) {
        ApplicationsState.sInstance = null;
        mApplicationsState = ApplicationsState.getInstance(activity.getApplication());
        mSession = mApplicationsState.newSession(mCallbacks);
        mSession.resume();
    }

    private class ApplicationsStateCallbacks implements ApplicationsState.Callbacks {

        @Override
        public void onRunningStateChanged(boolean running) {
        }

        @Override
        public void onPackageListChanged() {
        }

        @Override
        public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
        }

        @Override
        public void onPackageIconChanged() {
        }

        @Override
        public void onPackageSizeChanged(String packageName) {
            if (mApplicationsState == null) {
                return;
            }

            mAppEntry = mApplicationsState.getEntry(packageName, UserHandle.myUserId());

            Log.e(TAG, "----------entry:" + packageName + "\t " + (mAppEntry.cacheSize + mAppEntry.externalCacheSize) + "  " + mAppEntry + ":" + mIsChange);

            if (mIsChange) { // clear
                ArrayList<String> list = new ArrayList<String>();
                for (int i = 0; i < mData.mInCacheMap.size(); i++) {
                    String name = new File(mData.mInCacheMap.keyAt(i)).getName();
                    ApplicationsState.AppEntry appEntry = mApplicationsState.getEntry(name, UserHandle.myUserId());
                    Log.i(TAG, " i:" + i + "\tappEntry:" + name + "\tsize:" + (mAppEntry.cacheSize + mAppEntry.externalCacheSize) + " \t" + mData.mInCacheMap.valueAt(i));
                    if (mAppEntry.cacheSize + mAppEntry.externalCacheSize <= 0
                            || (mAppEntry.cacheSize == 4096 * 3 && mAppEntry.externalCacheSize == 0)) {
                        mInCacheSize -= mData.mInCacheMap.valueAt(i);
                        list.add(mData.mInCacheMap.keyAt(i));
                    }
                }
                for (int i = 0; i < list.size(); i++) {
                    Log.i(TAG, "remove " + i + ":\t" + list.get(i));
                    mData.mInCacheMap.remove(list.get(i));
                }
                mData.mInCacheMap.clear();
                mIsChange = false;
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
                        mData.mInCacheMap.put((Environment.getDataDirectory()
                                .getAbsoluteFile()
                                + File.separator
                                + "data"
                                + File.separator + packageName), (mAppEntry.cacheSize
                                + mAppEntry.externalCacheSize));
                        mInCacheSize += mAppEntry.cacheSize
                                + mAppEntry.externalCacheSize;

                        Log.i(TAG, "mInRubbishMap \t\tfile:" + packageName + ":\t" + mInCacheSize);
                        //mInhandler.sendEmptyMessage(UPDATE_CACHE_SIZE);

                    }
                }
            }

            Log.e("TAG", "onPack:" + packageName);
        }

        @Override
        public void onAllSizesComputed() {
            if (mIsChange) {
                Log.i(TAG, "..........onAllSizeComputed......................");
                mIsChange = false;
                return;
            }
            mAllSizesComputedEnd = true;
        }

        @Override
        public void onLauncherInfoChanged() {
        }

        @Override
        public void onLoadEntriesCompleted() {
        }
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

    private void ScanRubbish() {
        mRubbishScanEnd = false;
        Log.d(TAG, "startScan");
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (mListener == null) {
            mListener = (Listener) activity;
        }
        scanInCached(activity); // Cache

        new Thread() {
            @Override
            public void run() {
                try {
                    File internalFile = EnvironmentEx.getInternalStoragePath();
                    Log.i(TAG, "Internal Start--------------------------------");
                    Log.i(TAG, "internalFile:" + internalFile.getName() + " ;path:"
                            + internalFile.getAbsolutePath());
                    scanFiles(internalFile.listFiles(), false);
                    Log.i(TAG, "Internal End-------------------------------");

                    if (hasSDcard()) {
                        File externalFile = EnvironmentEx.getExternalStoragePath();
                        Log.i(TAG, "external Start--------------------------------");
                        Log.i(TAG, "externalFile:" + externalFile.getName() + " ;path:"
                                + externalFile.getAbsolutePath());
                        scanFiles(externalFile.listFiles(), true);
                        Log.i(TAG, "external End-------------------------------");
                    }
                } catch (Exception e) {
                    Log.i(TAG, "startScan e = " + e);
                } finally {
                    while (true) {
                        if (mAllSizesComputedEnd) {
                            mAllSizesComputedEnd = false;
                            mHandler.sendEmptyMessage(FINISH_RUBBISH_SCAN);
                            break;
                        }
                        /* SPRD: added for #602621 @{ */
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {

                        }
                        /* @} */
                    }
                }
            }

        }.start();

    }

    private void ScanAppsCache() {
        mAppsCacheScanEnd = false;
        mRunningState.resume(new RunningState.OnRefreshUiListener() {
            @Override
            public void onRefreshUi(int what) {
                mBackgroundItems.clear();
                mBackgroundItems = mRunningState.getCurrentBackgroundItems();
                if (mBackgroundItems.size() > 0 || mRubbishScanEnd) {
                    mRunningState.pause();
                    mHandler.sendEmptyMessage(FINISH_APPS_CACHE_SCAN);
                }

            }
        });

    }

    private void scanFiles(File[] files, Boolean isExternalFile) {
        if (files != null) {
            for (File file : files) {
                Log.i(TAG, file.getName() + ":\t" + file.getAbsolutePath());
                Message msg = mHandler.obtainMessage(UPDATE_PATH, file);
                mHandler.sendMessage(msg);
                if (file.isDirectory()) {
                    Log.i(TAG, "Dir == list>" + file.listFiles());
                    if (isExternalFile) {
                        if (file.getName().equals("cache")
                                | file.getName().equals("code_cache")) {
                            Log.i(TAG, "file.path:" + file.getAbsolutePath());
                            long size = scanDirSize(file);
                            Log.i(TAG, "\t\tsize:" + size);
                            if (size > 0) {
                                mData.mExCacheMap.put((file.getAbsolutePath()), size);
                                mExCacheSize += size;
                                mHandler.sendEmptyMessage(UPDATE_SCORE);
                            }
                            continue;
                        }
                    }
                    scanFiles(file.listFiles(), isExternalFile);
                } else {
                    if (isExternalFile) {
                        refreshExSize(file);
                    } else {
                        String name = file.getName();
                        final String size = Formatter.formatFileSize(mContext, file.length());
                        Log.i(TAG, "name:" + name + "\t size:" + size);
                        if (name.endsWith(RUBBISH_FILE2_EXT)) {
                            mInRubbishSize += file.length();
                            mData.mRubbishCategorySize = mInRubbishSize;
                            mData.mInRubbishMap.put(file.getAbsolutePath(), file.length());
                            mHandler.sendEmptyMessage(UPDATE_SCORE);
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
                            mHandler.sendEmptyMessage(UPDATE_SCORE);
                        } else if (name.endsWith(TMP_FILE_EXT) || name.startsWith(TMP_FILE_PREFIX)) {
                            mInTmpSize += file.length();
                            mData.mTempCategorySize = mInTmpSize;
                            mData.mInTmpMap.put(file.getAbsolutePath(), file.length());
                            mHandler.sendEmptyMessage(UPDATE_SCORE);
                        }
                    }
                }
            }
        }
    }

    private void refreshExSize(File file) {
        String name = file.getName();
        if (name.endsWith(RUBBISH_FILE2_EXT)) {
            Log.i(TAG,
                    "file.path:" + file.getAbsolutePath() + "   name:"
                            + file.getName() + "   size:"
                            + Formatter.formatFileSize(mContext, file.length()));
            mExRubbishSize += file.length();
            mData.mExRubbishCategorySize = mExRubbishSize;
            mData.mExRubbishMap.put(file.getAbsolutePath(), file.length());
            mHandler.sendEmptyMessage(UPDATE_SCORE);
        } else if (name.endsWith(TMP_FILE_EXT) || name.startsWith(TMP_FILE_PREFIX)) {
            mExTmpSize += file.length();
            mData.mExTempCategorySize = mExTmpSize;
            mData.mExTmpMap.put(file.getAbsolutePath(), file.length());
            mHandler.sendEmptyMessage(UPDATE_SCORE);
        } else if (name.endsWith(APK_FILE_EXT)) {
            mExApkSize += file.length();
            mData.mExAPKCategorySize = mExApkSize;
            mData.mExApkMap.put(file.getAbsolutePath(), file.length());
            mHandler.sendEmptyMessage(UPDATE_SCORE);
        }
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

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (getActivity() == null || mListener == null) {
                return;
            }
            String summery;
            switch (msg.what) {
                case REQUEST_SIZE:
                    // Refresh size info
                    removeMessages(REQUEST_SIZE);
                    mIsChange = true;
                    Log.d(TAG, "refresh >" + msg.obj.toString());
                    String packageName = msg.obj.toString();
                    mApplicationsState.requestSize(packageName, UserHandle.myUserId());
                    Log.d(TAG, "refresh end");
                    break;
                case UPDATE_SCORE:
                    if (!mNeed) {
                        mListener.onRefreshMainUi(getScore(getCurrentRubbishSize(),
                                RUBBISH_OPTIMIZE_ITEM, true), UPDATE_SCORE, false);
                    }
                    break;
                case UPDATE_PATH:
                    File file = (File) msg.obj;
                    if (file != null && isVisible()) {
                        summery = mContext.getResources().getString(R.string.doing_scan_memory_button) + file.getAbsolutePath();
                        refreshListView(RUBBISH_OPTIMIZE_ITEM, summery, false);
                    }
                    break;
                case FINISH_RUBBISH_SCAN:
                    Log.i(TAG, "FINISH_RUBBISH_SCAN mNeed = " + mNeed + "mAppsCacheScanEnd =" +
                            mAppsCacheScanEnd);
                    removeMessages(FINISH_RUBBISH_SCAN);
                    mRubbishScanEnd = true;
                    mScanRubbishSize = getCurrentRubbishSize();
                    Log.i(TAG, "mScanRubbishSize = " + mScanRubbishSize / MB);
                    if (!mNeed) {
                        mListener.onRefreshMainUi(getScore(mScanRubbishSize, RUBBISH_OPTIMIZE_ITEM, true),
                                Contract.FINISH_SCAN, mAppsCacheScanEnd);
                        if (!mAppsCacheScanEnd) {
                            ScanAppsCache();
                        }
                    } else {
                        if (mAppsCacheScanEnd) {
                            startAppsCacheOptimize();
                            startRubbishOptimize();
                        }
                    }
                    break;
                case FINISH_APPS_CACHE_SCAN:
                    Log.i(TAG, "FINISH_APPS_CACHE_SCAN");
                    removeMessages(FINISH_APPS_CACHE_SCAN);
                    mAppsCacheScanEnd = true;
                    mOldBackgroundItems.clear();
                    mOldBackgroundItems.addAll(mBackgroundItems);
                    mScanAppCacheSize = getAppCacheSize();
                    Log.i(TAG, "mScanRubbishSize = " + mScanAppCacheSize / MB);
                    if (!mNeed) {
                        mListener.onRefreshMainUi(getScore(mScanAppCacheSize, MEMORY_OPTIMIZE_ITEM, true),
                                Contract.FINISH_SCAN, mRubbishScanEnd);
                    } else {
                        if (mRubbishScanEnd) {
                            startAppsCacheOptimize();
                            startRubbishOptimize();
                        }
                    }
                    break;
                case FINISH_RUBBISH_OPTIMIZE:
                    Log.i(TAG, "FINISH_RUBBISH_OPTIMIZE");
                    removeMessages(FINISH_RUBBISH_OPTIMIZE);
                    mRubbishOptimizeEnd = true;
                    mListener.onRefreshMainUi(getScore(getCurrentRubbishSize(), RUBBISH_OPTIMIZE_ITEM, false),
                            Contract.FINISH_OPTIMIZE, mAppsCacheOptimizeEnd);
                    if (mScanRubbishSize == 0) {
                        summery = getActivity().getString(R.string.no_rubbish);
                    } else {
                        long size = mScanRubbishSize - getCurrentRubbishSize();
                        summery = getActivity().getString(R.string.clean_rubbish,
                                Formatter.formatFileSize(mContext, size >= 0 ? size : mScanRubbishSize));
                    }
                    refreshListView(RUBBISH_OPTIMIZE_ITEM, summery, false);
                    if (mAppsCacheOptimizeEnd) {
                        refreshListView(DATA_SET_OPTIMIZE_ITEM, null, CheckDataFlowSet(true));
                    }
                    break;
                case FINISH_APPS_CACHE_OPTIMIZE:
                    Log.i(TAG, "FINISH_APPS_CACHE_OPTIMIZE");
                    removeMessages(FINISH_APPS_CACHE_OPTIMIZE);
                    mAppsCacheOptimizeEnd = true;
                    if (mOldBackgroundItems != null && mOldBackgroundItems.size() == 0) {
                        summery = getActivity().getString(R.string.nothing_can_removed);
                    } else {
                        Log.i(TAG, " mOldBackgroundItems.size() = " + mOldBackgroundItems.size()
                                + " mBackgroundItems.size() = " + mBackgroundItems.size());
                        long size = mScanAppCacheSize - getAppCacheSize();
                        summery = getActivity().getString(R.string.clean_running_process,
                                mOldBackgroundItems.size() - mBackgroundItems.size(),
                                Formatter.formatFileSize(mContext, size >= 0 ? size : mScanAppCacheSize));
                    }
                    refreshListView(MEMORY_OPTIMIZE_ITEM, summery, false);
                    if (mRubbishOptimizeEnd) {
                        refreshListView(DATA_SET_OPTIMIZE_ITEM, null, CheckDataFlowSet(true));
                    }
                    mListener.onRefreshMainUi(getScore(getAppCacheSize(), MEMORY_OPTIMIZE_ITEM, false),
                            Contract.FINISH_OPTIMIZE, mRubbishOptimizeEnd);
                    break;
            }
        }
    };

    private void refreshListView(int position, String summery, Boolean isDataSet) {
        if (mOptimizeList == null) {
            return;
        }
        View item = mOptimizeList.getChildAt(position);
        if (item == null) {
            return;
        }
        final ProgressBar bar = (ProgressBar) item
                .findViewById(R.id.progress);
        final ImageView done = (ImageView) item
                .findViewById(R.id.done);
        final ImageView set = (ImageView) item
                .findViewById(R.id.set);

        final TextView description = (TextView) item.findViewById(R.id.description);
        if (bar == null || done == null || description == null || set == null) {
            return;
        }
        if (position == RUBBISH_OPTIMIZE_ITEM) {
            done.setVisibility(mRubbishScanEnd &&
                    mRubbishOptimizeEnd ? View.VISIBLE : View.GONE);
            bar.setVisibility(mRubbishScanEnd &&
                    mRubbishOptimizeEnd ? View.GONE : View.VISIBLE);
            set.setVisibility(View.GONE);
            description.setText(summery);
        } else if (position == MEMORY_OPTIMIZE_ITEM) {
            done.setVisibility(View.VISIBLE);
            bar.setVisibility(View.GONE);
            set.setVisibility(View.GONE);
            description.setText(summery);
        } else if (position == DATA_SET_OPTIMIZE_ITEM
                && TeleUtils.getSimCount(mContext) != 0) {
            if (isDataSet) {
                done.setVisibility(View.VISIBLE);
                bar.setVisibility(View.GONE);
                set.setVisibility(View.GONE);
                description.setText(R.string.data_set);
            } else {
                done.setVisibility(View.GONE);
                bar.setVisibility(View.GONE);
                set.setVisibility(View.VISIBLE);
                set.setImageDrawable(getResources().getDrawable(R.drawable.arrow));
                description.setText(R.string.data_unset);
            }
        }
    }

    private long getCurrentRubbishSize() {
        return (mInCacheSize + mInRubbishSize + mInApkSize + mInTmpSize
                + mExCacheSize + mExRubbishSize + mExApkSize + mExTmpSize);
    }

    private long getAppCacheSize() {
        if (mBackgroundItems == null || mBackgroundItems.size() == 0) {
            return 0;
        }
        long size = 0;
        for (int i = 0; i < mBackgroundItems.size(); i++) {
            Object item = mBackgroundItems.get(i);
            if (item instanceof RunningState.MergedItem) {
                size += ((RunningState.MergedItem) item).mSize;
            }
        }
        return size;
    }

    private int getScore(long allSize, int position, Boolean isScan) {
        Log.i(TAG, "allSize = " + allSize / MB + "position = " + position + "isScan = " + isScan);
        if (allSize < 0) {
            return mScore;
        }
        long tempScore;
        long size = allSize / MB;
        switch (position) {
            case RUBBISH_OPTIMIZE_ITEM:
                tempScore = (size % 200 == 0) ? (size / 200) : (size / 200 + 1);
                if (mBeforeRubbishScore != tempScore) {
                    if (isScan) {
                        mScore -= tempScore - mBeforeRubbishScore;
                    } else {
                        mScore += mBeforeRubbishScore - tempScore;
                    }
                    mBeforeRubbishScore = tempScore;
                }
                break;
            case MEMORY_OPTIMIZE_ITEM:
                tempScore = (size % 100 == 0) ? (size / 100) : (size / 100 + 1);
                if (mBeforeMemoryhScore != tempScore) {
                    if (isScan) {
                        mScore -= tempScore - mBeforeMemoryhScore;
                    } else {
                        mScore += mBeforeMemoryhScore - tempScore;
                    }
                    mBeforeMemoryhScore = tempScore;
                }
                break;
            default:
                break;
        }

        if (mScore > Contract.MAX_SCORE) {
            mScore = Contract.MAX_SCORE;
        } else if (mScore < Contract.MIN_SCORE) {
            mScore = Contract.MIN_SCORE;
        }
        return mScore;
    }


    class ClearCacheObserver extends IPackageDataObserver.Stub {
        public void onRemoveCompleted(final String packageName,
                                      final boolean succeeded) {
            Log.i(TAG, "clear cache>" + packageName + "  :" + succeeded);
            final Message msg = mHandler.obtainMessage(REQUEST_SIZE);
            ApplicationsState.AppEntry appEntry = mApplicationsState.getEntry(packageName,
                    UserHandle.myUserId());
            // SPRD: Bug706665 optimization continues without stopping
            if (appEntry != null) {
                Log.i(TAG, "appEntry:  " + appEntry.cacheSize + " ex:"
                        + appEntry.externalCacheSize);
            }
            msg.obj = packageName;
            mHandler.sendMessage(msg);
        }
    }

    public void startRubbishOptimize() {
        mRubbishOptimizeEnd = false;
        mRubbishThread = new Thread() {
            @Override
            public void run() {
                deleteFiles(mData.mInRubbishMap, RUBBISH_ITEM, false);
                deleteFiles(mData.mInTmpMap, TMP_ITEM, false);
                deleteFiles(mData.mInApkMap, APK_ITEM, false);
                notifyMediaScanDir(mContext, EnvironmentEx.getInternalStoragePath());
                if (mData.mInCacheMap != null && mData.mInCacheMap.size() > 0) {
                    // delete cache
                    if (mClearCacheObserver == null) {
                        mClearCacheObserver = new ClearCacheObserver();
                    }
                    for (int i = 0; i < mData.mInCacheMap.size(); i++) {
                        File file = new File(mData.mInCacheMap.keyAt(i));
                        String packageName = file.getName();
                        Log.i(TAG, " i:" + i);
                        Log.i(TAG, i + ": " + packageName);
                        mPm.deleteApplicationCacheFiles(packageName,
                                mClearCacheObserver);
                    }
                }

                if (hasSDcard()) {
                    deleteFiles(mData.mExCacheMap, CACHE_ITEM, true);
                    if (!ActivityManager.isUserAMonkey()) {
                        deleteFiles(mData.mExRubbishMap, RUBBISH_ITEM, true);
                    }
                    deleteFiles(mData.mExTmpMap, TMP_ITEM, true);
                    deleteFiles(mData.mExApkMap, APK_ITEM, true);
                    notifyMediaScanDir(mContext, EnvironmentEx.getExternalStoragePath());
                }
                while (true) {
                    if (mData.mInCacheMap == null || mData.mInCacheMap.size() <= 0) {
                        if (mOptimizeList != null && mOptimizeList.getChildCount()
                                == mOptimizeList.getCount()) {
                            mHandler.sendEmptyMessage(FINISH_RUBBISH_OPTIMIZE);
                            break;
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {

                    }
                }
            }
        };
        mRubbishThread.start();
    }

    private void deleteFiles(ArrayMap<String, Long> map, int type,
                             Boolean isExternal) {
        if (isExternal) {
            for (int i = 0; i < map.size(); i++) {
                File file = new File(map.keyAt(i));
                Message msg = mHandler.obtainMessage(UPDATE_PATH);
                msg.obj = file;
                mHandler.sendMessage(msg);
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
                } else {
                    if (dirDelete(new File(map.keyAt(i)))) {
                        mExCacheSize -= size;
                    }
                    Log.i(TAG, "mExCacheSize:" + mExCacheSize + "   fileSize:"
                            + size);
                    map.removeAt(i);
                }
            }
            map.clear();
        } else {
            for (int i = 0; i < map.size(); i++) {
                File file = new File(map.keyAt(i));
                Message msg = mHandler.obtainMessage(UPDATE_PATH, file);
                msg.obj = file;
                mHandler.sendMessage(msg);
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
        Log.i(TAG, "send broadcast to scan dir = " + dir);
        String path = dir.getPath();
        Intent intent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_DIR");
        Bundle bundle = new Bundle();
        bundle.putString("scan_dir_path", path);
        intent.putExtras(bundle);
        context.sendBroadcast(intent);
    }

    public void startAppsCacheOptimize() {
        mAppsCacheOptimizeEnd = false;
        mAppsCacheThread = new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < mBackgroundItems.size(); i++) {
                    Object item = mBackgroundItems.get(i);
                    if (item instanceof RunningState.MergedItem) {
                        ActivityManager.RunningAppProcessInfo appProcessInfo = ((RunningState.MergedItem) item).mProcess.mRunningProcessInfo;
                        if (appProcessInfo.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE) {
                            String[] pkgList = appProcessInfo.pkgList;
                            for (int j = 0; j < pkgList.length; ++j) {
                                Log.d(TAG, j + ": It will be killed, package name : "
                                        + pkgList[j]);
                                mAm.killBackgroundProcesses(pkgList[j]);
                            }
                        }
                    }
                }

                while (true) {
                    if (mOptimizeList != null && mOptimizeList.getChildCount()
                            == mOptimizeList.getCount()) {
                        mBackgroundItems.clear();
                        mHandler.sendEmptyMessage(FINISH_APPS_CACHE_OPTIMIZE);
                        break;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {

                    }
                }
            }
        };
        mAppsCacheThread.start();
    }
}


