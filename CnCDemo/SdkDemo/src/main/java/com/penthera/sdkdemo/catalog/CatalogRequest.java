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

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Class containing a hard-coded catalog
 * 
 * In a real world project, the catalog would be retrieved via a HTTP transaction from a host server
 * @author Glen
 */
public class CatalogRequest {
	
	/**
	 * Get catalog server response
	 * 
	 * @return The catalog
	 */
	public JSONObject executeToJson() {
		JSONObject obj = null;
		try {
			obj = new JSONObject(TEST_DATA2);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return obj;
	}

	private static final int NEVER = -1;
	private static final int ONE_DAY = 86400;
	private static final int FORTNIGHT = 86400*14;
	private static final long TWO_YEAR = ONE_DAY*2*365;
	private static final long NOW = System.currentTimeMillis() / 1000;
	private static final long TWO_YEARS_FROM_NOW = NOW + TWO_YEAR;
	private static final long ONE_DAY_FROM_NOW = NOW + ONE_DAY;
	private static final int FIVE_MIN = 60 * 5;
	private static final int TEN_MIN = FIVE_MIN * 2;
	private static final long TEN_MIN_FROM_NOW = NOW + TEN_MIN;
	private static final long TEN_MIN_AGO = NOW - TEN_MIN;
	
	private static final String TEST_DATA2 = new String(
			"{" +
				"\"catalog_type\":\"test\"," +
				"\"last_updated\":"+TEN_MIN_AGO+"," +
				"\"contentItems\":["  + 
				"{" +
					"\"genre\":\"Movies\"," +
					"\"downloadURL\":\"http://virtuoso-demo-content.s3.amazonaws.com/College_512kb.mp4\"," +
					"\"desc\":\"To reconcile with his girlfriend, a bookish college student tries to become an athlete.\"," +
					"\"contentSize\":279821079," +
					"\"featured\":false," +
					"\"downloadExpiry\":" + FORTNIGHT + "," +
					"\"availableFrom\":" + NOW + "," +
					"\"expiryAfterPlay\":" + NEVER + "," +
					"\"type\":4000," +
					"\"remoteUUID\":\"COLLEGE_MP4\"," +
					"\"mediaType\":2," +
					"\"title\":\"(MP4) College, Buster Keaton, 1927\"," +
					"\"duration\":3882," +
					"\"mime\":\"video/mp4\"," +
					"\"streamURL\":\"http://virtuoso-demo-content.s3.amazonaws.com/College_512kb.mp4\"," +
					"\"popular\":false," +
					"\"downloadEnabled\":true," +
					"\"contentRating\":\"PG\"," +
					"\"imageAssets\":[" +
										"{\"impKey\":\"gridCellImage\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/College.jpg\"}," +
										"{\"impKey\":\"gridCellImage@2x\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/College.jpg\"}," +
										"{\"impKey\":\"detailBackgroundImage\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/College.jpg\"}," +
										"{\"impKey\":\"detailBackgroundImage@2x\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/College.jpg\"}" +
									"]," +
					"\"categories\":[" +
										"{\"name\":\"movies\"}" +
									"]," +
					"\"networkUUID\":\"1\"," +
					"\"airplayEnabled\":true," +
					"\"catalogExpiry\":" + TWO_YEARS_FROM_NOW +

				"}," + 
				"{" +
					"\"genre\":\"Speeches\"," +
					"\"downloadURL\":\"http://virtuoso-demo-content.s3.amazonaws.com/Steve_Jobs_Stanford_Speech.mp4\"," +
					"\"desc\":\"At his Stanford University commencement speech, Steve Jobs, CEO and co-founder of Apple and Pixar, urges us to pursue our dreams and see the opportunities in life's setbacks - including death itself.\"," +
					"\"contentSize\":37121244," +
					"\"featured\":false," +
					"\"downloadExpiry\":" + FORTNIGHT + "," +
					"\"availableFrom\":" + NOW + "," +
					"\"expiryAfterPlay\":" + NEVER + "," +
					"\"type\":4000," +
					"\"remoteUUID\":\"JOBS_MP4\"," +
					"\"mediaType\":2," +
					"\"title\":\"(MP4) Steve Jobs, Stanford\"," +
					"\"duration\":904," +
					"\"mime\":\"video/mp4\"," +
					"\"popular\":false," +
					"\"downloadEnabled\":true," +
					"\"contentRating\":\"PG\"," +
					"\"imageAssets\":[" +
										"{\"impKey\":\"gridCellImage\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/jobs.jpg\"}," +
										"{\"impKey\":\"gridCellImage@2x\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/jobs.jpg\"}," +
										"{\"impKey\":\"detailBackgroundImage\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/jobs.jpg\"}," +
										"{\"impKey\":\"detailBackgroundImage@2x\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/jobs.jpg\"}" +
									"]," +
					"\"categories\":[" +
										"{\"name\":\"speeches\"}" +
									"]," +
					"\"networkUUID\":\"1\"," +
					"\"airplayEnabled\":true," +
					"\"catalogExpiry\":" + TWO_YEARS_FROM_NOW +

				"},"	+ 
				"{" +
					"\"genre\":\"Movies\"," +
					"\"downloadURL\":\"http://virtuoso-demo-content.s3.amazonaws.com/College/college.m3u8\"," +
					"\"desc\":\"To reconcile with his girlfriend, a bookish college student tries to become an athlete.\"," +
					"\"featured\":false," +
					"\"downloadExpiry\":" + FORTNIGHT + "," +
					"\"availableFrom\":" + NOW + "," +
					"\"expiryAfterPlay\":" + NEVER + "," +
					"\"type\":4000," +
					"\"remoteUUID\":\"COLLEGE_HLS\"," +
					"\"mediaType\":3," +
					"\"mime\":\"application/x-mpegurl\"," +
					"\"title\":\"(HLS) College, Buster Keaton, 1927\"," +
					"\"duration\":3882," +
					"\"streamURL\":\"http://virtuoso-demo-content.s3.amazonaws.com/College/college.m3u8\"," +
					"\"popular\":false," +
					"\"downloadEnabled\":true," +
					"\"contentRating\":\"PG\"," +
					"\"imageAssets\":[" +
					"{\"impKey\":\"gridCellImage\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/College.jpg\"}," +
					"{\"impKey\":\"gridCellImage@2x\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/College.jpg\"}," +
					"{\"impKey\":\"detailBackgroundImage\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/College.jpg\"}," +
					"{\"impKey\":\"detailBackgroundImage@2x\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/College.jpg\"}" +
					"]," +
						"\"categories\":[" +
							"{\"name\":\"movies\"}" +
							"]," +
					"\"networkUUID\":\"1\"," +
					"\"airplayEnabled\":true," +
					"\"catalogExpiry\":" + TWO_YEARS_FROM_NOW +
				"},"	 +
				"{" +
					"\"genre\":\"Movies\"," +
					"\"downloadURL\":\"https://storage.googleapis.com/wvmedia/clear/h264/tears/tears_sd.mpd\"," +
					"\"desc\":\"DASH SD TEARS.\"," +
					"\"featured\":false," +
					"\"downloadExpiry\":" + FORTNIGHT + "," +
					"\"availableFrom\":" + NOW + "," +
					"\"expiryAfterPlay\":" + NEVER + "," +
					"\"type\":4000," +
					"\"remoteUUID\":\"DASH_TEARS\"," +
					"\"mediaType\":6," +
					"\"mime\":\"application/octet-stream\"," +
					"\"title\":\"(DASH) Tears\"," +
					"\"duration\":3882," +
					"\"streamURL\":\"https://storage.googleapis.com/wvmedia/clear/h264/tears/tears_sd.mpd\"," +
					"\"popular\":false," +
					"\"downloadEnabled\":true," +
					"\"contentRating\":\"PG\"," +
					"\"imageAssets\":[" +
					"{\"impKey\":\"gridCellImage\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/College.jpg\"}," +
					"{\"impKey\":\"gridCellImage@2x\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/College.jpg\"}," +
					"{\"impKey\":\"detailBackgroundImage\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/College.jpg\"}," +
					"{\"impKey\":\"detailBackgroundImage@2x\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/College.jpg\"}" +
					"]," +
					"\"categories\":[" +
					"{\"name\":\"movies\"}" +
					"]," +
					"\"networkUUID\":\"1\"," +
					"\"airplayEnabled\":true," +
					"\"catalogExpiry\":" + TWO_YEARS_FROM_NOW +
					"},"	 +
					"{" +
					"\"genre\":\"Movies\"," +
					"\"downloadURL\":\"https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears_sd.mpd\"," +
					"\"desc\":\"WIDEVINE SECURE DASH SD TEARS.\"," +
					"\"featured\":false," +
					"\"downloadExpiry\":" + FORTNIGHT + "," +
					"\"availableFrom\":" + NOW + "," +
					"\"expiryAfterPlay\":" + NEVER + "," +
					"\"type\":4000," +
					"\"remoteUUID\":\"DASH_TEARS_WV\"," +
					"\"mediaType\":6," +
					"\"mime\":\"application/octet-stream\"," +
					"\"title\":\"(DASH) Widevine Secure Tears\"," +
					"\"duration\":3882," +
					"\"streamURL\":\"https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears_sd.mpd\"," +
					"\"popular\":false," +
					"\"downloadEnabled\":true," +
					"\"contentRating\":\"PG\"," +
					"\"imageAssets\":[" +
					"{\"impKey\":\"gridCellImage\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/College.jpg\"}," +
					"{\"impKey\":\"gridCellImage@2x\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/College.jpg\"}," +
					"{\"impKey\":\"detailBackgroundImage\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/College.jpg\"}," +
					"{\"impKey\":\"detailBackgroundImage@2x\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/College.jpg\"}" +
					"]," +
					"\"categories\":[" +
					"{\"name\":\"movies\"}" +
					"]," +
					"\"networkUUID\":\"1\"," +
					"\"airplayEnabled\":true," +
					"\"catalogExpiry\":" + TWO_YEARS_FROM_NOW +
				"},"	 +

				"{" +
				"\"genre\":\"Speeches\"," +
				"\"downloadURL\":\"http://virtuoso-demo-content.s3.amazonaws.com/Steve/steve.m3u8\"," +
				"\"streamURL\":\"http://virtuoso-demo-content.s3.amazonaws.com/Steve/steve.m3u8\"," +
				"\"desc\":\"At his Stanford University commencement speech, Steve Jobs, CEO and co-founder of Apple and Pixar, urges us to pursue our dreams and see the opportunities in life's setbacks - including death itself.\"," +
				"\"featured\":false," +
				"\"downloadExpiry\":" + FORTNIGHT + "," +
				"\"availableFrom\":" + NOW + "," +
				"\"expiryAfterPlay\":" + NEVER + "," +
				"\"type\":4000," +
				"\"remoteUUID\":\"JOBS_HLS\"," +
				"\"mediaType\":3," +
				"\"title\":\"(HLS) Steve Jobs, Stanford\"," +
				"\"duration\":904," +
				"\"mime\":\"application/x-mpegurl\"," +
				"\"popular\":false," +
				"\"downloadEnabled\":true," +
				"\"contentRating\":\"PG\"," +
				"\"imageAssets\":[" +
									"{\"impKey\":\"gridCellImage\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/jobs.jpg\"}," +
									"{\"impKey\":\"gridCellImage@2x\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/jobs.jpg\"}," +
									"{\"impKey\":\"detailBackgroundImage\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/jobs.jpg\"}," +
									"{\"impKey\":\"detailBackgroundImage@2x\",\"url\":\"http://virtuoso-demo-content.s3.amazonaws.com/jobs.jpg\"}" +
								"]," +
				"\"categories\":[" +
									"{\"name\":\"speeches\"}" +
								"]," +
				"\"networkUUID\":\"1\"," +
				"\"airplayEnabled\":true," +
				"\"catalogExpiry\":" + TWO_YEARS_FROM_NOW +
			"},"	+
			"{" +
					"\"genre\":\"Custom\"," +
					"\"downloadURL\":\"http://\"," +
					"\"streamURL\":\"http://\"," +
					"\"desc\":\"Use this to test your own url\"," +
					"\"featured\":false," +
					"\"downloadExpiry\":" + FORTNIGHT + "," +
					"\"availableFrom\":" + NOW + "," +
					"\"expiryAfterPlay\":" + NEVER + "," +
					"\"type\":4000," +
					"\"remoteUUID\":\"CUSTOM_HLS\"," +
					"\"mediaType\":3," +
					"\"title\":\"Custom Title\"," +
					"\"duration\":0," +
					"\"mime\":\"application/x-mpegurl\"," +
					"\"popular\":false," +
					"\"downloadEnabled\":true," +
					"\"contentRating\":\"PG\"," +
					"\"imageAssets\":[" +
					"{\"impKey\":\"gridCellImage\",\"url\":\" \"}," +
					"{\"impKey\":\"gridCellImage@2x\",\"url\":\" \"}," +
					"{\"impKey\":\"detailBackgroundImage\",\"url\":\" \"}," +
					"{\"impKey\":\"detailBackgroundImage@2x\",\"url\":\" \"}" +
					"]," +
					"\"categories\":[" +
					"{\"name\":\"custom\"}" +
					"]," +
					"\"networkUUID\":\"1\"," +
					"\"airplayEnabled\":true," +
					"\"catalogExpiry\":" + TWO_YEARS_FROM_NOW +

				"}"	+
			"]}");
}
