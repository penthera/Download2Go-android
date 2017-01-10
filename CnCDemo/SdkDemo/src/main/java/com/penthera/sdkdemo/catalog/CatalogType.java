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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Catalog Type
 * 
 * Can be used to build a navigation widget for the catalog
 */
public final class CatalogType {
	private CatalogType() {
	}
	/**
	 * Catalog Type table
	 */
	public static final class CatalogTypeColumns implements BaseColumns {
		private CatalogTypeColumns() {
		}
		
		/**
		 * The content:// style URL for this table
		 */
		public static final Uri CONTENT_URI(String Authority) { return Uri.parse("content://" + Authority + "/CatalogType");}

		/**
		 * The MIME type of CONTENT_URI providing a directory of content.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.virtuososdk.CatalogType";

		/**
		 * The MIME type of a CONTENT_URI sub-directory of a single
		 * content item.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.virtuososdk.CatalogType";

		public static final String _ID = "_id";
		/** The name of the catalog type: {classic_tv, ...} ,  */
		public static final String NAME = "name";		
		/** The friendly name of the catalog type: {Classic TV, Radio Programs, ...} */
		public static final String FRIENDLY_NAME = "friendly_name";		
		/** Numeric type of item */
		public static final String TYPE = "type";
		/** 1, item should be displayed */
		public static final String DISPLAY = "display";
		/** The order of the item */
		public static final String ORDER = "typeOrder";
		/** The most recent time the item was modified.  A timestamp, represented as the number of seconds since the unix epoch.*/
		public static final String DATE_MODIFIED = "dateModified";
		/** The image/icon for the item */
		public static final String IMAGE_URL = "imageUrl";
		/** The thumbnail/icon for the item */ 
		public static final String IMAGE_THUMBNAIL_URL = "imageThumbnailUrl";
	}
		
	/**
	 * Queries
	 */
	public static final class Query {		
		/**
		 * Query item Id
		 */
		public static final String WHERE_ID_IS = CatalogType.CatalogTypeColumns._ID + "=?";
		/**
		 * Query where item is allowed to be displayed
		 */
		public static final String WHERE_DISPLAY = CatalogType.CatalogTypeColumns.DISPLAY + "=1";
	}
}
