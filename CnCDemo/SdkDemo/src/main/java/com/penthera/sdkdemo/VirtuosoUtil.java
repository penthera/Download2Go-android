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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.penthera.sdkdemo.catalog.Catalog.CatalogColumns;
import com.penthera.sdkdemo.catalog.PermissionManager;
import com.penthera.sdkdemo.catalog.PermissionManager.Permission;
import com.penthera.sdkdemo.exoplayer.PlayerActivity;
import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.client.AncillaryFile;
import com.penthera.virtuososdk.client.IAssetManager;
import com.penthera.virtuososdk.client.IAssetPermission;
import com.penthera.virtuososdk.client.IQueue;
import com.penthera.virtuososdk.client.ISegment;
import com.penthera.virtuososdk.client.ISegmentedAssetFromParserObserver;
import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.IFile;
import com.penthera.virtuososdk.client.IIdentifier;
import com.penthera.virtuososdk.client.ISegmentedAsset;
import com.penthera.virtuososdk.client.Virtuoso;
import com.penthera.virtuososdk.client.builders.HLSAssetBuilder;
import com.penthera.virtuososdk.client.builders.MPDAssetBuilder;

public class VirtuosoUtil {
	/** Log tag */
	private static final String TAG = VirtuosoUtil.class.getName();

	
	/**
	 * Get uuid from asset ID
	 * 
	 * @param assetId
	 * @return
	 */
	public static String getFileUuid(IAssetManager mAssetManager, String assetId) {
		if (mAssetManager == null)
			return null;
		
		List<IIdentifier> ls = mAssetManager.getByAssetId(assetId);
		if (ls == null || ls.size() == 0) {
			return null;
		}
		
		return ls.get(0).getUuid();
	}

	/**
	 * Get asset from asset ID
	 * 
	 * @param assetId
	 * @return
	 */
	public static IAsset getVirtuosoAsset(IAssetManager manager, String assetId) {
		if (manager == null)
			return null;
		
		List<IIdentifier> ls = manager.getByAssetId(assetId);
		if (ls == null || ls.size() == 0) {
			return null;
		}
		
		IAsset asset = (IAsset) ls.get(0);
		return asset;
	}

