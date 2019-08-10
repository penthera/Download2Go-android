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

import java.net.URL;
import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.penthera.VirtuosoSDK;
import com.penthera.sdkdemo.R;
import com.penthera.sdkdemo.Util;
import com.penthera.sdkdemo.framework.SdkDemoBaseActivity;
import com.penthera.virtuososdk.Common.AuthenticationStatus;
import com.penthera.virtuososdk.Common.EngineStatus;
import com.penthera.virtuososdk.client.EngineObserver;
import com.penthera.virtuososdk.client.IBackplane;
import com.penthera.virtuososdk.client.IBackplaneSettings;
import com.penthera.virtuososdk.client.IIdentifier;
import com.penthera.virtuososdk.client.IService;
import com.penthera.virtuososdk.client.ISettings;
import com.penthera.virtuososdk.client.QueueObserver;
import com.penthera.virtuososdk.client.Observers.IBackplaneObserver;
import com.penthera.virtuososdk.client.Observers.IEngineObserver;
import com.penthera.virtuososdk.client.Observers.IQueueObserver;

/**
 * Demonstration of obtaining various diagnostics information
 */
public class DiagnosticsActivity extends SdkDemoBaseActivity {
	/** Log tag */
	private static final String TAG = DiagnosticsActivity.class.getSimpleName();

	/** Used for posting on the UI thread */
	private Handler mHandler = new Handler();
	/** Retrieves all the diagnostics information from the service */
	private Thread mDataRetrievalThread = null;

	/** The Battery level */
	private int mBatteryLevel = -1;
	/** true, the device is charging */
	private boolean mCharging = false;
	/** Check that there is sufficient power for Virtuoso to download */
	private String mPowerOkay = "";
	/** The current battery threshold as a fraction of the battery level */
	private double mBatThresh = 0.0;

	/** true, the device is registered */
	private boolean mRegistered = false;

	/** The total amount of storage used in MB */
	private long mUsedStorage = 0L;
	/** The total amount of storage available in MB */
	private long mRemainingStorage = 0L;
	/** Check that there is disk space available for Virtuoso to download use */
	private String mDiskOkay = "";
	/** The current headroom in MB */
	private long mHeadroom =  0L;

	/** The current download speed for the current asset */
	private double mCurrentThroughput = 0.0;
	/** The overall download speed for the current session */
	private double mOverallThroughput = 0.0;
	/** The windowed download speed */
	private double mWindowedThroughput = 0.0;

	/** Check that there is a network available for Virtuoso to download */
	private String mNetworkOkay = "";
	/** true, Virtuoso is not currently blocked on a setting related to cell quota */
	private String mCellOkay = "";
	/** Timestamp in milliseconds */
	private long mQuotaStart = 0L;
	/** The amount of cell quota used in MB */
	private long mCellquota = 0L;
	/** The amount of cell quota used in MB */
	private long mUsedCellQuota = 0L;

	/** The download engine status {Blocked, Paused, Downloading, ...} */
	private int mEngStat;
	/** The current max storage setting in MB */
	private long mMaxStorageAllowed = 0L;

	/** The authorization status */
	private int mAuthStatus = AuthenticationStatus.NOT_AUTHENTICATED;
	/** Number of devices enabled for download */
	private long mMaxDeviceDownlaodEnabled;
	/** The number of devices used against the overall quota */
	private long mUsedDownloadEnabledQuota = 0;

	/** specifies the amount of time (in seconds) for which a file is available after it has completed download */
	private long mExpiryAfterDownload;
	/** This value specifies the amount of time (in seconds) after which clients should expire files once they have been played */
	private long mExpiryAfterPlay = 0;

	/** he Max Offline value or -1 if the client has not yet authenticated with the Backplane to retrieve the setting */
	private long mMaxOffline;

	/** The Max Offline value or -1 if the client has not yet authenticated with the Backplane to retrieve the setting */
	private long mLastAuth = 0;
	/** true, device is enabled for download */
	private boolean mIsDownloadEnabledDevice = false;
	/** maximum number of downloaded items allowed on the device. */
	long mMaxPermittedDownloads = 0;
	/** maximum number of times an asset can be downloaded across user's devices. */
	long mMaxPermittedAssetDownloads = 0;
	/** maximum number of downloaded items allowed across user's devices. */
	long mMaxPermittedAccountDownloads = 0;
	/** maximum number of copies of an asset across all devices */
	long mMaxPermittedAssetCopies = 0;

