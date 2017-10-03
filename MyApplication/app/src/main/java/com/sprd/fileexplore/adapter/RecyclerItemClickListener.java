package com.sprd.fileexplore.adapter;

import android.view.View;

/**
 * Created by Xuehao.Jiang on 2017/4/14.
 */

public interface RecyclerItemClickListener {


    public void onItemClick(View v, int position);
    public void onItemLongClick(View v, int position);
}
