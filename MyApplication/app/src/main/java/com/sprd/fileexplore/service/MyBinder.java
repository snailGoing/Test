package com.sprd.fileexplore.service;

import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.util.Log;

import com.sprd.fileexplore.file.FileInfo;
import com.sprd.fileexplore.file.FileInfoManager;
import com.sprd.fileexplore.load.ImageCache;
import com.sprd.fileexplore.load.ImageLoadTask;
import com.sprd.fileexplore.service.FileManageService.OperationEventListener;
import com.sprd.fileexplore.service.FileOperationTask.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


/**
 * Created by Xuehao.Jiang on 2017/6/30.
 */

public class MyBinder extends Binder {

    private static String TAG =MyBinder.class.getSimpleName();
    private Context context;

    private final HashMap<String, FileManagerActivityInfo> mActivityMap =
            new HashMap<String, FileManagerActivityInfo>();
    public MyBinder(Context context){
        this.context = context;
    }

    private static class FileManagerActivityInfo {
        private BaseAsyncTask mTask = null;
        private FileInfoManager mFileInfoManager = null;
        private int mFilterType = FileManageService.FILE_FILTER_TYPE_DEFAULT;

        public void setTask(BaseAsyncTask task) {
            this.mTask = task;
        }

        public void setFileInfoManager(FileInfoManager fileInfoManager) {
            this.mFileInfoManager = fileInfoManager;
        }

        public void setFilterType(int filterType) {
            this.mFilterType = filterType;
        }

        BaseAsyncTask getTask() {
            return mTask;
        }

        FileInfoManager getFileInfoManager() {
            return mFileInfoManager;
        }

        int getFilterType() {
            return mFilterType;
        }
    }
    /**
     * This method initializes FileInfoManager of certain activity.
     *
     * @param activityName name of activity, which the FileInfoManager attached to
     * @return FileInforManager of certain activity
     */
    public FileInfoManager initFileInfoManager(String activityName) {
        FileManagerActivityInfo activityInfo = mActivityMap.get(activityName);
        if (activityInfo == null) {
            activityInfo = new FileManagerActivityInfo();
            activityInfo.setFileInfoManager(new FileInfoManager());
            Log.d(TAG,"  initFileInfoManager put:  activityName= "+activityName);
            mActivityMap.put(activityName, activityInfo);
        }
        return activityInfo.getFileInfoManager();
    }
    /**
     * This method checks that weather the service is busy or not, which means id any task exist for
     * certain activity
     *
     * @param activityName name of activity, which will be checked
     * @return true for busy, false for not busy
     */
    public boolean isBusy(String activityName) {
        boolean ret = false;
        FileManagerActivityInfo activityInfo = mActivityMap.get(activityName);
        if (activityInfo == null) {
            Log.w(TAG, "isBusy return false,because activityInfo is null!");
            return ret;
        }
        BaseAsyncTask task = activityInfo.getTask();
        if (task != null) {
            return task.isTaskBusy();
        }
        return false;
    }

    private FileManagerActivityInfo getActivityInfo(String activityName) {
        Log.d(TAG,"  getActivityInfo    activityName= "+activityName);
        FileManagerActivityInfo activityInfo = mActivityMap.get(activityName);
        if (activityInfo == null) {
            throw new IllegalArgumentException(
                    "this activity not init in Service");
        }
        return activityInfo;
    }

    /**
     * This method sets list filter, which which type of items will be listed in listView
     *
     * @param type type of list filter
     * @param activityName name of activity, which operations attached to
     */
    public void setListType(int type, String activityName) {
        getActivityInfo(activityName).setFilterType(type);
    }

    /**
     * This method sets list filter, which which type of items will be listed in listView
     *
     * @param activityName name of activity, which operations attached to
     */
    public int getListType(String activityName) {
        return getActivityInfo(activityName).getFilterType();
    }

    /**
     * This method does create folder job by starting a new CreateFolderTask
     *
     * @param activityName name of activity, which the CreateFolderTask attached to
     * @param destFolder information of file, which needs to be created
     * @param listener listener of CreateFolderTask
     */
    public void createFolder(String activityName, String destFolder,
                             OperationEventListener listener) {
        Log.d(TAG, " createFolder Start ");
        if (isBusy(activityName)) {
            listener.onTaskResult(OperationEventListener.ERROR_CODE_BUSY);
        } else {
            FileInfoManager fileInfoManager = getActivityInfo(activityName)
                    .getFileInfoManager();
            int filterType = getActivityInfo(activityName).getFilterType();
            if (fileInfoManager != null) {
                BaseAsyncTask task = new CreateFolderTask(fileInfoManager,
                        listener, context, destFolder, filterType);
                getActivityInfo(activityName).setTask(task);
                task.execute();
            }
        }
    }

