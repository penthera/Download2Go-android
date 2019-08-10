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

import java.util.Date;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.penthera.sdkdemo.R;
import com.penthera.sdkdemo.framework.SdkDemoBaseActivity;
import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.Common.AuthenticationStatus;
import com.penthera.virtuososdk.client.BackplaneException;
import com.penthera.virtuososdk.client.EngineObserver;
import com.penthera.virtuososdk.client.IBackplane;
import com.penthera.virtuososdk.client.IBackplaneSettings;
import com.penthera.virtuososdk.client.ISettings;
import com.penthera.virtuososdk.client.Observers.IBackplaneObserver;
import com.penthera.virtuososdk.client.Observers.IEngineObserver;

/**
 * Demonstrate the settings capabilities of the Virtuoso SDK
 */
public class SettingsActivity extends SdkDemoBaseActivity {

	/** The current value for battery threshold */
	private SeekBar mBatterythreshold;
	/** Change the current setting for max storage */
	private EditText mMaxstorage;
	/** Change the current value for headroom */
	private EditText mHeadroom;
	/** Change the current vlaue for cell quota */
	private EditText mCellquota;
	/** The time the cell quota started from */
	private TextView mCellquotastart;
	/** Change the current value for number of permitted segment errors */
	private EditText mPermittedSegmentErrors;
	/** Change the current value for the http code to be returned by proxy on an errored segment */
	private EditText mProxySegmentErrorHttpCode;
	/** Apply the settings */
	private Button mApply; 
	/** The battery level */
	private TextView mBatteryDetail;
	/** the current fragment progress rate used by Virtuoso */
	private EditText mHlsFragmentProgressRate;
	

	/** The current percent based progress rate used by Virtuoso */
	private SeekBar mProgressPercent;
	/** The percent level */
	private TextView mProgressPercentDetails;
	/** The current time based progress rate used by Virtuoso */
	private EditText mProgressTimed;
	
	/** The connection timeout for HTTP transactions */
	private EditText mConnectionTimeout;
	/** The socket timeout for HTTP transactions */
	private EditText mSocketTimeout;
	
	/** The download enablement be changed */
	private TextView mEnablement;
	/** Chang ethe download enablement */
	private Button mEnable;

	/** Change where items will be stored on disk relative to the root of the application */
	private EditText mDestination;

	/** Toggle if the downloader should always request download permissions from the backplane,
	 * regardless of whether there are known limits on downloads.
	 */
	private ToggleButton mAlwaysRequestPermissions;

    /** Toggle if the downloader should leave background mode and hide any current notification when
     * downloads are paused. */
	private ToggleButton mBackgroundOnPause;


	/** Toggle whether DRM licenses are automitcally renewed */
	private ToggleButton mAutoRenewDrmLicenses;

	/** Change which video codecs to include in downloaded content*/
	private EditText mCodecs;

	/** Handle to the backplane interface */
	private IBackplane mBackplane;

	/** Handle to the backplane settings interface */
	private IBackplaneSettings mBackplaneSettings;

	/** Handle to the settings interface */
	private ISettings mSettings;
	
	private Handler iHandler = new Handler();
	
	/**
	 * Retrieve settings values form the service
	 */
	private final Runnable iUpdater = new Runnable(){
		@SuppressWarnings("deprecation")
		@Override
		public void run() {

			long quotaStart = mSettings.getCellularDataQuotaStart();

			Date d = new Date(quotaStart*1000);
			mCellquotastart.setText(d.toLocaleString());
			
			int val = (int)(mSettings.getBatteryThreshold() * 100);
			val = val < 0 ? 0: val > 100 ? 100 : val;
			
			mBatterythreshold.setProgress(val);
			mBatteryDetail.setText("Battery Threshold " + val +"%");
			mMaxstorage.setText(""+mSettings.getMaxStorageAllowed());
			mHeadroom.setText(""+mSettings.getHeadroom());
			mCellquota.setText(""+mSettings.getCellularDataQuota());
			mDestination.setText(mSettings.getDestinationPath());
			mHlsFragmentProgressRate.setText(""+ mSettings.getProgressUpdatesPerSegment());
			mEnablement.setText("" + mBackplaneSettings.getDownloadEnabled());
			mEnable.setText(mBackplaneSettings.getDownloadEnabled() ? "Disable":"Enable");
			mEnable.setEnabled(mBackplane.getAuthenticationStatus() != AuthenticationStatus.NOT_AUTHENTICATED);
			mProgressPercent.setProgress(mSettings.getProgressUpdateByPercent());
			mProgressPercentDetails.setText("Report Progress "+mSettings.getProgressUpdateByPercent()+"%");
			mProgressTimed.setText(""+mSettings.getProgressUpdateByTime());
			mPermittedSegmentErrors.setText(""+mSettings.getMaxPermittedSegmentErrors());
			mProxySegmentErrorHttpCode.setText(""+mSettings.getSegmentErrorHttpCode());
			mCodecs.setText(mSettings.getAudioCodecsToDownload() != null ? TextUtils.join(",", mSettings.getAudioCodecsToDownload()) : "");
            mBackgroundOnPause.setChecked(mSettings.getRemoveNotificationOnPause());
            mAutoRenewDrmLicenses.setChecked(mSettings.isAutomaticDrmLicenseRenewalEnabled());
		}};
			
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mBackplane = mVirtuoso.getBackplane();
        mBackplaneSettings = mBackplane.getSettings();
        mSettings = mVirtuoso.getSettings();
        
