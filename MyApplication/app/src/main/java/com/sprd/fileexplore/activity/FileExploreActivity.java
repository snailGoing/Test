package com.sprd.fileexplore.activity;

import android.app.ActionBar;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.sprd.fileexplore.R;
import com.sprd.fileexplore.adapter.BanViewPager;
import com.sprd.fileexplore.fragment.BaseFragment;
import com.sprd.fileexplore.fragment.DetailListFragment;
import com.sprd.fileexplore.fragment.OverViewFragment;
import com.sprd.fileexplore.service.FileManageService;
import com.sprd.fileexplore.service.MountRootManagerRf;
import com.sprd.fileexplore.service.MyBinder;
import com.sprd.fileexplore.util.LogUtil;

import java.util.ArrayList;
import java.util.List;

public class FileExploreActivity extends AppCompatActivity {

    private static final String TAG = FileExploreActivity.class.getSimpleName();
    private ActionBar mActionBar;
    private BanViewPager mViewPager;
    private SectionsPagerAdapter mPagerAdapter;
    private TabLayout mTabLayout;
    private LayoutInflater mInflater;
    private List<String> mTitleList = new ArrayList<>();
    private BaseFragment mCurrentFragment;
    private TabLayout tabLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explore);

        mViewPager = (BanViewPager)findViewById(R.id.banViewPager);
        mPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        mTitleList.add(getString(R.string.file_category_title));
        mTitleList.add(getString(R.string.local_storage_title));
        OverViewFragment mCategoryFragment = new OverViewFragment();
        mCategoryFragment.setTabLayout(tabLayout).setViewPager(mViewPager);
        mCurrentFragment = mCategoryFragment;
        DetailListFragment mDetailListFragment = new DetailListFragment();
        mDetailListFragment.setTabLayout(tabLayout).setViewPager(mViewPager);

        mPagerAdapter.addFragment(new Pair<BaseFragment, String>(mCategoryFragment, mTitleList.get(0)));
        mPagerAdapter.addFragment(new Pair<BaseFragment, String>(mDetailListFragment, mTitleList.get(1)));
        mViewPager.setAdapter(mPagerAdapter);
        mTabLayout = (TabLayout)findViewById(R.id.tabLayout);
        mTabLayout.setupWithViewPager(mViewPager);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mCurrentFragment = mPagerAdapter.getItem(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Toast.makeText(this,"men: ",Toast.LENGTH_LONG).show();
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent intent = new Intent(this, SettingActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_search:
                this.startActivity(new Intent().setClass(
                        this.getApplicationContext(), FileSearchActivity.class));
                return true;
            case R.id.menu_clean:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    public class SectionsPagerAdapter extends FragmentPagerAdapter{

        ArrayList<Pair<BaseFragment, String>> mFragmentList;
        FragmentManager fm;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            this.fm =fm;
            this.mFragmentList = new ArrayList<Pair<BaseFragment, String>>();
        }

        @Override
        public BaseFragment getItem(int position) {
            return mFragmentList.get(position).first;
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }
        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentList.get(position).second;
        }

        public void addFragment(Pair<BaseFragment, String> fragment){
            if(mFragmentList != null && fragment != null && mFragmentList.contains(fragment)){
                return;
            }
            if(mFragmentList != null && fragment != null){
                mFragmentList.add(fragment);
            }
        }

    }
    @Override
    public void onBackPressed() {
        Log.d(TAG," onBackPressed... ");
        if (mCurrentFragment.onBackPressed()) {
            Log.d(TAG," onBackPressed...send event to activity ");
            super.onBackPressed();    //Need to test
        }
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy()  activity ");

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"onStart()  activity ");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume()  activity ");
    }

    /**
    *  This is designed for pasting files to target dir and  stayying here
    *  @author Xuehao.Jiang
    *  created at 2017/7/24 20:17
    */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("DetailListFragment"," onActivityResult:   requestCode="+requestCode+" resultCode= "+resultCode);
        if( resultCode == FilePasteActivity.RESULT_PASTE_CODE){
            if(mCurrentFragment instanceof DetailListFragment){
                String path = data.getStringExtra("path");
                if(path!=null && MountRootManagerRf.getInstance().isRootPathMount(path)){
                    ((DetailListFragment) mCurrentFragment).showDirectoryFileInfo(path);
                }
            }else if(mCurrentFragment instanceof OverViewFragment){
                mViewPager.setCurrentItem(1);
                if(mCurrentFragment instanceof DetailListFragment){
                    String path = data.getStringExtra("path");
                    if(path!=null && MountRootManagerRf.getInstance().isRootPathMount(path)){
                        ((DetailListFragment) mCurrentFragment).showDirectoryFileInfo(path);
                    }
                }
            }
        }
    }
}
