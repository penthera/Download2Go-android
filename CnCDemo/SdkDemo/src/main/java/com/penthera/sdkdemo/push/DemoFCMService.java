//  Copyright (c) 2017 Penthera Partners, LLC. All rights reserved.
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
package com.penthera.sdkdemo.push;

import com.google.firebase.messaging.RemoteMessage;
import com.penthera.virtuososdk.client.subscriptions.FcmMessagingService;

/**
 * Demonstrates how to override the FCMMessagingService to handle messages not handled by SDK.
 */
public class DemoFCMService extends FcmMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage msg) {
        //always call super to ensure sdk messages get handled correctly
        super.onMessageReceived(msg);
        //handle your own messages
    }

    /**
     * Demonstrates how to capture the device token if required.
     */
    @Override
    public void onNewToken(String token) {
        // Please always call super to ensure SDK can also use the updated push token
        super.onNewToken(token);

    }
}
