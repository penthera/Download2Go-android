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
import com.penthera.sdkdemo.fragment.AboutFragment;
import com.penthera.sdkdemo.framework.SdkDemoBaseActivity;

/**
 * About Activity
 * 
 * Displays various version numbers
 */
public class AboutActivity extends SdkDemoBaseActivity
{
	/** The log file */
	public static final String TAG = AboutActivity.class.getName();
	
	// onCreate
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		if (savedInstanceState == null) {
			super.onCreate(savedInstanceState);		
			setContentView(R.layout.content_frame);

			// About Fragment
			AboutFragment aboutFragment = new AboutFragment();
			getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, aboutFragment).commit();		

			// Action Bar
			ActionBar ab = getSupportActionBar();
			ab.setTitle(R.string.app_name);
			ab.setSubtitle(R.string.about);			
			ab.setDisplayHomeAsUpEnabled(true);		
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