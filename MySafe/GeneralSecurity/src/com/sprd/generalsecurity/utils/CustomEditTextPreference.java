/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sprd.generalsecurity.utils;

import android.content.Context;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sprd.generalsecurity.R;

import java.text.DecimalFormat;

public class CustomEditTextPreference extends DialogPreference {

    Context mContext;

    private final LinearLayout layout = new LinearLayout(this.getContext());
    private final EditText mEditText = new EditText(this.getContext());
    private final TextView tv = new TextView(this.getContext());
    private final static long MAX_VALUE = 5000000;// max value, about 5000GB
    private final static String ZERO_DATA_FLOW = "0";
    private final static String UNIT_DATA_FLOW = " MB";
    private final static Character POINT = '.';
    private final static Character ZERO = '0';

    public CustomEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        setPersistent(true);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //limit the input number as 2 number at most after the point
                int index = s.toString().indexOf('.');
                if (index > 0) {
                    if (s.toString().length() - index > 3) {
                        mEditText.setText(s.toString().substring(0, index + 3));
                        mEditText.setSelection(mEditText.getText().length());
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String editable = s.toString();
                String standardValue = String.valueOf(MAX_VALUE);

                //disallow 0 as the first number if it is not include '.' .
                if(standardValue.equalsIgnoreCase(editable)){
                    //avoid loop call.
                    return;
                }

                //float values are not exact, so just check the numbers before '.' .
                if (editable.indexOf('.') > 0) {
                    String tmp = editable.substring(0, editable.indexOf('.'));
                    if (tmp.equalsIgnoreCase(standardValue)) {
                        mEditText.setText(standardValue);
                        mEditText.setSelection(mEditText.getText().length());
                        Toast.makeText(mContext,R.string.max_data_flow_value,Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                //disallow 0 as the first number if it is not include '.' .
                if (mEditText.getText().length() >= 2 && mEditText.getText().charAt(0) == '0') {
                    if (mEditText.getText().charAt(1) != '.') {
                        mEditText.setText(mEditText.getText().toString().substring(1));
                        mEditText.setSelection(mEditText.getText().length());
                    }
                }

                // disallow input as '.' only
                if (mEditText.getText().toString().length() == 1 && mEditText.getText().charAt(0) == '.') {
                    mEditText.setText("");
                }

                if (TextUtils.isEmpty(mEditText.getText().toString()) == false) {
                    if (Float.parseFloat(mEditText.getText().toString()) > MAX_VALUE) {
                        mEditText.setText(standardValue);
                        mEditText.setSelection(mEditText.getText().length());
                        Toast.makeText(mContext,R.string.max_data_flow_value,Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    public CustomEditTextPreference(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.editTextPreferenceStyle);
    }

    public CustomEditTextPreference(Context context) {
        this(context, null);
    }


    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

    }

    @Override
    protected View onCreateDialogView() {
        mEditText.setWidth(200);
        mEditText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
        /**
         * SPRD: Bug702762 must call removeView() on the child's parent first @{
         */
        if (layout.getParent() != null) {
            ((ViewGroup) layout.getParent()).removeView(layout);
        }
        layout.removeAllViews();
        if (mEditText.getParent() != null) {
            ((ViewGroup) mEditText.getParent()).removeView(mEditText);
        }
        layout.addView(mEditText);
        if (tv.getParent() != null) {
            ((ViewGroup) tv.getParent()).removeView(tv);
        }
        layout.addView(tv);
        /**
         * @}
         */
        return layout;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        mEditText
                .setText(
                        (getPersistedString(ZERO_DATA_FLOW)
                                .equalsIgnoreCase("") ? ZERO_DATA_FLOW
                                : getPersistedString(ZERO_DATA_FLOW)),
                        TextView.BufferType.NORMAL);
        mEditText.setSelection(mEditText.getText().length());
        tv.setText(UNIT_DATA_FLOW);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        String editText = formatString(mEditText.getText().toString());
        if (positiveResult && shouldPersist()) {
            persistString(editText);
            setSummary(editText + UNIT_DATA_FLOW);
        }

        /**
         * SPRD: Bug705771 it occured crash probably when enter the dataflow limit @{
         */
        if(mEditText.getParent() != null){
            ((ViewGroup) mEditText.getParent()).removeView(mEditText);
        }
        if(tv.getParent() != null){
            ((ViewGroup) tv.getParent()).removeView(tv);
        }
        if(layout.getParent() != null){
            ((ViewGroup) layout.getParent()).removeView(layout);
        }
        /**
         * @}
         */
        callChangeListener(editText);
    }

    private String formatString(String summary) {
        if (summary != null && !summary.equalsIgnoreCase("")) {
            Float f = Float.parseFloat(summary);
            summary = new DecimalFormat("#.##").format(f);
        } else {
            summary = ZERO_DATA_FLOW;
        }

        return summary;
    }
}