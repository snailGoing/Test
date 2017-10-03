package com.sprd.fileexplore.fragment;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.widget.Toast;

import com.sprd.fileexplore.R;
import com.sprd.fileexplore.activity.SettingActivity;

/**
 * Created by Xuehao.Jiang on 2017/4/6.
 */

public abstract class BaseFragment extends Fragment {
    protected boolean mEnableExit;

    /**
     * Our fragment has onBackPressed now, we can use it to pop folder
     *
     * @return return if allow finish the activity
     */
    abstract public boolean onBackPressed();

    //public abstract int getIcon();

    //abstract public RecyclerView.Adapter getAdapter();

}