	/** Backplane configuration settings */
	private String mDeviceNickname, mUser, mBackplaneUrl = "";

	//handle on the backplane
	private IBackplane mBackplane;

	//handle on the backplane settings
	private IBackplaneSettings mBackplaneSettings;

	//handle on Virtuoso's settings
	private ISettings mSettings;

	boolean mCanUpdate = true;
	final Object mBlocker = new Object();


	/**
	 * Retrive settings from service
	 */
	private final Runnable iRetriever = new Runnable(){
		@Override
		public void run() {
			//no Service connection needed for these
			//no connection needed
			mUsedStorage = mVirtuoso.getStorageUsed();
			mRemainingStorage = mVirtuoso.getAllowableStorageRemaining();
			mPowerOkay = mVirtuoso.isPowerStatusOK() ? "OKAY":"NOT OKAY";
			mNetworkOkay = mVirtuoso.isNetworkStatusOK()  ? "OKAY":"NOT OKAY";
			mCellOkay = mVirtuoso.isCellularDataQuotaOK() ? "OKAY":"NOT OKAY";
			mUsedCellQuota = mVirtuoso.getUtilizedCellularDataQuota();
			mDiskOkay = mVirtuoso.isDiskStatusOK() ? "OKAY":"NOT OKAY";


			mCellquota = mSettings.getCellularDataQuota();
			mQuotaStart = mSettings.getCellularDataQuotaStart()*1000;
			mMaxStorageAllowed = mSettings.getMaxStorageAllowed();
			mHeadroom = mSettings.getHeadroom();
			mBatThresh = mSettings.getBatteryThreshold();
			mAuthStatus = mBackplane.getAuthenticationStatus();
			mMaxDeviceDownlaodEnabled = mBackplaneSettings.getMaxDevicesAllowedForDownload();
			mUsedDownloadEnabledQuota = mBackplaneSettings.getUsedDownloadQuota();
			mExpiryAfterDownload = mBackplaneSettings.getExpiryAfterDownload();
			mExpiryAfterPlay = mBackplaneSettings.getExpiryAfterPlay();
			mMaxOffline = mBackplaneSettings.getMaxOffline();
			mIsDownloadEnabledDevice = mBackplaneSettings.getDownloadEnabled();
			mMaxPermittedDownloads = mBackplaneSettings.getMaxPermittedDownloads();
			mMaxPermittedAssetDownloads = mBackplaneSettings.getMaxDownloadsPerAsset();
			mMaxPermittedAccountDownloads = mBackplaneSettings.getMaxDownloadsPerAccount();
			mMaxPermittedAssetCopies = mBackplaneSettings.getMaxCopiesPerAsset();
			mDeviceNickname = mBackplaneSettings.getDeviceNickname();
			mUser = mBackplaneSettings.getUserId();
			URL url = mBackplaneSettings.getURL();
			String value = null;
			if(url != null)
				value = url.toString();
			if(TextUtils.isEmpty(value)) value = "https://demo.penthera.com/";
			mBackplaneUrl = value;
			mLastAuth = mBackplane.getLastAuthentication();

			if(mConnectedService != null  && mConnectedService.isBound()){
				try{
					mEngStat = mConnectedService.getStatus();
					mCurrentThroughput = mConnectedService.getCurrentThroughput();
					mOverallThroughput = mConnectedService.getOverallThroughput();
					mWindowedThroughput = mConnectedService.getWindowedThroughput();
				}

				catch(Exception e) {
					e.printStackTrace();
				}
			}
			synchronized(mBlocker){
				if(mCanUpdate);
				mHandler.post(iUpdater);
			}
			mDataRetrievalThread = null;
		}
	};

