package com.sprd.generalsecurity.storage;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.EnvironmentEx;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sprd.generalsecurity.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ConcurrentModificationException;

public class StorageClearManagement extends Activity implements View.OnClickListener,
        OnItemClickListener {

    private static final String TAG = "StorageClearManagement";
    public static final int CACHE_ITEM = 0;
    public static final int RUBBISH_ITEM = 1;
    public static final int APK_ITEM = 2;
    public static final int TMP_ITEM = 3;
    public static final int LARGE_FILE_ITEM = 4;

    public static final int RUBBISH_LOG_EXT = 0;
    public static final int RUBBISH_BAK_EXT = 1;
    public static final int TMP_FILE_PREFIX = 2;
    public static final int TMP_FILE_EXT = 3;
    public static final int APK_FILE_EXT = 4;
    public static final int APK_CATCHE1_EXT = 5;
    public static final int APK_CATCHE2_EXT = 6;
    public static final int FILE_LARGE_EXT = 7;
    public static final int FILE_LARGE_AUDIO_EXT = 8;
    public static final int FILE_LARGE_VIDEO_EXT = 9;

    private static final int UPDATE_SIZE = 10;
    private static final int DONE_CLEAN = 11;
    private static final int RESTART_SCAN = 12;

    private static final String SDCARD_PREFIX = "sdcard";

    private ListView mStorageListView;
    private Context mContext;
    private FileAdapter mStorageAdatper;
    private Button clearBtn;
    private HashMap<Integer, Boolean> mCheckedMap;
    private long mSizeTotal;
    private DataGroup mData = DataGroup.getInstance();
    private ProgressDialog mWorkingProgress;
    private static final int DIALOG_STAND_TIME = 200;
    private TextView totalRubbishSize;
    private TextView scoreLable;
    private PackageManager mPm;
    private StorageManager mStorageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.manage_storage_clear);
        mPm = mContext.getPackageManager();
        mStorageManager = getSystemService(StorageManager.class);
        mStorageManager.registerListener(mStorageListener);
        mStorageListView = (ListView) findViewById(R.id.internal_list);
        totalRubbishSize = (TextView) findViewById(R.id.score_bt);
        scoreLable = (TextView) findViewById(R.id.score_label);
        clearBtn = (Button) findViewById(R.id.clear_bt);
        clearBtn.setOnClickListener(this);
        mStorageAdatper = new FileAdapter(mContext);
        mStorageListView.setAdapter(mStorageAdatper);
        mStorageListView.setOnItemClickListener(this);
        mCheckedMap = new HashMap<Integer, Boolean>();
        mSizeTotal = 0;
        initSizeAndCheckBox();
    }


    public void initSizeAndCheckBox() {
        mCheckedMap.clear();
        for (int i = 0; i < mFileDetailTypes.length; i++) {
            mSizeTotal += mData.getDetailTotalSize(mFileDetailTypes[i]);
            mCheckedMap.put(mFileDetailTypes[i], true);
        }
        if (mSizeTotal <= 0) {
            updateUihandler.sendEmptyMessage(RESTART_SCAN);
        } else {
            updateUihandler.sendEmptyMessage(UPDATE_SIZE);
        }
    }

    private final int mNameIds[] = {R.string.cache_file, R.string.rubbish_file, R.string.invalid_file,
            R.string.temp_file, R.string.large_file};

    private final int mFileTypes[] = {CACHE_ITEM, RUBBISH_ITEM, APK_ITEM, TMP_ITEM, LARGE_FILE_ITEM};

    private final int mFileDetailTypes[] = {RUBBISH_LOG_EXT, RUBBISH_BAK_EXT, TMP_FILE_EXT,
            APK_FILE_EXT, APK_CATCHE1_EXT, APK_CATCHE2_EXT, FILE_LARGE_EXT, FILE_LARGE_AUDIO_EXT, FILE_LARGE_VIDEO_EXT};

    private class FileAdapter extends BaseAdapter {

        class ViewHolder {
            TextView name;
            TextView size;
            ImageView img;
            LinearLayout l;
        }

        private LayoutInflater mInflater;
        private TextView fileTypeName = null;
        private TextView fileSize = null;
        private CheckBox fileChecked = null;
        private View v = null;

        public FileAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
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
                convertView = mInflater.inflate(R.layout.storage_type_item2,
                        null);
                holder.name = (TextView) convertView.findViewById(R.id.name);
                holder.size = (TextView) convertView.findViewById(R.id.size);
                holder.img = (ImageView) convertView.findViewById(R.id.img);
                holder.l = (LinearLayout) convertView.findViewById(R.id.detail_file_list);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            if (mData.isDisplayIcon(mFileTypes[position])) {
                holder.img.setVisibility(View.VISIBLE);
            } else {
                holder.img.setVisibility(View.GONE);
            }
            holder.l.removeAllViews();
            HashMap<Integer, ArrayList<FileDetailModel>> detailMap = mData.getDetailAssortmentType(mFileTypes[position]);

            for (Iterator<Map.Entry<Integer, ArrayList<FileDetailModel>>> it = detailMap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Integer, ArrayList<FileDetailModel>> entry = it.next();
                int key = entry.getKey();
                switch (key) {
                    case RUBBISH_BAK_EXT:
                        setDetailItemView(key, detailMap.get(key), R.string.file_bak_ext);
                        break;
                    case RUBBISH_LOG_EXT:
                        setDetailItemView(key, detailMap.get(key), R.string.file_log_ext);
                        break;
                    case TMP_FILE_EXT:
                        setDetailItemView(key, detailMap.get(key), R.string.file_tmp_ext);
                        break;
                    case APK_FILE_EXT:
                        setDetailItemView(key, detailMap.get(key), R.string.file_apk_ext);
                        break;
                    case APK_CATCHE1_EXT:
                        setDetailItemView(key, detailMap.get(key), R.string.file_cache1_ext);
                        break;
                    case APK_CATCHE2_EXT:
                        setDetailItemView(key, detailMap.get(key), R.string.file_cache2_ext);
                        break;
                    case FILE_LARGE_EXT:
                        setDetailItemView(key, detailMap.get(key), R.string.file_large_ext);
                        break;
                    case FILE_LARGE_AUDIO_EXT:
                        setDetailItemView(key, detailMap.get(key), R.string.file_large_audio_ext);
                        break;
                    case FILE_LARGE_VIDEO_EXT:
                        setDetailItemView(key, detailMap.get(key), R.string.file_large_video_ext);
                        break;
                }
                if (v != null) {
                    fileChecked = (CheckBox) v.findViewById(R.id.file_clean_isChecked);
                    fileChecked.setChecked(mCheckedMap.get(key));
                    fileChecked.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                            mCheckedMap.put(key, isChecked);
                            if (!isChecked) {
                                mSizeTotal -= mData.getDetailTotalSize(key);
                            } else {
                                mSizeTotal += mData.getDetailTotalSize(key);
                            }
                            if (mSizeTotal <= 0) {
                                updateUihandler.sendEmptyMessage(DONE_CLEAN);
                            } else {
                                updateUihandler.sendEmptyMessage(UPDATE_SIZE);
                            }
                        }
                    });
                    holder.l.addView(v);
                    v = null;
                }
            }
            long sizeCurrentType = mData.getCategorySizeByType(mFileTypes[position]);
            holder.size.setText(String.format(getResources().getString(R.string.file_current_size),
                    Formatter.formatFileSize(StorageClearManagement.this, sizeCurrentType)));
            holder.name.setText(mNameIds[position]);
            return convertView;
        }

        public void setDetailItemView(int key, ArrayList<FileDetailModel> list, int rsid) {
            v = View.inflate(StorageClearManagement.this, R.layout.storage_type_detail_item, null);
            if (list.size() == 0) {
                v.setVisibility(View.GONE);
            }
            fileTypeName = (TextView) v.findViewById(R.id.file_type_drawable_name);
            fileSize = (TextView) v.findViewById(R.id.file_type_size);
            long size = mData.getTotalSizeByType(list);
            fileTypeName.setText(rsid);
            fileSize.setText(String.format(getResources().getString(R.string.file_current_size),
                    Formatter.formatFileSize(StorageClearManagement.this, size)));
        }

        @Override
        public boolean isEnabled(int position) {
            if (fileSize != null) {
                return true;
            }
            return false;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        LinearLayout fileDetail = (LinearLayout) view.findViewById(R.id.detail_file_list);
        ImageView imageView = (ImageView) view.findViewById(R.id.img);
        if (!mData.isDisplayIcon(mFileTypes[position])) {
            return;
        }
        if (fileDetail.getVisibility() == View.GONE) {
            fileDetail.setVisibility(View.VISIBLE);
            imageView.setImageResource(R.drawable.list_up);
        } else if (fileDetail.getVisibility() == View.VISIBLE) {
            fileDetail.setVisibility(View.GONE);
            imageView.setImageResource(R.drawable.list_down);
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

    class FileDeleteTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            onStart();
        }

        private void onStart() {
            if (((Activity) mContext).isResumed()
                    && mWorkingProgress != null && mWorkingProgress.isShowing()) {
                mWorkingProgress.dismiss();
            }
            mWorkingProgress = new ProgressDialog(mContext);
            mWorkingProgress.setCancelable(false);
            mWorkingProgress.setTitle(R.string.clean_storage_menu);
            mWorkingProgress.setMessage(mContext.getResources().getString(
                    R.string.cleaning_wait));
            mWorkingProgress.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            Set<Integer> s = mCheckedMap.keySet();
            // SPRD: bug 761826 ConcurrentModificationException leading to crash
            try {
              for (int fType : s) {
                if (mCheckedMap.get(fType)) {
                    switch (fType) {
                        case RUBBISH_BAK_EXT:
                            for (FileDetailModel f : mData.mRubbish_bak_ext) {
                                deleteFiles(f);
                            }
                            mData.mRubbish_bak_ext.clear();
                            break;
                        case RUBBISH_LOG_EXT:
                            for (FileDetailModel f : mData.mRubbish_log_ext) {
                                deleteFiles(f);
                            }
                            mData.mRubbish_log_ext.clear();
                            break;
                        case TMP_FILE_EXT:
                            for (FileDetailModel f : mData.mRubbish_tmp_ext) {
                                deleteFiles(f);
                            }
                            mData.mRubbish_tmp_ext.clear();
                            break;
                        case APK_FILE_EXT:
                            for (FileDetailModel f : mData.mRubbish_apk_ext) {
                                deleteFiles(f);
                            }
                            mData.mRubbish_apk_ext.clear();
                            break;
                        case APK_CATCHE1_EXT:
                            // clean innercache
                            for (FileDetailModel f : mData.mRubbish_cach1_ext) {
                                File file = new File(f.getFilePath());
                                String packageName = file.getName();
                                mPm.deleteApplicationCacheFiles(packageName, null);
                                mSizeTotal -= f.getFileSize();
                            }
                            mData.mRubbish_cach1_ext.clear();
                            break;
                        case APK_CATCHE2_EXT:
                            for (FileDetailModel f : mData.mRubbish_cach2_ext) {
                                deleteFiles(f);
                            }
                            mData.mRubbish_cach2_ext.clear();
                            break;
                        case FILE_LARGE_EXT:
                            for (FileDetailModel f : mData.mRubbish_large_ext) {
                                deleteFiles(f);
                            }
                            mData.mRubbish_large_ext.clear();
                            break;
                        case FILE_LARGE_AUDIO_EXT:
                            for (FileDetailModel f : mData.mRubbish_large_audio_ext) {
                                deleteFiles(f);
                            }
                            mData.mRubbish_large_audio_ext.clear();
                            break;
                        case FILE_LARGE_VIDEO_EXT:
                            for (FileDetailModel f : mData.mRubbish_large_video_ext) {
                                deleteFiles(f);
                            }
                            mData.mRubbish_large_video_ext.clear();
                            break;
                    }
                }
              }
            } catch (ConcurrentModificationException e) {
              Log.i(TAG, "ConcurrentModificationException");
            }
            try {
                Thread.sleep(DIALOG_STAND_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (((Activity) mContext).isResumed() && mWorkingProgress != null
                    && mWorkingProgress.isShowing()) {
                mWorkingProgress.dismiss();
            }
            mWorkingProgress = null;
            if (mSizeTotal <= 0) {
                updateUihandler.sendEmptyMessage(DONE_CLEAN);
            }
            mStorageAdatper.notifyDataSetChanged();
            /**
             * SPRD: Bug705779 the rubbish is cleared but still show in pc
             * @{
             */
            notifyMediaScanDir(mContext, EnvironmentEx.getInternalStoragePath());
            if (hasSDcard()) {
                notifyMediaScanDir(mContext, EnvironmentEx.getExternalStoragePath());
            }
            /**
             * @}
             */
        }
    }

    Handler updateUihandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                default:
                case UPDATE_SIZE:
                    String size_lable = Formatter.formatFileSize(mContext, mSizeTotal);
                    String[] strSize_lable = size_lable.split(" ");
                    if (strSize_lable != null && strSize_lable.length > 1) {
                        totalRubbishSize.setText(strSize_lable[0]);
                        scoreLable.setText(strSize_lable[1]);
                    }
                    clearBtn.setText(mContext.getText(R.string.clean_memory_button) + " (" +
                            Formatter.formatFileSize(StorageClearManagement.this, mSizeTotal) + ")");
                    break;
                case DONE_CLEAN:
                    /**
                     * SPRD: Bug705036 after clean rubbish done, the size display error
                     * @{
                     */
                    if (mSizeTotal < 0) {
                        mSizeTotal = 0;
                    }
                    /**
                     * @}
                     */
                    String size_lable_done = Formatter.formatFileSize(mContext, mSizeTotal);
                    String[] strSize_lable_done = size_lable_done.split(" ");
                    if (strSize_lable_done != null && strSize_lable_done.length > 1) {
                        totalRubbishSize.setText(strSize_lable_done[0]);
                        scoreLable.setText(strSize_lable_done[1]);
                    }
                    clearBtn.setText(mContext.getText(R.string.end_clean_memory_button));
                    break;
                case RESTART_SCAN:
                    startUpdateActivity();
                    break;
            }
        }
    };

    private void startUpdateActivity() {
        Intent intent = new Intent(StorageClearManagement.this, StorageManagement.class);
        startActivity(intent);
        finish();
    }

    public void deleteFiles(FileDetailModel f) {
        File file = new File(f.getFilePath());
        // SPRD: Bug759869 files still show after sd card is uninstalled
        if (dirDelete(file) || !file.exists()) {
            mSizeTotal -= f.getFileSize();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /**
         * SPRD: Bug705779 the rubbish is cleared but still show in pc
         * @{
         */
        if (mStorageManager != null && mStorageListener != null) {
            try {
                mStorageManager.unregisterListener(mStorageListener);
            } catch (Exception e) {
                Log.i(TAG, "unregisterListener... exception");
            }
        }
        /**
         * @}
         */
    }

    @Override
    public void onClick(View view) {
        final String text = ((Button) view).getText().toString().trim();
        if (view.getId() == R.id.clear_bt) {
            if (text.contains(mContext.getText(R.string.clean_memory_button))) {
                FileDeleteTask deleteTask = new FileDeleteTask();
                deleteTask.execute();
            } else if (text.contains(mContext.getText(R.string.end_clean_memory_button))
                    || text.contains(mContext.getText(R.string.clear_memory))) {
                finish();
            }
        }
    }

    /**
     * SPRD: Bug705779 the rubbish is cleared but still show in pc
     * @{
     */
    private void notifyMediaScanDir(Context context, File dir) {
        Log.i(TAG, "send broadcast to scan dir = " + dir);
        String path = dir.getPath();
        Intent intent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_DIR");
        Bundle bundle = new Bundle();
        bundle.putString("scan_dir_path", path);
        intent.putExtras(bundle);
        context.sendBroadcast(intent);
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
    /**
     * @}
     */
}