    /**
     * This method does rename job by starting a new RenameTask
     *
     * @param activityName name of activity, which the operations attached to
     * @param srcFile information of certain file, which needs to be renamed
     * @param dstFile information of new file after rename
     * @param listener listener of RenameTask
     */
    public void rename(String activityName, FileInfo srcFile, FileInfo dstFile,
                       OperationEventListener listener) {
        Log.d(TAG, " rename Start,activityName = " + activityName);

        if (isBusy(activityName)) {
            listener.onTaskResult(OperationEventListener.ERROR_CODE_BUSY);
        } else {
            FileInfoManager fileInfoManager = getActivityInfo(activityName)
                    .getFileInfoManager();
            int filterType = getActivityInfo(activityName).getFilterType();
            if (fileInfoManager != null) {
                BaseAsyncTask task = new RenameTask(fileInfoManager, listener,
                        context, srcFile, dstFile, filterType);
                getActivityInfo(activityName).setTask(task);
                task.execute();
            }
        }
    }

    private int filterPasteList(List<FileInfo> fileInfoList, String destFolder) {

        int remove = 0;
        Iterator<FileInfo> iterator = fileInfoList.iterator();
        while (iterator.hasNext()) {
            FileInfo fileInfo = iterator.next();
            if (fileInfo.isFolder()) {
                if ((destFolder + MountRootManagerRf.SEPARATOR)
                        .startsWith(fileInfo.getPath()
                                + MountRootManagerRf.SEPARATOR)) {
                    iterator.remove();
                    remove++;
                }
            }
        }
        return remove;
    }

    /**
     * This method does delete job by starting a new DeleteFilesTask.
     *
     * @param activityName name of activity, which the operations attached to
     * @param fileInfoList list of files, which needs to be deleted
     * @param listener listener of the DeleteFilesTask
     */
    public void deleteFiles(String activityName, List<FileInfo> fileInfoList,
                            OperationEventListener listener) {
        Log.d(TAG, " deleteFiles Start,activityName = " + activityName);
        if (isBusy(activityName)) {
            listener.onTaskResult(OperationEventListener.ERROR_CODE_BUSY);
        } else {
            FileInfoManager fileInfoManager = getActivityInfo(activityName)
                    .getFileInfoManager();
            if (fileInfoManager != null) {
                BaseAsyncTask task = new DeleteFilesTask(fileInfoManager,
                        listener, context, fileInfoList);
                getActivityInfo(activityName).setTask(task);
                task.execute();
            }
        }
    }

    public void clearFolder(String activityName, FileInfo fileInfo,
                           OperationEventListener listener){
        Log.d(TAG, " clearFolder Start,activityName = " + activityName);
        if (isBusy(activityName)) {
            listener.onTaskResult(OperationEventListener.ERROR_CODE_BUSY);
        } else {
            FileInfoManager fileInfoManager = getActivityInfo(activityName)
                    .getFileInfoManager();
            if (fileInfoManager != null) {
                BaseAsyncTask task = new ClearFolderTask(fileInfoManager,
                        listener, context, fileInfo);
                getActivityInfo(activityName).setTask(task);
                task.execute();
            }
        }
    }

    /**
     * This method cancel certain task
     *
     * @param activityName name of activity, which the task attached to
     */
    public void cancel(String activityName) {
        Log.d(TAG, " cancel service,activityName = " + activityName);
        BaseAsyncTask task = getActivityInfo(activityName).getTask();
        if (task != null) {
            task.cancel(true);
        }
    }

