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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;

import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.penthera.sdkdemo.Config;
import com.penthera.sdkdemo.R;
import com.penthera.sdkdemo.Util;
import com.penthera.sdkdemo.catalog.Catalog.CatalogColumns;
import com.penthera.sdkdemo.catalog.CatalogContentProvider;
import com.penthera.sdkdemo.framework.SdkDemoBaseActivity;
import com.penthera.virtuoso.net.security.client.IKeyStore;
import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.Common.BackplaneCallbackType;
import com.penthera.virtuososdk.client.EngineObserver;
import com.penthera.virtuososdk.client.IPushRegistrationObserver;
import com.penthera.virtuososdk.client.Observers;
import com.penthera.virtuososdk.client.ServiceException;
import com.penthera.virtuososdk.client.Observers.IBackplaneObserver;

/**
 * Splash screen
 */
public class SplashActivity extends SdkDemoBaseActivity {

	/** Log tag */
	private static final String TAG = SplashActivity.class.getName();
	
	// --- Messages
	/** Fade in message [not authenticated] */
	private static final int MSG_FADE_IN = 1;
	/** Fade in message [when authenticated] */
	private static final int MSG_FADE_IN_LOGGED_IN = 2;

	// --- Editors
	/** Backplane URL edit text*/
	private EditText mUrl;
	/** Backplane user edit text */
	private EditText mUser;
		
	// --- Options
	/** Fade in delay in milliseconds */
	private static final int FADE_IN_DELAY = 1000;
	/** true, shows splash briefly even if user is already registered */
	private static boolean ALWAYS_SHOW_SPLASH = false;

	/** Handler used to fade in splash screen */
	private static Handler mHandler;

	/** 
	 * UI Thread handler 
	 */
	private static class MyHandler extends Handler {
		/** The activity */
		private Activity mActivity;

		public MyHandler(Activity activity) {
			mActivity = activity;
		}
				
