package com.sprd.fileexplore.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.Log;

import com.sprd.fileexplore.file.FileInfo;
import com.sprd.fileexplore.util.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Xuehao.Jiang on 2017/6/28.
 */

public class MountRootManagerRf {

    private static final String TAG = MountRootManagerRf.class.getSimpleName();

    public static final String SEPARATOR = "/";
    public static final String HOME = "Home";
    public static final String ROOT_PATH = "/storage";

    private String mRootPath = "Root Path";


    protected MountReceiver mMountReceiver = null;
    private StorageManager mStorageManager;
    private final CopyOnWriteArrayList<StorageVolume> storageVolumeAll = new CopyOnWriteArrayList<StorageVolume>();
    private final CopyOnWriteArrayList <MountPoint> mMountPathList = new CopyOnWriteArrayList<MountPoint>();

    private static MountRootManagerRf sInstance = new MountRootManagerRf();

    private final static String METHOD_GET_VOLUME_LIST = "getVolumeList";
    private final static String METHOD_FIND_VOLUME_BY_UUID = "findVolumeByUuid";
    private final static String METHOD_GET_DISK = "getDisk";
    private final static String METHOD_IS_SD = "isSd";
    private final static String METHOD_IS_USB = "isUsb";
    private final static String METHOD_GET_PATH = "getPathFile";

    public final static String STATUS_MOUNTED ="mounted";
    public final static String STATUS_UNMOUNTED ="unmounted";
    public final static String STATUS_EJECTED ="ejected";
    public final static String STATUS_BAD_REMOVAL="bad_removal";
    public final static String STATUS_LOCAL_CHANGED="local_changed";

    private static  final int PHONE_ROOT = 0;
    private static  final int SD_ROOT = 1;
    private static  final int OTG_ROOT = 2;
    private static  final int NONE_ROOT = -1;
    private final ArrayList< MountListener> sListeners = new ArrayList< MountListener>();
    private Context context;
    public static final int SDK_VERSION = Build.VERSION.SDK_INT;

    /*
    *  use reflect method to get StorageVolume lists
    * */
    Method mGetVolumeListMethod = null;
    Method mFindVolumeByUuidMethod = null;
    Object[] mStorageVolumeList = null;

    public static boolean isNeedReflect(){
        if(SDK_VERSION > Build.VERSION_CODES.M){
            return false;
        }
        return true;
    }

