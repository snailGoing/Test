package com.sprd.fileexplore.load;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;
/**
 * Created by Xuehao.Jiang on 2017/4/7.
 */

public class ThreadPollManager {


    private static ThreadPollManager sInstance = new ThreadPollManager();
    private static final String TAG = ThreadPollManager.class.getSimpleName();
    private static final int CORE_POOL_SIZE = 2;
    private static final int MAXI_POOL_SIZE = 24;
    private static final int KEEP_ALIVE = 10;

    private static final ThreadPoolExecutor mExecutor;
    private static final ThreadFactory mThreadFactory;
    private static final PriorityBlockingQueue<Runnable> mWorkQueue;
    private static final Comparator<Runnable> mComparator;
    private static ConcurrentHashMap<String,ImageLoadTask> mHashMap;
    static {
        mComparator = new TaskComparator();
        mHashMap = new ConcurrentHashMap<String,ImageLoadTask>();
        mWorkQueue = new PriorityBlockingQueue<Runnable>(15, mComparator);
        mThreadFactory = new DefaultThreadFactory();
        mExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAXI_POOL_SIZE,
                KEEP_ALIVE,
                TimeUnit.SECONDS,
                mWorkQueue,
                mThreadFactory);
        // 当设置allowCoreThreadTimeOut(true)时，线程池中corePoolSize线程空闲时间达到keepAliveTime也将关闭
        mExecutor.allowCoreThreadTimeOut(true);
    }
    public static ThreadPollManager getInstance() {
        if (sInstance == null) {
            sInstance = new ThreadPollManager();
        }
        return sInstance;
    }
    public ThreadPollManager() {
        // TODO Auto-generated constructor stub
    }

    public  void removeTask(String path) {
        mHashMap.remove(path);
    }

    /**
    *  params: newTaskUrl -- means tasks which we dont't need; isScrolling -- means UI scrolling by hand
    *  @author Xuehao.Jiang
    *  created at 2017/7/6 14:36
    */
    public  void updateWorkQueue(ArrayList<String> newTaskUrl,boolean isScrolling) {
        for (ImageLoadTask task: mHashMap.values()) {
            boolean isContain = newTaskUrl.contains(task.mImageUrl);
            Log.d(TAG, "updateWorkQueue--->  isContain= "+ isContain + "   isScrolling ="+isScrolling+"  path="+task.mImageUrl);
            if (isContain && isScrolling) {
                mWorkQueue.remove(task);
                mHashMap.remove(task.mImageUrl);
            }
        }
    }

    public  void  submitTask(String path, ImageLoadTask task) {
        if (mHashMap.get(path) == null) {
            mHashMap.put(path, task);
            mExecutor.execute(task);
        } else {
            Log.e(TAG, "there is already a task running !");
        }
    }

    static class DefaultThreadFactory implements ThreadFactory {

        private final AtomicInteger mCount;
        DefaultThreadFactory() {
            mCount = new AtomicInteger(1);
        }

        public Thread newThread(Runnable runnable) {
            // Log.e(TAG, "New a Thread for  ImageLoadTask:" + mCount.toString());
            return new Thread(runnable, "ImageLoadTask #" + mCount.getAndIncrement());
        }
    }

     static class TaskComparator implements Comparator<Runnable> {

        @Override
        public int compare(Runnable runnable1, Runnable runnable2) {
            // TODO Auto-generated method stub
            return 0;
        }
    }
}
