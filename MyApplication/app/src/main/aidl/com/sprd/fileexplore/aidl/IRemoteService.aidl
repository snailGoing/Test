// IRemoteService.aidl
package com.sprd.fileexplore.aidl;

import com.sprd.fileexplore.aidl.IRemoteServiceCallback;

// Declare any non-default types here with import statements

 interface IRemoteService {

    /**
    *
    *   filePath :  need to analysis file path
    *   imageType:  the file type to resolve different image drawable
    *   priority:  measn postision in adapter and loading icon prority
    * */
     void loadImagesWithPath(String filePath, int imageType,IRemoteServiceCallback cb, int priority);
}
