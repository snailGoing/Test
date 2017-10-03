package com.sprd.fileexplore.adapter;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Created by Xuehao.Jiang on 2017/5/10.
 */

public class BanViewPager extends ViewPager {

    private boolean isCanScroll = true;
    private static final String TAG= BanViewPager.class.getSimpleName();

    public BanViewPager(Context context) {
        super(context);
    }

    public BanViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public void setNoScroll(boolean noScroll) {

        this.isCanScroll = !noScroll;
        Log.d(TAG," setNoScroll---> isCanScroll="+isCanScroll);
    }

    @Override
    public boolean onTouchEvent(MotionEvent arg0) {
        Log.d(TAG," onTouchEvent---> isCanScroll="+isCanScroll);
        if (!isCanScroll){
            return false;
        }else{
            return super.onTouchEvent(arg0);
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(x, y);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent arg0) {
        Log.d(TAG," onInterceptTouchEvent---> isCanScroll="+isCanScroll);
        if (!isCanScroll){
            return false;
        }else{
            return super.onInterceptTouchEvent(arg0);
        }
    }
}
