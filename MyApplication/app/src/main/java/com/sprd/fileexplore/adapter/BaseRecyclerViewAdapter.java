package com.sprd.fileexplore.adapter;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import com.sprd.fileexplore.R;
import com.sprd.fileexplore.file.FileInfo;
import com.sprd.fileexplore.file.FileType;
import com.sprd.fileexplore.fragment.BaseFragment;
import com.sprd.fileexplore.fragment.OverViewFragment;
import com.sprd.fileexplore.load.ImageCache;
import com.sprd.fileexplore.util.FileUtil;
import com.sprd.fileexplore.util.SparseBooleanArrayParcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xuehao.Jiang on 2017/4/6.
 */

public class BaseRecyclerViewAdapter extends RecyclerView.Adapter<BaseRecyclerViewAdapter.BaseViewHolder> {

    protected static final String TAG = BaseRecyclerViewAdapter.class.getSimpleName();
    protected List<FileInfo> mFileInfoList =new ArrayList<FileInfo>();
    private LayoutInflater inflater;
    private Context mContext;
    private RecyclerItemClickListener itemClickListener;
    protected SparseBooleanArrayParcelable mSelectedItemsIds;
    private Handler mHandler;
    private OverViewFragment fragment;

    private ImageLoadListener imageLoadListener;
    private ImageCache imageCache;

    public  ImageLoader imageLoader;
    private DisplayImageOptions options = new DisplayImageOptions.Builder()
            .cacheInMemory(true)      // 设置下载的图片是否缓存在内存中
            .cacheOnDisk(true)       // 设置下载的图片是否缓存在SD卡中
            .bitmapConfig(Bitmap.Config.RGB_565)            // 避免内存溢出 ,默认ARGB_8888，使用RGB_565会比使用ARGB_8888少消耗2倍的内存
            .imageScaleType(ImageScaleType.IN_SAMPLE_INT)   // 避免内存溢出
            .displayer(new RoundedBitmapDisplayer(20)) // 设置成圆角图片
            .build();         // 创建配置过得DisplayImageOption对象;

    // 初始化imageloader
    private void initImageLoader(Context context) {
        // 初始化参数
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                .threadPriority(Thread.NORM_PRIORITY - 2)    // 线程优先级
                .threadPoolSize(5)                       // 避免内存溢出 - 配置 1-5
                .memoryCache(new WeakMemoryCache())      // 避免内存溢出 ，建议弱内存或者不使用内存缓存
                .denyCacheImageMultipleSizesInMemory()     // 当同一个Uri获取不同大小的图片，缓存到内存时，只缓存一个，默认多个
                .discCacheFileNameGenerator(new Md5FileNameGenerator()) // 将保存的时候的URI名称用MD5
                .tasksProcessingOrder(QueueProcessingType.LIFO)   // 设置图片下载和显示的工作队列排序
                .writeDebugLogs()          // 打印debug log
                .build();

        // 全局初始化此配置
        ImageLoader.getInstance().init(config);
    }
    public BaseRecyclerViewAdapter(Context mContext,RecyclerItemClickListener itemClickListener ){
        this.mContext = mContext;
        mSelectedItemsIds = new SparseBooleanArrayParcelable();
        this.itemClickListener = itemClickListener;

    }

