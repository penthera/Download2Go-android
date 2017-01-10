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

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.penthera.sdkdemo.Keys;
import com.penthera.sdkdemo.catalog.Catalog.CatalogColumns;
import com.penthera.sdkdemo.catalog.CatalogType.CatalogTypeColumns;
import com.penthera.sdkdemo.catalog.db.CatalogDb;

/**
 * Catalog JSON parser
 * 
 *   . Parses JSON
 *   . Stores items in relational DB via the ContentProvider
 *   
 * @author Glen
 */
public class CatalogStore {	
	public static final String TAG = CatalogStore.class.getName();

	/**
	 * Parse and commit the catalog to SQL DB
	 * 
	 * @param context the context
	 * @param db the relational DB
	 * @param root the catalog
	 */
	@SuppressLint("CommitPrefEdits")
	public static void parseAndCommit(Context context, SQLiteDatabase db, JSONObject root) {
		try {
			long lastUpdatedServer = parseUpdate(context, root);			

			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);		
			long lastUpdatedClient = sp.getLong(Keys.CATALOG_CLIENT_LAST_UPDATED, 0);
			if (lastUpdatedClient > lastUpdatedServer) { // = could be bad for 0 case?
				return;
			}
			parseCatalogType(context, root);
			parseContentItems(context, db, root.getJSONArray("contentItems"), null);
			sp.edit().putLong(Keys.CATALOG_CLIENT_LAST_UPDATED, System.currentTimeMillis()).commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Determine when catalog was last updated on the server
	 * 
	 * @param the context
	 * @param root the catalog
	 * @return
	 */
	private static long parseUpdate(Context context, JSONObject root) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);		
		long lastUpdated = 0;
		try {
			lastUpdated = root.getLong("last_updated");
			sp.edit().putLong(Keys.CATALOG_LAST_UPDATED, lastUpdated).commit();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return lastUpdated;
	}

