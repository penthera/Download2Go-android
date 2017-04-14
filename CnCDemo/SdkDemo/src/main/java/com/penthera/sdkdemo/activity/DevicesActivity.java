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
package com.penthera.sdkdemo.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.penthera.sdkdemo.R;
import com.penthera.sdkdemo.dialog.ChangeNickNameDialog;
import com.penthera.sdkdemo.framework.SdkDemoBaseActivity;
import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.client.BackplaneException;
import com.penthera.virtuososdk.client.IBackplane;
import com.penthera.virtuososdk.client.IBackplaneDevice;
import com.penthera.virtuososdk.client.Observers.IBackplaneObserver;

public class DevicesActivity extends SdkDemoBaseActivity {

	private static final String LOG_TAG = DevicesActivity.class.getName();
	private String NICKNAME_DIALOG_TAG = "nickDlg";
	DialogFragment mDialog;
	private ProgressDialog mProgressDialog;
	@SuppressLint("SimpleDateFormat")
	SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy HH:mm:ss");
	
	private ArrayList<IBackplaneDevice> iBackplaneDevices;
	
	/**
	 * Backplane Observer to check name and download enabled changes.
	 */
	final IBackplaneObserver mBackplaneObserver = new IBackplaneObserver(){

		@Override
		public void requestComplete(int callbackType, int result) {
			if( callbackType == Common.BackplaneCallbackType.DOWNLOAD_ENABLEMENT_CHANGE ||
				callbackType == Common.BackplaneCallbackType.DEVICE_UNREGISTERED ||
				callbackType == Common.BackplaneCallbackType.NAME_CHANGE){
				
				if(result == Common.BackplaneResult.SUCCESS){
					handleRefresh();
				}
				else{
					dismissProgressDialog();
					Log.w(LOG_TAG, "Problem communicating wih the backplane result = " + result);
				}
			}
		}
		
	};

