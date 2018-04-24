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

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.penthera.sdkdemo.R;
import com.penthera.sdkdemo.Util;
import com.penthera.sdkdemo.activity.SplashActivity;
import com.penthera.virtuososdk.Common.BackplaneCallbackType;
import com.penthera.virtuososdk.Common.BackplaneResult;
import com.penthera.virtuososdk.client.BackplaneException;
import com.penthera.virtuososdk.client.Virtuoso;
import com.penthera.virtuososdk.client.Observers.IBackplaneObserver;

/**
 * Base activity for the all activities connection to the Virtuoso service
 */
public abstract class SdkDemoBaseActivity extends AppCompatActivity {

	/** Check whether intent extras contains this key to determine if a sync should be carried out on connection to Virtuoso */
	public static final String SYNC_ON_CONNECT = "sync_on_connnect";
	private boolean mSyncOnConnect = false;
	
	/**
	 * Handle on service
	 * 
	 * Creating here in the base class, but nothing wrong with creating in the individual activities
	 */
	protected Virtuoso mService;

	/** Menu inflater */
	private MenuInflater inf;

	/** Shows progress during logout */
	private ProgressDialog mLogoutProgress;

	// onCreate
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		


		//check whether we should sync
		Bundle args = getIntent().getExtras();
		mSyncOnConnect = args != null && args.getBoolean(SYNC_ON_CONNECT);
		
		mService = new Virtuoso(getApplicationContext());
		if(mSyncOnConnect){
			//cancel the sync flag
			mSyncOnConnect = false;
			try {
				mService.getBackplane().sync();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
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
		if (mService != null) {
		   	mService.onResume();
		   	mService.addObserver(mBackplaneUnregisterObserver);
		}
	}
	
	// onPause
	@Override
	protected void onPause() {
		super.onPause();
		if (mService != null) {
		   	mService.removeObserver(mBackplaneUnregisterObserver);
		   	mService.onPause();			
		}
	}	

	// --- Menu
		
	// onCreateOptionsMenu
	@Override
	public boolean onCreateOptionsMenu(Menu menu)  {		
		if (inf == null) {
			inf = this.getMenuInflater();
		}		
		inf.inflate(R.menu.activity_base, menu);			
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
	        case R.id.menu_logout: {
	        	handleLogout();
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
		try {
			mService.getBackplane().unregister();
			mLogoutProgress = ProgressDialog.show(this, "Logout", "Performing logout...");
		} catch (BackplaneException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Unregister / Logout observer
	 */
	private IBackplaneObserver mBackplaneUnregisterObserver = new IBackplaneObserver(){

		private void handleLogoutComplete(boolean success){
			
			if(mLogoutProgress != null){ try{ mLogoutProgress.dismiss();} catch(Exception e){}}
			if(success){
				//return to the splash activity
				Util.startActivity(SdkDemoBaseActivity.this, SplashActivity.class, null,Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);

				setResult( Util.CLOSURE_REASON_LOGOUT );
				finish();
			} else {
				Util.showToast(getApplicationContext(), "Logout failed. Try again later.", Toast.LENGTH_LONG);
			}
		}

		@Override
		public void requestComplete(final int request, final int result) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (BackplaneCallbackType.UNREGISTER == request || BackplaneCallbackType.REMOTE_WIPE == request)
					{
						handleLogoutComplete(result == BackplaneResult.SUCCESS);
					}				
				}
			});
		}

	};	
}