	/**
	 * Add catalog item to the download Q
	 * 
	 * @param context the context
	 * @param service the Virtuoso service
	 * @param cv The content values from cursor row
	 * 
	 * @return true, item added
	 */
	public static boolean downloadItem(final Context context, Virtuoso service, ContentValues cv) {
		if (cv == null || cv.size() == 0) {
			return false;
		}

		final IQueue.IQueuedAssetPermissionObserver permObserver = new IQueue.IQueuedAssetPermissionObserver() {
			@Override
			public void onQueuedWithAssetPermission(boolean queued,boolean aPermitted, final IAsset aAsset, final int aAssetPermissionError) {
				String error_string;
				final IAssetPermission permResponse = aAsset.getLastPermissionResponse();
				final String assetPerm = permResponse.getPermission() == IAssetPermission.PermissionCode.PERMISSION_DENIED_EXTERNAL_POLICY ?
						permResponse.friendlyName() : IAssetPermission.PermissionCode.friendlyName(aAssetPermissionError);
				String title;
				if (!queued) {

					title = "Queue Permission Denied";
					error_string = "Not permitted to queue asset [" + assetPerm + "]  response: " + permResponse;
					if (aAssetPermissionError == IAssetPermission.PermissionCode.PERMISSON_REQUEST_FAILED) {
						error_string = "Not permitted to queue asset [" + assetPerm + "]  This could happen if the device is currently offline.";


					}
					Log.e(TAG, error_string);
				} else {
					title = "Queue Permission Granted";
					error_string = "Asset "+ (aPermitted? "Granted":"Denied") +" Download Permission [" + assetPerm + "]  response: " + permResponse;
					Log.d(TAG, error_string);
				}
				final String dlg_title = title;
				final String message = error_string;

				((Activity)context).runOnUiThread(new Runnable() {
					public void run() {
						AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
						builder1.setTitle(dlg_title);
						builder1.setMessage(message);
						builder1.setCancelable(false);
						builder1.setPositiveButton("OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										dialog.cancel();
									}
								});

						AlertDialog alert11 = builder1.create();
						alert11.show();
					}
				});


			}
		};

		String remoteId = cv.getAsString(CatalogColumns._ID);
		String url = cv.getAsString(CatalogColumns.CONTENT_URL);
		long catalogExpiry = cv.getAsLong(CatalogColumns.CATALOG_EXPIRY);			
		final String title = cv.getAsString(CatalogColumns.TITLE);
		final String thumbnail = cv.getAsString(CatalogColumns.IMAGE_THUMBNAIL);
		final int mediaType = cv.getAsInteger(CatalogColumns.MEDIA_TYPE);
		final boolean downloadEnabledContent = cv.getAsInteger(CatalogColumns.DOWNLOAD_ENABLED) == 1;
		final String mimetype = cv.getAsString(CatalogColumns.MIME);

		if(isDash(mediaType)){  // DASH
			downloadDashItem(context,service,downloadEnabledContent,remoteId,url,catalogExpiry,
					title,thumbnail,permObserver);
		}
		else if(isHls(mediaType)){  // HLS
			downloadHlsItem(context, service, downloadEnabledContent, remoteId, url + cv.getAsString(CatalogColumns.FRAGMENT_PREFIX), catalogExpiry, title, thumbnail,permObserver);
		}
		else  {  // FLAT MP4
			// In the case of a flat file there is no parsing, so the extra attributes are set during initial creation.
			final long downloadExpiry = cv.getAsLong(CatalogColumns.DOWNLOAD_EXPIRY);
			final long expiryAfterPlay = cv.getAsLong(CatalogColumns.EXPIRY_AFTER_PLAY);
			final long availabilityStart = cv.getAsLong(CatalogColumns.AVAILABILITY_START);

			return downloadItem(context, service, downloadEnabledContent, remoteId, url, mimetype, catalogExpiry, downloadExpiry, expiryAfterPlay, availabilityStart, title,
					thumbnail,permObserver);
		}
		// Assume true for HLS and DASH
		return true;
	}

	/**
	 * Download a flat item (no manifest) checking permissions and informing user of problems
	 *
	 * @param context Activity Context
	 * @param virtuoso The virtuoso object
	 * @param downloadEnabledContent Indicates a catalog state for if the content is currently download enabled
	 * @param remoteId The remote catalog asset id that will be used to reference the asset within the SDK
	 * @param url The asset manifest url, or content url for a flat file
	 * @param mimetype The expected mime type of the asset
	 * @param catalogExpiry The expiry time from the catalog, used to check download permission prior to parsing
	 * @param downloadExpiry The expiry time to set for the downloaded asset to override any backplane default
	 * @param expiryAfterPlay The expiry after play interval to set for the asset to override any backplane default
	 * @param availabilityStart The time when the asset should be made available to play
	 * @param title Title to display for the asset, stored in the asset metadata
	 * @param thumbnail Url for the thumbnail to display for the asset, stored in the asset metadata
	 * @param permObserver A permission observer which will be informed of the asset permission that is
	 *                     checked at the time of queuing. Setting a permission observer will ensure the
	 *                     permission is checked during queueing.
	 */
	public static boolean downloadItem(final Context context, final Virtuoso virtuoso, boolean downloadEnabledContent,
									   final String remoteId, final String url, final String mimetype,
									   final long catalogExpiry, final long downloadExpiry, final long expiryAfterPlay,
									   final long availabilityStart, final String title, final String thumbnail,
									   final IQueue.IQueuedAssetPermissionObserver permObserver) {
		Log.i(TAG, "Downloading item");
		boolean success = false;
		
		PermissionManager pm = new PermissionManager();
		Permission authorized = pm.canDownload(virtuoso.getBackplane().getSettings().getDownloadEnabled(),
												downloadEnabledContent, catalogExpiry);

		// Authorization: Success
		if (authorized == Permission.EAccessAllowed) {
			// Create meta data for later display of download list
			String json = MetaData.toJson(title, thumbnail);				
			IAssetManager manager = virtuoso.getAssetManager();
			long now = System.currentTimeMillis()/1000;
			// Add file to the Q
			IFile file = null;
			file = manager.createFileAsset(url, remoteId, mimetype, json);
			file.setStartWindow(availabilityStart <=0 ? now:availabilityStart);
			file.setEndWindow(catalogExpiry <=0 ? Long.MAX_VALUE:catalogExpiry);
			file.setEap(expiryAfterPlay);
			file.setEad(downloadExpiry);
			manager.getQueue().add(file,permObserver);

			// Inform user
			Util.showToast(context, "Added to Downloads", Toast.LENGTH_SHORT);
			success = true;
		} 
		return success;
	}

	
	static class HlsResult{
		int error = 0;
		boolean queued = false;
	}

	/**
	 * Download a DASH item checking permissions and informing user of problems
	 * @param context Activity Context
	 * @param virtuoso The virtuoso object
	 * @param downloadEnabledContent Indicates a catalog state for if the content is currently download enabled
	 * @param remoteId The remote catalog asset id that will be used to reference the asset within the SDK
	 * @param url The asset manifest url, or content url for a flat file
	 * @param catalogExpiry The expiry time from the catalog, used to check download permission prior to parsing
	 * @param title Title to display for the asset, stored in the asset metadata
	 * @param thumbnail Url for the thumbnail to display for the asset, stored in the asset metadata
	 * @param permObserver A permission observer which will be informed of the asset permission that is
	 *                     checked at the time of queuing. Setting a permission observer will ensure the
	 *                     permission is checked during queueing.
	 */
	public static void downloadDashItem(final Context context, final Virtuoso virtuoso,
										boolean downloadEnabledContent, final String remoteId,
										final String url, final long catalogExpiry, final String title,
										final String thumbnail, final IQueue.IQueuedAssetPermissionObserver permObserver){

		PermissionManager pm = new PermissionManager();
		Permission authorized = pm.canDownload(virtuoso.getBackplane().getSettings().getDownloadEnabled(),
				downloadEnabledContent, catalogExpiry);

		if (authorized == Permission.EAccessAllowed) {

			// Create meta data for later display of download list
			final String json = MetaData.toJson(title, thumbnail);
			final IAssetManager manager = virtuoso.getAssetManager();

			//note we would not be able to use the progress dialog if running from doBackground in an async task
			final ProgressDialog pdlg = ProgressDialog.show(context, "Processing dash manifest","Adding fragments...");
			final ISegmentedAssetFromParserObserver observer = (aSegmentedAsset, aError, addedToQueue) -> ((Activity)context).runOnUiThread(() -> {
				try {
					pdlg.dismiss();
				} catch (Exception e) {
				}

				if (aSegmentedAsset == null) {
					AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
					builder1.setTitle("Could Not Create Asset");
					builder1.setMessage("Encountered error(" + aError + ") while creating asset.  This could happen if the device is currently offline, or if the asset manifest was not accessible.  Please try again later.");
					builder1.setCancelable(false);
					builder1.setPositiveButton("OK",
							(dialog, id) -> dialog.cancel());

					AlertDialog alert11 = builder1.create();
					alert11.show();
				}
				Log.i(TAG, "Finished procesing dash file addedToQueue:" + addedToQueue + " error:" + aError);
			});

			try {

				MPDAssetBuilder mpdAsset = new MPDAssetBuilder()
						.assetObserver(observer)
						.manifestUrl(new URL(url))
						.desiredAudioBitrate(0)
						.desiredVideoBitrate(0)
						.addToQueue(true)
						.assetId(remoteId)
						.withMetadata(json)
						.withPermissionObserver(permObserver);

				if(remoteId.contains("ANCILLARY"))
						mpdAsset.withAncillaryFiles(Arrays.asList(new AncillaryFile[]{new AncillaryFile(new URL("http://virtuoso-demo-content.s3.amazonaws.com/jobs.jpg"),"ancillary image", new String[] {"tag1,tag2"})}));
				manager.createMPDSegmentedAssetAsync(mpdAsset.build());
						//.createMPDSegmentedAssetAsync(observer,new URL(url),0,0,remoteId,json,true,permObserver);
			} catch (MalformedURLException e) {
				Log.e(TAG,"Problem with dash url.",e);
			}
		}

	}
	
	/**
	 * Download an HLS item checking permissions and informing user of problems
	 * @param context Activity Context
	 * @param virtuoso The virtuoso object
	 * @param downloadEnabledContent Indicates a catalog state for if the content is currently download enabled
	 * @param remoteId The remote catalog asset id that will be used to reference the asset within the SDK
	 * @param url The asset manifest url, or content url for a flat file
	 * @param catalogExpiry The expiry time from the catalog, used to check download permission prior to parsing
	 * @param title Title to display for the asset, stored in the asset metadata
	 * @param thumbnail Url for the thumbnail to display for the asset, stored in the asset metadata
	 * @param permObserver A permission observer which will be informed of the asset permission that is
	 *                     checked at the time of queuing. Setting a permission observer will ensure the
	 *                     permission is checked during queueing.
	 */
	public static void downloadHlsItem(final Context context, final Virtuoso virtuoso, boolean downloadEnabledContent,
									   final String remoteId, final String url, final long catalogExpiry, final String title,
									   final String thumbnail, final IQueue.IQueuedAssetPermissionObserver permObserver) {
		Log.i(TAG, "Downloading HLS item");
		
		final HlsResult result = new HlsResult();
		
		PermissionManager pm = new PermissionManager();
		Permission authorized = pm.canDownload(virtuoso.getBackplane().getSettings().getDownloadEnabled(),
												downloadEnabledContent, catalogExpiry);

		// Authorization: Success
		if (authorized == Permission.EAccessAllowed) {
			// Create meta data for later display of download list
			final String json = MetaData.toJson(title, thumbnail);
			final IAssetManager manager = virtuoso.getAssetManager();

			//note we would not be able to use the progress dialog if running from doBackground in an async task
			final ProgressDialog pdlg = ProgressDialog.show(context, "Processing manifest", "Adding fragments...");

			// This shows how we hand over an HLS file to the SDK for processing and let it choose the best bit rate.

			// We register an observer to receive notification of when the file has been generated
			final ISegmentedAssetFromParserObserver observer =
					(aHlsFile, aError, addedToQueue) -> {
						//store the result
						result.error = aError;
						result.queued = addedToQueue;

						try {
							pdlg.dismiss();
						} catch (Exception e) {
						}

						if (aHlsFile == null) {
							AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
							builder1.setTitle("Could Not Create Asset");
							builder1.setMessage("Encountered error(" + Integer.toString(aError) + ") while creating asset.  This could happen if the device is currently offline, or if the asset manifest was not accessible.  Please try again later.");
							builder1.setCancelable(false);
							builder1.setPositiveButton("OK",
									(dialog, id) -> dialog.cancel());

							AlertDialog alert11 = builder1.create();
							alert11.show();
						}
					};

			try {
				// Note, the value of the third parameter (aDesiredBitRate) should be between 1 and Integer.MAX_VALUE.  Sending
				// 1 means "choose the lowest possible bitrate" and sending Integer.MAX_VALUE means "choose the highest possible bitrate".

				HLSAssetBuilder hlsAsset = new HLSAssetBuilder()
						.assetObserver(observer)
						.manifestUrl(new URL(url))
						.desiredVideoBitrate(Integer.MAX_VALUE)
						.addToQueue(true)
						.assetId(remoteId)
						.withMetadata(json)
						.withPermissionObserver(permObserver);

				if (remoteId.contains("ANCILLARY"))
					hlsAsset.withAncillaryFiles(Arrays.asList(new AncillaryFile[]{new AncillaryFile(new URL("http://virtuoso-demo-content.s3.amazonaws.com/jobs.jpg"), "ancillary image", new String[]{"tag1,tag2"})}));

				manager.createHLSSegmentedAssetAsync(hlsAsset.build());
			} catch (MalformedURLException e) {
				Log.e(TAG, "problem with hls URL", e);
			}
		}
	}

	/**
	 * Helper to check if a catalog item is of DASH type
	 * @param type The media type from the catalog
	 * @return true if this is a dash item
	 */
	private static boolean isDash(int type){
		return type == 6;
	}

	/**
	 * Helper to check if a catalog item is of HLS type
	 * @param type The media type from the catalog
	 * @return true if this is an HLS item
	 */
	private static boolean isHls(int type) {
		return type == 3;
	}
	
	/**
	 * Watch item
	 * 
	 * @param context the context
	 * @param manager the asset manager insterface
	 * @param c the cursor
	 */
	public static void watchItem(Context context, IAssetManager manager, Cursor c) {
		Log.i(TAG, "WatchItem");		
		if (c == null || c.getCount() == 0) {
			return;
		}
		final String assetId = c.getString(c.getColumnIndex(CatalogColumns._ID));
		IAsset asset = getVirtuosoAsset(manager,assetId);
		
		// If we have an SDK object for the asset, then it's downloaded or downloading.
		// Try to play it back using the local data.
		if (asset != null) {
			if(canWatchVirtuosoItem(context, asset)){
				manager.recordOfflinePlay(asset);
				watchVirtuosoItem(context, asset);
				return;
			} else {
				// Util.showToast(context, "Asset not available", Toast.LENGTH_SHORT);
			}
			//we could do further checks here to see if a fall through to streaming the content is valid.
			//e.g.: canWatchVirtuosoItem could return the permission and we could check that to see if valid for streaming.
			showAlertDialog(context, asset, c, "Permission Checked Failed", "Would you like to stream this video over the network?");
		} else {
			//We don't have any local data.  Just stream the item from the remote URL directly.
			watchStream(context,c,assetId, null);
		}
		
	}
	
	public static Dialog showAlertDialog(final Context context, final IAsset asset, final Cursor c, String title, String message) {
		AlertDialog alertDialog = new AlertDialog.Builder(context).create();
		alertDialog.setTitle(title);
		alertDialog.setMessage(message);
		alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				try {

					watchStream(context,c,asset.getAssetId(),asset.getUuid());
					dialog.dismiss();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} 
		});		
		
		alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
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

	private static void watchStream(Context context, Cursor c, String assetId, String assetUuid){
		final long catalogExpiry = c.getLong(c.getColumnIndex(CatalogColumns.CATALOG_EXPIRY));
		final long availabilityStart = c.getLong(c.getColumnIndex(CatalogColumns.AVAILABILITY_START));
		final int mediaType = c.getInt(c.getColumnIndex(CatalogColumns.MEDIA_TYPE));
		if (mediaType == 4 || !canWatchStream(context, availabilityStart, catalogExpiry)){
			Util.showToast(context, "Item not available", Toast.LENGTH_SHORT);
			return;
		}
		String url = c.getString(c.getColumnIndex(CatalogColumns.CONTENT_URL));
		String streamUrl = c.getString(c.getColumnIndex(CatalogColumns.STREAM_URL));
		if (!TextUtils.isEmpty(streamUrl)) {
			url = streamUrl;
		}
        Uri uri = Uri.parse(url);

		// register a play stream event
		// this will modify the first play time on an asset if it exists in the Virtuoso db.
		// we may or may not want that for streaming.
        int assetType = Common.AssetIdentifierType.FILE_IDENTIFIER;
        if(isDash(mediaType)){
            assetType = ISegmentedAsset.SEG_FILE_TYPE_MPD;
        }
        else if(isHls(mediaType)){
            assetType = ISegmentedAsset.SEG_FILE_TYPE_HLS;
        }

		Intent intent =  new Intent(context, PlayerActivity.class)
				.setAction(PlayerActivity.ACTION_VIEW)
				.setData(uri)
				.putExtra(PlayerActivity.VIRTUOSO_CONTENT_TYPE,assetType);

		Common.Events.addPlayStartEvent(context, assetId, assetUuid);
		context.startActivity(intent);
	}
	
	public static boolean canWatchVirtuosoItem(Context context, IAsset asset) {
		PermissionManager pm = new PermissionManager();
		Permission authorized = pm.canPlay(asset);
		
		if (authorized != Permission.EAccessAllowed)
			Util.showToast(context, "" + authorized, Toast.LENGTH_SHORT);
		
		return authorized == Permission.EAccessAllowed;
	}
	
	public static boolean canWatchStream(Context context, long availabilityStart, long catalogExpiry) {
		PermissionManager pm = new PermissionManager();

		if (catalogExpiry <= 0) {
			catalogExpiry = Long.MAX_VALUE;
		}
		Permission authorized = pm.canPlay(availabilityStart, catalogExpiry);
		
		if (authorized != Permission.EAccessAllowed)
			Util.showToast(context, "" + authorized, Toast.LENGTH_SHORT);
		
		return authorized == Permission.EAccessAllowed;
	}
	
	public static String getPath(Context context, IIdentifier id) {
		boolean isHls = false;
		String path = "";
		if (!isHls) {
			IFile file = (IFile) id;
			path = file.getFilePath();
		} else {
			ISegmentedAsset file = (ISegmentedAsset) id;
			URL theUrl;
			try {
				theUrl = file.getPlaylist();
				if (theUrl != null) {
					path = theUrl.toString();
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		return path;		
	}

    private static Intent buildPlayerIntent(Context context,IAsset a) throws MalformedURLException {
        int type = Common.AssetIdentifierType.FILE_IDENTIFIER;
        Uri path;
        if(a.getType() == Common.AssetIdentifierType.SEGMENTED_ASSET_IDENTIFIER){
            ISegmentedAsset sa = (ISegmentedAsset)a;
            type = sa.segmentedFileType();
            path = Uri.parse(sa.getPlaylist().toString());
        }
        else{
            IFile f = (IFile)a;
            path = Uri.parse(f.getFilePath());
        }

		Intent intent =  new Intent(context, PlayerActivity.class)
				.setAction(PlayerActivity.ACTION_VIEW)
				.setData(path)
				.putExtra(PlayerActivity.VIRTUOSO_ASSET,a)
				.putExtra(PlayerActivity.VIRTUOSO_CONTENT_TYPE,type);
		return intent;
    }

	public static void watchVirtuosoItem(Context context, IAsset i) {

        try{
            Intent openIntent = buildPlayerIntent(context, i);

            //register a play start event
            Common.Events.addPlayStartEvent(context, i.getAssetId(), i.getUuid());
            context.startActivity(openIntent);
        }
        catch(Exception e){
            throw new RuntimeException("Not a playable virtuoso file",e);
        }
	}
	
	/**
	 * Used by inbox to print expiration time
	 * 
	 * @param asset the asset
	 * @return expiration in seconds, -1 never
	 */
    public static long getExpiration(IAsset asset) {
        long completionTime = asset.getCompletionTime();	
    	long endWindow = asset.getEndWindow();
        // Not downloaded
        if (completionTime == 0) {	       
        	if (endWindow == Long.MAX_VALUE)
        		return -1;
        	return endWindow;
        // Downloaded
        } else {
        	//here the minimum value is used in the calculation.
        	long playTime = asset.getFirstPlayTime();
        	long playExpiry = Long.MAX_VALUE;
        	long eap = asset.getEap();
        	long ead = asset.getEad();
        	long expiry = endWindow;
        	
        	if(playTime > 0 && eap > -1)
        		playExpiry = playTime + eap;
        	
        	expiry = Math.min(expiry, playExpiry);
        	
        	if(ead > -1)
        		expiry = Math.min(expiry,completionTime + ead);
        	
        	return getExpiration(asset.getCompletionTime(),asset.getEndWindow(),asset.getFirstPlayTime(),asset.getEap(),asset.getEad());
        }
    }

    public static long getExpiration(long completionTime, long endWindow, long firstPlayTime, long expiryAfterPlay, long expiryAfterDownload) {
        // Not downloaded
        if (completionTime == 0) {	       
        	if (endWindow == Long.MAX_VALUE)
        		return -1;
        	return endWindow;
        // Downloaded
        } else {
        	//here the minimum value is used in the calculation.
        	long playTime = firstPlayTime;
        	long playExpiry = Long.MAX_VALUE;
        	long expiry = endWindow;
        	
        	if(playTime > 0 && expiryAfterPlay > -1)
        		playExpiry = playTime + expiryAfterPlay;
        	
        	expiry = Math.min(expiry, playExpiry);
        	
        	if(expiryAfterDownload > -1)
        		expiry = Math.min(expiry,completionTime + expiryAfterDownload);
        	
        	return expiry == Long.MAX_VALUE ? -1: expiry;
        }
    }
}
