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

package com.penthera.sdkdemo.catalog.db;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQuery;
import android.os.Handler;
import android.util.Log;

import com.penthera.sdkdemo.catalog.Catalog.CatalogColumns;
import com.penthera.sdkdemo.catalog.CatalogType.CatalogTypeColumns;

/**
 * Catalog Database
 */
public class CatalogDb {
    private static String LOG_TAG = CatalogDb.class.getName();

    /** Cataog tabble */
	public static final String CATALOG_TABLE_NAME = "catalog";
	/** Catalog type table */
	public static final String CATALOG_TYPE_TABLE_NAME = "catalogType";

	/** The name of the DB on internal storage */
	private static final String DATABASE_NAME = "catalog.db";
	/** The version of the DB used for upgrades */
	private static final int DATABASE_VERSION = 4;

	private static CatalogDb iInstance = null;
	private DatabaseHelper iHelper = null;

	private static SQLiteDatabase iDb = null;
	private static Lock iLock = new ReentrantLock();
	
	public synchronized static void init(Context aContext,String Authority) {
		Log.d(LOG_TAG,"initialising the db.");
		if(iInstance == null) {
			Log.d(LOG_TAG,"creating the db instance.");
			iInstance = new CatalogDb(aContext,Authority);
		}
	}
	
	public static void release() {
		Log.d(LOG_TAG,"releasing the db instance.");
		iInstance.iHelper.iInternalhelper.close();
		iInstance = null;
	}

	public static CatalogDb getInstance() {
		return iInstance;
	}

	public static DatabaseHelper getHelper() {
		if(iInstance != null) return iInstance.iHelper;

		return null;
	}

	private CatalogDb(Context aContext,String authority) {
		this.iHelper = new DatabaseHelper(aContext);
	}

	public static class DatabaseHelper
	{
		private InternalDatabaseHelper iInternalhelper = null;

		DatabaseHelper(Context aContext)
		{
			iInternalhelper = new InternalDatabaseHelper(aContext);
			resetCloseHandler();
			createDatabase();
		}

		public SQLiteDatabase getReadableDatabase()
		{
			iLock.lock();
			resetCloseHandler();
			if (iDb == null || !iDb.isOpen()){
				createDatabase();
			}
			return iDb;
		}

		public SQLiteDatabase getWritableDatabase()
		{
			iLock.lock();
			resetCloseHandler();
			if (iDb == null || !iDb.isOpen()){
				createDatabase();
			}
			return iDb;
		}

		public void releaseDatabase()
		{
			try {
				iLock.unlock();
			}
			catch (Exception e)
			{
				Log.e(LOG_TAG, "exception while unlocking database",e);
			}
		}

		private synchronized void resetCloseHandler()
		{
			iCloseHandler.removeCallbacksAndMessages(null);
			iCloseHandler.postDelayed(iDatabaseCloser, 10*60*1000);
			iRequestClose = false;
		}

		private synchronized void createDatabase()
		{
			if (iDb == null || !iDb.isOpen()){
				iDb = iInternalhelper.getWritableDatabase();
				Log.d(LOG_TAG, "Database opened");
			}
		}

		private Handler iCloseHandler = new Handler();		
		private Runnable iDatabaseCloser = new Runnable(){

			@Override
			public void run() {
				if(iRequestClose)
				{
					closeDatabase();						
				}
				else
				{
					iCloseHandler.postDelayed(iDatabaseCloser, 10*60*1000);
				}

			}};	

			private boolean iRequestClose = false;

			public void setCloseRequested(boolean b) {
				iRequestClose = b;
			}

			public void closeDatabase(){
				try {
					iLock.lock();
					iInternalhelper.close();
					Log.d(LOG_TAG, "Database closed");
					iLock.unlock();
				}
				catch (Exception e)
				{
					Log.e(LOG_TAG, "exception while closing database",e);
				}
			}