    private void refresh(){

        final String defaultPath = getDefaultPath();
        Log.d(TAG, "refresh ,defaultPath = " + defaultPath);
        if (!TextUtils.isEmpty(defaultPath)) {
            mRootPath = ROOT_PATH;
        }
        storageVolumeAll.clear();
        mMountPathList.clear();
        try {
            mGetVolumeListMethod = mStorageManager.getClass().getMethod(METHOD_GET_VOLUME_LIST, new Class[0]);
            mStorageVolumeList=(Object[]) mGetVolumeListMethod.invoke(mStorageManager, new Object[0]);

            if(mStorageVolumeList != null && mStorageVolumeList.length>0){

                for(int i=0; i< mStorageVolumeList.length ; i++){

                    File f = (File) mStorageVolumeList[i].getClass()
                            .getMethod(METHOD_GET_PATH, new Class[0]).invoke(mStorageVolumeList[i], new Object[0]);

                    StorageVolume sv =(StorageVolume) mStorageVolumeList[i];

                    String uuid="";
                    String desc="";
                    String state="";

                    if(isNeedReflect()){
                        uuid=(String) sv.getClass().getMethod("getUuid",new Class[0]).invoke(sv,new Object[0]);
                        desc=(String) sv.getClass().getMethod("getDescription",Context.class).invoke(sv,context);
                        state=(String)sv.getClass().getMethod("getState",new Class[0]).invoke(sv,new Object[0]);
                    }else{
                        uuid = sv.getUuid();
                        desc = sv.getDescription(context);
                        state= sv.getState();
                    }
                    Log.d(TAG,"   ....storageVolumes[ "+i+"] = "+" mfsUuid="+ uuid+"  path="+ f.getPath()
                            +" label="+ desc+" avaliable_state = "+ state );

                    MountPoint mountPoint = new MountPoint();
                    mountPoint.mDescription = desc;
                    mountPoint.mPath= f.getPath();
                    mountPoint.rootType = getRootPathType(sv);
                    mountPoint.mIsMounted = Environment.MEDIA_MOUNTED.equals(state);
                        /* //(long)sv.getClass().getMethod("getMaxFileSize",new Class[0]).invoke(sv,new Object[0]);*/
                    mountPoint.mMaxFileSize= f.getTotalSpace();
                    mountPoint.mFreeSpace = f.getFreeSpace();
                    mMountPathList.add(mountPoint);

                    Log.d(TAG,"  mMountPathList:  add "+mountPoint);

                    if( f.exists()&& f.isDirectory() && f.canWrite()){
                        if(Environment.MEDIA_MOUNTED.equals(state)){
                            storageVolumeAll.add(sv);
                        }
                    }
                }
                Log.d(TAG,"  storageVolumeAll  size="+ storageVolumeAll.size());
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }
    public static MountRootManagerRf getInstance() {
        return sInstance;
    }

    private MountRootManagerRf() {}

    public void init(Context context) {
        mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        this.context = context;
        registerMountReceiver(context);
        refresh();

    }


    public List<StorageVolume> getStorageVolumes(){

        ArrayList<StorageVolume> temp = new ArrayList<StorageVolume>();
        List<StorageVolume> rootInfos = new ArrayList<StorageVolume>();
        for(StorageVolume sv: storageVolumeAll){
            temp.add(sv);
        }
        rootInfos = (ArrayList<StorageVolume>)temp.clone();

        return rootInfos;
    }

    public boolean getPhoneMountPointStatue() {
        boolean isMounted= false;
        for (MountPoint mp : mMountPathList) {
            if (mp.mIsMounted) {
                if(mp.rootType==PHONE_ROOT){
                    isMounted =true;
                    break;
                }
            }
        }
        return isMounted;
    }
    public boolean getSdMountPointStatue() {
        boolean isMounted= false;
        for (MountPoint mp : mMountPathList) {
            if (mp.mIsMounted) {
                if(mp.rootType==SD_ROOT){
                    isMounted =true;
                    break;
                }
            }
        }
        return isMounted;
    }
    public boolean getOtgMountPointStatue() {
        boolean isMounted= false;
        for (MountPoint mp : mMountPathList) {
            if (mp.mIsMounted) {
                if(mp.rootType==OTG_ROOT){
                    isMounted =true;
                    break;
                }
            }
        }
        return isMounted;
    }
    public FileInfo getPhoneFileInfo(){
        FileInfo f = null;
        for (MountPoint mp : mMountPathList) {
            if (mp.mIsMounted) {
                if(mp.rootType == PHONE_ROOT){
                    f = new FileInfo(mp.mPath);
                    f.setName(mp.mDescription);
                    break;
                }
            }
        }
        return f;
    }
    public FileInfo getSdFileInfo(){
        FileInfo f = null;
        for (MountPoint mp : mMountPathList) {
            if (mp.mIsMounted) {
                if(mp.rootType == SD_ROOT){
                    f = new FileInfo(mp.mPath);
                    f.setName(mp.mDescription);
                    break;
                }
            }
        }
        return f;
    }


    public int getRootPathType(StorageVolume sv){

        try {
            File f = (File) sv.getClass()
                    .getMethod(METHOD_GET_PATH, new Class[0]).invoke(sv, new Object[0]);
            String path= f.getPath();
            if (path !=null && path.startsWith("/storage/emulated")) {
                return PHONE_ROOT;
            }

            /*  androidL has no  "findVolumeByUuid" method ,so need to handle it*/
            boolean isSd = false;
            boolean isUsb= false;
            if( SDK_VERSION <= Build.VERSION_CODES.LOLLIPOP_MR1){
                isSd = (boolean)sv.getClass().getMethod("isRemovable",new Class[0]).invoke(sv,new Object[0]);
                Log.d(TAG,"  getRootPathType ....<   SDK version: 22 , isSd= "+isSd  +"  path="+path);
            }
            if(isSd){
                return SD_ROOT;
            }

            mFindVolumeByUuidMethod = mStorageManager.getClass().getMethod(METHOD_FIND_VOLUME_BY_UUID, String.class);
            String uuid="";
            if(isNeedReflect()){
                uuid = (String)sv.getClass().getMethod("getUuid",new Class[0]).invoke(sv,new Object[0]);
            }else{
                uuid = sv.getUuid();
            }
            Object volumeInfo = mFindVolumeByUuidMethod.invoke(mStorageManager,uuid);
            Object diskInfo = volumeInfo.getClass().getMethod(METHOD_GET_DISK, new Class[0]).invoke(volumeInfo,new Object[0]);
            isUsb=(boolean) diskInfo.getClass().getMethod(METHOD_IS_USB,new Class[0]).invoke(diskInfo,new Object[0]);
            isSd = (boolean) diskInfo.getClass().getMethod(METHOD_IS_SD,new Class[0]).invoke(diskInfo,new Object[0]);

            if(isSd){
                return SD_ROOT;
            }else if(isUsb){
                return OTG_ROOT;
            }

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return NONE_ROOT;
    }

    public  int getRootPathType(String path){
        int value = NONE_ROOT;
        if(storageVolumeAll.size()>0){
            for(StorageVolume sv: storageVolumeAll){
                value =  getRootPathType(sv);
            }
        }
        return value;
    }

    public void onDestroy() {
        if(context!=null){
            context.unregisterReceiver(mMountReceiver);
            context.unregisterReceiver(localReceiver);
        }
        Log.d(TAG,"  onDestroy .... ");
    }

    public String getDefaultPath() {
        String defaultPath = Environment.getExternalStorageDirectory().getPath();
        Log.d(TAG, "getDefaultPath:" +defaultPath);
        return defaultPath;
    }

    public interface MountListener{
        public void notifyStorageChanged(String path, String oldState, String newState);
    }

    private final Object sLock = new Object();
    public void registerMountListener( MountListener scl) {
        synchronized (sLock) {
            sListeners.add(scl);
        }
    }

    public void unregisterMountListener( MountListener scl) {
        synchronized (sLock) {
            sListeners.remove(scl);
        }
    }
    public void notifyStorageChanged(String path, String oldState, String newState) {
        synchronized (sLock) {
            for ( MountListener l : sListeners) {
                l.notifyStorageChanged(path, oldState, newState);
            }
        }
    }

    private BroadcastReceiver localReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG,"  localReceiver--->  onReceive     action= "+action);
            if(Intent.ACTION_LOCALE_CHANGED.equals(action)){
                refresh();
                notifyStorageChanged(null,"",STATUS_LOCAL_CHANGED);
            }
        }
    };