        mDestination = (EditText) findViewById(R.id.destination_value);
        mCellquota = (EditText) findViewById(R.id.cellquota_value);
        mCellquotastart = (TextView) findViewById(R.id.cellquota_date_value);
        mHeadroom = (EditText) findViewById(R.id.headroom_value);
        mMaxstorage = (EditText) findViewById(R.id.maxstorage_value);
        mBatterythreshold = (SeekBar) findViewById(R.id.battery_value);
        mApply = (Button)findViewById(R.id.apply);
        mBatteryDetail = (TextView)findViewById(R.id.battery);
        mHlsFragmentProgressRate = (EditText) findViewById(R.id.edt_fragment_progress_rate);
        mConnectionTimeout = (EditText) findViewById(R.id.edt_connection_timeout);
        mSocketTimeout = (EditText) findViewById(R.id.edt_socket_timeout);
		mPermittedSegmentErrors = (EditText) findViewById(R.id.edt_max_segment_errors);
		mProxySegmentErrorHttpCode = (EditText) findViewById(R.id.edt_proxy_segment_error_code);
        
        mConnectionTimeout.setText("" + Common.DEFAULT_HTTP_CONNECTION_TIMEOUT);
        mSocketTimeout.setText("" + Common.DEFAULT_HTTP_SOCKET_TIMEOUT);

        mConnectionTimeout.setText("" + mSettings.getHTTPConnectionTimeout());
        mSocketTimeout.setText("" + mSettings.getHTTPSocketTimeout());
        
        mProgressPercent = (SeekBar) findViewById(R.id.progress_percent_value);
        mProgressTimed = (EditText) findViewById(R.id.progress_timed_value);
        mProgressPercentDetails  = (TextView)findViewById(R.id.ProgressPercent);
        
