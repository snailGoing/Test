// IRemoteServiceCallback.aidl
package com.sprd.fileexplore.aidl;

// Declare any non-default types here with import statements

oneway interface IRemoteServiceCallback {

   /**
    *   this will be called when service has finished load-image task to notify
    *   different progress activity refreshing UI
    * */
//    void OnImageDrawableLoadComplete(String fileUrl, boolean success,
//                                   int imageType, Bitmap bitmap, int pos);
    void OnImageDrawableLoadComplete(String path,int imageType,int pos);
}