	/**
	 * Device Listing Observer
	 */
	final IBackplane.IBackplaneDevicesObserver mDeviceListingObserver = new IBackplane.IBackplaneDevicesObserver() {

		@Override
		public void backplaneDevicesComplete(final IBackplaneDevice[] aDevices) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					iBackplaneDevices.clear();
					if(aDevices.length > 0){
						Collections.addAll(iBackplaneDevices, aDevices);
					} else {
						Toast.makeText(getBaseContext(), "Cannot get Devices: Registered? Connection?", Toast.LENGTH_SHORT).show();							
						Log.w(LOG_TAG, "Devices call failed: unable to retrive devices from server");
					}
					iAdapter.notifyDataSetChanged();
					dismissProgressDialog();
					
				}
			});
			
		}
	};
	
	private class ItemAdapter extends ArrayAdapter<IBackplaneDevice> {

        private ArrayList<IBackplaneDevice> devices;

        public ItemAdapter(Context context, int textViewResourceId, ArrayList<IBackplaneDevice> devices) {
                super(context, textViewResourceId, devices);
                this.devices = devices;
        }

        @SuppressLint("InflateParams")
		@Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.device_row, null);
                }
                final IBackplaneDevice i = devices.get(position);
                if (i != null) {
                        TextView tt = (TextView) v.findViewById(R.id.current_device);
                        if (tt != null) {
                            tt.setText("Is this Device: " + i.isCurrentDevice());                      
                        }
                        tt = (TextView) v.findViewById(R.id.device_id);
                        if (tt != null) {
                              tt.setText("Id: " + i.id());                      
                        }
                        tt = (TextView) v.findViewById(R.id.device_nickname);
                        if (tt != null) {
                              tt.setText("Name: "+ i.nickname());                      
                        }
                        tt = (TextView) v.findViewById(R.id.last_sync);
                        if (tt != null) {
                              tt.setText("Synced:" + formatter.format(i.lastSync()));                      
                        }
                        tt = (TextView) v.findViewById(R.id.last_modified);
                        if (tt != null) {
                              tt.setText("Modified:" + formatter.format(i.lastModified()));                      
                        }
                        Button btn = (Button) v.findViewById(R.id.btn_change_nickname);
                        btn.setOnClickListener(new OnClickListener(){

							@Override
							public void onClick(View v) {
								FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
								try {
						
									if (mDialog != null) { try {mDialog.dismiss(); } catch (Exception e){} }
									
									
									// Create Dialog
									mDialog = ChangeNickNameDialog.newInstance(mDialogCallback);
									
									Bundle b = new Bundle();
									b.putString("nickname", i.nickname());
									b.putParcelable("device", i);
									mDialog.setArguments(b);
						
									// Show
									mDialog.show(ft, NICKNAME_DIALOG_TAG);
								} catch (Exception e) {
									Log.e(LOG_TAG, e.toString());
								}
								
							}});
                        
                        btn = (Button) v.findViewById(R.id.btn_enable_disable);
                    	btn.setText(i.downloadEnabled() ? getString(R.string.disable):getString(R.string.enable));
                    	btn.setOnClickListener(new OnClickListener(){

							@Override
							public void onClick(View v) {
								try {
									mProgressDialog = ProgressDialog.show(DevicesActivity.this, "Change Device Enablement", 
											i.downloadEnabled() ? "Disabling Download on Device " + i.id():"Enabling Download on Device " + i.id(),
											true);
									mService.getBackplane().changeDownloadEnablement(!i.downloadEnabled(), i);
								} catch (BackplaneException e) {
									Log.e(LOG_TAG, "Caught exception changing enablement",e);
									dismissProgressDialog();
								}
							}});

					btn = (Button) v.findViewById(R.id.btn_deregister);

					if(i.isCurrentDevice()){
						btn.setEnabled(false);
					}
					else {
						btn.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								try {
									mProgressDialog = ProgressDialog.show(DevicesActivity.this, "Unregister Device",
											"Unregistering device " + i.id(),
											true);
									mService.getBackplane().unregisterDevice(i);
								} catch (BackplaneException e) {
									Log.e(LOG_TAG, "Caught exception unregistering device", e);
									dismissProgressDialog();
								}
							}
						});
					}
                }
                return v;
        }
	}
	
	public static String getAuthority(Context context) {
		String authority = "";
		try {
			ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			Bundle b =ai.metaData;
			authority = b.getString("com.penthera.virtuososdk.client.pckg");  
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}		
		return authority;
	}

	private ItemAdapter iAdapter;
	
	//ChangeNicknameDialog	
	ChangeNickNameDialog.DialogCallback mDialogCallback = new ChangeNickNameDialog.DialogCallback(){

		@Override
		public void onSelected(DialogFragment dialog, int trigger, Bundle b) {
			switch (trigger) {
			case ChangeNickNameDialog.BTN_APPLY:
				handleApply(b);
				break;
			case ChangeNickNameDialog.BTN_CANCEL:				
				break;			
			default:
				Log.i(LOG_TAG, "bad case: " + trigger);
			}
			try{dialog.dismiss();}catch(Exception e){}
			mDialog = null;
		}
		
	};
		
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_view);
        iBackplaneDevices = new ArrayList<IBackplaneDevice>();
        iAdapter = new ItemAdapter(getApplicationContext(),R.layout.row,iBackplaneDevices);
        ListView lv = (ListView) this.findViewById(android.R.id.list);
        lv.setAdapter(iAdapter);
    }
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		mService.removeObserver(mBackplaneObserver);
		mService.onPause();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		mService.addObserver(mBackplaneObserver);
		mService.onResume();
		if(iBackplaneDevices.size() > 0){
			iBackplaneDevices.clear();
			iAdapter.notifyDataSetChanged();
		}
		handleRefresh();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	private void handleApply(Bundle args) {
		IBackplaneDevice device = args.getParcelable("device");
		if(device != null){
			try {
				mProgressDialog = ProgressDialog.show(DevicesActivity.this, "Change Device Nickname", 
						"Changing Nickname...",
						true);
				mService.getBackplane().changeNickname(args.getString("nickname"), device);
			} catch (BackplaneException e) {
				Log.e(LOG_TAG, "Caught exception requesting nickname change",e);
				dismissProgressDialog();
			}
		}
	}
	
	private void dismissProgressDialog(){
		if(mProgressDialog != null){
			try {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			} catch (Exception e){}
		}
	}
	
	private void handleRefresh(){
		try {
			dismissProgressDialog();
			mProgressDialog = ProgressDialog.show(DevicesActivity.this, "Retrieve Devices", "Requesting Devices from Backplane...",true);
			mService.getBackplane().getDevices(mDeviceListingObserver);
		} catch (BackplaneException be) {
			Log.e(LOG_TAG, "Caught exception requesting devices",be);
			dismissProgressDialog();
		};
	}
}
