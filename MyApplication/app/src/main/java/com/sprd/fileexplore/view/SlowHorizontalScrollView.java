package com.sprd.fileexplore.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;
import android.widget.Scroller;

/**
 * Created by Xuehao.Jiang on 2017/6/29.
 */

public class SlowHorizontalScrollView extends HorizontalScrollView {
    private static final String TAG = "SlowHorizontalScrollView";
    private static final int SCROLL_DURATION = 2000;
    private final Scroller mScroller = new Scroller(getContext());

    public SlowHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SlowHorizontalScrollView(Context context) {
        super(context);
    }

    public SlowHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressLint("LongLogTag")
    public void startHorizontalScroll(int startX, int dx) {
        Log.d(TAG, "start scroll");
        mScroller.startScroll(startX, 0, dx, 0, SCROLL_DURATION);
        invalidate();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), 0);
            postInvalidate();
        }
        super.computeScroll();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mScroller.abortAnimation();
        return super.onTouchEvent(ev);
    }
}
