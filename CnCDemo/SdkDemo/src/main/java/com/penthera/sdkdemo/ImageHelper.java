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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.download.URLConnectionImageDownloader;

/**
 * Miscellaneous Image Routines
 */
public class ImageHelper {
	/**
	 * Make image translucent
	 * 
	 * @param bitmap the image
	 * @param opacity the opacity
	 * 
	 * @return translucent image
	 */
	public static Bitmap adjustOpacity(Bitmap bitmap, int opacity) {
	    Bitmap mutableBitmap = bitmap.isMutable()
	                           ? bitmap
	                           : bitmap.copy(Bitmap.Config.ARGB_8888, true);
	    Canvas canvas = new Canvas(mutableBitmap);
	    int colour = (opacity & 0xFF) << 24;
	    canvas.drawColor(colour, PorterDuff.Mode.DST_IN);
	    return mutableBitmap;
	}
	
	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
	    Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
	        bitmap.getHeight(), Config.ARGB_8888);
	    Canvas canvas = new Canvas(output);

	    final int color = 0xff424242;
	    final Paint paint = new Paint();
	    final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
	    final RectF rectF = new RectF(rect);
	    final float roundPx = 12;

	    paint.setAntiAlias(true);
	    canvas.drawARGB(0, 0, 0, 0);
	    paint.setColor(color);
	    canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

	    paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
	    canvas.drawBitmap(bitmap, rect, rect, paint);

	    return output;
	  }
	
	public static Bitmap getDropShadow(Bitmap bitmap) {
		BlurMaskFilter blurFilter = new BlurMaskFilter(4, BlurMaskFilter.Blur.OUTER);
		Paint shadowPaint = new Paint();
		shadowPaint.setMaskFilter(blurFilter);

		int[] offsetXY = new int[2];
		Bitmap shadowImage = bitmap.extractAlpha(shadowPaint, offsetXY);
		Bitmap shadowImage32 = shadowImage.copy(Bitmap.Config.ARGB_8888, true);

		Canvas c = new Canvas(shadowImage32);
		c.drawBitmap(bitmap, -offsetXY[0], -offsetXY[1], null);

		return shadowImage32;
	}
	
	public static ImageLoaderConfiguration getImgaeLoaderConfiguration(Context context) {
		DisplayImageOptions options = new DisplayImageOptions.Builder()
	    .cacheInMemory().cacheOnDisc()
	    .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2).build();

		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
			    .memoryCacheExtraOptions(125, 125)
			    .discCacheExtraOptions(125, 125, CompressFormat.JPEG, 75)
			    .threadPoolSize(3)
			    .threadPriority(Thread.NORM_PRIORITY - 1)
			    .denyCacheImageMultipleSizesInMemory()
			    .offOutOfMemoryHandling()
			    .memoryCache(new UsingFreqLimitedMemoryCache(2 * 1024 * 1024))
			    .discCacheFileNameGenerator(new HashCodeFileNameGenerator())
			    .imageDownloader(new URLConnectionImageDownloader(5 * 1000, 20 * 1000))
			    .defaultDisplayImageOptions(DisplayImageOptions.createSimple())
			    .enableLogging().defaultDisplayImageOptions(options).build();
		return config;
	}
	
	public static DisplayImageOptions getDisplayImageOptions() {
		DisplayImageOptions options = new DisplayImageOptions.Builder()
	    .cacheInMemory().cacheOnDisc()
	    .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2).build();
		return options;
	}
	
	public static DisplayImageOptions.Builder getDisplayImageOptionsBuilder() {		
		DisplayImageOptions.Builder builder = new DisplayImageOptions.Builder()
				.cacheInMemory()
				.cacheOnDisc().showStubImage(R.drawable.no_image);
		return builder;
	}
}
