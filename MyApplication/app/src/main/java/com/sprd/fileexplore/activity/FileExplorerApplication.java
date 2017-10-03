package com.sprd.fileexplore.activity;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;


import com.sprd.fileexplore.file.FileInfoComparator;
import com.sprd.fileexplore.file.FileType;
import com.sprd.fileexplore.service.FileManageService;
import com.sprd.fileexplore.util.LogUtil;

/**
 * Created by Xuehao.Jiang on 2017/5/22.
 */

public class FileExplorerApplication extends Application {
    public static final String TAG = "FileExplorerApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG," onCreate... ");
        FileType ft = FileType.getInstance();
        ft.init(this);

        /*
        *  We need get sort type from data base "sharePreferences",
        *  we just update it when set sort type by hand.
        *  If this app not die, wo can alse get this "sortType" from
        *  sinagle instance "FileInfoComparator" which contains it by filed " mSortType".
        *
        * */
        final SharedPreferences sortSpf = this
                .getSharedPreferences(FileInfoComparator.SORT_KEY, 0);
        int sortType = sortSpf.getInt(FileInfoComparator.SORT_KEY, FileInfoComparator.SORT_BY_NAME);
        // init sort class by default: SORT_BY_NAME
        FileInfoComparator.getInstance(sortType);
        Intent intent = new Intent(this, FileManageService.class);
        startService(intent);

    }
}