    /**
     * This method does paste job by starting a new CutPasteFilesTask or CopyPasteFilesTask according
     * to parameter of type
     *
     * @param activityName name of activity, which the task and operations attached to
     * @param fileInfoList list of files which needs to be paste
     * @param dstFolder destination, which the files should be paste to
     * @param type indicate the previous operation is cut or copy
     * @param listener listener of the started task
     */
    public void pasteFiles(String activityName, List<FileInfo> fileInfoList,
                           String dstFolder, int type, OperationEventListener listener) {
        Log.d(TAG, " pasteFiles Start,activityName = " + activityName);
        if (isBusy(activityName)) {
            listener.onTaskResult(OperationEventListener.ERROR_CODE_BUSY);
            return;
        }
        if (filterPasteList(fileInfoList, dstFolder) > 0) {
            listener.onTaskResult(OperationEventListener.ERROR_CODE_PASTE_TO_SUB);
        }
        FileInfoManager fileInfoManager = getActivityInfo(activityName)
                .getFileInfoManager();
        if (fileInfoManager == null) {
            Log.w(TAG, "mFileInfoManagerMap.get FileInfoManager = null");
            listener.onTaskResult(OperationEventListener.ERROR_CODE_UNKOWN);
            return;
        }
        BaseAsyncTask task = null;
        if (fileInfoList.size() > 0) {
            switch (type) {
                case FileInfoManager.PASTE_MODE_CUT:
                    if (isCutSamePath(fileInfoList, dstFolder)) {
                        listener.onTaskResult(OperationEventListener.ERROR_CODE_CUT_SAME_PATH);
                        return;
                    }
                    task = new CutPasteFilesTask(fileInfoManager, listener, context.getApplicationContext(),
                            fileInfoList, dstFolder);
                    getActivityInfo(activityName).setTask(task);
                    task.execute();
                    break;
                case FileInfoManager.PASTE_MODE_COPY:
                    task = new CopyPasteFilesTask(fileInfoManager, listener, context.getApplicationContext(),
                            fileInfoList, dstFolder);
                    getActivityInfo(activityName).setTask(task);
                    task.execute();
                    break;
                default:
                    listener.onTaskResult(OperationEventListener.ERROR_CODE_UNKOWN);
                    return;
            }

        }
    }

