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

import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.client.Virtuoso;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

/**
 * This is used to demonstrate a workaround for the "Remove app from recent tasks kills background service on  4.4.1 and 4.4.2." 
 * To use a work around such as this in a released product clients should  capture all details they wish to display in the notification.
 * This implementation only works from the background service notification broadcasts hence if there are no current downloads then it may not ensure the services continue running.
 * Real world Applications should provide a full implementation and ensure that they capture startup of the application as well as the service restarting with no content to download.
 */
public class NotificationService extends Service {

	private static final String LOG_TAG = NotificationService.class.getName();
	private static final int NOTIFICATION_VIEW_ID = 200;
	private static final int START_SERVICE_ID = 1;
	
	
	public NotificationService(String name) {
		//super(name);
	}
	public NotificationService() {
		this("KitKatNotificationService");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		handleCommand(intent);
		return START_STICKY;
	}

	
	private void handleCommand(Intent aIntent) {
		String action = aIntent.getAction();
		boolean isKitKat = Build.VERSION.SDK_INT == 19; 
		Log.d(LOG_TAG, "received intent : " + action + " isKitKat:" +isKitKat);
		Context context = getApplicationContext();
		Virtuoso v = new Virtuoso(context);
		Notification notification = NotificationFactory.getNotification(context, aIntent, "SdkDemo", v.getAssetManager());
		if(notification != null) {
			//Only runs for kit kat compat so will issue a start foreground with the notification
			startForeground(NOTIFICATION_VIEW_ID, notification);
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onTaskRemoved(android.content.Intent)
	 * This is responsible for registering an alarm to fire off and restart the service when a task is removed.
	 * It ensures that the background downloads will continue on 4.4.1 and 4.4.2
	 */
	@SuppressLint("NewApi")
	@Override
	public void onTaskRemoved(Intent rootIntent) {
		if(Build.VERSION.SDK_INT == 19){
			//KitKat
			Log.d(this.getClass().getName(),"KITKAT TASK REMOVED");
			Context c = getApplicationContext();
			
			// Create intent
			Intent intent = new Intent(c, NotificationService.class);
			intent.setPackage(getPackageName());
			intent.setAction(Common.START_VIRTUOSO_SERVICE);			
	
			// Scheduled  alarm
			AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);			
			if (am != null) {
				PendingIntent pending = PendingIntent.getService(c, START_SERVICE_ID, intent, PendingIntent.FLAG_ONE_SHOT);
				am.setExact(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 4000, pending);
			}
		}
		if(Build.VERSION.SDK_INT >= 14){
			super.onTaskRemoved(rootIntent);
		}
	}
}
