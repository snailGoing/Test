package com.sprd.generalsecurity.storage;

import android.util.ArrayMap;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DataGroup {
    private final static String TAG = "DataGroup";
    public ArrayMap<String, Long> mInCacheMap;
    public ArrayMap<String, Long> mExCacheMap;
    public ArrayMap<String, Long> mInRubbishMap;
    public ArrayMap<String, Long> mExRubbishMap;
    public ArrayMap<String, Long> mInApkMap;
    public ArrayMap<String, Long> mExApkMap;
    public ArrayMap<String, Long> mInTmpMap;
    public ArrayMap<String, Long> mExTmpMap;
    public ArrayMap<String, Long> mLargeMap;
    public ArrayMap<String, Long> mExLargeMap;

    public ArrayList<FileDetailModel> mRubbish_log_ext;
    public ArrayList<FileDetailModel> mRubbish_bak_ext;
    public ArrayList<FileDetailModel> mRubbish_tmp_prefix;
    public ArrayList<FileDetailModel> mRubbish_tmp_ext;
    public ArrayList<FileDetailModel> mRubbish_apk_ext;
    public ArrayList<FileDetailModel> mRubbish_large_ext;
    public ArrayList<FileDetailModel> mRubbish_large_audio_ext;
    public ArrayList<FileDetailModel> mRubbish_large_video_ext;

    public ArrayList<FileDetailModel> mRubbish_cach1_ext;
    public ArrayList<FileDetailModel> mRubbish_cach2_ext;

    public ArrayList<String> mTempKey = new ArrayList<String>();
    public ArrayList<String> mTempValues = new ArrayList<String>();
    private static DataGroup instance;

    public static final int RUBBISH_FILE_BIT = 1;
    public static final int TMP_FILE_BIT = 2;
    public static final int APK_FILE_BIT = 4;
    public static final int LARGE_FILE_BIT = 8;

    public int mFileUpdateBits;

    public static DataGroup getInstance() {
        if (instance == null) {
            instance = new DataGroup();
        }
        return instance;
    }

    private DataGroup() {
        mInCacheMap = new ArrayMap<String, Long>();
        mExCacheMap = new ArrayMap<String, Long>();
        mInRubbishMap = new ArrayMap<String, Long>();
        mExRubbishMap = new ArrayMap<String, Long>();
        mInApkMap = new ArrayMap<String, Long>();
        mExApkMap = new ArrayMap<String, Long>();
        mInTmpMap = new ArrayMap<String, Long>();
        mExTmpMap = new ArrayMap<String, Long>();
        mLargeMap = new ArrayMap<String, Long>();
        mExLargeMap = new ArrayMap<String, Long>();

        mRubbish_log_ext = new ArrayList<FileDetailModel>();
        mRubbish_bak_ext = new ArrayList<FileDetailModel>();
        mRubbish_tmp_prefix = new ArrayList<FileDetailModel>();
        mRubbish_tmp_ext = new ArrayList<FileDetailModel>();
        mRubbish_apk_ext = new ArrayList<FileDetailModel>();
        mRubbish_large_ext = new ArrayList<FileDetailModel>();
        mRubbish_large_audio_ext = new ArrayList<FileDetailModel>();
        mRubbish_large_video_ext = new ArrayList<FileDetailModel>();
        mRubbish_cach1_ext = new ArrayList<FileDetailModel>();
        mRubbish_cach2_ext = new ArrayList<FileDetailModel>();
    }


    public ArrayMap<String, Long> getNeedMap(boolean isExternal, int type) {
        if (!isExternal) {
            if (type == StorageManagement.CACHE_ITEM) {
                return mInCacheMap;
            } else if (type == StorageManagement.RUBBISH_ITEM) {
                return mInRubbishMap;
            } else if (type == StorageManagement.APK_ITEM) {
                return mInApkMap;
            } else if (type == StorageManagement.TMP_ITEM) {
                return  mInTmpMap;
            } else if (type == StorageManagement.LARGE_FILE_ITEM) {
                return mLargeMap;
            }
        } else {
            if (type == StorageManagement.CACHE_ITEM) {
                return mExCacheMap;
            } else if (type == StorageManagement.RUBBISH_ITEM) {
                return mExRubbishMap;
            } else if (type == StorageManagement.APK_ITEM) {
                return mExApkMap;
            } else if (type == StorageManagement.TMP_ITEM) {
                return mExTmpMap;
            } else if (type == StorageManagement.LARGE_FILE_ITEM) {
                return mExLargeMap;
            }
        }

        return null;
    }

    public void destroy() {
        mInCacheMap.clear();
        mExCacheMap.clear();
        mInRubbishMap.clear();
        mExRubbishMap.clear();
        mInApkMap.clear();
        mExApkMap.clear();
        mInTmpMap.clear();
        mExTmpMap.clear();
        mLargeMap.clear();
        mExLargeMap.clear();

        mRubbish_log_ext.clear();
        mRubbish_bak_ext.clear();
        mRubbish_tmp_prefix.clear();
        mRubbish_tmp_ext.clear();
        mRubbish_apk_ext.clear();
        mRubbish_large_ext.clear();
        mRubbish_large_audio_ext.clear();
        mRubbish_large_video_ext.clear();
        mRubbish_cach1_ext.clear();
        mRubbish_cach2_ext.clear();
        instance = null;
    }

    public long getTotalSize(boolean isExternal, int type) {
        if (!isExternal) {
            if (type == StorageManagement.CACHE_ITEM) {
                return getCategorySize(mInCacheMap);
            } else if (type == StorageManagement.RUBBISH_ITEM) {
                return getCategorySize(mInRubbishMap);
            } else if (type == StorageManagement.APK_ITEM) {
                return getCategorySize(mInApkMap);
            } else if (type == StorageManagement.TMP_ITEM) {
                return  getCategorySize(mInTmpMap);
            } else if (type == StorageManagement.LARGE_FILE_ITEM) {
                return  getCategorySize(mLargeMap);
            }
        } else {
            if (type == StorageManagement.CACHE_ITEM) {
                return getCategorySize(mExCacheMap);
            } else if (type == StorageManagement.RUBBISH_ITEM) {
                return getCategorySize(mExRubbishMap);
            } else if (type == StorageManagement.APK_ITEM) {
                return getCategorySize(mExApkMap);
            } else if (type == StorageManagement.TMP_ITEM) {
                return getCategorySize(mExTmpMap);
            } else if (type == StorageManagement.LARGE_FILE_ITEM) {
                return  getCategorySize(mExLargeMap);
            }
        }
        return 0;
    }


    public long mAPKCategorySize;
    public long mExAPKCategorySize;
    public long mTempCategorySize;
    public long mExTempCategorySize;
    public long mRubbishCategorySize;
    public long mExRubbishCategorySize;
    public long mLargeFileCategorySize;
    public long mExLargeFileCategorySize;
    public long mUniqueLargeFileSize;
    public long mExUniqueLargeFileSize; //large file size that not contained in APK, tmp category.
    public long mExCacheFileSize;

    public long getDetailTotalSize(int type) {
        long size = 0;
        switch (type) {
            case RUBBISH_BAK_EXT:
                size = getTotalSizeByType(mRubbish_bak_ext);
                break;
            case RUBBISH_LOG_EXT:
                size = getTotalSizeByType(mRubbish_log_ext);
                break;
            case TMP_FILE_PREFIX:
                size = getTotalSizeByType(mRubbish_tmp_prefix);
                break;
            case TMP_FILE_EXT:
                size = getTotalSizeByType(mRubbish_tmp_ext);
                break;
            case APK_FILE_EXT:
                size = getTotalSizeByType(mRubbish_apk_ext);
                break;
            case APK_CATCHE1_EXT:
                size = getTotalSizeByType(mRubbish_cach1_ext);
                break;
            case APK_CATCHE2_EXT:
                size = getTotalSizeByType(mRubbish_cach2_ext);
                break;
            case FILE_LARGE_EXT:
                size = getTotalSizeByType(mRubbish_large_ext);
                break;
            case FILE_LARGE_AUDIO_EXT:
                size = getTotalSizeByType(mRubbish_large_audio_ext);
                break;
            case FILE_LARGE_VIDEO_EXT:
                size = getTotalSizeByType(mRubbish_large_video_ext);
                break;
        }
        return size;
    }
    public long getTotalSizeByType(ArrayList<FileDetailModel> list){
        long size = 0;
        for(FileDetailModel m : list){
            size += m.getFileSize();
        }
        return size;
    }

    public long getCategorySizeByType(int type) {
        long size = 0;
        if (type == StorageManagement.CACHE_ITEM) {
            size = getTotalSizeByType(mRubbish_cach1_ext)+getTotalSizeByType(mRubbish_cach2_ext);
        } else if (type == StorageManagement.RUBBISH_ITEM) {
            size = getTotalSizeByType(mRubbish_bak_ext)+getTotalSizeByType(mRubbish_log_ext);
        } else if (type == StorageManagement.APK_ITEM) {
            size = getTotalSizeByType(mRubbish_apk_ext);
        } else if (type == StorageManagement.TMP_ITEM) {
            size = getTotalSizeByType(mRubbish_tmp_ext)+getTotalSizeByType(mRubbish_tmp_prefix);
        } else if (type == StorageManagement.LARGE_FILE_ITEM) {
            size =  getTotalSizeByType(mRubbish_large_ext) + getTotalSizeByType(mRubbish_large_audio_ext)
            + getTotalSizeByType(mRubbish_large_video_ext);
        }
        return size;
    }

    public void updateSize(int updateType, boolean isExternal) {
        if (!isExternal) {
            if ((updateType & RUBBISH_FILE_BIT) > 0) {
                mRubbishCategorySize = getTotalSize(isExternal, StorageManagement.RUBBISH_ITEM);
            }
            if ((updateType & TMP_FILE_BIT) > 0) {
                mTempCategorySize = getTotalSize(isExternal, StorageManagement.TMP_ITEM);
            }
            if ((updateType & APK_FILE_BIT) > 0) {
                mAPKCategorySize = getTotalSize(isExternal, StorageManagement.APK_ITEM);
            }
            if ((updateType & LARGE_FILE_BIT) > 0) {
                mLargeFileCategorySize = getTotalSize(isExternal, StorageManagement.LARGE_FILE_ITEM);
            }
        } else {
            if ((updateType & RUBBISH_FILE_BIT) > 0) {
                mExRubbishCategorySize = getTotalSize(isExternal, StorageManagement.RUBBISH_ITEM);
            }
            if ((updateType & TMP_FILE_BIT) > 0) {
                mExTempCategorySize = getTotalSize(isExternal, StorageManagement.TMP_ITEM);
            }
            if ((updateType & APK_FILE_BIT) > 0) {
                mExAPKCategorySize = getTotalSize(isExternal, StorageManagement.APK_ITEM);
            }
            if ((updateType & LARGE_FILE_BIT) > 0) {
                mExLargeFileCategorySize = getTotalSize(isExternal, StorageManagement.LARGE_FILE_ITEM);
            }
        }
    }

    private long getCategorySize(ArrayMap<String,Long> map) {
        long size = 0;

        if (map == mExLargeMap) {
            mExUniqueLargeFileSize = 0;
        } else if (map == mLargeMap) {
            mUniqueLargeFileSize = 0;
        }

        for (Iterator<Map.Entry<String, Long>> it =map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Long> entry = it.next();
            String key = entry.getKey();

            File f = new File(key);
            if (f.exists()) {
                size += f.length();
                if (map == mExLargeMap) {
                    if (StorageManagement.isLargeFileUnique(f)) {
                        mExUniqueLargeFileSize += f.length();
                    }
                } else if (map == mLargeMap) {
                    if (StorageManagement.isLargeFileUnique(f)) {
                        mUniqueLargeFileSize += f.length();
                    }
                }
            } else {
                it.remove();
            }
        }

        return size;
    }
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

    public HashMap<Integer, ArrayList<FileDetailModel>> getDetailAssortmentType(int type) {

        HashMap<Integer, ArrayList<FileDetailModel>> typeList = new HashMap<Integer, ArrayList<FileDetailModel>>();
        switch (type) {
            case CACHE_ITEM:
                typeList.clear();
                typeList.put(APK_CATCHE1_EXT, mRubbish_cach1_ext);
                typeList.put(APK_CATCHE2_EXT, mRubbish_cach2_ext);
                break;
            case RUBBISH_ITEM:
                typeList.clear();
                typeList.put(RUBBISH_LOG_EXT, mRubbish_log_ext);
                typeList.put(RUBBISH_BAK_EXT, mRubbish_bak_ext);
                break;
            case APK_ITEM:
                typeList.clear();
                typeList.put(APK_FILE_EXT, mRubbish_apk_ext);
                break;
            case TMP_ITEM:
                typeList.clear();
                typeList.put(TMP_FILE_EXT, mRubbish_tmp_ext);
                typeList.put(TMP_FILE_PREFIX, mRubbish_tmp_prefix);
                break;
            case LARGE_FILE_ITEM:
                typeList.clear();
                typeList.put(FILE_LARGE_AUDIO_EXT, mRubbish_large_audio_ext);
                typeList.put(FILE_LARGE_VIDEO_EXT, mRubbish_large_video_ext);
                typeList.put(FILE_LARGE_EXT, mRubbish_large_ext);
                break;
        }

        return typeList;
    }

    public Long getFileListTotalSize(ArrayList<FileDetailModel> fileList){
        long size = 0;
        for(FileDetailModel f:fileList){
            size += f.getFileSize();
        }
        return size;
    }

    public boolean isDisplayIcon(int type) {
        boolean flag = false;
        switch (type) {
            case StorageManagement.CACHE_ITEM:
                flag = this.mRubbish_cach1_ext.size() > 0 || this.mRubbish_cach2_ext.size() > 0;
                break;
            case StorageManagement.RUBBISH_ITEM:
                flag = this.mRubbish_log_ext.size() > 0 || this.mRubbish_bak_ext.size() > 0;
                break;
            case StorageManagement.APK_ITEM:
                flag = this.mRubbish_apk_ext.size() > 0;
                break;
            case StorageManagement.TMP_ITEM:
                /** SPRD: Bug 705066 there is no expand icon when has temp file */
                flag = this.mRubbish_tmp_prefix.size() > 0 || this.mRubbish_tmp_ext.size() > 0;
                break;
            case StorageManagement.LARGE_FILE_ITEM:
                flag = this.mRubbish_large_ext.size() > 0 || this.mRubbish_large_audio_ext.size() > 0
                        || this.mRubbish_large_video_ext.size() > 0;
                break;
        }
        return flag;
    }
}
