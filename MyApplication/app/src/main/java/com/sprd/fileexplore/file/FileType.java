package com.sprd.fileexplore.file;


import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.sprd.fileexplore.R;
import com.sprd.fileexplore.util.FileUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Created by Xuehao.Jiang on 2017/4/6.
 */

public class FileType {

    private static final String TAG = FileType.class.getSimpleName();

    public static final  String FIEL_TYPE = "file_type";
    public static final int FILE_TYPE_INVALID = -1;
    /**
    *  Those are basic datas type class
    *  @author Xuehao.Jiang
    *  created at 2017/7/11 15:38
    */
    public static final int FILE_TYPE_UNKNOWN = R.mipmap.file_item_default_ic;   //means others
    public static final int FILE_TYPE_IMAGE = R.mipmap.file_item_image_ic;
    public static final int FILE_TYPE_DOC = R.mipmap.file_item_doc_ic;
    public static final int FILE_TYPE_APK = R.mipmap.file_item_apk_default_ic;
    public static final int FILE_TYPE_VIDEO_DEFAULT = R.mipmap.file_item_video_ic;
    public static final int FILE_TYPE_AUDIO_DEFAULT = R.mipmap.file_item_audio_ic;
    /* end */

    public static final int FILE_TYPE_FOLDER = R.mipmap.file_item_folder_ic;

    public static final int FILE_TYPE_VCARD = R.mipmap.file_vcard_ic;
    public static final int FILE_TYPE_VCALENDER = R.mipmap.file_vcalender_ic;

    public static final int FILE_TYPE_WEBTEXT = R.mipmap.file_item_web_ic;
    public static final int FILE_TYPE_TEXT = R.mipmap.file_doc_txt_ic;
    public static final int FILE_TYPE_WORD = R.mipmap.file_doc_word_ic;
    public static final int FILE_TYPE_EXCEL = R.mipmap.file_doc_excel_ic;
    public static final int FILE_TYPE_PPT = R.mipmap.file_doc_ppt_ic;
    public static final int FILE_TYPE_PDF = R.mipmap.file_doc_pdf_ic;
    public static final int FILE_TYPE_VIDEO_MP4 = R.mipmap.file_video_mp4_ic;
    public static final int FILE_TYPE_VIDEO_MKV = R.mipmap.file_video_mkv_ic;
    public static final int FILE_TYPE_VIDEO_RMVB = R.mipmap.file_video_rmvb_ic;
    public static final int FILE_TYPE_VIDEO_3GP = R.mipmap.file_video_3gp_ic;
    public static final int FILE_TYPE_VIDEO_AVI = R.mipmap.file_video_avi_ic;
    public static final int FILE_TYPE_VIDEO_MPEG = R.mipmap.file_video_mpeg_ic;
    public static final int FILE_TYPE_VIDEO_FLV = R.mipmap.file_video_flv_ic;
    public static final int FILE_TYPE_VIDEO_ASF = R.mipmap.file_video_asf_ic;
    public static final int FILE_TYPE_VIDEO_DIVX = R.mipmap.file_video_divx_ic;
    public static final int FILE_TYPE_VIDEO_MPE = R.mipmap.file_video_mpe_ic;
    public static final int FILE_TYPE_VIDEO_MPG = R.mipmap.file_video_mpg_ic;
    public static final int FILE_TYPE_VIDEO_RM = R.mipmap.file_video_rm_ic;
    public static final int FILE_TYPE_VIDEO_VOB = R.mipmap.file_video_vob_ic;
    public static final int FILE_TYPE_VIDEO_WMV = R.mipmap.file_video_wmv_ic;
    public static final int FILE_TYPE_VIDEO_M4V = R.mipmap.file_video_m4v_ic;
    public static final int FILE_TYPE_VIDEO_F4V = R.mipmap.file_video_f4v_ic;
    public static final int FILE_TYPE_VIDEO_WEBM = R.mipmap.file_video_webm_ic;
    public static final int FILE_TYPE_VIDEO_3G2 = R.mipmap.file_video_3g2_ic;
    public static final int FILE_TYPE_VIDEO_TS = R.mipmap.file_video_ts_ic;
    public static final int FILE_TYPE_VIDEO_M2TS = R.mipmap.file_video_m2ts_ic;
    public static final int FILE_TYPE_VIDEO_MOV = R.mipmap.file_video_mov_ic;

