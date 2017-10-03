package com.sprd.fileexplore.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
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
import com.sprd.fileexplore.file.FileInfoManager;
import com.sprd.fileexplore.file.FileType;
import com.sprd.fileexplore.fragment.BaseFragment;
import com.sprd.fileexplore.fragment.DetailListFragment;
import com.sprd.fileexplore.load.ImageCache;
import com.sprd.fileexplore.util.FileUtil;
import com.sprd.fileexplore.util.LogUtil;
import com.sprd.fileexplore.util.SparseBooleanArrayParcelable;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Xuehao.Jiang on 2017/4/11.
 */

public class DetailRecyclerViewAdapter extends RecyclerView.Adapter<DetailRecyclerViewAdapter.DetailViewHolder> {


    private static final String TAG= DetailRecyclerViewAdapter.class.getSimpleName();
    private  Context mContext;
    protected List<FileInfo> mFileInfoList =new ArrayList<FileInfo>();
    private LayoutInflater inflater;
    private SparseBooleanArrayParcelable mSelectedItemsIds;
    private RecyclerItemClickListener itemClickListener;
    private DetailListFragment fragment;
    private ImageLoadListener loadListener=null;
    private Handler mHandler;

    public DetailRecyclerViewAdapter(Context mContext, RecyclerItemClickListener listener, BaseFragment f){
        this.mContext = mContext;
        mSelectedItemsIds = new SparseBooleanArrayParcelable();
        itemClickListener = listener;
        fragment = (DetailListFragment) f;
        loadListener = new ImageLoadListener();
        mHandler= new Handler(mContext.getApplicationContext().getMainLooper());
        // 初始化imageloader
        initImageLoader(mContext);
        imageLoader = ImageLoader.getInstance();
    }

    public DetailRecyclerViewAdapter(Context mContext, RecyclerItemClickListener listener){
        this(mContext,listener,null);
    }