			private static class InternalDatabaseHelper extends SQLiteOpenHelper {	
				InternalDatabaseHelper(Context context) {
					super(context, DATABASE_NAME, new VirtuosoSDKCursorFactory(), DATABASE_VERSION);
				}

				@Override
				public void onCreate(SQLiteDatabase db) {			
					performRecUpgrade(db,0,DATABASE_VERSION);
				}

				@Override
				public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
					Log.d(LOG_TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
					performRecUpgrade(db,oldVersion,newVersion);	
				}

				int performRecUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

					if (newVersion==9999 || oldVersion==9999) {
						onCreate(db);
						db.execSQL("DROP TABLE IF EXISTS " + CATALOG_TABLE_NAME);
						db.execSQL("DROP TABLE IF EXISTS " + CATALOG_TYPE_TABLE_NAME);
						return newVersion;
					}

					if(oldVersion > newVersion) {
						throw new RuntimeException("Invalid Upgrade - Old version cannot be greater than the new version");
					}
					if(oldVersion != newVersion) {
						int upgradedVersion = oldVersion;
						//do the upgrade
						switch(upgradedVersion) {
						case 0:
							createCatalogTable(db);		
							createCatalogTypeTable(db);		
							upgradedVersion = DATABASE_VERSION;
							break;
						case 1: // upgrades are unnecessary - simply recreate
						case 2:
                        case 3:  // recreate because structure changed
                            db.execSQL("DROP TABLE IF EXISTS " + CATALOG_TABLE_NAME);
                            createCatalogTable(db);
							upgradedVersion = DATABASE_VERSION;
							break;
						}
					return performRecUpgrade(db, upgradedVersion, newVersion);
					}
					return oldVersion;			
				}
				
			
				void createCatalogTable(SQLiteDatabase db) {
					db.execSQL("CREATE TABLE " + CATALOG_TABLE_NAME + " ("
							+ CatalogColumns._ID
							+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
							+ CatalogColumns.ASSET_ID
							+ " TEXT UNIQUE, " // REMOTE - ASSET ID [FROM CATALOG/SERVER]
							+ CatalogColumns.ACCESS_WINDOW
							+ " INTEGER DEFAULT 0, "
							+ CatalogColumns.CONTENT_RATING 
							+ " TEXT, "
							+ CatalogColumns.USER_RATING 
							+ " INTEGER DEFAULT 1, "
							+ CatalogColumns.CATALOG_EXPIRY 
							+ " INTEGER DEFAULT 0, "
							+ CatalogColumns.CREATION_TIME 
							+ " INTEGER DEFAULT 0, "
							+ CatalogColumns.MODIFY_TIME 
							+ " INTEGER DEFAULT 0, "
							+ CatalogColumns.DESC
							+ " TEXT, "
							+ CatalogColumns.DOWNLOAD_ENABLED
							+ " BOOLEAN DEFAULT 1, "
							+ CatalogColumns.DOWNLOAD_EXPIRY
							+ " INTEGER DEFAULT 0, "
							+ CatalogColumns.EXPIRY_AFTER_PLAY
							+ " INTEGER DEFAULT 0, "
							+ CatalogColumns.AVAILABILITY_START
							+ " INTEGER DEFAULT 0, "
							+ CatalogColumns.DURATION
							+ " INTEGER DEFAULT 0, "
							+ CatalogColumns.EXPIRY_DATE
							+ " INTEGER DEFAULT 0, "
							+ CatalogColumns.GENRE
							+ " TEXT, "						
							+ CatalogColumns.FEATURED
							+ " BOOLEAN DEFAULT 0, "						
							+ CatalogColumns.TITLE
							+ " TEXT, "
							+ CatalogColumns.TYPE
							+ " INTEGER DEFAULT 0, "
							+ CatalogColumns.POPULAR
							+ " BOOLEAN DEFAULT 0, "
							+ CatalogColumns.CONTENT_URL
							+ " TEXT, "						
							+ CatalogColumns.CONTENT_SIZE
							+ " INTEGER DEFAULT 0, "						
							+ CatalogColumns.IMAGE_THUMBNAIL
							+ " TEXT, "
							+ CatalogColumns.IMAGE
							+ " TEXT, "
							+ CatalogColumns.CATEGORY
							+ " TEXT, "
							+ CatalogColumns.MIME
							+ " TEXT, "
							+ CatalogColumns.PARENT
							+ " TEXT, "
							+ CatalogColumns.STREAM_URL
							+ " TEXT, "
							+ CatalogColumns.VIEWED_TIME
							+ " INTEGER DEFAULT 0, "
							+ CatalogColumns.IS_COLLECTION
							+ " BOOLEAN DEFAULT 0, "						
							+ CatalogColumns.MEDIA_TYPE
							+ " INTEGER DEFAULT 0, "				
							+ CatalogColumns.FRAGMENT_COUNT
							+ " INTEGER DEFAULT 0, "
							+ CatalogColumns.FRAGMENT_PREFIX
							+ " TEXT, "
                            + CatalogColumns.SUBCRIPTION_ASSET
                            + " BOOLEAN DEFAULT 0, "
                            + CatalogColumns.DRM_SCHEME_UUID
                            + " TEXT"
							+ ");");
				}

