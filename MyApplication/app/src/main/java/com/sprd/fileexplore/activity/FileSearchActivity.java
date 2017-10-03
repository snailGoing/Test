
package com.sprd.fileexplore.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.sprd.fileexplore.R;
import com.sprd.fileexplore.file.FileInfo;
import com.sprd.fileexplore.service.MountRootManagerRf;
import com.sprd.fileexplore.util.FileUtil;
import com.sprd.fileexplore.util.FileSearch;
import com.sprd.fileexplore.file.FileType;

public class FileSearchActivity extends Activity implements
        View.OnClickListener {

    private static final String TAG = "FileSearchActivity";
    private String mSearchKey;
    private CheckBox mImageType;
    private CheckBox mVideoType;
    private CheckBox mAudioType;
    private CheckBox mApkType;
    private CheckBox mOthertype;
    private CheckBox mDocType;

    public Set<Integer> mSearchType = new HashSet<Integer>();
    public static final String KEY_NO_PATH ="key_no_path";
    public static final String KEY_ALL_PATH ="key_all_path";

    private RelativeLayout mSearchViewContainer;
    private EditText mSearchView;
    private ImageView mClearAll;

    private View mSearchLoactionLayout;
    private TextView mSearchLocationTextView;

    private List<String> paths = new ArrayList<>();
    private List<String> descs = new ArrayList<>();
    private String destPath="";

    private static final String IMAGE_KEY = "image_type";
    private static final String VIDEO_KEY = "video_type";
    private static final String AUDIO_KEY = "audio_type";
    private static final String APK_KEY = "apk_type";
    private static final String OTHER_KEY = "other_type";
    private static final String DOC_KEY = "doc_type";
    private SharedPreferences prfs;
    private MountRootManagerRf mountRootManagerRf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("FileSearchActivity", "onCreate");
        setContentView(R.layout.fragment_search_view);
        mountRootManagerRf = MountRootManagerRf.getInstance();

        updateSearchLocations();
        mSearchViewContainer = (RelativeLayout) findViewById(R.id.search_view_container);
        if (mSearchViewContainer != null) {
            mSearchView = (EditText) findViewById(R.id.search_view);
            mSearchView.setOnEditorActionListener(new OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId,
                        KeyEvent event) {
                    mSearchKey = v.getText().toString();
                    if (mSearchKey != null && !mSearchKey.isEmpty()) {
                        if (mSearchKey.contains("*") || mSearchKey.contains("\"")
                                || mSearchKey.contains("\\") ||
                                mSearchKey.contains("/") || mSearchKey.contains("?")
                                || mSearchKey.contains("|")
                                || mSearchKey.contains("|") || mSearchKey.contains("<")
                                || mSearchKey.contains(">") || mSearchKey.contains("\'")) {
                            String invalidchar = "*\":/?|<>";
                            Toast.makeText(FileSearchActivity.this, getResources().getString(
                                    R.string.invalid_char, invalidchar), Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        if (mSearchType.size() == 0) {
                            Toast.makeText(FileSearchActivity.this, R.string.search_type_empty,
                                    Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        Bundle bun = new Bundle();
                        Log.d(TAG,"  mSearchKey="+mSearchKey+"  destPath="+destPath);
                        bun.putString(FileSearch.SEARCH_KEY, mSearchKey);
                        // location path:
                        bun.putString(FileSearch.SEARCH_LOCATION,  destPath);
                        bun.putIntegerArrayList(FileSearch.SEARCH_TYPE, new ArrayList<Integer>(mSearchType));
                       // FileUtil.startSearchMode(bun, FileSearchActivity.this);
                        startActivity(new Intent().setClass(getApplicationContext(),FileSearchResultActivity.class)
                                    .putExtra(FileSearch.SEARCH_ATTACH,bun));
                       // finish();
                    } else {
                        Toast.makeText(FileSearchActivity.this, R.string.search_empty,
                                Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            });
            mSearchView.addTextChangedListener(mTextWatcher);
            mSearchView.clearFocus();
            mClearAll = (ImageView) findViewById(R.id.clear_all_img);
            mClearAll.setOnClickListener(this);
            ActionBar actionbar = getActionBar();
            actionbar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);

            this.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        mImageType = (CheckBox) findViewById(R.id.fragment_search_type_image);
        mVideoType = (CheckBox) findViewById(R.id.fragment_search_type_vedio);
        mAudioType = (CheckBox) findViewById(R.id.fragment_search_type_audio);
        mApkType = (CheckBox) findViewById(R.id.fragment_search_type_apks);
        mDocType = (CheckBox) findViewById(R.id.fragment_search_type_document);
        mOthertype = (CheckBox) findViewById(R.id.fragment_search_type_other);
        mSearchLoactionLayout = findViewById(R.id.fragment_search_location_parent);
        mSearchLocationTextView = (TextView) findViewById(R.id.fragment_search_location_type);
        mImageType.setOnClickListener(this);
        mVideoType.setOnClickListener(this);
        mAudioType.setOnClickListener(this);
        mApkType.setOnClickListener(this);
        mDocType.setOnClickListener(this);
        mOthertype.setOnClickListener(this);
        prfs = PreferenceManager.getDefaultSharedPreferences(FileSearchActivity.this);

        MountRootManagerRf mf = MountRootManagerRf.getInstance();
        // if storage card counts is more than one, so need show dialog to user to choose search location
        if( mf.getMountCount() > 1 ){
            mSearchLoactionLayout.setOnClickListener(this);
        }
        initCheckBoxStatus();
        int size = paths.size();
        if(size > 0){
            destPath = paths.get(0); // init default path
            mSearchLocationTextView.setText(descs.get(0));
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d("FileSearchActivity", "onDestroy");

        storeSearchTypeToPreference();
    };

    TextWatcher mTextWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable s) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
        }

        @Override
        public void onTextChanged(CharSequence queryString, int start,
                int before, int count) {
            if (queryString.length() >= 84) {
                Toast.makeText(FileSearchActivity.this, R.string.length_limited, Toast.LENGTH_SHORT)
                        .show();
            }
            if (!TextUtils.isEmpty(queryString.toString())) {
                if (mClearAll != null) {
                    mClearAll.setVisibility(View.VISIBLE);
                }
            } else {
                if (mClearAll != null) {
                    mClearAll.setVisibility(View.GONE);
                }
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fragment_search_type_image:
            case R.id.fragment_search_type_vedio:
            case R.id.fragment_search_type_audio:
            case R.id.fragment_search_type_document:
            case R.id.fragment_search_type_apks:
            case R.id.fragment_search_type_other:
                updateSearchType(v);
                break;
            case R.id.clear_all_img:
                if (mSearchView != null && mClearAll != null) {
                    mSearchView.setText("");
                    mClearAll.setVisibility(View.GONE);
                }
                break;
            case R.id.fragment_search_location_parent:
                showSearchLocationsDialog();
                break;
        }
    }

    private void updateSearchType(View v) {
        boolean isChecked = false;
        if (v instanceof CheckBox) {
            isChecked = ((CheckBox) v).isChecked();
        }
        switch (v.getId()) {
            case R.id.fragment_search_type_image:
                if (!isChecked) {
                    mSearchType.remove(FileType.FILE_TYPE_IMAGE);
                } else {
                    mSearchType.add(FileType.FILE_TYPE_IMAGE);
                }
                break;
            case R.id.fragment_search_type_vedio:
                if (!isChecked) {
                    mSearchType.remove(FileType.FILE_TYPE_VIDEO_DEFAULT);
                } else {
                    mSearchType.add(FileType.FILE_TYPE_VIDEO_DEFAULT);
                }
                break;
            case R.id.fragment_search_type_audio:
                if (!isChecked) {
                    mSearchType.remove(FileType.FILE_TYPE_AUDIO_DEFAULT);
                } else {
                    mSearchType.add(FileType.FILE_TYPE_AUDIO_DEFAULT);
                }
                break;
            case R.id.fragment_search_type_document:
                if (!isChecked) {
                    mSearchType.remove(FileType.FILE_TYPE_DOC);
                } else {
                    mSearchType.add(FileType.FILE_TYPE_DOC);
                }
                break;
            case R.id.fragment_search_type_apks:
                if (!isChecked) {
                    mSearchType.remove(FileType.FILE_TYPE_APK);
                } else {
                    mSearchType.add(FileType.FILE_TYPE_APK);
                }
                break;
            case R.id.fragment_search_type_other:
                if (!isChecked) {
                    mSearchType.remove(FileType.FILE_TYPE_UNKNOWN);
                } else {
                    mSearchType.add(FileType.FILE_TYPE_UNKNOWN);
                }
                break;
        }
    }

    private void showSearchLocationsDialog() {

        new AlertDialog.Builder(this)
                .setTitle(R.string.fragment_search_location)
                .setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
                        descs), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setSearchLoactions(which);
                    }
                }).show();
    }
    private void updateSearchLocations(){
        List<FileInfo> lists = mountRootManagerRf.getMountPointFileInfo();
        int size = lists.size();
        paths.clear();;
        descs.clear();

        if(size==0){
            paths.add("key_no_path");
            descs.add("No storage");
        }else if(size>1){
            paths.add("key_all_path");
            descs.add("All storage");
            for(int i=0;i<size;i++){
                FileInfo fi = lists.get(i);
                paths.add( fi.getPath());
                descs.add(fi.getName());
            }
        }else{
            FileInfo fi = lists.get(0);
            paths.add( fi.getPath());
            descs.add(fi.getName());
        }
        Log.d(TAG," size= "+ paths.size());
        for(String str: paths){
            Log.d(TAG," str= "+str);
        }
    }

    public void setSearchLoactions(int position) {
        if(  paths.size()> position){
            destPath = paths.get(position);
            mSearchLocationTextView.setText(descs.get(position));
        }
    }
    private void initCheckBoxStatus() {
        mImageType.setChecked(prfs.getBoolean(IMAGE_KEY, false));
        mVideoType.setChecked(prfs.getBoolean(VIDEO_KEY, false));
        mAudioType.setChecked(prfs.getBoolean(AUDIO_KEY, false));
        mApkType.setChecked(prfs.getBoolean(APK_KEY, false));
        mDocType.setChecked(prfs.getBoolean(DOC_KEY, false));
        mOthertype.setChecked(prfs.getBoolean(OTHER_KEY, false));
        updateSearchType(mImageType);
        updateSearchType(mVideoType);
        updateSearchType(mAudioType);
        updateSearchType(mApkType);
        updateSearchType(mDocType);
        updateSearchType(mOthertype);
     }

    private void storeSearchTypeToPreference() {
         SharedPreferences.Editor editor = prfs.edit();
         editor.putBoolean(IMAGE_KEY, mImageType.isChecked());
         editor.putBoolean(VIDEO_KEY, mVideoType.isChecked());
         editor.putBoolean(AUDIO_KEY, mAudioType.isChecked());
         editor.putBoolean(APK_KEY, mApkType.isChecked());
         editor.putBoolean(DOC_KEY, mDocType.isChecked());
         editor.putBoolean(OTHER_KEY, mOthertype.isChecked());
         editor.commit();
     }


}