	/**
	 * Get the catalog type
	 * 
	 * @param context the context
	 * @param root the catalog
	 */
	private static void parseCatalogType(Context context, JSONObject root) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		try {
			String catalogType = root.getString("catalog_type");
			sp.edit().putString(Keys.CATALOG_TYPE, catalogType).commit();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Parse individual items
	 * 
	 * @param db the database
	 * @param contentItems the content items
	 * @param parentId the parent of the items (if any)
	 */
	private static void parseContentItems(Context context, SQLiteDatabase db, JSONArray contentItems, String parentId) {
		ContentValues cv = new ContentValues();
		try {
			for (int i = 0; i < contentItems.length(); ++i) {
				cv.clear();
				JSONObject item = contentItems.getJSONObject(i);				
				parseItem(item, cv, parentId);
				cv.put(CatalogColumns.PARENT, parentId);
				commit(context, db, CatalogDb.CATALOG_TABLE_NAME, cv, CatalogColumns._ID);
			}	
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}


	
	/**
	 * Commit the content values to the DB
	 * 
	 * @param db the database
	 * @param table the table to commit
	 * @param cv the content values
	 * @param field the field for the ugpdate query
	 */
	private static void commit(Context context, SQLiteDatabase db, String table, ContentValues cv, String field) {		
		String value = cv.getAsString(field);
		boolean exists = exists(db, table, field, value);

		if (!exists) {
			Log.d(TAG, "inserting: " + cv);
			long mod = db.insert(table, field, cv);
			if (mod == -1) {
				Log.w(TAG, "Insert Failed: " + cv);				
			}
		} else {
			Log.i(TAG, "Updating: " + cv);
			int updated = db.update(table, cv, field + "=?", new String[]{value});
			if (updated <= 0) {
				Log.w(TAG, "Update Failed: " + cv);
			}
		}
	}
	
	/**
	 * Determine if the item exists
	 * 
	 * @param db the relational DB
	 * @param table the table
	 * @param field the field
	 * @param value the value
	 * 
	 * @return true, exists
	 */
	private static boolean exists(SQLiteDatabase db, String table, String field, String value) {
		int count = 0;
		Cursor c = null;

		// Build Query
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(table);

		// Run query
		try {
			c = qb.query(db, null, field + "=?", new String[]{value}, null, null, null);
			// Obtain count
			if (c != null) {
				count = c.getCount();
				if (count > 0) {
					return true;
				}
			}
		} finally {
			if (c != null) {
				c.close();
				c = null;
			}
		}
		return false;
	}

	/**
	 * Parse the catalog navigation panel: Not used in the demo
	 * 
	 * @param root
	 * @param ls
	 */
	public static void parseCatalogTypeList(JSONObject root, ArrayList<ContentValues> ls) {
		try {
			// Networks
			JSONArray networks = root.getJSONArray("types");
			for (int m = 0; m < networks.length(); ++m) {
				ContentValues cv = new ContentValues();
				JSONObject item = networks.getJSONObject(m);
				insertValue(item, cv, "name", CatalogTypeColumns.NAME, "");
				insertValue(item, cv, "friendly_name", CatalogTypeColumns.FRIENDLY_NAME , "");
				insertValue(item, cv, "type", CatalogTypeColumns.TYPE , "");
				insertValue(item, cv, "display", CatalogTypeColumns.DISPLAY , "");
				insertValue(item, cv, "order", CatalogTypeColumns.ORDER , "");
				cv.put(CatalogTypeColumns.DATE_MODIFIED, System.currentTimeMillis());					
				try {
					JSONArray imageArray = item.getJSONArray("imageAssets");
					JSONObject thumb = (JSONObject) imageArray.get(0);
					JSONObject full = (JSONObject) imageArray.get(1);				
					insertValue(thumb, cv, "url", CatalogTypeColumns.IMAGE_THUMBNAIL_URL, "");	
					insertValue(full, cv, "url", CatalogTypeColumns.IMAGE_URL, "");
				} catch (Exception e) {}
				ls.add(cv);
			}	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Parse catalog item
	 * 
	 * @param item the item to parse
	 * @param cv the content values for the item
	 */
	public static void parseItem(JSONObject item, ContentValues cv, String parentId) {
		insertValue(item, cv, "type", Catalog.CatalogColumns.TYPE, "0");
		insertValue(item, cv, "remoteUUID", Catalog.CatalogColumns._ID, "-1"); 		
		insertValue(item, cv, "accessWindow", Catalog.CatalogColumns.ACCESS_WINDOW, "0");		
		insertValue(item, cv, "catalogExpiry", Catalog.CatalogColumns.CATALOG_EXPIRY, "0");
		insertValue(item, cv, "contentRating", Catalog.CatalogColumns.CONTENT_RATING, "");
		insertValue(item, cv, "desc", Catalog.CatalogColumns.DESC, "");
		insertValue(item, cv, "downloadEnabled", Catalog.CatalogColumns.DOWNLOAD_ENABLED, "1");
		insertValue(item, cv, "expiryAfterPlay", Catalog.CatalogColumns.EXPIRY_AFTER_PLAY, "-1");
		insertValue(item, cv, "availableFrom", Catalog.CatalogColumns.AVAILABILITY_START, "-1");
		insertValue(item, cv, "downloadExpiry", Catalog.CatalogColumns.DOWNLOAD_EXPIRY, "-1");
		insertValue(item, cv, "expiryDate", Catalog.CatalogColumns.EXPIRY_DATE, "0");
		insertValue(item, cv, "featured", Catalog.CatalogColumns.FEATURED, "0");
		insertValue(item, cv, "genre", Catalog.CatalogColumns.GENRE, "0");
		insertValue(item, cv, "title", Catalog.CatalogColumns.TITLE, "0");		
		insertValue(item, cv, "popular", Catalog.CatalogColumns.POPULAR, "0");
		insertValue(item, cv, "downloadURL", Catalog.CatalogColumns.CONTENT_URL, "");
		insertValue(item, cv, "contentSize", Catalog.CatalogColumns.CONTENT_SIZE, "0");
		insertValue(item, cv, "duration", Catalog.CatalogColumns.DURATION, "0");
		insertValue(item, cv, "streamURL", Catalog.CatalogColumns.STREAM_URL, "");
		insertValue(item, cv, "mime", Catalog.CatalogColumns.MIME, null);
		insertValue(item, cv, "mediaType", Catalog.CatalogColumns.MEDIA_TYPE, "");
		insertValue(item, cv, "fragmentCount", Catalog.CatalogColumns.FRAGMENT_COUNT, "0");
		insertValue(item, cv, "fragmentPrefix", Catalog.CatalogColumns.FRAGMENT_PREFIX, "");
		
		// Images
		JSONArray imageAssets = null;
		JSONObject image = null;
		try {
			imageAssets = item.getJSONArray("imageAssets");
			image = imageAssets.getJSONObject(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		insertValue(image, cv, "url", Catalog.CatalogColumns.IMAGE_THUMBNAIL, "");
			
		image = null;
		try {
			image = imageAssets.getJSONObject(1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		insertValue(image, cv, "url", Catalog.CatalogColumns.IMAGE, "");	

		// Categories
		JSONArray categories;
		String value = "";
		try {
			if (item.has("categories")) {
				categories = item.getJSONArray("categories");
				for (int j = 0; j < categories.length(); ++j) {
					if (j != 0) {
						value += ",";
					}

					JSONObject category = categories.getJSONObject(j);
					value += category.getString("name");
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		cv.put(Catalog.CatalogColumns.CATEGORY, value);											
		cv.put(Catalog.CatalogColumns.PARENT, parentId);
	}

	/**
	 * Safely insert values in content value
	 * 
	 * @param obj
	 * @param cv content values
	 * @param jsonKey the JSON key
	 * @param cursorKey the corresponding DB key
	 * @param defaultValue the default value
	 */
	private static void insertValue(JSONObject obj, ContentValues cv, final String jsonKey, final String cursorKey, String defaultValue) {
		String value = defaultValue;
		try {
			if (obj.has(jsonKey)) {
				value = obj.getString(jsonKey);				
				value = !TextUtils.isEmpty(value) && value.equals("true") ? "1" : value;
				value = !TextUtils.isEmpty(value) && value.equals("false") ? "0" : value;
			}			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		cv.put(cursorKey, value);		
	}
}