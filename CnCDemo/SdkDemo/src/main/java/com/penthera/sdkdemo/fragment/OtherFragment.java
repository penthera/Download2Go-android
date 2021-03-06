//Copyright (c) 2017 Penthera Partners, LLC. All rights reserved.
//
//PENTHERA CONFIDENTIAL
//
//(c) 2015 Penthera Partners Inc. All Rights Reserved.
//
//NOTICE: This file is the property of Penthera Partners Inc.
//The concepts contained herein are proprietary to Penthera Partners Inc.
//and may be covered by U.S. and/or foreign patents and/or patent
//applications, and are protected by trade secret or copyright law.
//Distributing and/or reproducing this information is forbidden
//unless prior written permission is obtained from Penthera Partners Inc.
//
package com.penthera.sdkdemo.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.fragment.app.ListFragment;

import com.penthera.sdkdemo.R;
import com.penthera.sdkdemo.Util;
import com.penthera.sdkdemo.activity.AboutActivity;
import com.penthera.sdkdemo.activity.DevicesActivity;
import com.penthera.sdkdemo.activity.DiagnosticsActivity;
import com.penthera.sdkdemo.activity.SettingsActivity;

/**
 * A wee navigation fragment for less used screens
 */
public class OtherFragment extends ListFragment {
	private static final String TAG = OtherFragment.class.getName();

	/**
	 * Create instance of the other fragment
	 * @return
	 */
	public static OtherFragment newInstance() {
		OtherFragment inf = new OtherFragment();
		return inf;
	}		

	// onListItemClick
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		switch (position) {
			case 0: {
				Util.startActivity(getActivity(), SettingsActivity.class, null);
				break;
			}
			case 1: {
				Util.startActivity(getActivity(), DevicesActivity.class, null);
				break;			
			}
			case 2: {
				Util.startActivity(getActivity(), DiagnosticsActivity.class, null);
				break;			
			}
			case 3: {
				Util.startActivity(getActivity(), AboutActivity.class, null);
				break;			
			}
			default: {
				Log.e(TAG, "bad option: " + position);
			}	
		}
	}

	// onCreate
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	// onCreateView
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {	
		// List Options
		String[] screens = new String[] { getString(R.string.settings), getString(R.string.user_devices), getString(R.string.diagnostics), getString(R.string.about)};
		ArrayAdapter<String> ad = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, screens);
		setListAdapter(ad);
		
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	// onActivityCreated
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	// Start
	@Override
	public void onStart() {
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

	// onStop
	@Override
	public void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	// onDestroy
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}