    public static final int FILE_TYPE_AUDIO_MP3 = R.mipmap.file_audio_mp3_ic;
    public static final int FILE_TYPE_AUDIO_OGG = R.mipmap.file_audio_ogg_ic;
    public static final int FILE_TYPE_AUDIO_OGA = R.mipmap.file_audio_oga_ic;
    public static final int FILE_TYPE_AUDIO_ACC = R.mipmap.file_audio_acc_ic;
    public static final int FILE_TYPE_AUDIO_WAV = R.mipmap.file_audio_wav_ic;
    public static final int FILE_TYPE_AUDIO_WMA = R.mipmap.file_audio_wma_ic;
    public static final int FILE_TYPE_AUDIO_AMR = R.mipmap.file_audio_amr_ic;
    public static final int FILE_TYPE_AUDIO_AIFF = R.mipmap.file_audio_aiff_ic;
    public static final int FILE_TYPE_AUDIO_APE = R.mipmap.file_audio_ape_ic;
    public static final int FILE_TYPE_AUDIO_AV = R.mipmap.file_audio_av_ic;
    public static final int FILE_TYPE_AUDIO_CD = R.mipmap.file_audio_cd_ic;
    public static final int FILE_TYPE_AUDIO_MIDI = R.mipmap.file_audio_midi_ic;
    public static final int FILE_TYPE_AUDIO_VQF = R.mipmap.file_audio_vqf_ic;
    public static final int FILE_TYPE_AUDIO_AAC = R.mipmap.file_audio_aac_ic;
    public static final int FILE_TYPE_AUDIO_MID = R.mipmap.file_audio_mid_ic;
    public static final int FILE_TYPE_AUDIO_M4A = R.mipmap.file_audio_m4a_ic;
    public static final int FILE_TYPE_AUDIO_IMY = R.mipmap.file_audio_imy_ic;
    /* SPRD 435235 @{ */
    public static final int FILE_TYPE_AUDIO_MP4 = R.mipmap.file_audio_mp4_ic;
    public static final int FILE_TYPE_AUDIO_3GPP = R.mipmap.file_audio_3gpp_ic;
    /* @} */
    // SPRD 435235
    public static final int FILE_TYPE_AUDIO_3GP = R.mipmap.file_audio_3gp_ic;
    // SPRD: Add for bug507035.
    public static final int FILE_TYPE_AUDIO_3G2 = R.mipmap.file_audio_3g2_ic;
    // SPRD 498509
    public static final int FILE_TYPE_AUDIO_OPUS = R.mipmap.file_audio_opus_ic;
    /* SPRD 457501 @{ */
    public static final int FILE_TYPE_AUDIO_AWB = R.mipmap.file_audio_awb_ic;
    public static final int FILE_TYPE_AUDIO_FLAC = R.mipmap.file_audio_flac_ic;
    /* @} */
    // SPRD: Add for bug510953.
    public static final int FILE_TYPE_AUDIO_MKA = R.mipmap.file_audio_mka_ic;
    /* SPRD: Add for bug511015. @{ */
    public static final int FILE_TYPE_AUDIO_M4B = R.mipmap.file_audio_m4b_ic;
    public static final int FILE_TYPE_AUDIO_M4R = R.mipmap.file_audio_m4r_ic;

    /* SPRD: Add for bug663872. @{ */
    public static final int FILE_TYPE_AUDIO_MKV = R.mipmap.file_audio_mkv_ic;
    /* @} */
    public static final int FILE_TYPE_UNKNOE = R.mipmap.file_item_default_ic;

    private static Set<String> mImageFileType = new HashSet<String>();

    private static Set<String> mAudioFileType = new HashSet<String>();

    private static Set<String> mVideoFileType = new HashSet<String>();

    private static Set<String> mDocFileType = new HashSet<String>();

    private static Set<String> mTextFileType = new HashSet<String>();

    private static Set<String> mWordFileType = new HashSet<String>();

    private static Set<String> mExcelFileType = new HashSet<String>();

    private static Set<String> mPPTFileType = new HashSet<String>();

    private static Set<String> mWebTextFileType = new HashSet<String>();

    private static Set<String> mPdfFileType = new HashSet<String>();

    private static Set<String> mPackageFileType = new HashSet<String>();

    private static Set<String> mVcardFileType = new HashSet<String>();

    private static Set<String> mVcalenderFileType = new HashSet<String>();


    private static Context mContext;
    private Resources resource;
    private static final FileType ft = new FileType();
    public static FileType getInstance(){
        return ft;
    }

