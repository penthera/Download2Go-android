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
package com.penthera.sdkdemo.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import com.penthera.sdkdemo.R;

/**
 * Allow users to multi-select inbox rows when CAB is open
 */
public class InboxRow extends CheckedRow {
	/**
	 * Create inbox row
	 * 
	 * @param context the context
	 * @param attrs the attributes from the XML file
	 */
	public InboxRow(Context context, AttributeSet attrs) {	
		super(context, attrs);
			
		LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layoutInflater.inflate(R.layout.row_inbox, this);
	}
}
