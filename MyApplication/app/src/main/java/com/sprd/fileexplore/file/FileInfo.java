package com.sprd.fileexplore.file;

import android.net.Uri;
import android.util.Log;

import com.sprd.fileexplore.service.MyBinder;
import com.sprd.fileexplore.util.FileUtil;

import java.io.File;
import java.lang.reflect.InvocationTargetException;


/**
 * Created by Xuehao.Jiang on 2017/4/6.
 */

public class FileInfo {

    private static final String TAG = FileInfo.class.getSimpleName();
    public static final String MIMETYPE_EXTENSION_NULL = "unknown_ext_null_mimeType";
    public static final String MIMETYPE_EXTENSION_UNKONW = "unknown_ext_mimeType";
    public static final String MIMETYPE_UNRECOGNIZED = "application/zip";
    public static final String TXT_MIME_TYPE = "text/plain";

    /** File name's max length */
    public static final int FILENAME_MAX_LENGTH = 255;

    public static  final int COMMON_FILE_FOLDER= -1;
    public static  final int ROOT_INTERNAL= 0;
    public static  final int ROOT_EXTERNAL= 1;
    public static  final int ROOT_USB= 2;

    protected String mName;
    protected String mPath;

    protected boolean isFolder;
    protected boolean isHidden;
    protected boolean isExist;
    protected String mMimeTpye = null;
    protected int mFileIcon =  FileType.FILE_TYPE_UNKNOE;
    protected long mLastModifiedTime = -1;
    protected long mFileSize = -1;
    protected File mFile;
    protected boolean isChecked;
    protected int mFileRootCategory = COMMON_FILE_FOLDER;
    private String mParentPath = null;

    // those params means storage root, example : internal storage, external storage
    protected boolean isStorageRoot;
    protected String avaliableSize;
    protected String totalSize;
    public int getFileRootCategory() {
        return mFileRootCategory;
    }

    public void setFileRootCategory(int mFileRootCategory) {
        this.mFileRootCategory = mFileRootCategory;
    }

    public FileInfo(){}
    public FileInfo(String mPath) {
        this.mPath = mPath;
        File file =new File(mPath);
        init(file);
    }

    public FileInfo(File mFile) {
        init(mFile);
    }

