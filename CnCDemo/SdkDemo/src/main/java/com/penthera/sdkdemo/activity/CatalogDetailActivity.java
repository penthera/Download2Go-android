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
import com.actionbarsherlock.app.ActionBar;
import com.penthera.sdkdemo.R;
import com.penthera.sdkdemo.fragment.CatalogDetailFragment;
import com.penthera.sdkdemo.framework.SdkDemoBaseActivity;

/**
 * Show details for items in the catalog
 */
public class CatalogDetailActivity extends SdkDemoBaseActivity {
	
	/** Log tag */
	public static final String TAG = CatalogDetailActivity.class.getName();

	// onCreate
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.content_frame);

		if (savedInstanceState == null) {			
			// Catalog Detail Fragment
			CatalogDetailFragment cdf = CatalogDetailFragment.newInstance(mService);
			getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, cdf, "cdf").commit();		
			cdf.setArguments(getIntent().getExtras());
			
			// Action Bar
			ActionBar ab = getSupportActionBar();
			ab.setTitle(R.string.app_name);
			ab.setSubtitle(R.string.catalog_detail);		
			ab.setDisplayHomeAsUpEnabled(true);	
		} else {
			CatalogDetailFragment cdf = (CatalogDetailFragment) getSupportFragmentManager().findFragmentByTag("cdf");
			cdf.setService(mService);
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
