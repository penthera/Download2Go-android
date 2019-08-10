//  Copyright (c) 2013 Penthera Partners, LLC. All rights reserved.
// 
// PENTHERA CONFIDENTIAL
//
// (c) 2013 Penthera Partners Inc. All Rights Reserved.
// 
// NOTICE: This file is the property of Penthera Partners Inc.  
// The concepts contained herein are proprietary to Penthera Partners Inc.
// and may be covered by U.S. and/or foreign patents and/or patent 
// applications, and are protected by trade secret or copyright law.
// Distributing and/or reproducing this information is forbidden 
// unless prior written permission is obtained from Penthera Partners Inc.
//

package com.penthera.sdkdemo.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.util.Log;

import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import com.penthera.sdkdemo.R;
import com.penthera.sdkdemo.fragment.CatalogFragment;
import com.penthera.sdkdemo.fragment.InboxFragment;
import com.penthera.sdkdemo.fragment.OtherFragment;
import com.penthera.sdkdemo.framework.SdkDemoBaseActivity;

/**
 * The main activity
 */
public class MainActivity extends SdkDemoBaseActivity {
	/** Log tag */
    private static final String TAG = MainActivity.class.getName(); 		
    
	/** Tab: Settings */
	private MyTabListener<InboxFragment> mInboxTabListener;
   
	/** Tab: Alerts */
	private MyTabListener<CatalogFragment> mCatalogTabListener;
    		
	/** Tab: History */
	private MyTabListener<OtherFragment> mOtherTabListener;

	/** Tab Index: Settings */
	private static String TAB_TAG_INBOX = "0";
	/** Tab Index: Alerts */
	private static String TAB_TAG_CATALOG = "1";
	/** Tab Index: History*/
	private static String TAB_TAG_OTHER = "2";
        
	/** ActionBar */
	private ActionBar mActionBar;

	/** View Pager: Horizontal slide */
	private ViewPager mPager;
    		
	/** View Pager Adapter: bind data to the view pager */
	private PagerAdapter mPagerAdapter;

	public interface DemoTabListener {
		public void onChange(int index);
	}
	
	public class MyDemoTabListener implements DemoTabListener {
		@Override
		public void onChange(int index) {
			mPager.setCurrentItem(index);
		}		
	}
	MyDemoTabListener mMyDemoTabListener = new MyDemoTabListener();
	
	
	@SuppressLint("DefaultLocale") @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mActionBar = getSupportActionBar();

		// --- Pager
		mPager = (ViewPager)findViewById(R.id.pgr);
		mPager.setAdapter(mPagerAdapter);
		
		mPager.setOffscreenPageLimit(1);
		mPager.setOnPageChangeListener(new OnPageChangeListener() {            	
			@Override
			public void onPageScrollStateChanged(int arg0) {
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageSelected(int index) {
				mActionBar.getTabAt(index).select();  
			}
		});
		
		mPagerAdapter = new PagerAdapter(getSupportFragmentManager(), mActionBar);
		mPager.setAdapter(mPagerAdapter);

		// --- Action Bar
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);		        
		addTabs();
		mPagerAdapter.notifyDataSetChanged();
	}
		
	@Override
	public void onNewIntent(Intent i) {
	}
	 
	// onStart
	@Override
	public void onStart()
	{
		super.onStart();
	}
        
	// onResume
	@Override
	public void onResume() {		
		super.onResume();
	}
	        	
	// onPause
	@Override
	public void onPause() {
		super.onPause();
	}

	// onDestroy
	@Override
	public void onDestroy() {    	
		super.onDestroy();
	}
        
	/**
	 * Add the tabs
	 */
	private void addTabs()
	{	 
		mInboxTabListener = new MyTabListener<InboxFragment>(TAB_TAG_INBOX, mPager);
		mActionBar.addTab(mActionBar.newTab()
    				.setText(R.string.inbox)
    				.setTabListener(mInboxTabListener));		 

		mCatalogTabListener = new MyTabListener<CatalogFragment>(TAB_TAG_CATALOG, mPager);
		mActionBar.addTab(mActionBar.newTab()
    				.setText(R.string.catalog)
    				.setTabListener(mCatalogTabListener));		

		mOtherTabListener = new MyTabListener<OtherFragment>(TAB_TAG_OTHER, mPager);
		mActionBar.addTab(mActionBar.newTab()
    				.setText(R.string.other)
    				.setTabListener(mOtherTabListener));				
	}

	/**
	 * Tab listener
	 */
	public class MyTabListener<T extends Fragment> implements TabListener
	{
		private final String mTag;
		private ViewPager mPager;

		/**
		 * Constructor
		 * 
		 * @param tag
		 * @param viewPager
		 */
		public MyTabListener(String tag, ViewPager viewPager) 
		{
			mTag = tag;
			mPager = viewPager;
		}

		// onTabSelected
		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			mPager.setCurrentItem(Integer.parseInt(mTag));
		}

		// onTabUnselected
		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		// onTabReselected
		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}
	}
    	
	/**
	 * Pager Adapter
	 */
	public class PagerAdapter extends FragmentPagerAdapter {
		ActionBar mActionBar;
    
		// TODO: Shouldn't keep handle here re-factor
		private InboxFragment mIf;
		
		public PagerAdapter(FragmentManager fm, ActionBar actionBar) {
			super(fm);
			mActionBar = actionBar;
		}

		@Override
		public int getCount() {
			return mActionBar.getNavigationItemCount();  
		}

		@Override
		public Fragment getItem(int position) {
			Log.i(TAG,"getItem " + position);
			switch (position) {
				case 0: {
					mIf = InboxFragment.newInstance(mVirtuoso, mMyDemoTabListener);
					return mIf;
				}
				case 1: {
					return CatalogFragment.newInstance(mVirtuoso);
				}
				case 2: {
					return OtherFragment.newInstance();
				}
			}
			return null;
		}
	}
}	
	