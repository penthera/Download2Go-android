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

package com.penthera.sdkdemo;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

public class Config {
	/** The backplane base URL */
	public static final String BACKPLANE_URL = "https://demo.penthera.com/";

	//SdkDemo Keys
	/** The backplane public key.  NOTE: YOU MUST REPLACE THIS VALUE WITH YOUR OWN PENTHERA-ASSIGNED KEYS */
	public static final String BACKPLANE_PUBLIC_KEY = ;
	
	/** The backplane private key.  NOTE: YOU MUST REPLACE THIS VALUE WITH YOUR OWN PENTHERA-ASSIGNED KEYS */
	public static final String BACKPLANE_PRIVATE_KEY = ;
	
	/** Test download */
	public static final String  SMALL_DOWNLOAD = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";

	/** Test download */
	public static final String  SMALL_DOWNLOAD2 = "http://d22oovgqp3p1ij.cloudfront.net/WVGA5/Onion_News_Network/Behind_The_Pen-_The_Chinese_Threat_21-May-2012_14-30-00_GMT.mp4";	

	/** Within this number of days a warning is shown on the catalog detail page */
	public static final long EXPIRY_WARNING_DAYS = 730L;
	
	/** How frequency to check the timestamp on the catalog for modifications */
	public static final int CATALOG_UPDATE_INTERVAL = 30 * 60 * 1000;

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

}
