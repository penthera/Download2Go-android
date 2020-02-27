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
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;

import com.google.android.material.tabs.TabLayoutMediator;
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
    
	public static final int NUM_TABS = 3;

	private static int[] tabTitles =  new int[]{R.string.inbox, R.string.catalog, R.string.other};
        
	/** View Pager: Horizontal slide */
	private ViewPager2 mPager;
    		
	/** View Pager Adapter: bind data to the view pager */
	private DemoFragmentStateAdapter mFragmentAdapter;

	@SuppressLint("DefaultLocale") @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// --- Pager
		mPager = findViewById(R.id.pgr);
		mPager.setOffscreenPageLimit(1);
		mFragmentAdapter = new DemoFragmentStateAdapter(this);
		mPager.setAdapter(mFragmentAdapter);

		// --- Tabs
		TabLayout tabLayout = findViewById(R.id.tab_layout);
		new TabLayoutMediator(tabLayout, mPager,
				(tab, position) -> tab.setText(tabTitles[position])
		).attach();

		mFragmentAdapter.notifyDataSetChanged();
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
	 * Pager Adapter
	 */
	public class DemoFragmentStateAdapter extends FragmentStateAdapter {

		public DemoFragmentStateAdapter(FragmentActivity fa) {
			super(fa);
		}

		@Override
		public int getItemCount() {
			return NUM_TABS;
		}

		@NonNull
		@Override
		public Fragment createFragment(int position) {
			Log.i(TAG,"getItem " + position);
			switch (position) {
				case 0: {
					return InboxFragment.newInstance(mVirtuoso);
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
	