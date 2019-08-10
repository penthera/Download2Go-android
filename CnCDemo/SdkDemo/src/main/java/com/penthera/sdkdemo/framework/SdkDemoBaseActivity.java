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

package com.penthera.sdkdemo.framework;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.penthera.sdkdemo.Config;
import com.penthera.sdkdemo.R;
import com.penthera.sdkdemo.Util;
import com.penthera.sdkdemo.activity.SplashActivity;
import com.penthera.sdkdemo.dialog.PermissionsExplanationDialog;
import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.Common.BackplaneCallbackType;
import com.penthera.virtuososdk.Common.BackplaneResult;
import com.penthera.virtuososdk.client.BackplaneException;
import com.penthera.virtuososdk.client.EngineObserver;
import com.penthera.virtuososdk.client.IIdentifier;
import com.penthera.virtuososdk.client.ISegmentedAsset;
import com.penthera.virtuososdk.client.IService;
import com.penthera.virtuososdk.client.Observers;
import com.penthera.virtuososdk.client.ServiceException;
import com.penthera.virtuososdk.client.Virtuoso;
import com.penthera.virtuososdk.client.Observers.IBackplaneObserver;

/**
 * Base activity for the all activities connection to the Virtuoso service
 */
public abstract class SdkDemoBaseActivity extends AppCompatActivity {

    private static final String LOG_TAG = SdkDemoBaseActivity.class.getSimpleName();

    /* Permission request id for storage permissions */
    public static final int PERMISSION_REQUEST = 1;

	/** Check whether intent extras contains this key to determine if a sync should be carried out on connection to Virtuoso */
	public static final String SYNC_ON_CONNECT = "sync_on_connnect";
	private boolean mSyncOnConnect = false;

	/**
	 * Handle on the base Penthera SDK API
	 *
	 * Creating here in the base class, but nothing wrong with creating in the individual activities
	 */
	protected Virtuoso mVirtuoso;

	/**
	 * Handle on the service which controls background downloading and management of assets.
	 */
	protected IService mConnectedService;

	/** Menu inflater */
	private MenuInflater inf;

	/** Shows progress during logout */
	private ProgressDialog mLogoutProgress;

    /**
     * Track if we are currently handling a permissions request, to ensure multiple events do not
     * result in multiple dialogs.
     */
    private boolean mHandlingPermissionRequest = false;

    /**
     * Connection observer monitors when the service is bound
     */
    protected IService.IConnectionObserver mConnectionObserver = new IService.IConnectionObserver(){

        @Override
        public void connected() {
            Log.i(LOG_TAG, "serviceConnected");
            startDataRetriever();
        }

        @Override
        public void disconnected() {
        }

    };

