package com.sprd.fileexplore.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sprd.fileexplore.R;
import com.sprd.fileexplore.file.FileInfo;
import com.sprd.fileexplore.file.FileType;
import com.sprd.fileexplore.service.FileManageService;
import com.sprd.fileexplore.service.FileManageService.OperationEventListener;
import com.sprd.fileexplore.service.MountRootManagerRf;
import com.sprd.fileexplore.service.MyBinder;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by Xuehao.Jiang on 2017/4/7.
 */

public class FileUtil {

    public final static SimpleDateFormat SIMPLE_DATE_FOTMAT;
    public static final int MIN_CLICK_DELAY_TIME = 500;

    private static final int MAX_IMAGE_WIDTH = 4096;
    private static final int MAX_IMAGE_HEIGTH = 4096;
    public static final int MAX_NUM_COMPRESSION = 5;
    private static final int MAX_FILENAME_LENGTH = 50;
    private static final int MAX_IMAGE_SIZE = 20 * 1024 * 1024;

    static {
        SIMPLE_DATE_FOTMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }
    public static boolean isHidden(String path){
        if (path != null && path.indexOf("/.") >= 0) {
            return true;
        } else {
            return false;
        }
    }
    public static boolean isValid(FileInfo file, boolean isShowHide) {
        if (file == null) {
            return false;
        }
        return file.isExist() && (isShowHide ? true : !isHidden(file.getPath())) ;
    }

    public static int validNoHiddenCount(Cursor mCursor){
        if (mCursor != null && mCursor.getCount() > 0) {
            int count =  mCursor.getCount();
            int mPathColumnIndex = mCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            while (mCursor.moveToNext()) {
                String path = mCursor.getString(mPathColumnIndex);
                File file = new File(path);
                FileInfo fileInfo =new FileInfo(file);
                if(!isValid(fileInfo,false)) count--;
            }
            return count;
        }
        return 0;
    }


    public static  String formatSize( Context context,File path, int choose){
        final long freeBytes = path.getFreeSpace();
        final long totalBytes = path.getTotalSpace();
        final long usedBytes = totalBytes - freeBytes;
        final String used = Formatter.formatFileSize(context, usedBytes);
        final String total = Formatter.formatFileSize(context, totalBytes);
        final String free = Formatter.formatFileSize(context, freeBytes);
        switch (choose){
            case 0:
                return used;
            case 1:
                return total;
            case 2:
                return free;
            default:
                break;
        }
        return null;
    }
    private static final String TAG = "FileUtils";
    public static final String UNIT_B = "B";
    public static final String UNIT_KB = "KB";
    public static final String UNIT_MB = "MB";
    public static final String UNIT_GB = "GB";
    public static final String UNIT_TB = "TB";
    private static final int UNIT_INTERVAL = 1024;
    private static final double ROUNDING_OFF = 0.005;
    private static final int DECIMAL_NUMBER = 100;