    public void init(Context context){

        mContext= context;
        resource = context.getResources();
        initReflect();

        for (String s : resource.getStringArray(R.array.ImageFileType)) {
            mImageFileType.add(s);
        }
        for (String s : resource.getStringArray(R.array.AudioFileType)) {
            mAudioFileType.add(s);
        }
        for (String s : resource.getStringArray(R.array.VideoFileType)) {
            mVideoFileType.add(s);
        }
        for (String s : resource.getStringArray(R.array.TextFileType)) {
            mTextFileType.add(s);
        }
        for (String s : resource.getStringArray(R.array.WebTextFileType)) {
            mWebTextFileType.add(s);
        }
        for (String s : resource.getStringArray(R.array.WordFileType)) {
            mWordFileType.add(s);
        }
        for (String s : resource.getStringArray(R.array.ExcelFileType)) {
            mExcelFileType.add(s);
        }
        for (String s : resource.getStringArray(R.array.PackageType)) {
            mPackageFileType.add(s);
        }
        for (String s : resource.getStringArray(R.array.PdfFileType)) {
            mPdfFileType.add(s);
        }
        for (String s : resource.getStringArray(R.array.PPTFileType)) {
            mPPTFileType.add(s);
        }
        for (String s : resource.getStringArray(R.array.VcardType)) {
            mVcardFileType.add(s);
        }
        for (String s : resource.getStringArray(R.array.VcalenderType)) {
            mVcalenderFileType.add(s);
        }
        mDocFileType.addAll(mTextFileType);
        mDocFileType.addAll(mWordFileType);
        mDocFileType.addAll(mPdfFileType);
        mDocFileType.addAll(mPPTFileType);
        mDocFileType.addAll(mExcelFileType);
    }