    private boolean isCutSamePath(List<FileInfo> fileInfoList, String dstFolder) {
        for (FileInfo fileInfo : fileInfoList) {
            if (fileInfo.getFileParentPath().equals(dstFolder)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method lists files of certain directory by starting a new ListFileTask.
     *
     * @param activityName name of activity, which the ListFileTask attached to
     * @param path the path of certain directory
     * @param listener listener of the ListFileTask
     */
    public void listFiles(String activityName, String path, OperationEventListener listener) {
        Log.d(TAG, "listFiles,activityName = " + activityName + ",path = " + path);

        if (isBusy(activityName)) {
            Log.d(TAG, "listFiles, cancel other background task...");
            BaseAsyncTask task = getActivityInfo(activityName).getTask();
            if (task != null) {
                task.removeListener();
                task.cancel(true);
            }
        }
        Log.d(TAG, "listFiles,do list.");
        FileInfoManager fileInfoManager = getActivityInfo(activityName).getFileInfoManager();
        int filterType = getActivityInfo(activityName).getFilterType();
        if (fileInfoManager != null) {
            Log.d(TAG, "listFiles fiterType = " + filterType);
            BaseAsyncTask task = new ListFileTask(context.getApplicationContext(), fileInfoManager, listener, path, filterType);
            getActivityInfo(activityName).setTask(task);
            task.execute();
        }
        // }
    }

    /**
     * This method gets detail information of a file (or directory) by starting a new
     * DetailInfotask.
     *
     * @param activityName name of activity, which the task and operations attached to
     * @param file a certain file (or directory)
     * @param listener listener of the DetailInfotask
     */
    public void getDetailInfo(String activityName, FileInfo file,
                              OperationEventListener listener) {
        Log.d(TAG, "getDetailInfo,activityName = " + activityName);
        if (isBusy(activityName)) {
            listener.onTaskResult(OperationEventListener.ERROR_CODE_BUSY);
        } else {
            FileInfoManager fileInfoManager = getActivityInfo(activityName)
                    .getFileInfoManager();
            if (fileInfoManager != null) {
                BaseAsyncTask task = new DetailInfoTask(fileInfoManager,
                        listener, file);
                getActivityInfo(activityName).setTask(task);
                task.execute();
            }
        }
    }

    /**
     * This method removes listener from task when service disconnected.
     *
     * @param activityName name of activity, which the task attached to
     */
    public void disconnected(String activityName) {
        Log.d(TAG, "disconnected,activityName = " + activityName);
        BaseAsyncTask task = getActivityInfo(activityName).getTask();
        if (task != null) {
            task.removeListener();
        }
    }

    /**
     * This method reconnects to the running task by setting a new listener to the task, when dialog
     * is destroyed and recreated
     *
     * @param activityName name of activity, which the task and dialog attached to
     * @param listener new listener for the task and dialog
     */
    public void reconnected(String activityName, OperationEventListener listener) {
        Log.d(TAG, "reconnected,activityName = " + activityName);
        BaseAsyncTask task = getActivityInfo(activityName).getTask();
        if (task != null) {
            task.setListener(listener);
        }
    }

    /**
     * This method return whether background task is get detail info task.
     * @param activityName name of activity, which the task attached to
     * @return true if background task is detail info task, others false.
     */
    public boolean isDetailTask(String activityName) {
        FileManagerActivityInfo aInfo = mActivityMap.get(activityName);
        if (null == aInfo) {
            Log.d(TAG, "activity is not attach: " + activityName);
            return false;
        }
        BaseAsyncTask task = aInfo.getTask();
        if (task != null && task instanceof DetailInfoTask) {
            return true;
        }
        return false;
    }

    /**
     * This method return whether background task is heavy operation task.
     * @param activityName name of activity, which the task attached to
     * @return true if background task is heavy operation task, others false.
     */
    public boolean isHeavyOperationTask(String activityName) {

        FileManagerActivityInfo aInfo = mActivityMap.get(activityName);
        if (null == aInfo) {
            Log.d(TAG, "activity is not attach: " + activityName);
            return false;
        }
        BaseAsyncTask task = aInfo.getTask();
        if (task != null && (task instanceof CutPasteFilesTask
                || task instanceof CopyPasteFilesTask || task instanceof DeleteFilesTask)) {
            return true;
        }
        return false;

    }

    /**
     * This method do search job by starting a new search task
     *
     * @param activityName name of activity which starts the search
     * @param searchName the search target
     * @param path the path to limit the search in
     * @param operationEvent the listener corresponds to this search task
     */
    public void search(String activityName, String searchName, String path,
                       OperationEventListener operationEvent) {
        Log.d(TAG, "search, activityName = " + activityName + ",searchName = " + searchName + ",path = " + path);
        if (isBusy(activityName)) {
            cancel(activityName);
            // operationEvent.onTaskResult(OperationEventListener.ERROR_CODE_BUSY);
        } else {
            FileInfoManager fileInfoManager = getActivityInfo(activityName)
                    .getFileInfoManager();
            if (fileInfoManager != null) {
                BaseAsyncTask task = new SearchTask(fileInfoManager,
                        operationEvent, searchName, path, context.getContentResolver());
                getActivityInfo(activityName).setTask(task);
                task.execute();
            }
        }
    }
    /**
     * This method do search job by starting a new search task
     *
     * @param activityName name of activity which starts the search
     * @param searchName the search target
     * @param paths the paths to limit the search in
     * @param mSearchType the mSearchType to limit the search in
     * @param operationEvent the listener corresponds to this search task
     */
    public void search(String activityName, String searchName, List<String> paths, ArrayList<Integer> mSearchType,
                       OperationEventListener operationEvent) {
        Log.d(TAG, "search, activityName = " + activityName + ",searchName = " + searchName  );
        if (isBusy(activityName)) {
            cancel(activityName);
            // operationEvent.onTaskResult(OperationEventListener.ERROR_CODE_BUSY);
        } else {
            FileInfoManager fileInfoManager = getActivityInfo(activityName)
                    .getFileInfoManager();
            if (fileInfoManager != null) {
                BaseAsyncTask task = new MultiSearchTask(fileInfoManager,
                        operationEvent, searchName, paths,mSearchType, context);
                getActivityInfo(activityName).setTask(task);
                task.execute();
            }
        }
    }

    /**
    *  sort Task
    *  @author Xuehao.Jiang
    *  created at 2017/7/7 10:13
    */

    public void sortBy(String activityName,int sortType,OperationEventListener operationEvent){

        Log.d(TAG, "sortBy, activityName = " + activityName + ",sortType = " + sortType );
        if (isBusy(activityName)) {
            cancel(activityName);
        } else {
            FileInfoManager fileInfoManager = getActivityInfo(activityName)
                    .getFileInfoManager();
            if (fileInfoManager != null) {
                BaseAsyncTask task = new SortByTask(fileInfoManager,
                        operationEvent,sortType);
                getActivityInfo(activityName).setTask(task);
                task.execute();
            }
        }

    }
//    public void loadImagesWithPath(Context context, String filePath,
//                                   ImageCache.OnImageLoadCompleteListener listener, boolean isImage, int priority, Handler mHandler){
//        imageCache.loadImageBitmap(context,filePath,listener,isImage,priority,mHandler);
//    }
//    private ImageCache imageCache = ImageCache.getInstance();
}
