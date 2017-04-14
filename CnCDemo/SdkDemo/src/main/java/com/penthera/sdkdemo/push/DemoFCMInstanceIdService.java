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

import com.penthera.virtuososdk.client.subscriptions.FcmInstanceIdService;

/**
 * Demonstrates how to subclass the FcmInstanceIdService to capture the device token.
 */
public class DemoFCMInstanceIdService extends FcmInstanceIdService {
    @Override
    public void onTokenRefresh() {
        //always call super so the the device token is registered with SDK
        super.onTokenRefresh();
        //Obtain the token e.g.: FirebaseInstanceId.getInstance().getToken()
        //send to you own servers if needed.
    }
}
