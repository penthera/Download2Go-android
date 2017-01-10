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
package com.penthera.sdkdemo.catalog;

import java.net.MalformedURLException;

import com.penthera.virtuososdk.Common.AssetStatus;
import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.IFile;
import com.penthera.virtuososdk.client.ISegmentedAsset;

/**
 * Check download and play permissions
 */
public class PermissionManager {
	/**
	 * Permissions
	 */
	public enum Permission {
		EAccessAllowed, 
		EAccessDeniedWindow,
		EAccessDeniedExpired,
		EAccessDeniedDisabled,
		EAccessDeniedDeviceDisabled,
		EAccessDeniedAssetNotLocal, //single video which has not yet completed download - must be streamed to watch
		EAccessDeniedInvalidAsset //unidentified type of IAsset (should never happen) or cannot generate a valid URL for an HLS file
		};

	/**
	 * this detects whether an item can be played from the catalog
	 * 
	 * @param availabilityStart the timestamp for availability in seconds
	 * @param catalogExpiry the timestamp for expiration in seconds
	 * @return
	 */
	public Permission canPlay(long availabilityStart, long catalogExpiry){
		long now = System.currentTimeMillis() / 1000;
		if (now < availabilityStart)
			return Permission.EAccessDeniedWindow;
		if (catalogExpiry <= 0) {
			catalogExpiry = Long.MAX_VALUE;
		}
		if (now > catalogExpiry)
			return Permission.EAccessDeniedExpired;
		return Permission.EAccessAllowed;
	}
	//detect whether an item can be downloaded from the catalog
	public Permission canDownload(boolean deviceEnabled, boolean contentDownloadable, long catalogExpiry){
		if(!deviceEnabled) 
			return Permission.EAccessDeniedDeviceDisabled;
		if(!contentDownloadable) 
			return Permission.EAccessDeniedDisabled;
		if (catalogExpiry <= 0) {
			catalogExpiry = Long.MAX_VALUE;
		}
		if(System.currentTimeMillis() / 1000 > catalogExpiry) 
			return Permission.EAccessDeniedExpired;
		return Permission.EAccessAllowed;
	}
	
	// this detects whether an item can be played from the catalog
	public Permission canPlay(IAsset asset) {
		if(asset instanceof ISegmentedAsset){
			try {
				return ((ISegmentedAsset)asset).getPlaylist() == null ? Permission.EAccessDeniedWindow:Permission.EAccessAllowed;
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		} else if(asset instanceof IFile){
			IFile f = (IFile)asset;
			int fds = f.getDownloadStatus();
			if(f.getFilePath() != null && fds != AssetStatus.DOWNLOAD_COMPLETE){
				return Permission.EAccessDeniedAssetNotLocal;
			}
			if(f.getFilePath() == null){
				//has expired
				if(fds == AssetStatus.EXPIRED) {
					return Permission.EAccessDeniedExpired;
				} else if (fds == AssetStatus.DOWNLOAD_COMPLETE){
					//outside of availability window not yet marked as expired
					return Permission.EAccessDeniedWindow;
				} else {
					//just not downloaded for local playback yet
					return Permission.EAccessDeniedAssetNotLocal;
				}
			} else {
				return Permission.EAccessAllowed;
			}
		}
		return Permission.EAccessDeniedInvalidAsset;
	}
}