		// Handle message
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				// Fade in second stage UI elements
				case MSG_FADE_IN_LOGGED_IN: {
					fadeIn(R.id.txt_welcome);
					fadeIn(R.id.btn_continue);
					break;
				}
				case MSG_FADE_IN: {
					fadeIn(R.id.txt_welcome);
					fadeIn(R.id.btn_continue);

					fadeIn(R.id.lyt_url);
					fadeIn(R.id.lyt_user);
					break;
				}
				default: {
 					Log.e(TAG, "Error: " + msg.what);
				}
			}
		}
		
		/**
		 * Fade in screen elements
		 * 
		 * @param id the resource Id to fade in
		 */
		private void fadeIn(int id) {
			View v = mActivity.findViewById(id);
			v.setVisibility(View.VISIBLE);
			Animation a = AnimationUtils.loadAnimation(mActivity, R.anim.fade_in);
			v.setAnimation(a);			
		}
	};

	// onCreate
	@Override
    public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);		                
        Log.i(TAG, "onCreate splash");

        setContentView(R.layout.activity_splash);        
    	mHandler = new MyHandler(this);
    	    	
    	// Editors
    	mUrl = (EditText) this.findViewById(R.id.edt_url);
    	mUrl.setText(Config.BACKPLANE_URL);
    	mUser = (EditText) this.findViewById(R.id.edt_user);
    	String identifier = mVirtuoso.getBackplane().getSettings().getDeviceId();
    	mUser.setText(identifier);
    	
    	// Authentication
		int status = mVirtuoso.getBackplane().getAuthenticationStatus();
		if (status == Common.AuthenticationStatus.NOT_AUTHENTICATED) {
	    	mHandler.sendEmptyMessageDelayed(MSG_FADE_IN, FADE_IN_DELAY);
		} else {				
			if (ALWAYS_SHOW_SPLASH) {
				mHandler.sendEmptyMessageDelayed(MSG_FADE_IN_LOGGED_IN, FADE_IN_DELAY);
			} else {
				Util.startActivity(SplashActivity.this, MainActivity.class, null);
			}
		}
    	
    	// Seed Catalog
    	new SeedCatalogTask().execute();
    	
    	// Import Certificate
    	new ImportCertificateTask().execute();
    	Common.Events.addAppLaunchEvent(SplashActivity.this);
    }

	// onCreateOptionsMenu
	@Override
	public boolean onCreateOptionsMenu(Menu menu)  {		
		return false;
	}
	
	/**
	 * Get URL from editor
	 * 
	 * @return
	 * @throws MalformedURLException 
	 */
	private URL getUrl() throws MalformedURLException {
		String url = mUrl.getText().toString();		
		if (TextUtils.isEmpty(url)) {
			url = Config.BACKPLANE_URL;
		}
		return new URL(url);
	}
	
	/**
	 * get user from editor
	 * 
	 * @return
	 */
	private String getUser() {
		String user = mUser.getText().toString();		
		if (TextUtils.isEmpty(user)) {
			user = mVirtuoso.getBackplane().getSettings().getDeviceId();
		}
		return user;
	}

	// onStart
	@Override
	public void onStart()
	{
		super.onStart();		
	}
    
	// onResume
    @Override
    public void onResume() {
    	super.onResume();    	
    	if (mVirtuoso!=null) {
			mVirtuoso.addObserver(mBackplaneObserver);
			mVirtuoso.addObserver(mEngineObserver);
    	}
    }

    // onPause
    @Override
    public void onPause() {
    	super.onPause();
    	if (mVirtuoso != null) {
			mVirtuoso.removeObserver(mBackplaneObserver);
			mVirtuoso.removeObserver(mEngineObserver);
    	}

    }

    // onDestroy
    @Override
    public void onDestroy() {
    	if (mHandler != null) {
    		mHandler.removeCallbacksAndMessages(null);
    	}
    	super.onDestroy();
    }

	private void configureDefaultSettings(){

		try{
			Class.forName( "com.amazon.device.messaging.ADM" );
			/*
			Fire OS 4 could not cope with the amount of progress notifications.
			Change the progress update rate on percent value to 2 to minimise issues
			with the cursor notifications.
			 */
			if(Build.VERSION.SDK_INT <= 20){
				Log.d(TAG,"Amazon device based on kitkat or below.");
				mVirtuoso.getSettings().setProgressUpdateByPercent(4);
			}
			else{

				Log.d(TAG,"Amazon device based on lollipop or better.");
			}
		} catch (ClassNotFoundException e) {
			Log.d(TAG,"Not an amazon device");
		}
	}


    public void onContinue(View v) {
		configureDefaultSettings();
    	try {
    			final URL url = getUrl();
    			final String user = getUser();
			mVirtuoso.startup(url, user, null, Config.BACKPLANE_PUBLIC_KEY, Config.BACKPLANE_PRIVATE_KEY, new IPushRegistrationObserver() {
					@Override
					public void onServiceAvailabilityResponse(int pushService, int errorCode) {
						if(pushService == Common.PushService.FCM_PUSH && errorCode != ConnectionResult.SUCCESS)
						{
							final GoogleApiAvailability gApi = GoogleApiAvailability.getInstance();
							if(gApi.isUserResolvableError(errorCode)){

								runOnUiThread(new Runnable(){

									@Override
									public void run() {
										gApi.makeGooglePlayServicesAvailable(SplashActivity.this)
												.addOnCompleteListener(new OnCompleteListener<Void>() {
													@Override
													public void onComplete(@NonNull Task<Void> task) {
														Log.d(TAG,"makeGooglePlayServicesAvailable complete");
														mGcmRegistered = true;
														if(task.isSuccessful()){
															Log.d(TAG,"makeGooglePlayServicesAvailable completed successfully");
														}
														else{
															Exception e = task.getException();
															Log.e(TAG,"makeGooglePlayServicesAvailable completed with exception " + e.getMessage(),e );
														}
														if (mRegistered) {
															Util.startActivity(SplashActivity.this, MainActivity.class, null);
														}
													}
												});
									}
								});

							}
						}
						else{
							mGcmRegistered = true;
							if (mRegistered) {
								Util.startActivity(SplashActivity.this, MainActivity.class, null);
							}
						}
					}

//					@Override
//					public void onRegisterSuccess(String registrationId) {
//						mGcmRegistered = true;
//						Log.i(TAG, "GCM registration succeeded: " + registrationId);
//						if (mRegistered) {
//							Util.startActivity(SplashActivity.this, MainActivity.class, null);
//						}
//					}

//					@Override
//					public void onRegisterFailed(int code, String reason) {
//						mGcmRegistered = true;
//						Log.i(TAG, "GCM registration failed: " + code);
//		    			try {
//		    				checkGooglePlayServices(SplashActivity.this, getApplicationContext(), Config.SENDER_ID);
//							if (mRegistered) {
//								Util.startActivity(SplashActivity.this, MainActivity.class, null);
//							}
//		    			} catch (Exception e) {
//		    				e.printStackTrace();
//		    			}
//					}

					/**
					 * This is how you prompt users to install GooglePlay services -- Note not all Android devices come with
					 * GooglPlay services pre-installed
					 *
					 * @param a
					 * @param c
					 * @param senderId
					 * @return
					 * @throws ServiceException
					 */
//					public int checkGooglePlayServices(Activity activity, Context context, String senderId) throws ServiceException {
//		    			if (TextUtils.isEmpty(senderId)) {
//		    				throw new ServiceException("to register pass the GCM Sender ID");
//		    			}
//
//		    			int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
//		    			if (resultCode != ConnectionResult.SUCCESS) {
//		    				if (GooglePlayServicesUtil.isUserRecoverableError(resultCode) && activity != null) {
//		    					Log.i(TAG, "Recoverable error: showing directions dialog");
//		    					GooglePlayServicesUtil.getErrorDialog(resultCode, activity, PLAY_SERVICES_RESOLUTION_REQUEST).show();
//		    				} else {
//		    		             Log.i(TAG, "This device is not supported.");
//		    		         }
//		    		     }
//		    			return resultCode;
//		    		 }

//					@Override
//					public void onServiceAvailabilityResponse(int connectionResponse) {
//					}
    				
    			});
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IllegalArgumentException iae) {
            Log.w(TAG, "Missing login details");
            Toast.makeText(this, getString(R.string.error_login_details), Toast.LENGTH_LONG).show();
		}
    }
     
    /**
     * Seed Catalog
     * @author Glen
     */
    class SeedCatalogTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			Log.d(TAG, "seeding catalog");
			
			Cursor c = null;
			ContentResolver cr = getContentResolver();
			try {
				c = cr.query(CatalogContentProvider.CATALOG_URI, new String[]{CatalogColumns._ID}, null, null, null);
			} finally {
				if (c != null) {
					c.close();
					c = null;
				}
			}	
			return null;
		}
    }   

    private boolean mRegistered;
    private boolean mGcmRegistered;
    
    /**
     * Observe the registration request
     */	
	private IBackplaneObserver mBackplaneObserver = new IBackplaneObserver() {

		/**
		 * Registration request succeeded
		 * 
		 * @param request
		 */
		private void handleSuccess(int request) {
			switch(request){
				case BackplaneCallbackType.SYNC:{
					Log.i(TAG, "sync");
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (mGcmRegistered)
								Util.startActivity(SplashActivity.this, MainActivity.class, null);
						}					
					});
					break;					
				}

				case BackplaneCallbackType.REGISTER:{
					Log.i(TAG, "registered");
					mRegistered = true;
					break;
				}
			}
		}
			
		/**
		 * Registration request failed
		 * 
		 * @param result
		 */
		private void handleFailure(final int result) {



			if(result == Common.BackplaneResult.INVALID_CREDENTIALS){
				showAlertDialog(R.string.invalid_credentials, R.string.invalid_credentials);
			} else {
				showAlertDialog(R.string.reg_failure_title, R.string.reg_failure_msg);
			}			
		}
		
	    private void showAlertDialog(int title, int body) {
	    	AlertDialog ad = new AlertDialog.Builder(SplashActivity.this).create();
	    	ad.setTitle(title);
	    	ad.setMessage(getString(body));
	    	ad.show();
	    }

		@Override
		public void requestComplete(final int request, final int result, String errorMessage) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (result == Common.BackplaneResult.SUCCESS || request == BackplaneCallbackType.SYNC) {
						handleSuccess(request);
					} else {
						handleFailure(result);
					}				
				}		
			});
		}
	};

	/**
	 * Import the certificate
	 */
	class ImportCertificateTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			Log.d(TAG, "seeding catalog");
			
			try {
				IKeyStore keystore = mVirtuoso.getKeyStore();
				if(!keystore.containsAlias("lfclient")){
					InputStream is = getApplicationContext().getResources().openRawResource(R.raw.clientnopassword);
					keystore.importPKCS12(is, null);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
    }

	private Observers.IEngineObserver mEngineObserver = new EngineObserver() {
		@Override
		public void engineDidNotStart(String reason) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(SplashActivity.this, R.string.error_start_service, Toast.LENGTH_LONG).show();
				}
			});
		}
	};
}