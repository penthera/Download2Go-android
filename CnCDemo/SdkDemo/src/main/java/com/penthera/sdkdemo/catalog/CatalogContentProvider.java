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

import org.json.JSONObject;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import com.penthera.sdkdemo.Config;
import com.penthera.sdkdemo.catalog.Catalog.CatalogColumns;
import com.penthera.sdkdemo.catalog.db.CatalogDb;
import com.penthera.sdkdemo.catalog.db.CatalogDb.DatabaseHelper;

/**
 * Content provider
 * 
 * Provides access to catalog
 */
public class CatalogContentProvider extends ContentProvider 
{
	/** Log Tag */
	private static final String TAG = CatalogContentProvider.class.getName();

	/** true catalog is refreshed from cloud */
	private static long mUpdateTime = 0;

	/** Thread to update catalog */
	private volatile CatalogUpdateThread mCatalogUpdateThread;

	/** DB Helpers */
	private DatabaseHelper mOpenHelper;	

	// --- URI 
	/** Catalog Table URI */
	public static final Uri CATALOG_URI = Uri.parse("content://" + CatalogContentProvider.AUTHORITY + "/");
	/** Catalog Table URI */
	public static final Uri CATALOG_TYPE_URI = Uri.parse("content://" + CatalogContentProvider.AUTHORITY + "/catalogType/");

	// --- URI Matching
	/** Catalog URI matcher */
	private static final UriMatcher mCatalogUriMatcher;
	/** Catalog URI matcher */
	private static final UriMatcher mCatalogTypeUriMatcher;
		
	/** The authority */
	public static final String AUTHORITY = "com.penthera.sdkdemo.catalog";
		
	/** URI Code */
	final static class CATALOG_PROV_CODE 
	{
		private CATALOG_PROV_CODE()
		{
		}
		final static int CATALOG		= 1;
	}

	/* Static initialisation block */
	static
	{
		/** Catalog */
		mCatalogUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);//set the root code
		// Pre Android 4.3 you could specify the root uri as shown below.
		// mCatalogUriMatcher.addURI(AUTHORITY, "/", CATALOG_PROV_CODE.CATALOG);
		// Doing it like that on 4.3 will result in a child node of UriMatcher being added for "" (empty string)
		// Simply changing to an empty string will not work as it will then fail on Pre 4.3.
		// Adding an empty string as well as "/" results in additional children to traverse.
		// Solution is to add in a uri specifying null. Creates same amount of children and is interpreted as root authority.
		mCatalogUriMatcher.addURI(AUTHORITY, null, CATALOG_PROV_CODE.CATALOG);//set the root code for 4.3
		mCatalogUriMatcher.addURI(AUTHORITY, "catalog", CATALOG_PROV_CODE.CATALOG);		
		mCatalogUriMatcher.addURI(AUTHORITY, "catalog/*", CATALOG_PROV_CODE.CATALOG);		

