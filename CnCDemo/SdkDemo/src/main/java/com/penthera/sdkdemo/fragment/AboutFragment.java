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

package com.penthera.sdkdemo.fragment;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import android.view.MenuItem;

import com.penthera.VirtuosoSDK;
import com.penthera.sdkdemo.R;

/**
 * Displays version information
 * @author Glen
 */
public class AboutFragment extends Fragment {
	/** The layout */
	private View mLayout;
	
	// --- Life-Cycle Methods

	// onCreate
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setHasOptionsMenu(true);
	}

	// onCreateView
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mLayout = inflater.inflate(R.layout.fragment_about, null);
		
		String clientVersionString = "";
		try {
			clientVersionString = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;			
		} catch (Exception e){}
		
		// SDK Version
		TextView serverSdkVersion = (TextView) mLayout.findViewById(R.id.txt_server_sdk_verison);
		serverSdkVersion.setText(String.format(getString(R.string.server_sdk_version), VirtuosoSDK.FULL_VERSION));

		// SDK Demo Version
		TextView clientVersion = (TextView) mLayout.findViewById(R.id.txt_sdk_demo_verison);
		clientVersion.setText(String.format(getString(R.string.client_version), clientVersionString));		
		
		// Android SDK Version
		TextView androidSdkVersion = (TextView) mLayout.findViewById(R.id.txt_android_sdk_verison);
		androidSdkVersion.setText(String.format(getString(R.string.android_sdk_version), Build.VERSION.RELEASE));		
		
		return mLayout;
	}

	// onActivityCreated
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);		
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

	// onOptionsItemSelected
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		
		switch(id) {
			case android.R.id.home: {
				handleBack();
				break;
			}
		}		
		return super.onOptionsItemSelected(item);
	}
	
	private void handleBack() {
		getActivity().finish();
	}
}
