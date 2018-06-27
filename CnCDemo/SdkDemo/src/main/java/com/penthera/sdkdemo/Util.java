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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.penthera.sdkdemo.activity.SplashActivity;
import com.penthera.virtuososdk.Common.EngineStatus;

import java.text.DecimalFormat;

/**
 * Miscellaneous helper routines needed by the SDk Demo client
 */
public class Util {
	private static final String TAG = Util.class.getName();

	public static final int CLOSURE_REASON_REQUEST = 1;
	public static final int CLOSURE_REASON_BACK = 2;
	public static final int CLOSURE_REASON_LOGOUT = 3;

	/**
	 * Maps the EngineStatus to a String
	 * @param state one of the values from EngineStatus
	 * @return The string detailing the state
	 */
	public static String virtuosoStateToString(int state){
		switch(state){
		case EngineStatus.AUTH_FAILURE:
			return "AUTH_FAILURE";
		case EngineStatus.BLOCKED:
			return "BLOCKED";
		case EngineStatus.DISABLED:
			return "DISABLED";
		case EngineStatus.DOWNLOADING:
			return "DOWNLOADING";
		case EngineStatus.ERROR:
			return "ERROR";
		case EngineStatus.PAUSED:
			return "PAUSED";
		default:
			return "IDLE";
		}
	}
	/**
	 * Get the authority
	 *
	 * @param context the context
	 * @return the authority
	 */
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
	/**
	 * Helper to start an activity
	 *
	 * @param activity the context
	 * @param cls the activity to start
	 * @param b arguments for the new activity
	 */
	public static void startActivity(Activity activity, Class<?> cls, Bundle b) {
		startActivity(activity,cls,b,0);
	}

	/**
	 * Helper to start an activity
	 *
	 * @param activity the context
	 * @param cls the activity to start
	 * @param b arguments for the new activity
	 * @param flags flags for the new activity
	 */
	public static void startActivity(Activity activity, Class<?> cls, Bundle b,int flags) {
		Intent i = new Intent();
		i.setComponent(new ComponentName(activity.getApplicationContext(), cls));

		if(flags > 0)
			i.setFlags(flags);

		if (b != null)
			i.putExtras(b);

		try {
			if(activity.getClass().equals( SplashActivity.class ))
				activity.startActivity(i);
			else
				activity.startActivityForResult( i, CLOSURE_REASON_REQUEST);
		} catch (Exception e) {
			Toast.makeText(activity, activity.getString(R.string.generic_problem), Toast.LENGTH_SHORT).show();
			Log.e(TAG, e.toString());
		}
	}

	/**
	 * Helper to show toast
	 *
	 * @param context the context
	 * @param msg the toast message
	 * @param duration the duration of the toast message
	 */
	public static void showToast(Context context,  final String msg, int duration) {
		if (!TextUtils.isEmpty(msg)) {
			Toast.makeText(context, msg, duration).show();
		}
	}

	/**
	 * Helper to show basic alert dialog
	 *
	 * @param context the context
	 * @param title the title of the dialog
	 * @param message the message of the dialog
	 *
	 * @return the dialog
	 */
	public static Dialog showAlertDialog(Context context, String title, String message) {
		AlertDialog alertDialog = new AlertDialog.Builder(context).create();
		alertDialog.setTitle(title);
		alertDialog.setMessage(message);
		alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				try {
					dialog.dismiss();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		alertDialog.show();
		return alertDialog;
	}

	/**
	 * Convert bytes into human readable file size string
	 *
	 * @param size the size in bytes
	 *
	 * @return the file size string
	 */
	public static String readableFileSize(long size) {
	    if(size <= 0) return "0";
	    final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
	    int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
	    return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

	/**
	 * Convert seconds to human readable hours, minutes and seconds
	 *
	 * @param seconds
	 *
	 * @return human readable hours, minutes, seconds string
	 */
	public static String getDurationString(int seconds) {
		int hours = seconds / 3600;
		int minutes = (seconds % 3600) / 60;
		seconds = seconds % 60;
		return twoDigitString(hours) + " : " + twoDigitString(minutes) + " : " + twoDigitString(seconds);
	}

	/**
	 * Create a two digit time [prepend 0s to the time]
	 *
	 * @param number the number to pad
	 *
	 * @return two digit time as string
	 */
	public static String twoDigitString(int number) {
		if (number == 0) {
			return "00";
		}
		if (number / 10 == 0) {
			return "0" + number;
		}
	    return String.valueOf(number);
	}

	/**
	 * true, show information about the expiry time
	 *
	 * @param expiry timestamp in milliseconds
	 * @return
	 */
	public static boolean warnExpiry(long expiry) {
		if (expiry == 0) {
			return false;
		}

		long diff = expiry - System.currentTimeMillis();
		long warn = Config.EXPIRY_WARNING_DAYS * 24 * 60 * 60 * 1000;
		if (diff <= warn) {
			return true;
		}

		return false;
	}
}
