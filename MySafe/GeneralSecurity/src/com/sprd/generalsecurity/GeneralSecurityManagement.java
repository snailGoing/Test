package com.sprd.generalsecurity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sprd.generalsecurity.network.FloatViewService;
import com.sprd.generalsecurity.optimize.DefaultFragment;
import com.sprd.generalsecurity.optimize.OptimizeResultFragment;
import com.sprd.generalsecurity.utils.Contract;

public class GeneralSecurityManagement extends Activity implements View.OnClickListener,
        OptimizeResultFragment.Listener {
    private static final String TAG_OPTIMIZE_RESULT = "optimize_result";
    private static final String TAG_DEFAULT_FRAGMENT = "default_fragment";
    private static final String IS_OPTIMIZING = "is_optimizing";
    private static final String NEED_OPTIMIZE = "need_optimize";
    private static final String KEY_CURRENT_SCORE = "current_score";
    private static final String TAG = "GeneralSecurityManagement";
    public TextView mScoreText;
    public TextView mPromptText;
    public Button mStart;
    private DefaultFragment mDefaultFragment;
    private OptimizeResultFragment mOptimizeFragment;
    public boolean mOptimizing = false;
    public boolean mNeedOptimize = false;
    private int mCurrentScore;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry_main);
        if (ActivityManager.getCurrentUser() != UserHandle.USER_SYSTEM) {
            PackageManager pm = getPackageManager();
            Toast.makeText(this,
                    getResources().getString(R.string.use_gs_in_owner),
                    Toast.LENGTH_LONG).show();
            pm.setComponentEnabledSetting(new ComponentName(GeneralSecurityManagement.this, GeneralSecurityManagement.class),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            finish();
            return;
        }
        mScoreText = (TextView) findViewById(R.id.score_text);
        mPromptText = (TextView) findViewById(R.id.prompt_text);
        mStart = (Button) findViewById(R.id.start);
        mStart.setOnClickListener(this);
        if (savedInstanceState == null) {
            mDefaultFragment = new DefaultFragment();
            mOptimizeFragment = new OptimizeResultFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, mDefaultFragment, TAG_DEFAULT_FRAGMENT)
                    .add(R.id.fragment_container, mOptimizeFragment, TAG_OPTIMIZE_RESULT)
                    .hide(mOptimizeFragment)
                    .commitAllowingStateLoss();
            mNeedOptimize = false;
            mCurrentScore = Contract.MAX_SCORE;
        } else {
            mDefaultFragment = (DefaultFragment) getFragmentManager()
                    .findFragmentByTag(TAG_DEFAULT_FRAGMENT);
            mOptimizeFragment = (OptimizeResultFragment) getFragmentManager()
                    .findFragmentByTag(TAG_OPTIMIZE_RESULT);
            mOptimizing = savedInstanceState.getBoolean(IS_OPTIMIZING);
            final FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            if (mOptimizing) {
                if (mOptimizeFragment == null) {
                    mOptimizeFragment = new OptimizeResultFragment();
                    fragmentTransaction.add(R.id.fragment_container,
                            mOptimizeFragment, TAG_OPTIMIZE_RESULT)
                            .hide(mDefaultFragment);
                } else {
                    fragmentTransaction.hide(mDefaultFragment).show(mOptimizeFragment);
                }
                mNeedOptimize = true;
                mCurrentScore = savedInstanceState.getInt(KEY_CURRENT_SCORE, Contract.MAX_SCORE);
            } else {
                if (mOptimizeFragment != null) {
                    fragmentTransaction.show(mDefaultFragment).hide(mOptimizeFragment);
                } else {
                    mOptimizeFragment = new OptimizeResultFragment();
                    fragmentTransaction.add(R.id.fragment_container,
                            mOptimizeFragment, TAG_OPTIMIZE_RESULT).hide(mOptimizeFragment)
                            .show(mDefaultFragment);
                }
                mCurrentScore = Contract.MAX_SCORE;
            }
            fragmentTransaction.commitAllowingStateLoss();
        }
        initViews();
    }

    private void initViews() {
        if (mOptimizing) {
            mStart.setEnabled(true);
            mStart.setText(R.string.stop_optimize_button);
            mPromptText.setText(R.string.stop_optimize_prompt);
        } else {
            mPromptText.setText(R.string.scanning);
            mStart.setText(R.string.start_optimize_button);
            mStart.setEnabled(false);
            mStart.setTextColor(getResources().getColor(R.color.gray));
        }
        mScoreText.setText(String.valueOf(mCurrentScore));
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPref.getBoolean(Contract.KEY_REAL_SPEED_SWITCH, false)) {
            startService(new Intent(this, FloatViewService.class));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "mCurrentScore = " + mCurrentScore);
        outState.putBoolean(IS_OPTIMIZING, mOptimizing);
        outState.putBoolean(NEED_OPTIMIZE, mNeedOptimize);
        outState.putInt(KEY_CURRENT_SCORE, mCurrentScore);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        Log.i(TAG, "mOptimizing = " + mOptimizing);
        if (mOptimizing) {
            mOptimizing = false;
            showDefaultFragment();
        } else {
            showOptimizeResultFragment();
            mOptimizing = true;
        }
    }

    @Override
    public void onBackPressed() {
        if (mOptimizing) {
            mOptimizing = false;
            showDefaultFragment();
        } else {
            super.onBackPressed();
        }
    }

    private void showDefaultFragment() {
        final FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.remove(mOptimizeFragment).show(mDefaultFragment).commitAllowingStateLoss();
        mOptimizeFragment = null;
        mNeedOptimize = false;
        int score = Integer.parseInt(mScoreText.getText().toString());
        mStart.setText(R.string.start_optimize_button);
        if (score < Contract.MAX_SCORE) {
            mStart.setEnabled(true);
            mPromptText.setText(R.string.start_optimize_prompt);
        } else {
            mStart.setEnabled(false);
            mStart.setTextColor(getResources().getColor(R.color.gray));
            mPromptText.setText(R.string.keep_prompt);
        }
    }

    private void showOptimizeResultFragment() {
        final FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        if (mOptimizeFragment == null) {
            mNeedOptimize = true;
            mOptimizeFragment = new OptimizeResultFragment();
            fragmentTransaction.add(R.id.fragment_container, mOptimizeFragment,
                    TAG_OPTIMIZE_RESULT).hide(mDefaultFragment).commitAllowingStateLoss();
        } else {
            fragmentTransaction.hide(mDefaultFragment)
                    .show(mOptimizeFragment).commitAllowingStateLoss();
            mOptimizeFragment.CheckDataFlowSet(false);
            mOptimizeFragment.startRubbishOptimize();
            mOptimizeFragment.startAppsCacheOptimize();
        }
        mStart.setText(R.string.stop_optimize_button);
        mPromptText.setText(R.string.stop_optimize_prompt);
    }

    @Override
    public void onRefreshMainUi(int score, int mode, Boolean isScanOrOptimizeEnd) {
        if (mScoreText == null || mStart == null || mPromptText == null) {
            return;
        }
        mScoreText.setText(String.valueOf(score));
        mCurrentScore = score;

        if (!isScanOrOptimizeEnd) {
            return;
        }
        Log.i(TAG, "onRefreshMainUi mOptimizing = " + mOptimizing);
        switch (mode) {
            case Contract.FINISH_SCAN:
                if (!mOptimizing) {
                    if (score < Contract.MAX_SCORE) {
                        mStart.setEnabled(true);
                        mStart.setTextColor(getResources().getColor(R.color.white));
                        if (!mPromptText.getText().equals(
                                getResources().getString(R.string.start_optimize_prompt))) {
                            mPromptText.setText(R.string.start_optimize_prompt);
                        }
                    } else {
                        mStart.setEnabled(false);
                        mStart.setTextColor(getResources().getColor(R.color.gray));
                        if (!mPromptText.getText().equals(
                                getResources().getString(R.string.keep_prompt))) {
                            mPromptText.setText(R.string.keep_prompt);
                        }
                    }
                }
                break;
            case Contract.FINISH_OPTIMIZE:
                mStart.setEnabled(true);
                mStart.setText(R.string.finish_optimize);
                if (!mPromptText.getText().equals(
                        getResources().getString(R.string.keep_prompt))) {
                    mPromptText.setText(R.string.keep_prompt);
                }
                break;
            default:
                break;
        }
    }
}