    /**
     * A simple engine observer captures settings and licensing errors and informs the user.
     */
    protected Observers.IEngineObserver mSettingsErrorEngineObserver =  new EngineObserver() {

        @Override
        public void settingsError(final int aFlags) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String reasons = Util.buildIssuesString(aFlags);

                    Toast.makeText(SdkDemoBaseActivity.this, getString(R.string.error_blocked_reasons, reasons), Toast.LENGTH_LONG).show();

                    // Check if we should prompt for permissions.
                    // You need to be careful about doing this as multiple processes may raise this exception so we need to prevent it being showing multiple times at the UI level
                    doPermissionsCheck(false);
                }
            });
        }

		@Override
		public void assetLicenseRetrieved(final IIdentifier aItem, final boolean aSuccess) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {

				    // DRM can only occur on segmented assets
					ISegmentedAsset asset = (ISegmentedAsset)aItem;
					Toast.makeText(SdkDemoBaseActivity.this, getString(aSuccess ? R.string.license_fetch_success : R.string.license_fetch_failure, asset.getAssetId()), Toast.LENGTH_LONG).show();
				}
			});
		}
    };

	// onCreate
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);



		//check whether we should sync
		Bundle args = getIntent().getExtras();
		mSyncOnConnect = args != null && args.getBoolean(SYNC_ON_CONNECT);

		mVirtuoso = new Virtuoso(getApplicationContext());
        mConnectedService = mVirtuoso.getService();
		if(mSyncOnConnect){
			//cancel the sync flag
			mSyncOnConnect = false;
			try {
				mVirtuoso.getBackplane().sync();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
        doPermissionsCheck(false);
	}

	//onActivityResult

	/**
	 * Dispatch incoming result to the correct fragment.
	 *
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
	@Override
	protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
		super.onActivityResult( requestCode, resultCode, data );
		if(resultCode == Util.CLOSURE_REASON_LOGOUT){
			setResult( Util.CLOSURE_REASON_LOGOUT );
			finish();
		}
	}

	// onResume
	@Override
	protected void onResume() {
		super.onResume();
		if (mVirtuoso != null) {
			mVirtuoso.onResume();
			mVirtuoso.addObserver(mBackplaneUnregisterObserver);
			mVirtuoso.addObserver(mSettingsErrorEngineObserver);
		}
        mConnectedService.setConnectionObserver(mConnectionObserver);
        mConnectedService.bind();
	}

	// onPause
	@Override
	protected void onPause() {
		super.onPause();
		if (mVirtuoso != null) {
			mVirtuoso.removeObserver(mBackplaneUnregisterObserver);
			mVirtuoso.removeObserver(mSettingsErrorEngineObserver);
			mVirtuoso.onPause();
		}
        mConnectedService.unbind();
        mConnectedService.setConnectionObserver(null);
	}

	// --- Menu

	// onCreateOptionsMenu
	@Override
	public boolean onCreateOptionsMenu(Menu menu)  {
		if (inf == null) {
			inf = this.getMenuInflater();
		}
		inf.inflate(R.menu.activity_base, menu);

		boolean isShutdown = mVirtuoso.getBackplane().getAuthenticationStatus() == Common.AuthenticationStatus.SHUTDOWN;
		menu.findItem(R.id.menu_login).setVisible(isShutdown);
		menu.findItem(R.id.menu_logout).setVisible(!isShutdown);

		return true;
	}

	// onOptionsItemSelected
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home: {
				handleBack();
				return true;
			}
			case R.id.menu_login: {
				handleLogin();
				return true;
			}
	        case R.id.menu_logout: {
	        	handleLogout();
	        	return true;
	        }
			case R.id.menu_unregister: {
				handleUnregister();
				return true;
			}
			case R.id.menu_refresh_ads:{
				mVirtuoso.getAssetManager().getAdManager().refreshAllAds();
				return true;
			}
		}
		return false;
	}

	// --- Menu Handlers

	/**
	 * User pressed home / back carrot
	 */
	private void handleBack() {
		setResult( Util.CLOSURE_REASON_BACK );
		finish();
	}

	/**
	 * Logout the user
	 */
	private void handleLogout() {
		mVirtuoso.shutdown();
			invalidateOptionsMenu();
	}

	private void handleLogin() {
		try {
			mVirtuoso.startup(mVirtuoso.getBackplane().getSettings().getURL(),
					mVirtuoso.getBackplane().getSettings().getUserId(),
					null, Config.BACKPLANE_PUBLIC_KEY, Config.BACKPLANE_PRIVATE_KEY, null);
		} catch (IllegalArgumentException iae){
			Log.w(LOG_TAG, "Missing login details");
			Toast.makeText(SdkDemoBaseActivity.this, getString(R.string.error_login_details), Toast.LENGTH_LONG).show();
		}
		invalidateOptionsMenu();
	}

	/**
	 * Unregister the user
	 */
	private void handleUnregister() {
		try {
			mVirtuoso.getBackplane().unregister();
			mLogoutProgress = ProgressDialog.show(this, "Logout", "Performing logout...");
		} catch (BackplaneException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Unregister / Logout observer
	 */
	private IBackplaneObserver mBackplaneUnregisterObserver = new IBackplaneObserver(){

		private void handleUnregisterComplete(boolean success){

			if(mLogoutProgress != null){ try{ mLogoutProgress.dismiss();} catch(Exception e){}}
			if(success){
				//return to the splash activity
				Util.startActivity(SdkDemoBaseActivity.this, SplashActivity.class, null,Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);

				setResult( Util.CLOSURE_REASON_LOGOUT );
				finish();
			} else {
				Util.showToast(getApplicationContext(), "Unregister failed. Try again later.", Toast.LENGTH_LONG);
			}
		}

		@Override
		public void requestComplete(final int request, final int result, final String errorMessage) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (BackplaneCallbackType.UNREGISTER == request || BackplaneCallbackType.REMOTE_WIPE == request)
					{
						handleUnregisterComplete(result == BackplaneResult.SUCCESS);
					}
				}
			});
		}

	};

    /**
     * This provides a hook for individual activities to take an action when the service is bound.
     */
    public void startDataRetriever(){
        // Intentionally blank
    }

	// Permissions checks are necessary if you change the destination path in settings to a location other than the
	// application protected directory. In the case of any other external filesystem being used, READ_EXTERNAL_STORAGE
	// and WRITE_EXTERNAL_STORAGE permissions will be required.
	private void doPermissionsCheck(boolean force) {
		if (!mVirtuoso.isDiskPermissionOK() && !mHandlingPermissionRequest) {
			mHandlingPermissionRequest = true;
			if (!force && ActivityCompat.shouldShowRequestPermissionRationale(this,
					android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ) {
				// Show an explanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.
				PermissionsExplanationDialog dialog = PermissionsExplanationDialog.newInstance(new PermissionsExplanationDialog.PermissionResponseListener() {
					@Override
					public void permissionResponse(boolean allow) {
						mHandlingPermissionRequest = false;
						if (allow) {
							doPermissionsCheck(true);
						}
					}
				});
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				dialog.show(ft,"PERMISSIONS");
			} else {
				// No explanation needed; request the permission
				ActivityCompat.requestPermissions(this,
						new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
						PERMISSION_REQUEST);
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSION_REQUEST: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 1
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED
						&& grantResults[1] == PackageManager.PERMISSION_GRANTED) {
					// permission was granted
					try {
						if(mConnectedService != null && mConnectedService.isBound()) {
							if (mConnectedService.getStatus() == Common.EngineStatus.PAUSED) {
                                mConnectedService.resumeDownloads();
							}
						}
					} catch (ServiceException se) {
						Log.d(LOG_TAG, "Could not resume downloads on permission denied");
					}
				} else {
					// permission denied, boo! Disable the
					// functionality that depends on this permission.
					try {
						if(mConnectedService != null && mConnectedService.isBound()) {
                            mConnectedService.pauseDownloads();
						}
					} catch (ServiceException se) {
						Log.d(LOG_TAG, "Could not pause downloads on permission denied");
					}
				}
				mHandlingPermissionRequest = false;
				return;
			}

		}
	}
}