        SeekBar.OnSeekBarChangeListener seekProgressChangeListener = new SeekBar.OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if(fromUser){
					mProgressPercentDetails.setText("Report Progress " + progress +"%");				
				}
				
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
        	
        };
        mProgressPercent.setOnSeekBarChangeListener(seekProgressChangeListener);
        
        TextView.OnEditorActionListener actionListener = new TextView.OnEditorActionListener(){

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				// ignore enter key
				if (event!=null && (event.getAction() == KeyEvent.ACTION_DOWN) || (event.getAction() == KeyEvent.ACTION_MULTIPLE)){
					return true;
				}

				return false;
			}
        	
        };
        mDestination.setOnEditorActionListener(actionListener);
        
        SeekBar.OnSeekBarChangeListener seekChangeListener = new SeekBar.OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if(fromUser){
					mBatteryDetail.setText("Battery Threshold " + progress +"%");				
				}
				
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}        	
        };
        
        mBatterythreshold.setOnSeekBarChangeListener(seekChangeListener);
                
        mApply.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				save();
				
			}});

        findViewById(R.id.battery_reset).setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				mSettings.resetBatteryThreshold().save();
			}});

        findViewById(R.id.cellquota_date_reset).setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				mSettings.resetCellularDataQuotaStart().save();
			}});
        
        findViewById(R.id.cellquota_reset).setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				mSettings.resetCellularDataQuota().save();
			}});
        
        findViewById(R.id.headroom_reset).setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				mSettings.resetHeadroom().save();
			}});
        
        findViewById(R.id.maxstorage_reset).setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				mSettings.resetMaxStorageAllowed().save();
			}});
        
        findViewById(R.id.destination_reset).setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				mSettings.resetDestinationPath();
			}});
        
        mEnablement = (TextView) findViewById(R.id.download_enabled_value);
        mEnable = (Button)findViewById(R.id.enable_disable);
        mEnable.setOnClickListener(new OnClickListener(){
        	@Override
			public void onClick(View arg0) {
        		try {
					mBackplane.changeDownloadEnablement(!mBackplaneSettings.getDownloadEnabled());
				} catch (BackplaneException e) {
					e.printStackTrace();
				}
        	}
			
        });
        mEnable.setEnabled(false);

        mAlwaysRequestPermissions = (ToggleButton) findViewById(R.id.always_req_perm_toggle);
        mAlwaysRequestPermissions.setChecked(mSettings.alwaysRequestPermission());
        mAlwaysRequestPermissions.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mSettings.setAlwaysRequestPermission(mAlwaysRequestPermissions.isChecked()).save();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        mAlwaysRequestPermissions.setEnabled(true);

        mCodecs = findViewById(R.id.sdk_allowed_video_codecs);
        findViewById(R.id.sdk_allowed_codecs_reset).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mSettings.resetAudioCodecsToDownload().save();
			}
		});

        mBackgroundOnPause = (ToggleButton) findViewById(R.id.background_on_pause_toggle);
        mBackgroundOnPause.setChecked(mSettings.getRemoveNotificationOnPause());
        mBackgroundOnPause.setEnabled(true);
        mBackgroundOnPause.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mSettings.setRemoveNotificationOnPause(mBackgroundOnPause.isChecked()).save();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

		mAutoRenewDrmLicenses = (ToggleButton) findViewById(R.id.auto_renew_drm_licenses);
		mAutoRenewDrmLicenses.setChecked(mSettings.isAutomaticDrmLicenseRenewalEnabled());
		mAutoRenewDrmLicenses.setEnabled(true);
		mAutoRenewDrmLicenses.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					mSettings.setAutomaticDrmLicenseRenewalEnabled(mAutoRenewDrmLicenses.isChecked()).save();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		iHandler.post(iUpdater);	
    }

	// onPause
    @Override
	protected void onPause() {
		super.onPause();
		mVirtuoso.removeObserver(mBackplaneObserver);
		mVirtuoso.removeObserver(mEngineObserver);
	}

    // onResume
	@Override
	protected void onResume() {
		super.onResume();
		mVirtuoso.addObserver(mBackplaneObserver);
		mVirtuoso.addObserver(mEngineObserver);
	}
		
	private IBackplaneObserver mBackplaneObserver = new IBackplaneObserver(){

		@Override
		public void requestComplete(int callbackType, int result, final String errorMessage) {
			iHandler.post(iUpdater);

			if (callbackType == Common.BackplaneCallbackType.DOWNLOAD_ENABLEMENT_CHANGE &&
					result == Common.BackplaneResult.MAXIMUM_ENABLEMENT_REACHED) {
				// Show a warning message - in this case we show the server response directly,
				// but this would not be the case in a production application.
				iHandler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(SettingsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
					}
				});
			}
		}
	};
	
	private IEngineObserver mEngineObserver = new EngineObserver() {
		
		@Override
		public void settingChanged(int aFlags) {
			iHandler.post(iUpdater);}
		@Override
		public void backplaneSettingChanged(int aFlags) {
			iHandler.post(iUpdater);}

		};
	
	void save(){
		try{
			mSettings.setProgressUpdateByTime(Long.parseLong(mProgressTimed.getText().toString()))
					.setProgressUpdateByPercent(mProgressPercent.getProgress())
					.setBatteryThreshold(((float)mBatterythreshold.getProgress())/100)
					.setDestinationPath(mDestination.getText().toString().trim())
					.setCellularDataQuota(Long.parseLong(mCellquota.getText().toString()))
					.setHeadroom(Long.parseLong(mHeadroom.getText().toString()))
					.setMaxStorageAllowed(Long.parseLong(mMaxstorage.getText().toString()))
					.setProgressUpdatesPerSegment(Integer.parseInt(mHlsFragmentProgressRate.getText().toString()))
					.setHTTPConnectionTimeout(Integer.parseInt(mConnectionTimeout.getText().toString()))
					.setHTTPSocketTimeout(Integer.parseInt(mSocketTimeout.getText().toString()))
					.setSegmentErrorHttpCode(Integer.parseInt(mProxySegmentErrorHttpCode.getText().toString()))
					.setMaxPermittedSegmentErrors(Integer.parseInt(mPermittedSegmentErrors.getText().toString()))
					.setAudioCodecsToDownload(mCodecs.getText().toString().length() > 0 ? mCodecs.getText().toString().split(",") : null)
					.save();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	public void onFragmentProgressRateReset(View view) {
		mSettings.resetProgressUpdatesPerSegment().save();
	}
	
	public void onConnectionTimeoutReset(View view) {
		mSettings.resetHTTPConnectionTimeout().save();	
	}
	
	public void onSocketTimeoutReset(View view) {
		mSettings.resetHTTPSocketTimeout().save();
	}
	
	public void onProgressPercentReset(View view) {
		mSettings.resetProgressUpdateByPercent().save();
	}
	
	public void onProgressTimedReset(View view) {
		mSettings.resetProgressUpdateByTime().save();
	}

	public void onAllowedVideoCodecsReset(View view){
		mSettings.resetAudioCodecsToDownload().save();
	}
}