    public FileInfo(FileInfo mFileInfo){
        this.mName = mFileInfo.getName();
        this.mPath = mFileInfo.getPath();
        this.isFolder = mFileInfo.isFolder();
        this.isHidden = mFileInfo.isHidden();
        this.isExist= mFileInfo.isExist();
        this.isChecked= mFileInfo.isChecked();
        this.mFile = mFileInfo.getFile();
        this.mFileRootCategory = mFileInfo.getFileRootCategory();
        this.mFileIcon = mFileInfo.getFileIcon();
        this.mFileSize = mFileInfo.getFileSize();
        this.mLastModifiedTime = mFileInfo.getLastModifiedTime();
        this.mMimeTpye = mFileInfo.getMimeTpye();
        this.avaliableSize= mFileInfo.getAvaliableSize();
        this.totalSize = mFileInfo.getTotalSize();
        this.isStorageRoot = mFileInfo.isStorageRoot();

    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String mPath) {
        this.mPath = mPath;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public File getFile() {
        return mFile;
    }

    public void setFile(File mFile) {
        this.mFile = mFile;
    }

    public String getMimeTpye() {
        return mMimeTpye;
    }

    public void setMimeTpye(String mMimeTpye) {
        this.mMimeTpye = mMimeTpye;
    }

    public int getFileIcon() {
        return mFileIcon;
    }

    public void setFileIcon(int mFileIcon) {
        Log.d(TAG," setFileIcon:  mFileIcon= "+mFileIcon);
        this.mFileIcon = mFileIcon;
    }

    public long getLastModifiedTime() {
        return mLastModifiedTime;
    }

    public void setLastModifiedTime(long mLastModifiedTime) {
        this.mLastModifiedTime = mLastModifiedTime;
    }

    public long getFileSize() {
        return mFileSize;
    }

    public void setFileSize(long mFileSize) {
        this.mFileSize = mFileSize;
    }

    private void init(File file) {
        if(file!=null){
            mName = file.getName();
            isFolder = file.isDirectory();
            isHidden = file.isHidden();
            isExist= file.exists();
            mPath = file.getAbsolutePath();
            mLastModifiedTime = file.lastModified();
            mFileSize = file.length();
            mFile = file;
        }
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public boolean isExist() {
        return isExist;
    }

    public void setExist(boolean exist) {
        isExist = exist;
    }
    public boolean isStorageRoot() {
        isStorageRoot = (mFileRootCategory==0 || mFileRootCategory==1||mFileRootCategory==2) ? true:false;
        return isStorageRoot;
    }

    public void setStorageRoot(boolean storageRoot) {
        isStorageRoot = storageRoot;
    }
    public String getAvaliableSize() {
        return avaliableSize;
    }

    public void setAvaliableSize(String avaliableSize) {
        this.avaliableSize = avaliableSize;
    }

    public String getTotalSize() {
        return totalSize;
    }
    /**
     * This method gets the file packaged in FileInfo.
     *
     * @return the file packaged in FileInfo.
     */
    public Uri getUri() {
        return Uri.fromFile(mFile);
    }

    /*
    *  You can use service to do some time-consumed work ,to get mimeType from dataBase if possible
    * */
    public String getFileMimeType(MyBinder service) {
        Log.d(TAG, "getFileMimeType,service.");
        String mimeType = null;

        if (!isFolder()) {
            mimeType = getMimeType(mFile);
            Log.d(TAG, "getFileMimeType, mimetype is : " + mimeType);
        }
        return mimeType;
    }
    /**
     * This method gets the MIME type based on the extension of a file
     *
     * @param file the target file
     * @return the MIME type of the file
     */
    private String getMimeType(File file) {
        String fileName = getName();
        String extension = FileUtil.getFileExtension(fileName);
        Log.d(TAG, "getMimeType fileName=" + fileName + ",extension = " + extension);

        if (extension == null) {
            return FileInfo.MIMETYPE_EXTENSION_NULL;
        }
        Class className = null;
        String mimeType = "unknow";
        try {
            /*
            *  this obtain mimeType by framework common interface
            *  MediaFile.getMimeTypeForFile(String path)
            * */
            className = Class.forName("android.media.MediaFile");
            mimeType=(String) className.getMethod("getMimeTypeForFile",String.class).invoke(className,fileName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }catch (NoSuchMethodException e) {
            e.printStackTrace();
        }catch (InvocationTargetException e) {
            e.printStackTrace();
        }catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if("apk".equals(extension)){
            mimeType ="application/vnd.android.package-archive";
        }
        Log.d(TAG, "getMimeType mimeType =" + mimeType);
        if (mimeType == null) {
            return FileInfo.MIMETYPE_EXTENSION_UNKONW;
        }
        return mimeType;
    }

    public void setTotalSize(String totalSize) {
        this.totalSize = totalSize;
    }
    /**
     * This method gets a file's parent path
     *
     * @return file's parent path.
     */
    public String getFileParentPath() {
        if (mParentPath == null) {
            mParentPath = FileUtil.getFilePath(mPath);
            // LogUtils.d(TAG, "getFileParentPath = " + mParentPath);
        }
        return mParentPath;
    }
    /**
     * The method check the file is DRM file, or not.
     *
     * @return true for DRM file, false for not DRM file.
     */
    public boolean isDrmFile() {
        if (isFolder()) {
            return false;
        }
        return isDrmFile(mPath);
    }

    /**
     * This static method check a file is DRM file, or not.
     *
     * @param fileName the file which need to be checked.
     * @return true for DRM file, false for not DRM file.
     */
    public static boolean isDrmFile(String fileName) {
            String extension = FileUtil.getFileExtension(fileName);
            if (extension != null && extension.equalsIgnoreCase("dcf")) {
                return true; // all drm files cannot be copied
        }
        return false;
    }
    @Override
    public String toString() {
        return "FileInfo{" +
                "mName='" + mName + '\'' +
                ", mPath='" + mPath + '\'' +
                ", isFolder=" + isFolder +
                ", isHidden=" + isHidden +
                ", isExist=" + isExist +
                ", isStorageRoot=" + isStorageRoot +
                ", avaliableSize=" + avaliableSize +
                ", totalSize=" + totalSize +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            return true;
        } else{
            if (obj instanceof FileInfo){
                FileInfo o = (FileInfo)obj;
                if(this.mPath.equals(o.mPath) && this.mFile.equals(o.mFile)){
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.getPath().hashCode();
    }

}
