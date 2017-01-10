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

package com.penthera.sdkdemo.provider;

import com.penthera.virtuososdk.database.impl.provider.VirtuosoSDKContentProvider;

/**
 * Enables Virtuoso service to access this clients DB
 * 
 * @author Glen
 */
public class SdkDemoContentProvider extends VirtuosoSDKContentProvider {
	static{
		setAuthority("com.penthera.virtuososdk.provider.sdkdemo");
	}
	
	// getAuthority
	@Override
	protected String getAuthority() {
		return "com.penthera.virtuososdk.provider.sdkdemo";
	}
}