    public BaseRecyclerViewAdapter(Context mContext, RecyclerItemClickListener itemClickListener, Handler mHandler, BaseFragment f){
        this.mContext = mContext;
        mSelectedItemsIds = new SparseBooleanArrayParcelable();
        this.itemClickListener = itemClickListener;
        this.mHandler = mHandler;
        fragment = (OverViewFragment) f;

        imageLoadListener = new ImageLoadListener();
        imageCache = ImageCache.getInstance();


        initImageLoader(mContext);
        // 初始化imageloader
        imageLoader = ImageLoader.getInstance();

    }
    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.recycle_view_item,parent,false);

        return new BaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        FileInfo mFileInfo =mFileInfoList.get(position);

        int colorAccent = ContextCompat.getColor(mContext, R.color.gainsboro);
        /** Change background color of the selected items  **/
        holder.itemView
                .setBackgroundColor(mSelectedItemsIds.get(position, false) ? colorAccent
                        : Color.TRANSPARENT);
        holder.mDate.setText(FileUtil.SIMPLE_DATE_FOTMAT.format(mFileInfo.getLastModifiedTime()));

        holder.mName.setText(mFileInfo.getName());
        int iconId = mFileInfo.getFileIcon();
        holder.mIcon.setImageResource(iconId);
        String path = mFileInfo.getPath();

        if(mContext == null) return;
        Resources res = mContext.getResources();
        String timeStr = FileUtil.SIMPLE_DATE_FOTMAT.format(mFileInfo.getLastModifiedTime());
        if (mFileInfo.isFolder()) {
            holder.mDate.setText(res.getString(
                    R.string.file_list_flodermsg, timeStr));
        } else {
            String size= Formatter.formatFileSize(mContext, mFileInfo.getFileSize());
            holder.mDate.setText(res.getString(
                    R.string.file_list_filemsg, timeStr,size));
        }
        if(FileType.getInstance().isVideoFileType(path)) {
            Uri uri = mFileInfo.getUri();
            imageLoader.displayImage(uri.toString(), holder.mIcon, options);
        }

        Bitmap bitmap = null;
        Log.d(TAG," onBindViewHolder   mScrolling="+mScrolling);
        if(mScrolling){
            bitmap = imageCache.get(path);
            Log.d(TAG," mScrolling  true ");
        }else{
            bitmap = imageCache.get(path);
            if (null == bitmap ) {
                if(iconId == FileType.FILE_TYPE_APK ||
                        iconId == FileType.FILE_TYPE_IMAGE
                        ){
                    imageCache.loadImageBitmap(mContext, mFileInfo.getPath(), imageLoadListener, mHandler,
                            iconId, position);
                } else if(FileType.getInstance().isAudioFileType(path)){
                    iconId = FileType.FILE_TYPE_AUDIO_DEFAULT;
                    imageCache.loadImageBitmap(mContext, mFileInfo.getPath(), imageLoadListener, mHandler,
                            iconId, position);
                }
            }
        }
        if(bitmap != null && !mScrolling){
            holder.mIcon.setImageBitmap(bitmap);
        }
    }

    @Override
    public int getItemCount() {
        return mFileInfoList.size();
    }

    public List<FileInfo> getFileInfoList() {
        return mFileInfoList;
    }
    public void setFileInfoList(List<FileInfo> mFileInfoList) {
        this.mFileInfoList = mFileInfoList;

    }

    class BaseViewHolder extends  ViewHolder implements View.OnClickListener,
            View.OnLongClickListener{
        private ImageView mIcon;
        private TextView  mName;
        private TextView  mDate;
        private CheckBox  mCheckBox;

        public BaseViewHolder(View itemView) {
            super(itemView);

            mIcon = (ImageView) itemView.findViewById(R.id.file_icon);
            mName = (TextView) itemView.findViewById(R.id.file_name);
            mDate = (TextView) itemView.findViewById(R.id.file_date);
            mCheckBox = (CheckBox) itemView.findViewById(R.id.checkbox);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

        }
        @Override
        public void onClick(View v) {
            if(itemClickListener!=null){
                itemClickListener.onItemClick(itemView,getAdapterPosition());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if(itemClickListener!=null){
                itemClickListener.onItemLongClick(itemView,getAdapterPosition());
            }
            return false;
        }
    }
    //Toggle selection methods
    public void toggleSelection(int position) {
        selectView(position, !mSelectedItemsIds.get(position));
    }

    //Remove selected selections
    public void removeSelection() {
        mSelectedItemsIds.clear();
        notifyDataSetChanged();
    }

    //Put or delete selected position into SparseBooleanArray
    private void selectView(int position, boolean value) {
        if (value)
            mSelectedItemsIds.put(position, true);
        else
            mSelectedItemsIds.delete(position);
        notifyItemChanged(position);
    }

    //Get total selected count
    public int getSelectedCount() {
        return mSelectedItemsIds.size();
    }
    public FileInfo getCurrentPosFileInfo(int pos){
        if( pos >=0 && pos < mFileInfoList.size())
            return mFileInfoList.get(pos);
        return null;
    }
    //Return all selected ids
    public SparseBooleanArrayParcelable getSelectedIds() {
        return mSelectedItemsIds;
    }

    class ImageLoadListener  implements ImageCache.OnImageLoadCompleteListener{

        @Override
        public void OnImageLoadComplete(String fileUrl, boolean success, Bitmap bitmap,int pos) {

            int size= mFileInfoList.size();
            Log.d(TAG,"  ImageLoadListener ---> file="+fileUrl+"  pos="+ pos);
            if(pos < size && pos >= 0){
                FileInfo fi = mFileInfoList.get(pos);
                String path = fi.getPath();
                if(success && fileUrl!=null && fileUrl.equals(path)){
                    Log.d(TAG,"  ImageLoadListener --->notifyItemChanged : "+ "  pos="+ pos);
                    notifyItemChanged(pos);
                }
            }
        }
    }
    public boolean mScrolling = false;
    public void setScrolling(boolean scrolling) {
        mScrolling = scrolling;

    }
}
