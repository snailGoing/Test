package com.sprd.generalsecurity.network;

import android.app.Activity;
import android.app.AppGlobals;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.pm.IPackageManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.net.INetworkStatsSession;
import android.net.INetworkStatsService;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.sprd.generalsecurity.network.DataFlowService;
import com.sprd.generalsecurity.utils.DateCycleUtils;
import com.sprd.generalsecurity.utils.TeleUtils;
import com.sprd.generalsecurity.utils.SeekBarPreference;
import com.sprd.generalsecurity.utils.CustomEditTextPreference;
import com.sprd.generalsecurity.utils.Contract;
import com.sprd.generalsecurity.utils.Formatter;
import com.sprd.generalsecurity.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import static android.net.NetworkTemplate.buildTemplateMobileAll;
import android.app.ActionBar;

public class DataFlowSetting extends Activity implements ActionBar.TabListener
        ,ViewPager.OnPageChangeListener {
    private static final String TAG = "DataFlowSetting";
    private INetworkStatsSession mStatsSession;
    private INetworkStatsService mStatsService;
    private NetworkTemplate mTemplate;
    NetworkStats mStats;
    private GregorianCalendar mCalendar;
    int mSimId;
    private ViewPager mTabPager;//viewpager
    private String[] mTabTitles = {"SIM1","SIM2"};
    private TabPagerAdapter mTabPagerAdapter;
    private PrefsFragment simFragment1;
    private PrefsFragment simFragment2;
    private ArrayList<PrefsFragment> fragmentList = new ArrayList<PrefsFragment>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setTitle(getResources().getString(R.string.data_restrict_sim));
        setContentView(R.layout.data_setting);
        final FragmentManager fragmentManager = getFragmentManager();
        final FragmentTransaction transaction = fragmentManager.beginTransaction();
        int simCount = TeleUtils.getSimCount(this);
        if(simCount ==1 || simCount == 0){
            mSimId = TeleUtils.getPrimarySlot(this);
            PrefsFragment p = new PrefsFragment();
            p.setSimID(mSimId);
            getFragmentManager().beginTransaction().replace(R.id.setting,
                    p).commit();
        }else{
            mTabPager = getView(R.id.tab_pager);
            mTabPagerAdapter = new TabPagerAdapter();
            mTabPager.setAdapter(mTabPagerAdapter);
            //SPRD: Bug704824 add tab for dual sim card mode
            mTabPager.setOnPageChangeListener(this);
            setUpActionBar();
            setUpTabs();
            simFragment1 = new PrefsFragment();
            simFragment1.setSimID(1);
            simFragment2 = new PrefsFragment();
            simFragment2.setSimID(2);
            fragmentList.clear();
            fragmentList.add(simFragment1);
            fragmentList.add(simFragment2);
            transaction.add(R.id.tab_pager,simFragment1,"SIM1");
            transaction.add(R.id.tab_pager,simFragment2,"SIM2");
            transaction.commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        }
    }

    private void setUpActionBar() {
        final ActionBar actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
    }

    private void setUpTabs() {
        final ActionBar actionBar = getActionBar();
        for (int i = 0; i < mTabPagerAdapter.getCount(); ++i) {
            actionBar.addTab(actionBar.newTab()
                    .setText("SIM"+(i+1))
                    .setTabListener(this));
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mTabPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    /**
     * SPRD: Bug704824 add tab for dual sim card mode  @{
     */
    @Override
    public void onPageSelected(int i) {
        final ActionBar actionBar = getActionBar();
        actionBar.getTabAt(i).select();
    }

    @Override
    public void onPageScrollStateChanged(int i) {}

    @Override
    public void onPageScrolled(int i, float v, int i1) {}
    /**
     * @}
     */

    public <T extends View> T getView(int id) {
        T result = (T)findViewById(id);
        if (result == null) {
            throw new IllegalArgumentException("view 0x" + Integer.toHexString(id)
                    + " doesn't exist");
        }
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
    }

    //BEGIN_INCLUDE(fragment)
    public static class PrefsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

        private static final String KEY_MONTH_TOTAL = "key_edit_month_total";
        private static final String  KEY_MONTH_USED = "key_edit_month_used";
        private static final String  KEY_DAY_RESTRICT = "key_day_flow_restrict";
        private static final String KEY_USED_SETTING_TIME = "key_data_use_time";
        private static final String KEY_SEEKBAR = "seek_bar_restrict";
        private static final String KEY_MONTH_REMIND_SWITCH = "key_restrict_month_reminder";
        private static final String KEY_DAY_REMIND_SWITCH = "key_restrict_day_reminder";
        private static final String  KEY_MONTH_LEFT_RESTRICT = "key_month_flow_left_restrict";
        private static final String SIZE_UNIT = " MB";

        private SeekBarPreference seekBarPreference;
        private int mSeekBarProgress;
        private int mSimID;
        private long dataUserSet;
        private static long M2BITS = 1024 * 1024;

        /** SPRD: Bug700007 Unable to instantiate DataFlowSetting$PrefsFragment @{ */
        public PrefsFragment() {}
        /** SPRD: Bug700007 @} */

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            PreferenceManager pm = getPreferenceManager();
            pm.setSharedPreferencesName("sim" + mSimID);
            addPreferencesFromResource(R.xml.data_settings_pref);
            SwitchPreference switchPreference = (SwitchPreference) findPreference(KEY_MONTH_REMIND_SWITCH);
            switchPreference.setOnPreferenceChangeListener(this);

            switchPreference = (SwitchPreference) findPreference(KEY_DAY_REMIND_SWITCH);
            switchPreference.setOnPreferenceChangeListener(this);
            CustomEditTextPreference editTextPreference = (CustomEditTextPreference)findPreference(KEY_MONTH_TOTAL);
            editTextPreference.setOnPreferenceChangeListener(this);
            setSummary(KEY_MONTH_TOTAL, null);

            editTextPreference = (CustomEditTextPreference)findPreference(KEY_MONTH_USED);
            editTextPreference.setOnPreferenceChangeListener(this);
            setSummary(KEY_MONTH_USED, null);
            editTextPreference = (CustomEditTextPreference)findPreference(KEY_DAY_RESTRICT);
            editTextPreference.setOnPreferenceChangeListener(this);
            setSummary(KEY_DAY_RESTRICT, null);

            editTextPreference = (CustomEditTextPreference)findPreference(KEY_MONTH_LEFT_RESTRICT);
            editTextPreference.setOnPreferenceChangeListener(this);
            setSummary(KEY_MONTH_LEFT_RESTRICT, null);

            /*seekBarPreference = (SeekBarPreference)findPreference(KEY_SEEKBAR);

            seekBarPreference.setOnSeekBarPrefsChangeListener(new SeekBarPreference.OnSeekBarPrefsChangeListener() {
                //public void OnSeekBarChangeListener(SeekBar seekBar, boolean isChecked);
                public void onStopTrackingTouch(String key, SeekBar seekBar) {
                    android.util.Log.e(TAG, "onStopTracking:" + seekBar.getProgress() + ":" + mSeekBarProgress);
                    if (mSeekBarProgress != seekBar.getProgress()) {
                        updateSeekBarSetting();
                        mSeekBarProgress = seekBar.getProgress();
                    }
                }

                public void onStartTrackingTouch(String key, SeekBar seekBar) {

                }

                public void onProgressChanged(String key, SeekBar seekBar, int progress, boolean fromUser) {
                    seekBarPreference.setDataRestrict(progress + 50);
                }
            });*/
        }

        public void setSimID(int i) {
            mSimID = i;
        }

        @Override
        public void onResume() {
            super.onResume();
            queryLoader(QUERY_MONTH_TOTAL);
            /*seekBarPreference = (SeekBarPreference)findPreference(KEY_SEEKBAR);
            mSeekBarProgress = seekBarPreference.getProgress();
            SharedPreferences sp = seekBarPreference.getSharedPreferences();
            if (sp.getString(KEY_MONTH_TOTAL, "0") != "") {
                seekBarPreference.setMonthRestrict(
                                        Float.parseFloat(sp.getString(KEY_MONTH_TOTAL, "0")));
                preValueMonthRestrict = (sp.getInt(KEY_SEEKBAR, 0) + 50) *
                                        Float.parseFloat(sp.getString(KEY_MONTH_TOTAL, "0"));
                preValueMonthDataUsed = Float.parseFloat(sp.getString(KEY_MONTH_USED, "0"));
            } else {
                seekBarPreference.setMonthRestrict(0f);
            }
            seekBarPreference.setDataRestrict(sp.getInt(KEY_SEEKBAR, 0) + 50);

            updateSeekBarSetting();*/
        }

        private float preValueMonthRestrict;
        private float preValueMonthDataUsed;
        private void updateSeekBarSetting() {
            seekBarPreference = (SeekBarPreference)findPreference(KEY_SEEKBAR);
            SharedPreferences sp = seekBarPreference.getSharedPreferences();
            seekBarPreference.setMonthRestrict(Float.parseFloat(sp.getString(KEY_MONTH_TOTAL, "0")));
            seekBarPreference.setDataRestrict(sp.getInt(KEY_SEEKBAR, 0) + 50);

            //remind setting changed, start service to check again.
            float newValueMonthRestrict = (sp.getInt(KEY_SEEKBAR, 0) + 50) *
                                     Float.parseFloat(sp.getString(KEY_MONTH_TOTAL, "0"));
            if (preValueMonthRestrict != newValueMonthRestrict)  {
                preValueMonthRestrict = newValueMonthRestrict;
                resetRemindTrigger1AndCheckReminder();
            }

            float newValuseMonthDataUsed = Float.parseFloat(sp.getString(KEY_MONTH_USED, "0"));
            if (preValueMonthDataUsed != newValuseMonthDataUsed) {
                preValueMonthDataUsed = newValuseMonthDataUsed;
                resetRemindTrigger1AndCheckReminder();
            }
        }


        void resetRemindTrigger1AndCheckReminder() {
            SharedPreferences sp = getPreferenceManager().getSharedPreferences();
            Log.e(TAG, "update:" + mSimID + ":" + sp.getString(KEY_MONTH_TOTAL, "0"));

            SharedPreferences.Editor editor = sp.edit();
            //remind trigger time update to 0, so will remind again for the new setting.
            editor.putLong(Contract.KEY_MONTH_REMIND_TIME_TRIGGER_WARN, 0);
            editor.putLong(Contract.KEY_MONTH_REMIND_TIME_TRIGGER_OVER, 0);
            editor.apply();
            Intent it = new Intent(this.getActivity(), DataFlowService.class);
            getActivity().startService(it);
        }

        void setSummary(String key, String v) {
            CustomEditTextPreference editTextPreference = (CustomEditTextPreference)findPreference(key);
            if (v == null) {
                PreferenceManager pm = getPreferenceManager();
                SharedPreferences sharedPref = pm.getSharedPreferences();
                v = sharedPref.getString(key, "0");
            }
            editTextPreference.setSummary(v + SIZE_UNIT);
        }

        private long mDataUsedSetTime;

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            /* SPRD: Bug 670294 GeneralSecurity monkey crash @{ */
            if (this.getActivity() == null) {
                return true;
            }
            /* @} */
            if (preference.getKey().equalsIgnoreCase(KEY_MONTH_USED)) {
                //updateSeekBarSetting();
                resetRemindTrigger1AndCheckReminder();
                SharedPreferences.Editor editor = preference.getEditor();
                mDataUsedSetTime = System.currentTimeMillis();
                editor.putLong(KEY_USED_SETTING_TIME, mDataUsedSetTime);
                if (newValue != null
                        && !((String) newValue).equalsIgnoreCase("")) {
                    editor.putString(Contract.KEY_DATA_USED_ORIGINAL, (String) newValue);
                    dataUserSet = (long) (Float.parseFloat((String) newValue) * M2BITS);
                } else {
                    editor.putString(Contract.KEY_DATA_USED_ORIGINAL, "0");
                    dataUserSet = 0;
                }
                editor.apply();

                queryLoader(QUERY_USER_NOW);
                return true;
            } else if (preference.getKey().equalsIgnoreCase(KEY_MONTH_TOTAL)) {
                //updateSeekBarSetting();
                resetRemindTrigger1AndCheckReminder();
                return true;
            } else if (preference.getKey().equalsIgnoreCase(KEY_MONTH_REMIND_SWITCH) ||
                    preference.getKey().equalsIgnoreCase(KEY_DAY_REMIND_SWITCH)) {
                if ((boolean)newValue) {
                    //swich enabled
                    Intent it = new Intent(this.getActivity(), DataFlowService.class);
                    getActivity().startService(it);
                }
            } else if (preference.getKey().equalsIgnoreCase(KEY_DAY_RESTRICT)) {
                SharedPreferences.Editor editor = preference.getEditor();
                editor.putLong(Contract.KEY_DAY_REMIND_TIME_TRIGGER, 0);
                editor.apply();
                Intent it = new Intent(this.getActivity(), DataFlowService.class);
                getActivity().startService(it);
            }else if (preference.getKey().equalsIgnoreCase(KEY_MONTH_LEFT_RESTRICT)) {
                SharedPreferences.Editor editor = preference.getEditor();
                editor.putLong(Contract.KEY_MONTH_REMIND_TIME_TRIGGER_WARN, 0);
                editor.putLong(Contract.KEY_MONTH_REMIND_TIME_TRIGGER_OVER, 0);
                editor.apply();
                Intent it = new Intent(this.getActivity(), DataFlowService.class);
                getActivity().startService(it);
            }
            return true;
        }

        /*
         * Steps to set 'Data used' field.
         * 1. in OnResume, query type QUERY_MONTH_TOTAL, this will query the network API for used
         *    data.
         *    when use not set 'data used' field, the delta is 0, so set the query result as
         *    'Data used'.
         * 2. Once the user change 'Data used' field, call queryLoader with parameter
         * QUERY_USER_NOW,
         *    this will set the delta. The delta is used to calculate the 'Data used' after user
         *    setting, as following,
         *       monthUsed = QueryedTotalBytes - delta.
         *    Delta is also used in DataFlowMainEntry.java
        **/
        private static final int QUERY_MONTH_TOTAL = 1;
        private static final int QUERY_USER_NOW = 2;

        private void queryLoader(int type) {
            mStatsService = INetworkStatsService.Stub.asInterface(
                    ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
            try {
                mStatsSession = mStatsService.openSession();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }

            // SPRD: Bug759555 context is null after recreate activity
            mTemplate = buildTemplateMobileAll(TeleUtils.getActiveSubscriberId(getContext(), mSimID));

            long start = DateCycleUtils.getMonthCycleStart();
            /* SPRD: Bug 670294 GeneralSecurity monkey crash @{ */
            if (getActivity() != null) {
                if (type == QUERY_USER_NOW) {
                    getLoaderManager().restartLoader(QUERY_USER_NOW,
                            SummaryForAllUidLoader.buildArgs(mTemplate, start,
                                    mDataUsedSetTime), mSummaryCallbacks);
                } else {
                    // kick off loader for sim1 detailed stats
                    getLoaderManager().restartLoader(QUERY_MONTH_TOTAL,
                            SummaryForAllUidLoader.buildArgs(mTemplate, start,
                                    System.currentTimeMillis()), mSummaryCallbacks);
                }
            }
            /* @} */
        }

        private INetworkStatsSession mStatsSession;
        private INetworkStatsService mStatsService;
        private NetworkTemplate mTemplate;

        private final LoaderCallbacks<NetworkStats> mSummaryCallbacks = new LoaderCallbacks<
                NetworkStats>() {
            @Override
            public Loader<NetworkStats> onCreateLoader(int id, Bundle args) {
                // SPRD: Bug759555 context is null after recreate activity
                return new SummaryForAllUidLoader(getContext(), mStatsSession, args);
            }

            @Override
            public void onLoadFinished(Loader<NetworkStats> loader, NetworkStats data) {
                // updateStatsChart(loader.getId(), data);
                long totalBytes = 0;
                NetworkStats.Entry entry = null;

                for (int i = 0; i < data.size(); i++) {
                    entry = data.getValues(i, entry);
                    totalBytes += (entry.rxBytes + entry.txBytes);
                    Log.e(TAG, "--Got +" + entry.uid + ":" + (entry.rxBytes + entry.txBytes));
                }
                SharedPreferences sp = getPreferenceManager().getSharedPreferences();
                if (loader.getId() == QUERY_USER_NOW) {
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putLong(Contract.KEY_DATA_USED_QUERY_DELTA, (totalBytes - dataUserSet));
                    editor.apply();
                } else {
                    long delta = sp.getLong(Contract.KEY_DATA_USED_QUERY_DELTA, 0);
                    float monthUsed = ((float) (totalBytes - delta)) / M2BITS;
                    /** SPRD: Bug 705382 monthUsed is negative @{ */
                    if (monthUsed < 0) {
                        monthUsed = 0;
                    }
                    /** @} */
                    String used = new DecimalFormat("#.##").format(monthUsed);
                    setSummary(KEY_MONTH_USED, used);

                    Log.d(TAG, "======= put:" + monthUsed + ":" + delta);
                    totalBytes = 0;
                    for (int i = 0; i < data.size(); i++) {
                        entry = data.getValues(i, entry);
                        totalBytes += (entry.rxBytes + entry.txBytes);
                    }
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString(KEY_MONTH_USED, used);
                    editor.apply();
                }
            }

            @Override
            public void onLoaderReset(Loader<NetworkStats> loader) {

            }
        };
    }

    private class TabPagerAdapter extends PagerAdapter {
        private final FragmentManager mFragmentManager;
        private FragmentTransaction mCurTransaction = null;

        public TabPagerAdapter() {
            mFragmentManager = getFragmentManager();
        }

        @Override
        public int getCount() {
            return mTabTitles.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return ((Fragment) object).getView() == view;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment f = fragmentList.get(position);
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            mCurTransaction.show(f);
            return f;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (mCurTransaction == null) {
                mCurTransaction = mFragmentManager.beginTransaction();
            }
            mCurTransaction.hide((Fragment) object);
        }
    }
}