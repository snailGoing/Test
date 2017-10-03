package com.sprd.fileexplore.view;

/**
 * Created by Xuehao.Jiang on 2017/4/6.
 */

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sprd.fileexplore.R;

public class ImageViewText extends LinearLayout {

    private CharSequence mTitle;
    private Drawable mDrawable;

    private ImageView mImageView;
    private TextView mTiltleTextView;
    private TextView mCountTextView;
    private int mId;
    private Context mContext;

    public ImageViewText(Context context) {
        this(context, null);
        // TODO
    }

    public ImageViewText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        // TODO
    }

    public ImageViewText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        // TODO
        setGravity(Gravity.CENTER);
        setOrientation(LinearLayout.VERTICAL);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ImageViewText);
        mTitle = a.getText(R.styleable.ImageViewText_text);
        mDrawable = a.getDrawable(R.styleable.ImageViewText_image);
        a.recycle();   //recycle

        mId = getId();
        mImageView = new ImageView(context);
        mImageView.setScaleType(ScaleType.CENTER);
        mImageView.setImageDrawable(mDrawable);

        mTiltleTextView = new TextView(context);
        mTiltleTextView.setText(mTitle);
        mTiltleTextView.setGravity(Gravity.CENTER_HORIZONTAL);

        mCountTextView = new TextView(context);
        mCountTextView.setText(String.valueOf(0));
        mCountTextView.setGravity(Gravity.CENTER_HORIZONTAL);

        addView(mImageView);
        addView(mTiltleTextView);
        addView(mCountTextView);
    }

    public void setSummary(int num) {
        mCountTextView.setText(String.valueOf(num)+ mContext.getString(R.string.item_sum_summary));
    }
    public void setLoadingSummary(){
        mCountTextView.setText(mContext.getString(R.string.item_sum_loading_summary));
    }
}


