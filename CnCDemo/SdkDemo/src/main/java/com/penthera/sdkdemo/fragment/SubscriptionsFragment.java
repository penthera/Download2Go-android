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

import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.penthera.sdkdemo.Config;
import com.penthera.sdkdemo.R;
import com.penthera.virtuososdk.backplane.AddItemAndNotifyDeviceRequest;
import com.penthera.virtuososdk.backplane.Request;
import com.penthera.virtuososdk.client.SubscriptionObserver;
import com.penthera.virtuososdk.client.Virtuoso;
import com.penthera.virtuososdk.client.Observers.ISubscriptionObserver;

/**
 * Displays version information
 * @author Glen
 */
public class SubscriptionsFragment extends Fragment {
	/** Log tag */
	private static final String TAG = SubscriptionsFragment.class.getName();
    
	public static final String EXTRA_MODE = "mode";
	public static final int MODE_CREATE = 1;
	public static final int MODE_EDIT = 2;
	
	public static final int BTN_SUBSCRIBE = 0;
	public static final int BTN_UNSUBSCRIBE = 1;
	public static final int BTN_ADD = 2;
	public static final int BTN_CANCEL = 3;
	
	View mSubscribeLayout;
	View mUnsubscribeLayout;
	        
	private static final String SUBSCRIPTION_TEST_FEED_UUID = "CLIENT_ADD_ITEM_TEST_FEED";

	private Virtuoso mVirtuoso;

    // --- Construction
    
    /** 
     * Create a new instance of the dialog with a callback 
     */
    public static SubscriptionsFragment newInstance(Virtuoso service) {
        SubscriptionsFragment f = new SubscriptionsFragment();
        f.mVirtuoso = service;
        return f;
    }
    
    public void setService(Virtuoso service) {
    	mVirtuoso = service;
    }

    // --- Life-cycle Methods
    
    // onCreate
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private View mLayout;
    
    Button mSubButton;
    Button mUnsubButton;
    
	private ProgressDialog mProgressDialog;

    // onCreateView
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    	mLayout = inflater.inflate(R.layout.fragment_subscriptions, container, false);

    	mSubscribeLayout = mLayout.findViewById(R.id.lyt_subscribe_options);
    	mUnsubscribeLayout = mLayout.findViewById(R.id.lyt_unsubscribe_options);