    public class MountReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            String mountPoint = null;
            Log.d(TAG,"  MountReceiver--->  onReceive     action= "+action);
            Uri mountPointUri = intent.getData();
            if (mountPointUri != null) {
                mountPoint = mountPointUri.getPath();
            }
            if (mountPoint == null || mountPointUri == null) {
                return;
            }
            refresh();
            if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                notifyStorageChanged(mountPoint,"",STATUS_MOUNTED);
            } else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {
                notifyStorageChanged(mountPoint,"",STATUS_UNMOUNTED);
            } else if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
                notifyStorageChanged(mountPoint,"",STATUS_EJECTED);
            }else if( Intent.ACTION_MEDIA_BAD_REMOVAL.equals(action)){
                notifyStorageChanged(mountPoint,"",STATUS_BAD_REMOVAL);
            }
        }
    }

    /**
     * Register a MountReceiver for context. See
     * {@link Intent.ACTION_MEDIA_MOUNTED} {@link Intent.ACTION_MEDIA_UNMOUNTED}
     *
     * @param context Context to use
     * @return A mountReceiver
     */
    public  void registerMountReceiver(Context context) {
        mMountReceiver = new MountReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction( Intent.ACTION_MEDIA_BAD_REMOVAL);
        intentFilter.addDataScheme("file");
        context.registerReceiver(mMountReceiver, intentFilter);

        IntentFilter locaIntent = new IntentFilter();
        locaIntent.addAction(Intent.ACTION_LOCALE_CHANGED);
        context.registerReceiver(localReceiver,locaIntent);
    }

    private static class MountPoint {
        String mDescription;
        String mPath;
        /*  rootType means : internal, SD,OTG,commom file*/
        int  rootType;
        boolean mIsMounted;
        long mMaxFileSize;
        long mFreeSpace;
        long mTotalSpace;

        @Override
        public String toString() {
            return "MountPoint{" +
                    "mDescription='" + mDescription + '\'' +
                    ", mPath='" + mPath + '\'' +
                    ", rootType=" + rootType +
                    ", mIsMounted=" + mIsMounted +
                    ", mMaxFileSize=" + mMaxFileSize +
                    ", mFreeSpace=" + mFreeSpace +
                    ", mTotalSpace=" + mTotalSpace +
                    '}';
        }
    }
    /**
     * This method checks weather certain path is root path.
     *
     * @param path certain path to be checked
     * @return true for root path, and false for not root path
     */
    public boolean isRootPath(String path) {
        return mRootPath.equals(path);
    }

    /**
     * This method gets root path
     *
     * @return root path
     */
    public String getRootPath() {
        Log.d(TAG,"  mRootPath="+mRootPath);
        return mRootPath;
    }
    /**
     * This method gets informations of file of mount point path
     *
     * @return fileInfos of mount point path
     */
    public List<FileInfo> getMountPointFileInfo() {
        Log.d(TAG," getMountPointFileInfo ... ");
        List<FileInfo> fileInfos = new ArrayList<FileInfo>(0);
        for (MountPoint mp : mMountPathList) {
            Log.d(TAG," getMountPointFileInfo ..., mp=  "+mp );
            if (mp.mIsMounted) {
                File f = new File(mp.mPath);
                FileInfo fi = new FileInfo(mp.mPath);
                fi.setFileRootCategory(mp.rootType);
                fi.setName(mp.mDescription);
                fi.setFile(f);
                fi.setAvaliableSize(FileUtil.formatSize(context,f,2 ));
                fi.setTotalSize(FileUtil.formatSize(context,f,1 ));
                fileInfos.add(fi);
            }
        }
        Log.d(TAG," getMountPointFileInfo ...end , size=  "+fileInfos.size());
        return fileInfos;
    }


    /**
     * This method gets count of mount, number of mount point(s)
     *
     * @return number of mount point(s)
     */
    public int getMountCount() {
        int count = 0;
        for (MountPoint mPoint : mMountPathList) {
            if (mPoint.mIsMounted) {
                count++;
            }
        }
        Log.d(TAG, "getMountCount,count = " + count);
        return count;
    }

    /**
     * This method checks whether SDcard is mounted or not
     *
     * @param mountPoint the mount point that should be checked
     * @return true if SDcard is mounted, false otherwise
     */
    protected boolean isMounted(String mountPoint) {
        Log.d(TAG, "isMounted, mountPoint = " + mountPoint);
        if (TextUtils.isEmpty(mountPoint)) {
            return false;
        }
        String state  = null;
        try {
            state = (String)mStorageManager.getClass().getMethod("getVolumeState",String.class).invoke(mStorageManager,mountPoint );
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "state = " + state);
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /**
     * This method checks whether SDcard is mounted or not
     *
     * @param path the path that should be checked
     * @return true if SDcard is mounted, false otherwise
     */
    public boolean isRootPathMount(String path) {
        Log.d(TAG, "isRootPathMount,  path = " +  path);
        boolean ret = false;
        if (path == null) {
            return ret;
        }
        ret = isMounted(getRealMountPointPath(path));
        Log.d(TAG, "isRootPathMount,  ret = " + ret);
        return ret;
    }

    /**
     * This method gets real mount point path for certain path.
     *
     * @param path certain path to be checked
     * @return real mount point path for certain path, "" for path is not mounted
     */
    public String getRealMountPointPath(String path) {
        Log.d(TAG, "getRealMountPointPath ,path =" + path);
        for (MountPoint mountPoint : mMountPathList) {
            if ((path + SEPARATOR).startsWith(mountPoint.mPath + SEPARATOR)) {
                Log.d(TAG, "getRealMountPointPath = " + mountPoint.mPath);
                return mountPoint.mPath;
            }
        }
        Log.d(TAG, "getRealMountPointPath = \"\" ");
        return "";
    }

    /**
     * This method checks weather certain path is a FAT32 disk.
     *
     * @param path certain path to be checked
     * @return true for FAT32, and false for not.
     */
    public boolean isFat32Disk(String path) {
        Log.d(TAG, "isFat32Disk ,path =" + path);
        for (MountPoint mountPoint : mMountPathList) {
            if ((path + SEPARATOR).startsWith(mountPoint.mPath + SEPARATOR)) {
                Log.d(TAG, "isFat32Disk = " + mountPoint.mPath);
                if (mountPoint.mMaxFileSize > 0) {
                    Log.d(TAG, "isFat32Disk = true.");
                    return true;
                }
                Log.d(TAG, "isFat32Disk = false.");
                return false;
            }
        }

        Log.d(TAG, "isFat32Disk = false.");
        return false;
    }
    /**
     * This method changes mount state of mount point, if parameter path is mount point.
     *
     * @param path certain path to be checked
     * @param isMounted flag to mark weather certain mount point is under mounted state
     * @return true for change success, and false for fail
     */
    public boolean changeMountState(String path, Boolean isMounted) {
        boolean ret = false;
        for (MountPoint mountPoint : mMountPathList) {
            if (mountPoint.mPath.equals(path)) {
                if (mountPoint.mIsMounted == isMounted) {
                    break;
                } else {
                    mountPoint.mIsMounted = isMounted;
                    ret = true;
                    break;
                }
            }
        }
        Log.d(TAG, "changeMountState ,path =" + path + ",ret = " + ret);

        return ret;
    }
    /**
     * This method checks weather certain path is mount point.
     *
     * @param path certain path, which needs to be checked
     * @return true for mount point, and false for not mount piont
     */
    public boolean isMountPoint(String path) {
        boolean ret = false;
        Log.d(TAG, "isMountPoint ,path =" + path);
        if (path == null) {
            return ret;
        }
        for (MountPoint mountPoint : mMountPathList) {
            if (path.equals(mountPoint.mPath)) {
                ret = true;
                break;
            }
        }
        Log.d(TAG, "isMountPoint ,ret =" + ret);
        return ret;
    }
    /**
     * This method checks weather certain path is internal mount path.
     *
     * @param path path which needs to be checked
     * @return true for internal mount path, and false for not internal mount path
     */
    public boolean isInternalMountPath(String path) {
        Log.d(TAG, "isInternalMountPath ,path =" + path);
        if (path == null) {
            return false;
        }
        for (MountPoint mountPoint : mMountPathList) {
            if (mountPoint.rootType==PHONE_ROOT && mountPoint.mPath.equals(path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method checks weather certain path is external mount path.
     *
     * @param path path which needs to be checked
     * @return true for external mount path, and false for not external mount path
     */
    public boolean isExternalMountPath(String path) {
        Log.d(TAG, "isExternalMountPath ,path =" + path);
        if (path == null) {
            return false;
        }
        for (MountPoint mountPoint : mMountPathList) {
            if (mountPoint.rootType==SD_ROOT && mountPoint.mPath.equals(path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method checks weather certain path is otg mount path.
     *
     * @param path path which needs to be checked
     * @return true for otg mount path, and false for not external mount path
     */
    public boolean isOtgMountPath(String path) {
        Log.d(TAG, "isOtgMountPath ,path =" + path);
        if (path == null) {
            return false;
        }
        for (MountPoint mountPoint : mMountPathList) {
            if (mountPoint.rootType==OTG_ROOT && mountPoint.mPath.equals(path)) {
                return true;
            }
        }
        return false;
    }
    /**
     * This method checks weather certain file is External File.
     *
     * @param fileInfo certain file needs to be checked
     * @return true for external file, and false for not external file
     */
    public boolean isExternalFile(FileInfo fileInfo) {
        boolean ret = false;
        if (fileInfo != null) {
            String mountPath = getRealMountPointPath(fileInfo.getPath());
            if (mountPath.equals(fileInfo.getPath())) {
                Log.d(TAG, "isExternalFile,return false .mountPath = " + mountPath);
                ret = false;
            }
            if ( isExternalMountPath(mountPath) ||isOtgMountPath(mountPath) ) {
                ret = true;
            }
        }

        Log.d(TAG, "isExternalFile,ret = " + ret);

        return ret;
    }

    /**
     * This method gets description of certain path
     *
     * @param path certain path
     * @return description of the path
     */
    public String getDescriptionPath(String path) {
        Log.d(TAG, "getDescriptionPath ,path =" + path);
        if (mMountPathList != null) {
            for (MountPoint mountPoint : mMountPathList) {
                if ((path + SEPARATOR).startsWith(mountPoint.mPath + SEPARATOR)) {
                    return path.length() > mountPoint.mPath.length() + 1 ? mountPoint.mDescription
                            + SEPARATOR + path.substring(mountPoint.mPath.length() + 1)
                            : mountPoint.mDescription;
                }
            }
        }
        return path;
    }
    /**
     * This method judge whether one path indicates primary volume.
     *
     * @param path certain path
     * @return true for primary path, false for other path
     */
    public boolean isPrimaryVolume(String path) {
        Log.d(TAG, "isPrimaryVolume ,path =" + path);
        if (mMountPathList.size() > 0) {
            return mMountPathList.get(0).mPath.equals(path);
        } else {
            Log.w(TAG, "mMountPathList null!");
            return false;
        }
    }
    /**
     * This method update mount point space infomation(free space & total space)
     *
     */
    public void updateMountPointSpaceInfo() {
        Log.d(TAG, "updateMountPointSpaceInfo...");
        for (MountPoint mp : mMountPathList) {
            if (mp.mIsMounted) {
                File file = new File(mp.mPath);
                mp.mFreeSpace = file.getUsableSpace();
                mp.mTotalSpace = file.getTotalSpace();
            }
        }
    }

    /**
     * This method gets free space of some path, if this path indicates mount point.
     *
     * @param path certain path
     * @return free space of volume
     */
    public long getMountPointFreeSpace(String path) {
        Log.d(TAG, "getMountPointFreeSpace " + path);
        long freeSpace = 0;
        for (MountPoint mp : mMountPathList) {
            if (mp.mPath.equalsIgnoreCase(path)) {
                freeSpace = mp.mFreeSpace;
            }
        }
        return freeSpace;
    }

    /**
     * This method gets total space of some path, if this path indicates mount point.
     *
     * @param path certain path
     * @return total space of volume
     */
    public long getMountPointTotalSpace(String path) {
        Log.d(TAG, "getMountPointTotalSpace " + path);
        long totalSpace = 0;
        for (MountPoint mp : mMountPathList) {
            if (mp.mPath.equalsIgnoreCase(path)) {
                totalSpace = mp.mTotalSpace;
                if (totalSpace == 0) {
                    mp.mFreeSpace = new File(mp.mPath).getUsableSpace();
                    mp.mTotalSpace = new File(mp.mPath).getTotalSpace();
                }
            }
        }
        return totalSpace;
    }

    private ArrayList<FileInfo> operateFiles = new ArrayList<>();
    public void setOperateFileInfos(ArrayList<FileInfo> operateFile){
        operateFiles.clear();
        operateFiles = (ArrayList<FileInfo>)operateFile.clone();
    }
    public List<FileInfo> getOperateFileInfos(){
        return operateFiles;
    }
    public void clearOperateFileInfos(){
        operateFiles.clear();
    }
}