    public static String getSuffix(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return null;
        }
        String fileName = file.getName();
        if (fileName.equals("") || fileName.endsWith(".")) {
            return null;
        }
        int index = fileName.lastIndexOf(".");
        if (index != -1) {
            return fileName.substring(index).toLowerCase(Locale.US);
        } else {
            return null;
        }
    }

    public int getBasicFileType(String mimeType,FileInfo fileInfo){
        String suffic = getSuffix(fileInfo.getFile());
        Log.d(TAG," getBasicFileType   mimeType="+mimeType +" suffic="+suffic);

        if( mimeType != null && mimeType.startsWith("image/")){
            return FILE_TYPE_IMAGE;
        }else if( suffic!= null && mPackageFileType.contains(suffic) ){
            return FILE_TYPE_APK;
        }else if(mimeType != null &&  mimeType.startsWith("video/")){
            return FILE_TYPE_VIDEO_DEFAULT;
        }else if(mimeType != null &&  mimeType.startsWith("audio/")){
            return FILE_TYPE_AUDIO_DEFAULT;
        }else if(mDocFileType.contains(suffic)){
            return FILE_TYPE_DOC;
        }
        // for other files
        return FILE_TYPE_UNKNOE;
    }
    public int getFileType(File file) {

        String suffix = getSuffix(file);
        Log.d(TAG," getFileType= "+file +"  suffix="+suffix);
        if (suffix == null) {
            return FILE_TYPE_UNKNOE;
        } else if (mImageFileType.contains(suffix)) {
            return FILE_TYPE_IMAGE;
        } else if (mVideoFileType.contains(suffix)) {
            return getVideoFileIcon(suffix);
        } else if (mAudioFileType.contains(suffix)) {
            return getAudioFileIcon(suffix);
        } else if (mTextFileType.contains(suffix)) {
            return FILE_TYPE_TEXT;
        } else if (mWebTextFileType.contains(suffix)) {
            return FILE_TYPE_WEBTEXT;
        } else if (mWordFileType.contains(suffix)) {
            return FILE_TYPE_WORD;
        } else if (mExcelFileType.contains(suffix)) {
            return FILE_TYPE_EXCEL;
        } else if (mPPTFileType.contains(suffix)) {
            return FILE_TYPE_PPT;
        } else if (mPdfFileType.contains(suffix)) {
            return FILE_TYPE_PDF;
        } else if (mWebTextFileType.contains(suffix)) {
            return FILE_TYPE_WEBTEXT;
        } else if (mPackageFileType.contains(suffix)) {
            return FILE_TYPE_APK;
        } else if (mVcardFileType.contains(suffix)) {
            return FILE_TYPE_VCARD;
        } else if (mVcalenderFileType.contains(suffix)) {
            return FILE_TYPE_VCALENDER;
        } else{
            return FILE_TYPE_UNKNOE;
        }
    }
    public int getAudioFileIcon(String suffix) {
        if (suffix == null || !mAudioFileType.contains(suffix)) {
            return R.mipmap.file_item_audio_ic;
        } else if (suffix.equals(".mp3")) {
            return FILE_TYPE_AUDIO_MP3;
        } else if (suffix.equals(".ogg")) {
            return FILE_TYPE_AUDIO_OGG;
        } else if (suffix.equals(".oga")) {
            return FILE_TYPE_AUDIO_OGA;
        } else if (suffix.equals(".wma")) {
            return FILE_TYPE_AUDIO_WMA;
        } else if (suffix.equals(".wav")) {
            return FILE_TYPE_AUDIO_WAV;
        } else if (suffix.equals(".acc")) {
            return FILE_TYPE_AUDIO_ACC;
        } else if (suffix.equals(".amr")) {
            return FILE_TYPE_AUDIO_AMR;
        } else if (suffix.equals(".aiff")) {
            return FILE_TYPE_AUDIO_AIFF;
        } else if (suffix.equals(".ape")) {
            return FILE_TYPE_AUDIO_APE;
        } else if (suffix.equals(".av")) {
            return FILE_TYPE_AUDIO_AV;
        } else if (suffix.equals(".cd")) {
            return FILE_TYPE_AUDIO_CD;
        } else if (suffix.equals(".midi")) {
            return FILE_TYPE_AUDIO_MIDI;
        } else if (suffix.equals(".aac")) {
            return FILE_TYPE_AUDIO_AAC;
        } else if (suffix.equals(".mid")) {
            return FILE_TYPE_AUDIO_MID;
        } else if (suffix.equals(".m4a")) {
            return FILE_TYPE_AUDIO_M4A;
        } else if (suffix.equals(".vqf")) {
            return FILE_TYPE_AUDIO_VQF;
        } else if (suffix.equals(".imy")) {
            return FILE_TYPE_AUDIO_IMY;
        /* SPRD 435235 @{ */
        } else if (suffix.equals(".mp4")) {
            return FILE_TYPE_AUDIO_MP4;
        } else if (suffix.equals(".3gpp")) {
            return FILE_TYPE_AUDIO_3GPP;
        /* @} */
        /* SPRD 451527 @{ */
        } else if (suffix.equals(".3gp")) {
            return FILE_TYPE_AUDIO_3GP;
        /* @} */
        /* SPRD: Add for bug507035. @{ */
        } else if (suffix.equals(".3g2")) {
            return FILE_TYPE_AUDIO_3G2;
        /* @} */
        /* SPRD 498509 @{ */
        } else if (suffix.equals(".opus")) {
            return FILE_TYPE_AUDIO_OPUS;
        /* @} */
        /* SPRD 457501 @{ */
        } else if (suffix.equals(".awb")) {
            return FILE_TYPE_AUDIO_AWB;
        } else if (suffix.equals(".flac")) {
            return FILE_TYPE_AUDIO_FLAC;
        /* @} */
        /* SPRD: Add for bug510953. @{ */
        } else if (suffix.equals(".mka")) {
            return FILE_TYPE_AUDIO_MKA;
        /* @} */
        /* SPRD: Add for bug511015. @{ */
        } else if (suffix.equals(".m4b")) {
            return FILE_TYPE_AUDIO_M4B;
        } else if (suffix.equals(".m4r")) {
            return FILE_TYPE_AUDIO_M4R;
        /* @} */
        /* SPRD: Add for bug663872. @{ */
        } else if (suffix.equals(".mkv")) {
            return FILE_TYPE_AUDIO_MKV;
        /* @} */
        } else {
            return R.mipmap.file_item_audio_ic;
        }
    }
    public int getVideoFileIcon(String suffix) {
        if (suffix == null || !mVideoFileType.contains(suffix)) {
            return R.mipmap.file_item_video_ic;
        } else if (suffix.equals(".mp4")) {
            return FILE_TYPE_VIDEO_MP4;
        } else if (suffix.equals(".3gp")) {
            return FILE_TYPE_VIDEO_3GP;
        /* SPRD: Add for bug507035. @{ */
        } else if (suffix.equals(".3g2")) {
            return FILE_TYPE_VIDEO_3G2;
        /* @} */
        /* SPRD: Add for bug611635. @{ */
        } else if (suffix.equals(".m2ts")) {
            return FILE_TYPE_VIDEO_M2TS;
        } else if (suffix.equals(".mov")) {
            return FILE_TYPE_VIDEO_MOV;
        /* @} */
        } else if (suffix.equals(".avi")) {
            return FILE_TYPE_VIDEO_AVI;
        } else if (suffix.equals(".flv")) {
            return FILE_TYPE_VIDEO_FLV;
        } else if (suffix.equals(".rmvb")) {
            return FILE_TYPE_VIDEO_RMVB;
        } else if (suffix.equals(".mkv")) {
            return FILE_TYPE_VIDEO_MKV;
        } else if (suffix.equals(".mpeg")) {
            return FILE_TYPE_VIDEO_MPEG;
        } else if (suffix.equals(".asf")) {
            return FILE_TYPE_VIDEO_ASF;
        } else if (suffix.equals(".divx")) {
            return FILE_TYPE_VIDEO_DIVX;
        } else if (suffix.equals(".mpe")) {
            return FILE_TYPE_VIDEO_MPE;
        } else if (suffix.equals(".mpg")) {
            return FILE_TYPE_VIDEO_MPG;
        /* SPRD: Modify for bug498813. @{
        } else if (suffix.equals(".rm")) {
            return FILE_TYPE_VIDEO_RM;
        @} */
        } else if (suffix.equals(".vob")) {
            return FILE_TYPE_VIDEO_VOB;
        } else if (suffix.equals(".wmv")) {
            return FILE_TYPE_VIDEO_WMV;
            //SPRD : Add for 496451
        } else if (suffix.equals(".ts")) {
            return FILE_TYPE_VIDEO_TS;
        /* SPRD 452695 @{ */
        } else if (suffix.equals(".m4v")) {
            return FILE_TYPE_VIDEO_M4V;
        } else if (suffix.equals(".f4v")) {
            return FILE_TYPE_VIDEO_F4V;
        } else if (suffix.equals(".webm")) {
            return FILE_TYPE_VIDEO_WEBM;
        } else {
            return R.mipmap.file_item_video_ic;
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////
    ///     This will get file media type by reflect method using MediaFile Class                ///
    ////////////////////////////////////////////////////////////////////////////////////////////////

    Class<?> mMediaFile, mMediaFileType;
    Method getFileTypeMethod, isAudioFileTypeMethod, isVideoFileTypeMethod, isImageFileTypeMethod;
    String methodName = "getBoolean";
    String getFileType = "getFileType";

    String isAudioFileType = "isAudioFileType";
    String isVideoFileType = "isVideoFileType";
    String isImageFileType = "isImageFileType";
    Field fileType;

    public void initReflect() {
        try {
            mMediaFile = Class.forName("android.media.MediaFile");
            mMediaFileType = Class.forName("android.media.MediaFile$MediaFileType");

            fileType = mMediaFileType.getField("fileType");
            getFileTypeMethod = mMediaFile.getMethod(getFileType, String.class);
            isAudioFileTypeMethod = mMediaFile.getMethod(isAudioFileType, int.class);
            isVideoFileTypeMethod = mMediaFile.getMethod(isVideoFileType, int.class);
            isImageFileTypeMethod = mMediaFile.getMethod(isImageFileType, int.class);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

    }

    public int getMediaFileType(String path) {

        int type = FILE_TYPE_INVALID;
        try {
            Object obj = getFileTypeMethod.invoke(mMediaFile, path);
            if (obj != null) {
                type = fileType.getInt(obj);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return type;
    }

    private boolean isAudioFile(int fileType) {
        boolean isAudioFile = false;
        try {
            isAudioFile = (Boolean) isAudioFileTypeMethod.invoke(mMediaFile, fileType);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return isAudioFile;
    }

    private boolean isVideoFile(int fileType) {
        boolean isVideoFile = false;
        try {
            isVideoFile = (Boolean) isVideoFileTypeMethod.invoke(mMediaFile, fileType);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return isVideoFile;
    }

    private boolean isImageFile(int fileType) {
        boolean isImageFile = false;
        try {
            isImageFile = (Boolean) isImageFileTypeMethod.invoke(mMediaFile, fileType);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return isImageFile;
    }


    public boolean isAudioFileType(String path){
        int type = getMediaFileType(path);
        return isAudioFile(type);
    }
    public boolean isVideoFileType(String path){
        int type = getMediaFileType(path);
        return isVideoFile(type);
    }
    public boolean isImageFileType(String path){
        int type = getMediaFileType(path);
        return isImageFile(type);
    }
}