    /**
     * This method check the file name is valid.
     *
     * @param fileName the input file name
     * @return valid or the invalid type
     */
    public static int checkFileName(String fileName) {
        if (TextUtils.isEmpty(fileName) || fileName.trim().length() == 0) {
            return OperationEventListener.ERROR_CODE_NAME_EMPTY;
        } else {
            try {
                int length = 0;

                length = fileName.getBytes("UTF-8").length;
                // int length = fileName.length();
                Log.d(TAG, "checkFileName: " + fileName + ",lenth= " + length);
                if (length > FileInfo.FILENAME_MAX_LENGTH) {
                    Log.d(TAG, "checkFileName,fileName is too long,len=" + length);
                    return OperationEventListener.ERROR_CODE_NAME_TOO_LONG;
                } else {
                    return OperationEventListener.ERROR_CODE_NAME_VALID;
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return OperationEventListener.ERROR_CODE_NAME_EMPTY;
            }
        }
    }

    /**
     * This method gets extension of certain file.
     *
     * @param fileName name of a file
     * @return Extension of the file's name
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        String extension = null;
        final int lastDot = fileName.lastIndexOf('.');
        if ((lastDot >= 0)) {
            extension = fileName.substring(lastDot + 1).toLowerCase();
        }
        return extension;
    }

    /**
     * This method gets name of certain file from its path.
     *
     * @param absolutePath the file's absolute path
     * @return name of the file
     */
    public static String getFileName(String absolutePath) {
        int sepIndex = absolutePath.lastIndexOf(MountRootManagerRf.SEPARATOR);
        if (sepIndex >= 0) {
            return absolutePath.substring(sepIndex + 1);
        }
        return absolutePath;

    }

    /**
     * This method gets path to directory of certain file(or folder).
     *
     * @param filePath path to certain file
     * @return path to directory of the file
     */
    public static String getFilePath(String filePath) {
        int sepIndex = filePath.lastIndexOf(MountRootManagerRf.SEPARATOR);
        if (sepIndex >= 0) {
            return filePath.substring(0, sepIndex);
        }
        return "";

    }

    /**
     * This method generates a new suffix if a name conflict occurs, ex: paste a file named
     * "stars.txt", the target file name would be "stars(1).txt"
     *
     * @param file the conflict file
     * @return a new name for the conflict file
     */

    public static File genrateNextNewName(File file) {
        String parentDir = file.getParent();
        String fileName = file.getName();
        String ext = "";
        int newNumber = 0;
        if (file.isFile()) {
            int extIndex = fileName.lastIndexOf(".");
            if (extIndex != -1) {
                ext = fileName.substring(extIndex);
                fileName = fileName.substring(0, extIndex);
            }
        }

        if (fileName.endsWith(")")) {
            int leftBracketIndex = fileName.lastIndexOf("(");
            if (leftBracketIndex != -1) {
                String numeric = fileName.substring(leftBracketIndex + 1, fileName.length() - 1);
                if (numeric.matches("[0-9]+")) {
                    Log.v(TAG, "Conflict folder name already contains (): " + fileName
                            + "thread id: " + Thread.currentThread().getId());
                    try {
                        newNumber = Integer.parseInt(numeric);
                        newNumber++;
                        fileName = fileName.substring(0, leftBracketIndex);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Fn-findSuffixNumber(): " + e.toString());
                    }
                }
            }
        }
        StringBuffer sb = new StringBuffer();
        sb.append(fileName).append("(").append(newNumber).append(")").append(ext);
        if (FileUtil.checkFileName(sb.toString()) < 0) {
            return null;
        }
        return new File(parentDir, sb.toString());
    }

    /**
     * This method converts a size to a string
     *
     * @param size the size of a file
     * @return the string represents the size
     */
    public static String sizeToString(long size) {
        String unit = UNIT_B;
        if (size < DECIMAL_NUMBER) {
            Log.d(TAG, "sizeToString(),size = " + size);
            return Long.toString(size) + " " + unit;
        }

        unit = UNIT_KB;
        double sizeDouble = (double) size / (double) UNIT_INTERVAL;
        if (sizeDouble > UNIT_INTERVAL) {
            sizeDouble = (double) sizeDouble / (double) UNIT_INTERVAL;
            unit = UNIT_MB;
        }
        if (sizeDouble > UNIT_INTERVAL) {
            sizeDouble = (double) sizeDouble / (double) UNIT_INTERVAL;
            unit = UNIT_GB;
        }
        if (sizeDouble > UNIT_INTERVAL) {
            sizeDouble = (double) sizeDouble / (double) UNIT_INTERVAL;
            unit = UNIT_TB;
        }

        // Add 0.005 for rounding-off.
        long sizeInt = (long) ((sizeDouble + ROUNDING_OFF) * DECIMAL_NUMBER); // strict to two
        // decimal places
        double formatedSize = ((double) sizeInt) / DECIMAL_NUMBER;
        Log.d(TAG, "sizeToString(): " + formatedSize + unit);

        if (formatedSize == 0) {
            return "0" + " " + unit;
        } else {
            return Double.toString(formatedSize) + " " + unit;
        }
    }

    /**
     * This method gets the MIME type from multiple files (order to return: image->video->other)
     *
     * @param service service of FileInfoManager
     * @param currentDirPath the current directory
     * @param files a list of files
     * @return the MIME type of the multiple files
     */
    public static String getMultipleMimeType(FileManageService service, String currentDirPath,
                                             List<FileInfo> files) {
        String mimeType = null;

        for (FileInfo info : files) {
            mimeType = info.getMimeTpye();
            if ((null != mimeType)
                    && (mimeType.startsWith("image/") || mimeType.startsWith("video/"))) {
                break;
            }
        }

        if (mimeType == null || mimeType.startsWith("unknown")) {
            mimeType = FileInfo.MIMETYPE_UNRECOGNIZED;
        }
        Log.d(TAG, "Multiple files' mimetype is " + mimeType);
        return mimeType;
    }

    /**
     * This method checks weather extension of certain file(not folder) is changed.
     *
     * @param newFilePath path to file before modified.(Here modify means rename).
     * @param oldFilePath path to file after modified.
     * @return true for extension changed, false for not changed.
     */
    public static boolean isExtensionChange(String newFilePath, String oldFilePath) {
        File oldFile = new File(oldFilePath);
        if (oldFile.isDirectory()) {
            return false;
        }
        String origFileExtension =  getFileExtension(oldFilePath);
        String newFileExtension =  getFileExtension(newFilePath);
        if (((origFileExtension != null) && (!origFileExtension.equals(newFileExtension)))
                || ((newFileExtension != null) && (!newFileExtension.equals(origFileExtension)))) {
            return true;
        }
        return false;
    }

    public static void startUpFileByIntent(final Context context, final FileInfo selected, MyBinder service){
        // open file here
        boolean canOpen = true;
        String mimeType = selected.getMimeTpye();
        if(mimeType == null){
            mimeType = selected.getFileMimeType(service);
        }
        Log.d(TAG,"  startUpFileByIntent:   mimeType="+mimeType +"  file ="+selected.getPath());
        if(FileInfo.MIMETYPE_EXTENSION_UNKONW.equals(mimeType)){

            canOpen = false;
            // this will select open method by choose app
            TextView title = new TextView(context);
            title.setText(context.getString(R.string.open_as));
            title.setPadding(10 ,10,10,10);
            title.setGravity(Gravity.CENTER);
            title.setTextSize(23);
            title.setTextColor(context.getResources().getColor(R.color.black));
//            TextPaint tp = title.getPaint();
//            tp.setFakeBoldText(true);
            ListView listView=new ListView(context);

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setCustomTitle(title);
            builder.setView(listView);
            final AlertDialog dialog = builder.create();
            listView.setAdapter(
                    new BaseAdapter() {
                        class ViewHolder {
                            TextView fileName;
                        }
                        private final List<String> datas = Arrays.asList(
                                context.getString(R.string.quickscan_doc),
                                context.getString(R.string.quickscan_audio),
                                context.getString(R.string.quickscan_video),
                                context.getString(R.string.quickscan_image)
                                //context.getString(R.string.quickscan_other)
                        );

                        @Override
                        public int getCount() {
                            return datas.size();
                        }

                        @Override
                        public Object getItem(int position) {
                            return datas.get(position);
                        }

                        @Override
                        public long getItemId(int position) {
                            return 0;
                        }

                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            ViewHolder vHolder = null;
                            if (convertView == null) {
                                vHolder = new ViewHolder();
                                convertView = LayoutInflater.from(parent.getContext()).inflate(
                                        R.layout.file_open_as, null);
                                vHolder.fileName = (TextView) convertView
                                        .findViewById(R.id.file_item_list_name);
                                vHolder.fileName.setText(datas.get(position));
                                convertView.setTag(vHolder);
                            } else {
                                vHolder = (ViewHolder) convertView.getTag();
                            }
                            return convertView;
                        }
                    }
            );
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    int pos = -1;
                    pos = position;
                    String mimetype="";
                    switch (pos){
                        case 0: mimetype = "text/plain";    break;
                        case 1: mimetype = "audio/basic";   break;
                        case 2: mimetype = "video/mp4";     break;
                        case 3: mimetype = "image/jpeg";    break;
                        //case 4: mimetype = "text/plain";    break;
                        default:
                            break;
                    }
                    startItentApp(selected,mimetype,context);
                    dialog.dismiss();
                }
            });

            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    dialog.dismiss();
                }
            });
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
        if (canOpen) {
            startItentApp(selected,mimeType,context);
        }
    }

    public static void startItentApp(FileInfo fileInfo, String mimeType,Context context){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = fileInfo.getUri();
        Log.d(TAG, "onItemClick,Open uri file: " + uri +" mimeType="+ mimeType);
        intent.setDataAndType(uri, mimeType);

        try {
            if( Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
                context.startActivity(intent);
            }else{
                StrictMode.class.getMethod("disableDeathOnFileUriExposure",new Class[0]).invoke(StrictMode.class,new Object[0]);
                context.startActivity(intent);
            }
        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(context, R.string.msg_unable_open_file, Toast.LENGTH_SHORT).show();
            Log.w(TAG, "onItemClick,Cannot open file: "
                    + fileInfo.getPath());
        }finally {
            try {
                if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    StrictMode.class.getMethod("enableDeathOnFileUriExposure",new Class[0]).invoke(StrictMode.class,new Object[0]);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    /**
    *  This method is used to send share files
    *  @author Xuehao.Jiang
    *  created at 2017/7/7 19:25
    */
    public static void startUpMultiShare(Context context,List<FileInfo> lists, MyBinder service){
        ArrayList<Uri> uris = new ArrayList<Uri>();
        int size = 0;
        if(lists != null){
            size = lists.size();
        }
        if( size == 0){
            Toast.makeText(context,context.getString(R.string.no_valid_file),Toast.LENGTH_LONG).show();
            return;
        }
        for(int i=0;i<size;i++){
            uris.add(lists.get(i).getUri());
        }
        boolean multiple = uris.size() > 1;
        Intent intent = new Intent(multiple ? Intent.ACTION_SEND_MULTIPLE
                : Intent.ACTION_SEND);
        if (multiple) {
            intent.setType("*/*");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        } else {
            // this will firstly get mimetype from cursor about quickscan ui
            String mimeType = lists.get(0).getMimeTpye();
            // if not cursor obtain , so need get it from MediaFile interface
            if(mimeType == null){
                mimeType = lists.get(0).getFileMimeType(service);
            }

            intent.setType(mimeType);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        }
        if( Build.VERSION.SDK_INT > Build.VERSION_CODES.M ){
            startShareFileIntent(context,intent);
        }else{
            context.startActivity(Intent.createChooser(intent, context.getResources().getString(R.string.operate_share)));
        }
    }
    /**
    *  This method is used to start up share Intent fot Fragment or Activity
    *  @author Xuehao.Jiang
    *  created at 2017/7/7 19:12
    */
    public static void startShareFileIntent(Context context, Intent intent){

        try {
            StrictMode.class.getMethod("disableDeathOnFileUriExposure",new Class[0]).invoke(StrictMode.class,new Object[0]);
            context.startActivity(
                    Intent.createChooser(intent, context.getResources().getString(R.string.operate_share)));
        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(context, R.string.msg_unable_share_file, Toast.LENGTH_SHORT).show();
            Log.w(TAG, "onItemClick,Cannot share file " );
        }finally {
            try {
                StrictMode.class.getMethod("enableDeathOnFileUriExposure",new Class[0]).invoke(StrictMode.class,new Object[0]);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     *
     * @param textView
     *            The view to be set adjust with the long string.
     */
    public static void fadeOutLongString(TextView textView) {
        if (textView == null) {
            Log.w(TAG, "#adjustWithLongString(),the view is to be set is null");
            return;
        }
        if (!(textView instanceof TextView)) {
            Log.w(TAG, "#adjustWithLongString(),the view instance is not right,execute failed!");
            return;
        }

        textView.setHorizontalFadingEdgeEnabled(true);
        textView.setSingleLine(true);
        textView.setGravity(Gravity.LEFT);
        textView.setGravity(Gravity.CENTER_VERTICAL);
    }


    public static String getTotalSize(Context context,File path){

        if(path==null){
            return "";
        }
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long total = stat.getBlockCount();
        return Formatter.formatFileSize(context, blockSize * total);
    }
    public static int getUsedPercent(File path){
        if(path==null){
            return 0;
        }
        StatFs stat = new StatFs(path.getPath());
        long total = stat.getBlockCount();
        long availableBlocks = stat.getAvailableBlocks();
        return (int)(100- availableBlocks*100/total);
    }
    public static String getAvailableSize(Context context,File path) {
        if(path==null){
            return "";
        }
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return Formatter.formatFileSize(context, blockSize * availableBlocks);
    }

    public static String getUsedSize(Context context,File path) {
        if(path==null){
            return "";
        }
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long total = stat.getBlockCount();
        long availableBlocks = stat.getAvailableBlocks();
        return Formatter.formatFileSize(context, blockSize * (total-availableBlocks));
    }

    public static int computeSampleSize(BitmapFactory.Options options,
                                        int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength,maxNumOfPixels);

        int roundedSize;
        if (initialSize <= 8 ) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options,int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = (maxNumOfPixels == -1) ? 1 :
                (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128 :
                (int) Math.min(Math.floor(w / minSideLength),
                        Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }

        if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }
    public static Bitmap readBitMap(String filePath, int type,
                                    Context context) {
        Log.d("ImageLoadTask","  readBitMap-->  file="+filePath +" type="+type);

        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = null;

        if (type == FileType.FILE_TYPE_IMAGE) {
            File tempFile = new File(filePath);
            if (tempFile.length() > MAX_IMAGE_SIZE) {
                return null;
            }
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, opt);
            long px = opt.outWidth * opt.outHeight;

            opt.inJustDecodeBounds = false;
            opt.inPreferredConfig = Bitmap.Config.RGB_565;
            opt.inPurgeable = true;
            opt.inInputShareable = true;
            Bitmap bmp = null;

            boolean noBitmap = true;
            boolean wbmp = filePath.endsWith(".wbmp");
            int sampleSize = computeSampleSize(opt, -1, 128 * 128);
            int num_tries = 0;
            if (wbmp && (opt.outWidth > MAX_IMAGE_WIDTH || opt.outHeight > MAX_IMAGE_HEIGTH)) {
                return null;
            }
            int thumbSize = context.getResources().getDimensionPixelSize(
                    R.dimen.list_item_thumbnail_size);
            while (noBitmap) {
                try {
                    Log.d(TAG, "readBitMap().try: num_tries = " + num_tries + "; filePath = " + filePath);
                    opt.inSampleSize = sampleSize;
                    bmp = BitmapFactory.decodeFile(filePath, opt);
                    bmp = ThumbnailUtils.extractThumbnail(bmp, thumbSize, thumbSize,ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                    noBitmap = false;
                } catch (OutOfMemoryError error) {
                    Log.e(TAG, "Happen OOM ", error);
                    if (++num_tries >= MAX_NUM_COMPRESSION) {
                        noBitmap = false;
                    }
                    if (bmp != null) {
                        bmp.recycle();
                        bmp = null;
                    }
                    System.gc();
                    sampleSize *= 2;
                    Log.d(TAG, "readBitMap().catch: num_tries = " + num_tries + "; filePath = " + filePath);
                }
            }
            /* @} */
            return bmp;
        } else if(type == FileType.FILE_TYPE_APK){
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(filePath,
                    PackageManager.GET_ACTIVITIES);

            if (info != null) {
                ApplicationInfo appInfo = info.applicationInfo;
                appInfo.sourceDir = filePath;
                appInfo.publicSourceDir = filePath;
                Drawable drawable = appInfo.loadIcon(pm);
                if (drawable instanceof NinePatchDrawable) {
                    Bitmap bitmap = Bitmap.createBitmap(
                            drawable.getIntrinsicWidth(),
                            drawable.getIntrinsicHeight(),
                            Bitmap.Config.RGB_565);
                    Canvas canvas = new Canvas(bitmap);
                    drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
                            drawable.getIntrinsicHeight());
                    drawable.draw(canvas);
                    return bitmap;
                } else if (drawable instanceof BitmapDrawable) {
                    BitmapDrawable bd = (BitmapDrawable) drawable;
                    return bd.getBitmap();
                } else {
                    return null;
                }
            } else {
                return null;
            }

        }else if(type == FileType.FILE_TYPE_AUDIO_DEFAULT){
            Log.d("Base","  readBitmap audio");
            Bitmap thumbnail = null;
            //能够获取多媒体文件元数据的类
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(filePath); //设置数据源
                byte[] embedPic = retriever.getEmbeddedPicture(); //得到字节型数据
                thumbnail = BitmapFactory.decodeByteArray(embedPic, 0, embedPic.length); //转换为图片
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    retriever.release();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
            return thumbnail;

        }else if(type ==FileType.FILE_TYPE_VIDEO_DEFAULT){
            Log.d("Base","  readBitmap video");
            Bitmap thumbnail = null;
            String[] mediaColums = new String[]{MediaStore.Video.Media._ID};
            String whereClause = MediaStore.Video.Media.DATA + " = \"" + filePath + "\"";
            cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,mediaColums,whereClause,null,null );
            if (cursor != null && cursor.getCount() != 0) {
                cursor.moveToFirst();
                int id = cursor.getColumnIndex(MediaStore.Video.Media._ID);
                thumbnail = MediaStore.Video.Thumbnails.getThumbnail(resolver, id, MediaStore.Video.Thumbnails.MICRO_KIND, null);
            }
            Log.d("Base","  readBitmap video ="+( thumbnail==null? "null":"non-null"));
            return thumbnail;
        }else {
            return null;
        }
    }

    public void setDialog(Context context){
        final AlertDialog mDialog=new AlertDialog.Builder(context)
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null).create();
        mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positionButton=mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                Button negativeButton=mDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                positionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDialog.dismiss();
                    }
                });
                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 不消失
                    }
                });
            }
        });
        mDialog.show();
    }
}
