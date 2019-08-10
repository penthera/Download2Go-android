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

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import com.penthera.sdkdemo.R;
import com.penthera.sdkdemo.fragment.SubscriptionsFragment;
import com.penthera.sdkdemo.framework.SdkDemoBaseActivity;

/**
 * About Activity
 * 
 * Displays various version numbers
 */
public class SubscriptionsActivity extends SdkDemoBaseActivity
{
	/** The log file */
	public static final String TAG = SubscriptionsActivity.class.getName();

	SubscriptionsFragment mSf;
	
	// onCreate
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.content_frame);

		if (savedInstanceState == null) {			
			// Catalog Detail Fragment
			mSf = SubscriptionsFragment.newInstance(mVirtuoso);
			getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, mSf, "mSf").commit();		
			mSf.setArguments(getIntent().getExtras());

			// Action Bar
			ActionBar ab = getSupportActionBar();
			ab.setTitle(R.string.app_name);
			ab.setSubtitle(R.string.catalog_detail);		
			ab.setDisplayHomeAsUpEnabled(true);	
		} else {
			mSf = (SubscriptionsFragment) getSupportFragmentManager().findFragmentByTag("mSf");
			mSf.setService(mVirtuoso);
		}	
	}

	// onResume
	@Override
	public void onResume() {
		super.onResume();
	}

	// onStop
	@Override
	public void onStop() {
		super.onStop();
	}

	// onDestroy
	@Override
	protected void onDestroy() {
		super.onDestroy();		
	}
}