				void createCatalogTypeTable(SQLiteDatabase db) {
					db.execSQL("CREATE TABLE " + CATALOG_TYPE_TABLE_NAME + " ("
							+ CatalogTypeColumns._ID
							+ " TEXT PRIMARY KEY, "
							+ CatalogTypeColumns.NAME
							+ " TEXT, "
							+ CatalogTypeColumns.FRIENDLY_NAME
							+ " TEXT, "
							+ CatalogTypeColumns.IMAGE_URL
							+ " TEXT, "
							+ CatalogTypeColumns.IMAGE_THUMBNAIL_URL
							+ " TEXT, "
							+ CatalogTypeColumns.TYPE
							+ " INTEGER DEFAULT 0, "
							+ CatalogTypeColumns.DISPLAY
							+ " BOOLEAN DEFAULT 0, "
							+ CatalogTypeColumns.ORDER
							+ " INTEGER DEFAULT 0, "
							+ CatalogTypeColumns.DATE_MODIFIED
							+ " INTEGER DEFAULT 0"
							+ ");");
				}								
		}
	}
}

class VirtuosoSDKSQLiteCursor extends SQLiteCursor
{

	private static String LOG_TAG = VirtuosoSDKSQLiteCursor.class.getName();
	private static int counter = 0;

	@SuppressWarnings("deprecation")
	public VirtuosoSDKSQLiteCursor(SQLiteDatabase 		database, 
			SQLiteCursorDriver  driver,
			String				table,
			SQLiteQuery			query)
	{
		super(database, driver, table, query);
		int openCount = incCounter();

		if (openCount > 25)
			Log.d(LOG_TAG, "Cursor created, open: " + openCount);
	}

	@Override
	synchronized public void close()
	{
		if (!isClosed()) {
			super.close();
			int openCount = decCounter();

			if (openCount > 25)
				Log.d(LOG_TAG, "Cursor closed, open: " + openCount);
		}
	}

	private synchronized int incCounter(){
		if(counter == 0)
			CatalogDb.getHelper().setCloseRequested(false);		
		return ++counter;
	}

	private synchronized int decCounter(){

		--counter;
		if (counter == 0){
			if (CatalogDb.getHelper() != null) {
				CatalogDb.getHelper().setCloseRequested(true);
			}
		}
		return counter;
	}
}

class VirtuosoSDKCursorFactory implements SQLiteDatabase.CursorFactory
{

	@Override
	public Cursor newCursor(SQLiteDatabase db,
			SQLiteCursorDriver masterQuery, String editTable,
			SQLiteQuery query) {

		return new VirtuosoSDKSQLiteCursor(db, masterQuery, editTable, query);
	}
}


