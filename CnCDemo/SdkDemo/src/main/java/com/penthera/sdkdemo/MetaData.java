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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Meta Data stored in the virtuoso SDK
 * 
 * Catalog data associated with the file download
 */
public class MetaData {
	/** The image thumbnail */
	public static final String IMAGE_THUMBNAIL_URL = "image_thumbnail_url";
	/** The title */
	public static final String TITLE = "title";
	
	/**
	 * Turn the passed meta data into a JSON string
	 * 
	 * @param title the title of the item
	 * @param thumbnail the thumb-nail for the item
	 * 
	 * @return JSON string
	 */
	public static String toJson(String title, String thumbnail) {
		JSONObject obj = new JSONObject();		
		try {
			obj.put(MetaData.TITLE, title);
			obj.put(MetaData.IMAGE_THUMBNAIL_URL, thumbnail);
		} catch (JSONException e) {
			e.printStackTrace();
		}	
		return obj.toString();
	}
}
