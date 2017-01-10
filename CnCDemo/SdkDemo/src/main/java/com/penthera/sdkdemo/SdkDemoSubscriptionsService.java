package com.penthera.sdkdemo;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import com.penthera.sdkdemo.catalog.Catalog;
import com.penthera.sdkdemo.catalog.Catalog.CatalogColumns;
import com.penthera.sdkdemo.catalog.CatalogContentProvider;
import com.penthera.sdkdemo.catalog.CatalogStore;
import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.client.ISegmentedAsset;
import com.penthera.virtuososdk.client.subscriptions.SubscriptionKey;
import com.penthera.virtuososdk.client.subscriptions.SubscriptionsService;

/**
 * Extends SDK GCM Server
 * 
 * This component enables us to receive catalog updates form the SDK via the intent:
 * com.penthera.baratheon.baratheon.CATALOG_UPDATE
 * 
 * Also added a second intent to initiate downloads from within the app:
 * com.penthera.baratheon.baratheon.DOWNLOAD_UPDATE
 */
public class SdkDemoSubscriptionsService extends SubscriptionsService {
	/** Log Tag */
	private static final String TAG = SdkDemoSubscriptionsService.class.getName();

	/**
	 * Constructor the name of the server
	 * 
	 * @param name
	 */
	public SdkDemoSubscriptionsService(String name) {
		super(name);
	}

	/**
	 * Constructor
	 */
	public SdkDemoSubscriptionsService() {
		super("SdkDemoGcmService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i(TAG, "SdkDemoGCmService: onHandleIntent: " + intent.toString());
		
		super.onHandleIntent(intent);		
	}
		
	@Override
	public JSONObject processingFeedWithData(String uuid, JSONObject data, boolean complete) {
		return data;
	}	

	@SuppressLint("DefaultLocale")
	@Override
	public JSONObject processingAssetWithData(String uuid, JSONObject data, boolean complete) {				

		// --- Example of how to supply missing data
		if (!data.has(SubscriptionKey.DOWNLOAD_URL)) {
			try {
				data.put(SubscriptionKey.DOWNLOAD_URL, "### insert url for item here ###");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		String title = null;
		String image = null;
		
		if (data.has("title")){
			title = data.optString("title");
		}
		JSONObject metadata = data.optJSONObject(SubscriptionKey.META_DATA);
		if(metadata != null){
			if(TextUtils.isEmpty(title))
				title = metadata.optString("title");
			
			image = metadata.optString("imageURL");
		}
		
		if(TextUtils.isEmpty(title)) {
			title = uuid + ": A Subscribed asset";
		}
		
		if(!data.has(SubscriptionKey.ASSET_TYPE)){
			int assetType = -1;
			int segmentedType = -1;
			//attempt to determine from url
        	if(data.has(SubscriptionKey.DOWNLOAD_URL)){
        		String dURL =  data.optString(SubscriptionKey.DOWNLOAD_URL); 
        		//How do we differentiate between HLS and HSS here?
        		if(dURL != null) {
        			if(dURL.contains(".m3u8")){
        				assetType = Common.AssetIdentifierType.SEGMENTED_ASSET_IDENTIFIER;
        				segmentedType = ISegmentedAsset.SEG_FILE_TYPE_HLS;
        			}
        			else if(dURL.contains(".mpd")){
        				assetType = Common.AssetIdentifierType.SEGMENTED_ASSET_IDENTIFIER;
        				segmentedType = ISegmentedAsset.SEG_FILE_TYPE_MPD;
        			}
        			else if(dURL.contains("/manifest")){
        				assetType = Common.AssetIdentifierType.SEGMENTED_ASSET_IDENTIFIER;
        				segmentedType = ISegmentedAsset.SEG_FILE_TYPE_HSS;
        			}
        		}	
        	}
        	//or mime
        	if(assetType == -1 && data.has(SubscriptionKey.ASSET_MIME_TYPE)){
        		
        		String mime = data.optString(SubscriptionKey.ASSET_MIME_TYPE);
        		if(mime != null){
        			mime = mime.toLowerCase().trim();
        				
	        		if("application/x-mpegurl".equals(mime) || "application/vnd.apple.mpegurl".equals(mime)){
	        			assetType = Common.AssetIdentifierType.SEGMENTED_ASSET_IDENTIFIER;
        				segmentedType = ISegmentedAsset.SEG_FILE_TYPE_HLS;
	        		}
	        		else if("application/dash+xml".equals(mime) || "video/vnd.mpeg.dash.mpd".equals(mime)){
	        			assetType = Common.AssetIdentifierType.SEGMENTED_ASSET_IDENTIFIER;
        				segmentedType = ISegmentedAsset.SEG_FILE_TYPE_MPD;
	        		}
	        		else if(mime.startsWith("video")){
	        			assetType = Common.AssetIdentifierType.FILE_IDENTIFIER;	
	        		}
        		}
        	}
        	if(assetType > -1){
				try {
					data.put(SubscriptionKey.ASSET_TYPE, assetType);
					if(segmentedType > -1){
						data.put(SubscriptionKey.ASSET_SUBTYPE, segmentedType);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
        	}
		}
		
		
		if (TextUtils.isEmpty(image) && data.has("imageAssets")){
			JSONArray imageArray = data.optJSONArray("imageAssets");
			if(imageArray != null){
				JSONObject thumb = imageArray.optJSONObject(0);
				if(thumb != null)
					image = thumb.optString("url");
			}
		}
		
		if(TextUtils.isEmpty(image)) {
			image = "http://www.clubwebsite.co.uk/img/misc/noImageAvailable.jpg";
		}
		
		try {
			data.put(SubscriptionKey.ASSET_META_DATA, MetaData.toJson(title, image));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		//example of how to add into a local catalog cache - we do this to use the catalog detail fragment
		Uri uri = CatalogContentProvider.CATALOG_URI;
		boolean exists = exists(uri, Catalog.Query.WHERE_ID_IS, new String[]{uuid});
		if (!exists) {
			Log.i(TAG, "item does not exist: Creating in local catalog cache");	

			ContentValues cv = new ContentValues();
			
			CatalogStore.parseItem(data, cv, data.optString(SubscriptionKey.FEED_UUID));
			//we do this so that the item does not appear in our demos catalog
			cv.put(CatalogColumns.SUBCRIPTION_ASSET, true);
			ContentResolver cr = getApplicationContext().getContentResolver();
			Uri resultUri = cr.insert(uri, cv);
			if (resultUri == null) {
				Log.e(TAG, "insert failed: " + cv.toString());
			}
		}	
		
		return data;
	}
	
	//method to check existence of an item
	private boolean exists(Uri uri, String where, String[] args) {		
		ContentResolver cr = getApplicationContext().getContentResolver();
		Cursor c = null;

		try {
			c = cr.query(uri, null, where, args, null);
			if (c != null && c.getCount() > 0) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if ( c != null) {
				c.close();
				c = null;
			}
		}
		return false;
	}
}


