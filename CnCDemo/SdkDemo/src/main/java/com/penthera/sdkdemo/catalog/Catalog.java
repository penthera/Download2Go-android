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
 * Contains DB fields and queries for the catalog
 * @author Glen
 */
public final class Catalog {
	private Catalog() {
	}
	/**
	 * Catalog table columns
	 */
	public static final class CatalogColumns implements BaseColumns {
		/** Do not create instancce */
		private CatalogColumns() {
		}
		
		/** The content:// style URL for this table */
		public static final Uri CONTENT_URI(String Authority) { return Uri.parse("content://" + Authority + "/Catalog");}

		/** The MIME type of CONTENT_URI providing a directory of content */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.virtuososdk.Catalog";

		/** The MIME type of a CONTENT_URI sub-directory of a single content item */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.virtuososdk.Catalog";

		/** Asset ID from the server */
		public static final String _ID = "_id";
		/** Access window */
		public static final String ACCESS_WINDOW = "accessWindow";		
		/** timestamp when item expires in seconds */
		public static final String CATALOG_EXPIRY = "catalogExpiry";
		/** The content rating: G, PG, ... */
		public static final String CONTENT_RATING = "contentRating";
		/** The user rating 1,2,3,4 stars */
		public static final String USER_RATING = "userRating";
		/** Creation timestamp in seconds */
		public static final String CREATION_TIME = "creationTime";
		/** Modification timestamp in seconds */
		public static final String MODIFY_TIME = "modifyTime";
		/** The description/synopsis of the item */
		public static final String DESC = "desc";
		/** true, this item can be downloaded: false, item can only be streamed */
		public static final String DOWNLOAD_ENABLED = "downloadEnabled";
		/** How long a user can store an item in seconds after it is downloaded */
		public static final String DOWNLOAD_EXPIRY = "downloadExpiry";
		/** The playout lenght of the item in seconds */
		public static final String DURATION = "duration";
		/** The timestamp, in seconds. when the item expires */
		public static final String EXPIRY_DATE = "expiryDate";
		/** true, this is  featured item */
		public static final String FEATURED = "featured";
		/** Genre: Comedy, Horror, ... */
		public static final String GENRE = "genre";
		/** The title of the item: Drive, Forest Gump, ... */
		public static final String TITLE = "title";
		/** The type of item movies, tv-show */
		public static final String TYPE = "type";
		/** true, it is a popular item */
		public static final String POPULAR = "popular";
		/** The URL where the item can be downloaded */
		public static final String CONTENT_URL = "contentUrl";
		/** The streaming URL of the item */
		public static final String STREAM_URL = "streamUrl";
		/** The size of the item in MB */
		public static final String CONTENT_SIZE = "contentSize";
		/** The thumbnail image for the item: DVD cover */
		public static final String IMAGE_THUMBNAIL = "thumbnail";
		/** The image of the item: DVD cover */
		public static final String IMAGE = "image";
		/** The category of the item */
		public static final String CATEGORY = "category";
		/** The parent asset/catalog ID of the item */
		public static final String PARENT = "parent";	
		/**  The last viewed time of the item */
		public static final String VIEWED_TIME = "viewedTime";
		/**  true, item contain sub items */
		public static final String IS_COLLECTION = "isCollection";
		/** Media type */
		public static final String MEDIA_TYPE = "mediaType";
		/** The fragment count @see downloadItem */
		public static final String FRAGMENT_COUNT = "fragmentCount";
		/** The fragment prefix @see downloadItem */
		public static final String FRAGMENT_PREFIX = "fragmentPrefix";
		/** Time stamp of when item will be made available to play. */
		public static final String AVAILABILITY_START = "availableFrom";
		/** Duration item remains available after first play. */
		public static final String EXPIRY_AFTER_PLAY = "expiryAfterPlay";
		/** mime type of content. */
		public static final String MIME = "mime";
		/** boolean flag indicating this item came from a subscription - just here so we can filter the main catalog tab*/
		public static final String SUBCRIPTION_ASSET = "subscriptionAsset";
	}
		
	/**
	 * Queries
	 */
	public static final class Query {				
		/** Query on asset/catalog ID */
		public static final String WHERE_ID_IS = Catalog.CatalogColumns._ID + "=?";

		/** Query on item type */
		public static final String WHERE_TYPE_IS = Catalog.CatalogColumns.TYPE + "=?";

		/** Query on parent asset/catalog ID */
		public static final String WHERE_PARENT_IS = Catalog.CatalogColumns.PARENT + "=?";		
	}
}