	private final Runnable iUpdater = new Runnable(){

		@SuppressWarnings("deprecation")
		@Override
		public void run() {
			if(mVirtuoso != null){

				try {
					TextView tv = (TextView)findViewById(R.id.eng_state);
					tv.setText(""+Util.virtuosoStateToString(mEngStat));

					tv = (TextView)findViewById(R.id.auth_state);
					switch(mAuthStatus){
					case AuthenticationStatus.AUTHENTICATED:
						tv.setText("AUTHENTICATED");
						break;
					case AuthenticationStatus.NOT_AUTHENTICATED:
						tv.setText("NOT_AUTHENTICATED");
						break;
					case AuthenticationStatus.AUTHENTICATION_EXPIRED:
						tv.setText("AUTHENTICATION_EXPIRED");
						break;
					case AuthenticationStatus.INVALID_LICENSE:
						tv.setText("INVALID_LICENSE");
						break;
					case AuthenticationStatus.SHUTDOWN:
						tv.setText("LOGGED OUT");
						break;
					}

					tv = (TextView)findViewById(R.id.moff_val);
					tv.setText("" + mMaxOffline + " secs");
					tv = (TextView)findViewById(R.id.eap_val);
					tv.setText("" + mExpiryAfterPlay + " secs");
					tv = (TextView)findViewById(R.id.ead_val);
					tv.setText("" + mExpiryAfterDownload + " secs");
					tv = (TextView)findViewById(R.id.mdd_val);
					tv.setText("" + mMaxDeviceDownlaodEnabled);
					tv = (TextView)findViewById(R.id.deq_val);
					tv.setText("" + mUsedDownloadEnabledQuota);
					tv = (TextView)findViewById(R.id.isdd_val);
					tv.setText("" + mIsDownloadEnabledDevice);
					tv = (TextView)findViewById(R.id.mpd_val);
					tv.setText("" + mMaxPermittedDownloads);
					tv = (TextView)findViewById(R.id.mda_val);
					tv.setText("" + mMaxPermittedAccountDownloads);
					tv = (TextView)findViewById(R.id.mad_val);
					tv.setText("" + mMaxPermittedAssetDownloads);
                    tv = (TextView)findViewById(R.id.mac_val);
                    tv.setText("" + mMaxPermittedAssetCopies);
					tv = (TextView)findViewById(R.id.backplane_user);
					tv.setText(mUser);
					tv = (TextView)findViewById(R.id.device_name);
					tv.setText(mDeviceNickname);
					tv = (TextView)findViewById(R.id.backplane_url);
					tv.setText(mBackplaneUrl);

					tv = (TextView)findViewById(R.id.la_val);
					if(mLastAuth > 0){
						Date d = new Date(mLastAuth*1000);
						tv.setText("" + d.toLocaleString());
					} else {
						tv.setText("never");
					}

					tv = (TextView)findViewById(R.id.power_val);
					tv.setText(mPowerOkay);
					tv = (TextView)findViewById(R.id.network_val);
					tv.setText(mNetworkOkay);
					tv = (TextView)findViewById(R.id.disk_val);
					tv.setText(mDiskOkay);
					tv = (TextView)findViewById(R.id.cell_status_val);
					tv.setText(mCellOkay);

					tv = (TextView)findViewById(R.id.quota_start_val);
					Date d = new Date(mQuotaStart);
					tv.setText("" + d.toLocaleString());
					tv = (TextView)findViewById(R.id.cell_quota_setting_val);
					tv.setText("" + mCellquota);
					tv = (TextView)findViewById(R.id.quota_val);
					tv.setText("" + mUsedCellQuota);
					tv = (TextView)findViewById(R.id.used_storage_val);
					tv.setText("" + mUsedStorage);
					tv = (TextView)findViewById(R.id.available_val);
					tv.setText("" + mRemainingStorage);
					tv = (TextView)findViewById(R.id.ms_val);
					tv.setText("" + mMaxStorageAllowed);
					tv = (TextView)findViewById(R.id.headroom_val);
					tv.setText("" + mHeadroom);
					tv = (TextView)findViewById(R.id.battery_val);
					tv.setText("" + mBatThresh);
					tv = (TextView)findViewById(R.id.level_val);
					tv.setText("" + mBatteryLevel);
					tv = (TextView)findViewById(R.id.charging_val);
					tv.setText("" + mCharging);

					tv = (TextView)findViewById(R.id.cthroughput);
					tv.setText(String.format("%.2f Mb/s", mCurrentThroughput));
					tv = (TextView)findViewById(R.id.othroughput);
					tv.setText(String.format("%.2f Mb/s", mOverallThroughput));
					tv = (TextView)findViewById(R.id.wthroughput);
					tv.setText(String.format("%.2f Mb/s", mWindowedThroughput));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	};

	// onCreate
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_diagnostics);

		mSettings = mVirtuoso.getSettings();
		mBackplane = mVirtuoso.getBackplane();
		mBackplaneSettings = mBackplane.getSettings();
		mConnectedService = mVirtuoso.getService();

		if (!mRegistered) registerApiReceiver();

		TextView tvsdk = (TextView)findViewById(R.id.textView_version);
		tvsdk.setText(VirtuosoSDK.BUILD_VERSION);

		this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (mRegistered) unregisterApiReceiver();
		mVirtuoso.removeObserver(mEngineObserver);
		mVirtuoso.removeObserver(mBackplaneObserver);
		mVirtuoso.removeObserver(mEnginePauseResumeObserver);
		mVirtuoso.removeObserver(mQueueObserver);
		synchronized(mBlocker){
			mCanUpdate = false;
		}

		mHandler.removeCallbacks(iUpdater);
	}

	@Override
	protected void onResume() {
		super.onResume();

		synchronized(mBlocker){
			mCanUpdate = true;
		}

		mVirtuoso.addObserver(mBackplaneObserver);
		mVirtuoso.addObserver(mEngineObserver);
		mVirtuoso.addObserver(mEnginePauseResumeObserver);
		mVirtuoso.addObserver(mQueueObserver);

		if (!mRegistered) registerApiReceiver();
	}

	private IBackplaneObserver mBackplaneObserver = new IBackplaneObserver() {

		@Override
		public void requestComplete(int callbackType, int result, final String errorMessage) {
			startDataRetriever();
		}
	};

	private IQueueObserver mQueueObserver = new QueueObserver(){
		@Override
		public void enginePerformedProgressUpdateDuringDownload(IIdentifier aFile) {
			startDataRetriever();
		}
	};

	private IEngineObserver mEngineObserver = new EngineObserver() {
		@Override
		public void engineStatusChanged(int arg0) {
			startDataRetriever();
		}

		@Override
		public void settingChanged(int arg0) {
			startDataRetriever();
		}

		@Override
		public void backplaneSettingChanged(int arg0) {
			startDataRetriever();
		}
	};

	public void startDataRetriever(){
		if(mDataRetrievalThread == null){
			mDataRetrievalThread = new Thread(iRetriever);
			mDataRetrievalThread.start();
		}
	}

	private BroadcastReceiver mApiReceiver = new BroadcastReceiver(){
		static final String TAG = "Diagnostics-ClientMessageReceiver";
		@Override
		public void onReceive(Context context, Intent aIntent) {
			String action = aIntent.getAction();
			if (action == null) {
				Log.e(TAG,"onReceive(): null action");
				return;
			}

			Log.d(TAG, "Diagnostics Broadcast Receiver: received - " + action);

			if(action.equals(Intent.ACTION_POWER_CONNECTED)){
				mCharging = true;
				startDataRetriever();
			} else if(action.equals(Intent.ACTION_POWER_DISCONNECTED)){
				mCharging = false;
				startDataRetriever();
			} else if(action.equals(Intent.ACTION_BATTERY_CHANGED)){
				int plugged = aIntent.getIntExtra("plugged", 0);
				boolean charge = (plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB);
				if(mCharging != charge){
					mCharging = charge;
				}
				int raw_level = aIntent.getIntExtra("level", -1);
				int scale = aIntent.getIntExtra("scale", -1);

				if (raw_level >=0 && scale > 0) mBatteryLevel = (raw_level*100)/scale;
			} else {
				Log.w(TAG,"onReceive(): unknown action: " + action);
			}
		}
	};

	private void registerApiReceiver() {
		Log.d(TAG, "Registering for messages");

		Context context = getApplicationContext();
		context.registerReceiver(mApiReceiver, new IntentFilter(Intent.ACTION_POWER_CONNECTED));
		context.registerReceiver(mApiReceiver, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
		context.registerReceiver(mApiReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		mRegistered = true;
	}

	/**
	 * Unregister receivers
	 *
	 */
	private void unregisterApiReceiver() {
		Log.d(TAG, "Unregistering for messages");
		Context context = getApplicationContext();
		context.unregisterReceiver(mApiReceiver);
		mRegistered = false;
	}

	boolean mPause_requested = false;
	boolean mResume_requested = false;
	private IEngineObserver mEnginePauseResumeObserver = new EngineObserver() {
		@Override
		public void engineStatusChanged(
				int arg0) {
			if(mPause_requested){
				if(arg0 == EngineStatus.PAUSED) mPause_requested = false;
			}
			if(mResume_requested){
				if(arg0 != EngineStatus.PAUSED) mResume_requested = false;
			}
		}
	};
}

