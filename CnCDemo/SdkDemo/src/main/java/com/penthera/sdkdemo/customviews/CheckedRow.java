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
import android.widget.Checkable;
import android.widget.LinearLayout;

/**
 * Supports checked row
 * 
 * Used in conjunction with CAB [Contextual Action Bar]
 */
public class CheckedRow extends LinearLayout implements Checkable {
	private boolean mChecked;
	/**
	 * Create row
	 * 
	 * @param context the context
	 * @param attrs the attributes from the XML file
	 */
	public CheckedRow(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	// isChecked
	@Override
	public boolean isChecked() {
		return mChecked;
	}

	// toggle
	@Override
	public void toggle() {
		mChecked = !mChecked;
	}

	private static final int[] CheckedStateSet = {
	    android.R.attr.state_checked
	};
	
	// onCreateDrawableState
	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
	    final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
	    if (mChecked) {
	        mergeDrawableStates(drawableState, CheckedStateSet);
	    }
	    return drawableState;
	}

	// setChecked
	@Override
	public void setChecked(boolean checked) {
	    mChecked = checked;
	}
}