    	// Subscribe
        mSubButton = (Button) mLayout.findViewById(R.id.btn_subscribe);
        mSubButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleSubscribe(Integer.MAX_VALUE, false, -1,true);
			}
        });

        // Subscribe
        mUnsubButton = (Button) mLayout.findViewById(R.id.btn_unsubscribe);
        mUnsubButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleUnsubscribe();
			}
        });
        
        
        Button btn;
        // Add
        btn = (Button) mLayout.findViewById(R.id.btn_add);
        btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new AddItemTask(getActivity().getApplicationContext()).execute();
			}
        });
         	
        return mLayout;
    }
    
	private void handleSubscribe(int maxItems, boolean canDelete, int maxBitRate, boolean downloadSequentially) {
		mProgressDialog = ProgressDialog.show(getActivity(), "Subscribing", "Subscribing to: " + SUBSCRIPTION_TEST_FEED_UUID);
		mProgressDialog.setCancelable(true);		
		mVirtuoso.subscribe(SUBSCRIPTION_TEST_FEED_UUID, maxItems, canDelete, maxBitRate,downloadSequentially);
	}

	private void handleUnsubscribe() {
		mProgressDialog = ProgressDialog.show(getActivity(), "Unsubscribing", "Unsubscribing to: " + SUBSCRIPTION_TEST_FEED_UUID);
		mProgressDialog.setCancelable(true);
		mVirtuoso.unsubscribe(SUBSCRIPTION_TEST_FEED_UUID);			
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

	@Override
	public void onActivityCreated(Bundle b) {
		super.onActivityCreated(b);
		if(mVirtuoso != null){
	    	mVirtuoso.getSubscriptions();
		}
	}

	// onPause
	@Override
	public void onResume()
	{
		super.onResume();
		mVirtuoso.addObserver(mSubscriptionsObserver);
	}



	// onPause
	@Override
	public void onPause()
	{
		super.onPause();
		mVirtuoso.removeObserver(mSubscriptionsObserver);
	}

    // onDestroy
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	if (mVirtuoso != null) {
    		mVirtuoso.removeObserver(mSubscriptionsObserver);    		
    		mVirtuoso.onPause();
    	}
    }   
    
    int mSubscribed;

    final ISubscriptionObserver mSubscriptionsObserver = new SubscriptionObserver() {
		@Override
		public void onSubscribe(final int result, final String uuid) {
			
			
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (result == 0) {
						Toast.makeText(getActivity(), "Subscribe Success", Toast.LENGTH_SHORT).show();	
						mUnsubscribeLayout.setVisibility(View.VISIBLE);
						mSubscribeLayout.setVisibility(View.GONE);
					} else {
						Toast.makeText(getActivity(), "Subscribe Failure", Toast.LENGTH_SHORT).show();							
					}
					
					try {
						mProgressDialog.dismiss();
					} catch(Exception e){}
				}
			});
		}

		@Override
		public void onUnsubscribe(final int result,final String uuid) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (result == 0) {
						mSubscribeLayout.setVisibility(View.VISIBLE);
						mUnsubscribeLayout.setVisibility(View.GONE);
						Toast.makeText(getActivity(), "Unsubscribe Success", Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(getActivity(), "Unsubscribe Failure", Toast.LENGTH_SHORT).show();
					}

					try {
						mProgressDialog.dismiss();
					} catch(Exception e){}
				}
			});
		}

		@Override
		public void onSubscriptions(final int result, final String[] uuids) {
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {

					if (result == 0) {
						boolean found = false;
						for (int i = 0; i < uuids.length; ++i) {
							if (uuids[i].equals("CLIENT_ADD_ITEM_TEST_FEED")) {
								found = true;
								break;
							}
						}
						if (found) {
							mSubscribed = 1;
						} else { 
							mSubscribed = 0;
						}

						mSubscribeLayout.setVisibility(mSubscribed == 0 ? View.VISIBLE : View.GONE);
						mUnsubscribeLayout.setVisibility(mSubscribed == 1 ? View.VISIBLE : View.GONE);
					} else {
						Toast.makeText(getActivity(), "Cannot access subscriptions", Toast.LENGTH_SHORT).show();
					}
				}
			});
		}
    };

    private class AddItemTask extends AsyncTask<Void, Void, Void> {
    	JSONObject mObj;
    	private Context mContext;
    	
    	AddItemTask(Context c) {mContext = c;}
    	
        @Override
        protected Void doInBackground(Void... params) {
			AddItemAndNotifyDeviceRequest a = new AddItemAndNotifyDeviceRequest();
			mObj = a.executeToJson(mContext, new Bundle());
        	
        	return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        	try {
        		mProgressDialog.dismiss();
        	} catch(Exception e){}

        	if (Request.isSuccess(mObj)) {
        		Toast.makeText(mContext, "Item Added to feed: " + SUBSCRIPTION_TEST_FEED_UUID, Toast.LENGTH_SHORT).show();	
        		Log.i(TAG, "ok");
        	} else {
        		Toast.makeText(mContext, "Add to feed failed", Toast.LENGTH_SHORT).show();							
        	}
        	try {
        		mProgressDialog.dismiss();
        	} catch(Exception e){}
        }					

        @Override
        protected void onPreExecute() {
    		mProgressDialog = ProgressDialog.show(getActivity(), "Adding", "Adding test item to feed: " + SUBSCRIPTION_TEST_FEED_UUID);
    		mProgressDialog.setCancelable(true);		        	
        }

        @Override
        protected void onProgressUpdate(Void... values) {}

    }
}