		/** Catalog Type */
		mCatalogTypeUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		mCatalogTypeUriMatcher.addURI(AUTHORITY, null, CATALOG_PROV_CODE.CATALOG);		
		mCatalogTypeUriMatcher.addURI(AUTHORITY, "catalogType", CATALOG_PROV_CODE.CATALOG);		
		mCatalogTypeUriMatcher.addURI(AUTHORITY, "catalogType/*", CATALOG_PROV_CODE.CATALOG);		
	}

	
	@Override
	public void attachInfo(Context context, ProviderInfo info) {
		super.attachInfo(context, info);
	}


	// ------------------------------- P R O V I D E R  I N T E R F A C E  ----------------------------------------------

	// onCreaate
	@Override
	public boolean onCreate() {		
		Log.d(TAG, "getting helper");
		if (CatalogDb.getInstance() == null) {
			Log.d(TAG, "Orpheus Db is null. initialising...");
			CatalogDb.init(getContext().getApplicationContext(), CatalogContentProvider.AUTHORITY);
		}
		mOpenHelper = CatalogDb.getHelper();
		Log.d(TAG, "helper is " + mOpenHelper);
		return true;
	}

	// getType
	@Override
	public String getType(Uri aUri)
	{
		int matchNumber = -1;
		if ((matchNumber = mCatalogUriMatcher.match(aUri)) > 0) {

			if (matchNumber == CATALOG_PROV_CODE.CATALOG) {
				return "catalog";
			}
		}
		return null;
	}
	
	// Query
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] aArgs, String aSortOrder) {
		Cursor cursor = null;
		if (mCatalogUriMatcher.match(uri) > 0) {
			cursor = queryCatalog(uri, projection, selection, aArgs, aSortOrder);
		} else if (mCatalogTypeUriMatcher.match(uri) > 0) {
			cursor = doQueryCatalogType(uri, projection, selection, aArgs, aSortOrder);			
		} else {
			throw new IllegalArgumentException("Failed to query, unknown URI: " + uri);
		}
		
		return cursor;
	}	
	
	/**
	 * Query the catalog table
	 * 
	 * @param uri
	 * @param projection
	 * @param selection
	 * @param args
	 * @param order
	 * @return
	 */
	private Cursor queryCatalog(Uri uri, String[] projection, String selection, String[] args, String order) {
		if (isUpdateCatalog()) {
			doUpdateCatalog();
		}
		return doQueryCatalog(uri, projection, selection, args, order);
	}

	// insert
	@Override
	public Uri insert(Uri uri, ContentValues valueSet)
	{
		if (mCatalogUriMatcher.match(uri) > 0) {
			return insertCatalog(uri, valueSet);
		} else {	
			throw new IllegalArgumentException("Unknown URI " + uri);			
		}
	}
	
	/**
	 * Insert catalog table items to DB
	 * 
	 * @param uri
	 * @param values
	 * @return
	 */
	private Uri insertCatalog(Uri uri, ContentValues values) {
		Long now = Long.valueOf(System.currentTimeMillis());
		if (values.containsKey(CatalogColumns.CREATION_TIME) == false) {
			values.put(CatalogColumns.CREATION_TIME, now);
		}
		if (values.containsKey(CatalogColumns.MODIFY_TIME) == false) {
			values.put(CatalogColumns.MODIFY_TIME, now);
		}

		long rowId = -1;
		try {
			SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		    rowId = db.insert(CatalogDb.CATALOG_TABLE_NAME, CatalogColumns.DESC, values);
		} finally {
			mOpenHelper.releaseDatabase();
		}
		
		if (rowId > -1) {
			Uri contentDbUri = ContentUris.withAppendedId(CatalogContentProvider.CATALOG_URI, rowId);
			return contentDbUri;
		}

		return null;
	}
		
	// Update catalog data
	@Override
	public int update(Uri uri, ContentValues values, String where, String[] args)
	{
		if (mCatalogUriMatcher.match(uri) > 0) {
			return updateCatalog(uri, values, where, args);
		} else {	
			throw new IllegalArgumentException("Unknown URI " + uri);			
		}
	}
	
	private int updateCatalog(Uri uri, ContentValues values, String where, String[] args) {
		Long now = Long.valueOf(System.currentTimeMillis());
		if (values.containsKey(CatalogColumns.MODIFY_TIME) == false) {
			values.put(CatalogColumns.MODIFY_TIME, now);
		}

		int mod;
		try {
			SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		    mod = db.update(CatalogDb.CATALOG_TABLE_NAME, values, where, args);
		} finally {
			mOpenHelper.releaseDatabase();
		}

		getContext().getApplicationContext().getContentResolver().notifyChange(CATALOG_URI, null);							
		return mod;		
	}
			
	// Delete catalog data
	@Override
	public int delete(Uri uri, String where, String[] args)
	{		
		throw new IllegalArgumentException("Failed to delete, unknown URI: " + uri);
	}

	/**
	 * Query the catalog table in the SQL DB
	 * 
	 * @param uri
	 * @param projection
	 * @param selection
	 * @param args
	 * @param order
	 * @return
	 */
	private Cursor doQueryCatalog(Uri uri, String[] projection, String selection, String[] args, String order) {
		Cursor c = null;
		
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(CatalogDb.CATALOG_TABLE_NAME);


		try {
			// Get the database and run the query
			SQLiteDatabase db = mOpenHelper.getReadableDatabase();
			c = qb.query(db, projection, selection, args, null, null, order);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mOpenHelper.releaseDatabase();
		}
		return c;		
	}
	
	private Cursor doQueryCatalogType(Uri uri, String[] projection, String selection, String[] args, String order) {
		Cursor c = null;
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(CatalogDb.CATALOG_TYPE_TABLE_NAME);
		try {
			// Get the database and run the query
			SQLiteDatabase db = mOpenHelper.getReadableDatabase();
			c = qb.query(db, projection, selection, args, null, null, order);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mOpenHelper.releaseDatabase();
		}
		return c;		
	}

	// --- Catalog Helpers
		
	/**
	 * Thread to update the catalog
	 */
	class CatalogUpdateThread extends Thread {
		@Override
		public void run() {
			try {
				Log.d(TAG, "Checking catalog on server");
				boolean success = batchUpdate();
				if (success) {
					mUpdateTime = System.currentTimeMillis()/1000;
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				mCatalogUpdateThread = null;				
			}
		}
	}	

	/**
	 * Create catalog update thread
	 */
	private void doUpdateCatalog() {
		if (mCatalogUpdateThread == null) {
			mCatalogUpdateThread = new CatalogUpdateThread();
			mCatalogUpdateThread.start();
		}
	}
	
	/**
	 * @return true, catalog needs to be updated
	 */
	private boolean isUpdateCatalog() {
		// Update Already Running
		if (mCatalogUpdateThread != null) {
			return false;
		}
		
		long now = System.currentTimeMillis();
		long diff = now - mUpdateTime;
		
		// Not time to update
		if (diff < Config.CATALOG_UPDATE_INTERVAL) {
			return false;
		}
		
		// Okay
		return true;
	}	

	/**
	 * Retrieve catalog committing to DB
	 * 
	 * @return true, success
	 */
	private boolean batchUpdate() {
		boolean success = false;

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(CatalogDb.CATALOG_TABLE_NAME);
		
		try {
			// Get the database and run the query (NOTE: must be done after the network IO)
			SQLiteDatabase db = mOpenHelper.getWritableDatabase();
			success = commitBatch(db);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mOpenHelper.releaseDatabase();	
		}
		return success;
	}
	
	/**
	 * Get catalog data converting from JSON format to ContentValue format
	 * 
	 * @return true, success
	 */
	private boolean commitBatch(SQLiteDatabase db) {		
		JSONObject responseJson;
		
		CatalogRequest catalogRequest = new CatalogRequest();
		responseJson = catalogRequest.executeToJson();
		
		//If contacting a live catalog this would check the response codes - JSON response etc...
		//Our demo catalog is static data in request itself.
		boolean success = true;
		if (success) { 
			CatalogStore.parseAndCommit(getContext(), db, responseJson);
		} else {
			Log.w(TAG, "Catalog request failed");
		}
		return success;
	}
}
