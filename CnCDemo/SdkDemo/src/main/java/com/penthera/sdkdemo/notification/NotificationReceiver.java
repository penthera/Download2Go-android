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

package com.penthera.sdkdemo.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import com.penthera.sdkdemo.notification.NotificationFactory;
import com.penthera.sdkdemo.notification.NotificationService;
import com.penthera.virtuososdk.client.Virtuoso;

/**
 * This component receives progress events whilst files are downloading
 * 
 * Here we use it to send a sample notification
 */
public class NotificationReceiver extends BroadcastReceiver {
	/** Log tag */
	static final String LOG_TAG = NotificationReceiver.class.getName();

	/** The notification ID */
	private static final int NOTIFICATION_VIEW_ID = 100;
	/** Holding onto the notification stops it from blinking - not an issue on all devices.*/
	private static Notification mLastNotification = null;
	
	@Override
	public void onReceive(Context aContext, Intent aIntent) {
		//check for being kitkats
		boolean isKitKat = Build.VERSION.SDK_INT == 19;
		//flag to decide on whether the foreground service should be used
		boolean useService = false;
		if(isKitKat){
			Log.d(LOG_TAG, "Received broadcast iskitkat: " + isKitKat + " release: " + Build.VERSION.RELEASE);
			useService = Build.VERSION.RELEASE.equals("4.4.1") || Build.VERSION.RELEASE.equals("4.4.2");
		}
		Log.d(LOG_TAG, "isKitKat: " + isKitKat + " useService: " + useService);
		
		if(useService){
			aIntent.setComponent(new ComponentName(aContext.getApplicationContext(), NotificationService.class));
			aContext.startService(aIntent);
			return;
		}
		Virtuoso v = new Virtuoso(aContext);
		Notification notification = NotificationFactory.getNotification(aContext, aIntent, "SdkDemo", v.getAssetManager());
		
		if(mLastNotification != null && notification != null){
			mLastNotification.contentView = notification.contentView;
		}
		else if( notification != null ) {
			mLastNotification = notification;
		}
		NotificationManager manager = (NotificationManager) aContext.getSystemService(Context.NOTIFICATION_SERVICE);
		if(notification != null && manager != null){
			manager.notify(NOTIFICATION_VIEW_ID, mLastNotification);
		}
	}
}