    public void refreshAdapter(FileInfoManager fm){
        Log.d(TAG,"refreshAdapter .... " );
        mFileInfoList = (List<FileInfo>)fm.getShowFileList().clone();
        if(mFileInfoList.size()>0){
            int i = 0;
            for(FileInfo fi: mFileInfoList){
                Log.d(TAG," f["+i+"] = "+ fi);
                i++;
            }
        }
        if(fragment!=null){
            fragment.setEmptyViewShow(mFileInfoList.size() >0 ? false: true );
        }
        notifyDataSetChanged();
    }
    @Override
    public DetailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.recycle_view_item,parent,false);
        return new DetailViewHolder(view,itemClickListener);
    }

    @Override
    public void onBindViewHolder(DetailViewHolder holder, int position) {
        FileInfo mFileInfo =mFileInfoList.get(position);

        int iconId = mFileInfo.getFileIcon();
        holder.mName.setText(mFileInfo.getName());

        if(mFileInfo.getFileRootCategory() == FileInfo.ROOT_INTERNAL){
            holder.mIcon.setImageResource(R.mipmap.main_phone);
        }else if(mFileInfo.getFileRootCategory() == FileInfo.ROOT_EXTERNAL){
            holder.mIcon.setImageResource(R.mipmap.main_sd);
        }else if(mFileInfo.getFileRootCategory() == FileInfo.ROOT_USB){
            holder.mIcon.setImageResource(R.mipmap.main_otg);
        }else if(mFileInfo.isFolder()){
            holder.mIcon.setImageResource(R.mipmap.file_item_folder_ic);
        }else {

            // set show default icon
            holder.mIcon.setImageResource(iconId);
            // this will load image and video icon by ImageLoader
            String path =mFileInfo.getPath();
            FileType ft = FileType.getInstance();
            if(ft.isVideoFileType(path)) {
                Uri uri = mFileInfo.getUri();
                imageLoader.displayImage(uri.toString(), holder.mIcon, options);
            }

            Bitmap bitmap = null;
            ImageCache imageCache = ImageCache.getInstance();
            if(mScrolling){
                bitmap = imageCache.get(path);
            }else{
                bitmap = imageCache.get(path);
                if (null == bitmap ) {
                    if(iconId == FileType.FILE_TYPE_APK ||
                            iconId == FileType.FILE_TYPE_IMAGE){
                        imageCache.loadImageBitmap(mContext, mFileInfo.getPath(), loadListener, mHandler,
                                iconId, position);
                    } else if(FileType.getInstance().isAudioFileType(path)){
                        iconId = FileType.FILE_TYPE_AUDIO_DEFAULT;
                        imageCache.loadImageBitmap(mContext, mFileInfo.getPath(), loadListener, mHandler,
                                iconId, position);
                    }
                }
            }
            if(bitmap != null){
                holder.mIcon.setImageBitmap(bitmap);
            }
        }

        LogUtil.d(TAG," onBindViewHolder   getName="+ mFileInfo.getName() );
        if(mContext == null) return;

        Resources res = mContext.getResources();
        String timeStr = FileUtil.SIMPLE_DATE_FOTMAT.format(mFileInfo.getLastModifiedTime());
        if(mFileInfo.isStorageRoot()){
            holder.mDate.setText(res.getString(
                    R.string.file_list_storage_msg, mFileInfo.getAvaliableSize(),mFileInfo.getTotalSize() ));
        }else if (mFileInfo.isFolder()) {
            holder.mDate.setText(res.getString(
                    R.string.file_list_flodermsg, timeStr));
        } else {
            String size= Formatter.formatFileSize(mContext, mFileInfo.getFileSize());
            holder.mDate.setText(res.getString(
                    R.string.file_list_filemsg, timeStr, size));
        }
        /*
        *   You can find item View by locating position
        * */
        holder.getView().setTag(position);
        int colorAccent = ContextCompat.getColor(mContext, R.color.gainsboro);
        holder.itemView.setBackgroundColor(mSelectedItemsIds.get(position,false)?colorAccent: Color.TRANSPARENT);

    }

    @Override
    public int getItemCount() {
        if(mFileInfoList!=null)
            return mFileInfoList.size();
        return 0;
    }

    public class DetailViewHolder extends  RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{

        private ImageView mIcon;
        private TextView mName;
        private TextView  mDate;
        private CheckBox mCheckBox;
        private View rootView;
        private RecyclerItemClickListener recyclerItemClickListener;

        public DetailViewHolder(View itemView, RecyclerItemClickListener mListener) {
            super(itemView);

            rootView = itemView;
            recyclerItemClickListener = mListener;
           // mCheckBox = (CheckBox) itemView.findViewById(R.id.checkbox);
            mName = (TextView) itemView.findViewById(R.id.file_name);
            mDate = (TextView) itemView.findViewById(R.id.file_date);
            mIcon = (ImageView) itemView.findViewById(R.id.file_icon);
            rootView.setOnClickListener(this);
            rootView.setOnLongClickListener(this);

        }

        @Override
        public void onClick(View v) {
            if (recyclerItemClickListener != null) {
                recyclerItemClickListener.onItemClick(v, getAdapterPosition());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (recyclerItemClickListener != null) {
                recyclerItemClickListener.onItemLongClick(v, getAdapterPosition());
            }
            return false;
        }
        public View getView(){
            return rootView;
        }
    }

    public FileInfo getCurrentPosFileInfo(int index){
        if(index>=0 && index < mFileInfoList.size())
            return mFileInfoList.get(index);
        return null;
    }

    public void  addFileInfos(FileInfo fileInfo){
        if(mFileInfoList!=null ){
            LogUtil.d("PathObserver"," add before  : size = "+mFileInfoList.size());
            boolean isContain= mFileInfoList.contains(fileInfo);
            LogUtil.d("PathObserver","addFileInfos  isContain ="+isContain);
            if(!isContain){
                mFileInfoList.add(fileInfo);
            }
            LogUtil.d("PathObserver"," add after  : size = "+mFileInfoList.size());
        }
    }

    public void removeFileInfo(FileInfo fileInfo){
        if(mFileInfoList!=null ){
            LogUtil.d("PathObserver"," remove before  : size = "+mFileInfoList.size());
            boolean isContain= mFileInfoList.contains(fileInfo);
            LogUtil.d("PathObserver"," removeFileInfo  isContain ="+isContain);
            if(isContain){
                mFileInfoList.remove(fileInfo);
            }
            LogUtil.d("PathObserver"," remove after  : size = "+mFileInfoList.size());
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

    public void selectOrCancellAll(){
        int count = getItemCount();
        int selectCount = getSelectedCount();

        if( count >0 && selectCount < count){
            mSelectedItemsIds.clear();
            for(int i=0;i<count;i++){
                mSelectedItemsIds.put(i,true);
            }
        }else if(count >0  && selectCount == count){
            mSelectedItemsIds.clear();
        }
        notifyDataSetChanged();
    }
    public List<FileInfo> getmFileInfoList(){
        return mFileInfoList;
    }
    //Get total selected count
    public int getSelectedCount() {
        return mSelectedItemsIds.size();
    }
    public int getPosition(FileInfo fileInfo) {
        return mFileInfoList.indexOf(fileInfo);
    }
    //Return all selected ids
    public SparseBooleanArrayParcelable getSelectedIds() {
        return mSelectedItemsIds;
    }

    /**
    *  This method return all the files selected by us
    *  @author Xuehao.Jiang
    *  created at 2017/7/7 22:05
    */
    public  List<FileInfo> getCheckedFileInfoItemsList(){

        List<FileInfo> fileInfoCheckedList = new ArrayList<FileInfo>();
        int count = getItemCount();
        for(int i=0; i < count; i++){
            boolean isContain = mSelectedItemsIds.get(i);
            if(isContain){
                fileInfoCheckedList.add(mFileInfoList.get(i));
            }
        }
        Log.d(TAG,"getCheckedFileInfoItemsList: --> size="+fileInfoCheckedList.size()
                +" save checked size= "+mSelectedItemsIds.size());
        return fileInfoCheckedList;
    }
    /**
    *  This method gets the first item checked.
    *  @author Xuehao.Jiang
    *  created at 2017/7/8 10:56
    */
    public  FileInfo getFirstCheckedFileInfoItem(){
        List<FileInfo> list = getCheckedFileInfoItemsList();
        int size =list .size();
        if(size>0){
            return list.get(0);
        }
        return null;
    }
    class ImageLoadListener implements ImageCache.OnImageLoadCompleteListener{

        @Override
        public void OnImageLoadComplete(String fileUrl, boolean success,
                                        Bitmap bitmap,int pos) {
            Log.d(TAG," OnImageLoadComplete:  file="+fileUrl +"  pos="+pos);
            for(FileInfo fi: mFileInfoList){
                if(fi.getPath().equals(fileUrl) ){
                    if(pos== mFileInfoList.indexOf(fi)){
                        notifyItemChanged(pos);
                    }
                }
            }
        }
    }


    /*
    *   this is ImageLoader frame
    * */
    public ImageLoader imageLoader;
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

    public boolean mScrolling = false;
    public void setScrolling(boolean scrolling) {
        mScrolling = scrolling;

    }



}
