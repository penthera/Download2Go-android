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

import org.json.JSONException;
import org.json.JSONObject;

import com.penthera.sdkdemo.activity.SplashActivity;
import com.penthera.sdkdemo.MetaData;
import com.penthera.sdkdemo.R;
import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.Common.Notifications.DownloadStopReason;
import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.IAssetManager;
import com.penthera.virtuososdk.client.IEvent;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public final class NotificationFactory {

	static final String LOG_TAG = NotificationFactory.class.getName();
	private NotificationFactory() {
	}
	
	//types of notifications
	final static int PROGRESS_NOTIFICATION		= 0;
	final static int COMPLETED_NOTIFICATION		= 1;
	final static int STOPPED_NOTIFICATION		= 2;
	final static int PAUSED_NOTIFICATION		= 3;
	final static int RESTART_NOTIFICATION		= 4;
	
	/**
	 * Get the notification to be used for the intent.
	 * @param aContext the context to be used.
	 * @param aIntent the intent that was broadcast from the download service.
	 * @param aAppName the applications name.
	 * @param aAssetManager the asset manager 
	 * @return the notification to be used
	 */
	static Notification getNotification(Context aContext, Intent aIntent, String aAppName, IAssetManager aAssetManager){
		Notification notification = null;
		String clientReference = null;
		
		try{
			ApplicationInfo ai = aContext.getPackageManager().getApplicationInfo(aContext.getPackageName(), PackageManager.GET_META_DATA);
			Bundle b =ai.metaData;
			
			clientReference = b.getString(Common.CLIENT_PACKAGE);
		
		}catch(Exception e){
			throw new RuntimeException("cannot retrieve client",e);
		}

		String action = aIntent.getAction();
		Log.d(LOG_TAG, "got action: " + action);
		if(aContext == null || aIntent == null || clientReference == null || action == null){
			return notification;
		}

		// If the intent action contains the NOTIFICATION_EVENT_TAG, then this is a log event broadcast sent from
		// the SDK analytics system.  For debugging, we might want to post status bar notifications, but in general, 
		// these events are used to push SDK analytics events into a custom/3rd party analytics platform, and shouldn't
		// be sent to the status bar. For the purposes of this demo, we'll log them and return null, which prevents the
		// status bar notice from being shown.
		if( action.contains(Common.Notifications.NOTIFICATION_EVENT_TAG) ){
			
			IEvent event = aIntent.getParcelableExtra(Common.Notifications.EXTRA_NOTIFICATION_EVENT);
			
			Log.d(LOG_TAG,"Got event named("+event.name()+") asset("+event.assetId()+" data("+event.numericData()+")");
			return null;
		}
		
		// The other broadcasts are notification broadcasts specifically sent for (optional) status bar notification delivery.
		// Determine which action we are handling and create a notification for it.
		int notification_type = -1;
		IAsset file = null;
		
		if(action.equals(Common.START_VIRTUOSO_SERVICE)){
			notification_type = RESTART_NOTIFICATION;
		}
		else {
			boolean hasInfo = false;
			Bundle extras = aIntent.getExtras();
			int info = DownloadStopReason.NO_ERROR;
			
			if(extras != null) { 
				if(extras.containsKey(Common.Notifications.EXTRA_NOTIFICATION_DOWNLOAD_STOP_REASON)){
					hasInfo = true;
					info = extras.getInt(Common.Notifications.EXTRA_NOTIFICATION_DOWNLOAD_STOP_REASON);
				}
				file = extras.getParcelable(Common.Notifications.EXTRA_NOTIFICATION_FILE);
			}
			final String INTENT_ACTION = action.replace(clientReference, "");

			if(INTENT_ACTION.equals(Common.Notifications.INTENT_NOTIFICATION_DOWNLOAD_COMPLETE)) {

				notification_type = COMPLETED_NOTIFICATION;
				Log.d(LOG_TAG, "DOWNLOAD COMPLETE NOTIFICATION FOR " + file.getUuid() + " stat: " + (hasInfo? info:"unknown"));
				
			} else if(INTENT_ACTION.equals(Common.Notifications.INTENT_NOTIFICATION_DOWNLOAD_START)) {

				notification_type = PROGRESS_NOTIFICATION;
				Log.d(LOG_TAG, "DOWNLOAD START NOTIFICATION FOR " + file.getUuid() + " stat: " + (hasInfo? info:"unknown"));
				
			} else if(INTENT_ACTION.equals(Common.Notifications.INTENT_NOTIFICATION_DOWNLOAD_STOPPED)) {

				if(file != null){
					Log.d(LOG_TAG, "DOWNLOAD STOP NOTIFICATION FOR " + file.getUuid() + " stat: " + (hasInfo? info:"unknown"));
				} else {
					Log.d(LOG_TAG, "DOWNLOAD STOP NOTIFICATION FOR UNKNOWN" + " stat: " + (hasInfo? info:"unknown"));
				}
				notification_type = STOPPED_NOTIFICATION;
				
			} else if(INTENT_ACTION.equals(Common.Notifications.INTENT_NOTIFICATION_DOWNLOADS_PAUSED)) {

				if(file != null){
					Log.d(LOG_TAG, "DOWNLOAD PAUSED NOTIFICATION FOR " + file.getUuid() + " stat: " + (hasInfo? info:"unknown"));
				} else {
					Log.d(LOG_TAG, "DOWNLOAD PAUSED NOTIFICATION FOR UNKNOWN" + " stat: " + (hasInfo? info:"unknown"));
				}
				notification_type = PAUSED_NOTIFICATION;
				
			} else if(INTENT_ACTION.equals(Common.Notifications.INTENT_NOTIFICATION_DOWNLOAD_UPDATE)) {

				notification_type = PROGRESS_NOTIFICATION;
				Log.d(LOG_TAG, "DOWNLOAD UPDATE NOTIFICATION FOR " + file.getUuid() + " stat: " + (hasInfo? info:"unknown"));
				
			} 
			else {
				Log.d(LOG_TAG, "UNHANDLED NOTIFICATION ACTION "+ action);
			}
		}
		
		if(notification_type > -1){
			notification = createNotification(notification_type, aContext, aAssetManager, file, aAppName);
		}
		
		return notification;
	}
	
	/**
	 * create an intent for opening the application when the user clicks on the notification.
	 * @param aContext used to get the package name
	 * @return the intent
	 */
	static Intent createIntent(Context aContext) {
		Intent intent = new Intent(aContext.getPackageName() + ".HARNESS_NOTIFICATION");
		intent.setComponent(new ComponentName(aContext.getPackageName(), SplashActivity.class.getName()));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.setFlags(Intent.FLAG_FROM_BACKGROUND);
		return intent;
	}
	
	/**
	 * populate the view which will be used by the notification
	 * @param context the context
	 * @param aTitle The title
	 * @param aQueueSize Size of the queue
	 * @param aProgress Progress of currently downloading item.
	 * @return the view
	 */
	static RemoteViews getNotificationView(Context context, String aTitle, int aQueueSize, int aProgress) {
		RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.notification_layout); 
		view.setTextViewText(R.id.title,aTitle);
		if(aProgress >= 0){
			view.setViewVisibility(R.id.progress, View.VISIBLE);
			view.setProgressBar(R.id.progress, 100, aProgress, false);
		}
		else {
			view.setViewVisibility(R.id.progress, View.GONE);
		}
		view.setTextViewText(R.id.queued, "Num items in queue: " + aQueueSize);
		return view;
	}
	
	/**
	 * Get the size of the queue
	 * @param aAssetManager
	 * @return size
	 */
	static int getQueueSize(final IAssetManager aAssetManager){
		if(aAssetManager == null)
			return 0;
		return aAssetManager.getQueue().size();
	}
	
	/**
	 * calculates the progress of the current download.
	 * @param aAsset the current asset downloading
	 * @return progress
	 */
	static int getDownloadProgress(final IAsset aAsset){
		if(aAsset == null) 
			return -1;
		double expected = aAsset.getExpectedSize();
		if (expected <= .0001) {
			expected = aAsset.getContentLength();
		}		
		return (int)  (aAsset.getCurrentSize()/expected * 100.0);
	}
	
	/**
	 * Get the assets title or an empty string
	 * @param aAsset the asset 
	 * @return
	 */
	static String getAssetTitle(final IAsset aAsset){
		String title = "";

		if(aAsset != null){
			String md = aAsset.getMetadata();
			if(!TextUtils.isEmpty(md)){
				try {
					JSONObject obj = new JSONObject(md);
					title = obj.optString(MetaData.TITLE, "");
				} catch (JSONException e) {
				}
			}
		}
		return title;
	}
	
	/**
	 * Create the notification for the specified type.
	 * @param type The notification type.
	 * @param aContext The context to be used
	 * @param aAssetManager the asset manager to get the queue size.
	 * @param aAsset the asset (may be null)
	 * @param aAppName name of the application - this is used in the notification title
	 * @return the notification.
	 */
	static Notification createNotification(final int type, final Context aContext,
											final IAssetManager aAssetManager, final IAsset aAsset,
											final String aAppName){
		String title = aAppName + ": ";
		int progress = -1;
		int queued = getQueueSize(aAssetManager);
		
		switch(type){
		case PROGRESS_NOTIFICATION:
			progress = getDownloadProgress(aAsset);
			title += getAssetTitle(aAsset) + String.format(" ( %1$,.0f)",  aAsset.getCurrentSize());
			break;

		case COMPLETED_NOTIFICATION:
			progress = 100;
			title += getAssetTitle(aAsset) + " complete.";
			break;

		case STOPPED_NOTIFICATION:
			title += "stopped downloads.";
			break;

		case PAUSED_NOTIFICATION:
			title += "paused downloads.";
			break;

		case RESTART_NOTIFICATION:
			title += "is restarting.";
			break;
		}
		
		RemoteViews view = getNotificationView(aContext, title, queued, progress);
		Intent intent = createIntent(aContext);
		PendingIntent pIntent = PendingIntent.getActivity(aContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		//TODO change to the NotificationCompat builder
		@SuppressWarnings("deprecation")
		Notification notification = new Notification(R.drawable.plus, aAppName, System.currentTimeMillis());
		notification.contentView = view;
		notification.contentIntent = pIntent;
		return notification;
	}

}
