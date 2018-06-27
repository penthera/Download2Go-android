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
import android.content.Context;
import android.content.Intent;

import com.penthera.sdkdemo.SDKDemoServiceStarter;
import com.penthera.virtuososdk.client.Virtuoso;

/**
 * This component receives progress events whilst files are downloading
 *
 * Here we use it to send a sample notification
 */
public class NotificationReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context aContext, Intent aIntent) {
		SDKDemoServiceStarter.updateNotification( aContext, aIntent);
	}
}
