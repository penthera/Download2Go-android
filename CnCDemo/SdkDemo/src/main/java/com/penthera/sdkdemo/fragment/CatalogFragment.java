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

package com.penthera.sdkdemo.fragment;

import java.util.Hashtable;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.penthera.sdkdemo.Extras.CatalogDetail;
import com.penthera.sdkdemo.ImageHelper;
import com.penthera.sdkdemo.R;
import com.penthera.sdkdemo.Util;
import com.penthera.sdkdemo.activity.CatalogDetailActivity;
import com.penthera.sdkdemo.catalog.Catalog.CatalogColumns;
import com.penthera.sdkdemo.catalog.CatalogContentProvider;
import com.penthera.virtuososdk.client.Virtuoso;


/**
 * Display the entire catalog
 */
public class CatalogFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

	/** Log tag */
	private static final String TAG = CatalogFragment.class.getName();
	
	/** The image loader */
	private ImageLoader mImageLoader;
	/** Image Loader display options */
	private DisplayImageOptions mDisplayImageOptions;

	/** The loader ID */
	private static final int LOADER_ID = CatalogFragment.class.hashCode();

	/** The layout */
	private View mLayout;
	/** The grid view */
	private GridView mGridView;
	/** The catalog adapter */
	private CatalogAdapter mAdapter;

	/** Virtuoso service: Monitor downloads */
	private Virtuoso mService;
	
	public static CatalogFragment newInstance(Virtuoso service) {
		CatalogFragment cf = new CatalogFragment();
		cf.mService = service;
		return cf;
	}			
	
	// onCreate
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Catalog Adapter
		mAdapter = new CatalogAdapter(getActivity(), null);
		
		// Images
		ImageLoaderConfiguration config = ImageHelper.getImgaeLoaderConfiguration(getActivity());
		mDisplayImageOptions = ImageHelper.getDisplayImageOptionsBuilder().build();		
		mImageLoader = ImageLoader.getInstance();
		mImageLoader.init(config);		
		
		// Action Bar
		setHasOptionsMenu(true);
	}
	
	// onStart
	@Override
	public void onStart() {
		super.onStart();
	}
	
	// onResume
	@Override
	public void onResume() {
		super.onResume();
		LoaderManager lm = getActivity().getSupportLoaderManager();
		if (lm.hasRunningLoaders() == false) {
			lm.restartLoader(LOADER_ID, null, CatalogFragment.this);
		} else {
			Log.i(TAG, "LOADER RUNNING");
		}
	}
	
	// onPause
	@Override
	public void onPause() {
		super.onPause();
	}

	// onDestroy
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
				
	// onCreateView
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		Log.i(TAG, "onCreateView()");

		mLayout = inflater.inflate(R.layout.fragment_catalog, null);

		mGridView = (GridView) mLayout.findViewById(R.id.grd);
		mGridView.setSelector(android.R.color.transparent);
		
		return mLayout;
	}
		
	// onActivityCreated
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
 		super.onActivityCreated(savedInstanceState);
		setUi(UI_PROGRESS);

		// Set Adapter
		mGridView.setAdapter(mAdapter);
				
	    // Asynchronous load adapter
		LoaderManager lm = getActivity().getSupportLoaderManager();
		lm.initLoader(LOADER_ID, null, this);
		
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, final int pos, long arg3) {
				// Grow Animation
		    	Animation grow = AnimationUtils.loadAnimation(getActivity(), R.anim.grow);
		    	view.startAnimation(grow);

		    	// Animate image when clicked on
		    	grow.setAnimationListener(new AnimationListener() {

					@Override
					public void onAnimationEnd(Animation animation) {
						Cursor c = mAdapter.getCursor();				
						c.moveToPosition(pos);
						
						// Catalog Arguments
						Bundle args = new Bundle();
						String id = c.getString(c.getColumnIndex(CatalogColumns.ASSET_ID));
						String title = c.getString(c.getColumnIndex(CatalogColumns.TITLE));
						args.putString(CatalogDetail.EXTRA_ID, id);
						args.putString(CatalogDetail.EXTRA_TITLE, title);						

						// Open Catalog
    					Util.startActivity(getActivity(), CatalogDetailActivity.class, args);
					}

					@Override
					public void onAnimationRepeat(Animation animation) {}

					@Override
					public void onAnimationStart(Animation animation) {}
		    	});				
			}
		});
	}
	
	// --- Cursor Loaders

	// onCreateLoader
	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		Log.i(TAG, "onCreateLoader");
		CursorLoader cursorLoader = null;
		cursorLoader = new CursorLoader(getActivity(),	
					CatalogContentProvider.CATALOG_URI,
					null, 
					CatalogColumns.SUBCRIPTION_ASSET + "=0",
					null,
					CatalogColumns.TITLE + " ASC");			
		return cursorLoader;		    
	}

	// onLoadFinished
	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		Log.i(TAG, "onCreateLoader");
		
		if (cursor != null && cursor.getCount() > 0) {
			mLayout.findViewById(R.id.prb_progress).setVisibility(View.GONE);
			mAdapter.swapCursor(cursor);
			setUi(UI_GRID);
		} else {
			setUi(UI_PROBLEM);
		}		
	}

	// onLoaderReset
	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		Log.d(TAG, "onLoaderReset");
		
		if (mAdapter != null)
			mAdapter.swapCursor(null);
	}
	
	// --- Menu

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);		
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	// --- Adapter
	
	/**
	 * 
	 */
	public class CatalogAdapter extends CursorAdapter 
	{
		/** Keep track of checks */
		public Hashtable<Integer, Integer> mChecked = new Hashtable<Integer, Integer>();

		/** Inflates the list rows */
		private LayoutInflater mInflater;
		
		/**
		 * Constructor
		 * 
		 * @param context
		 * @param c
		 */
	    public CatalogAdapter(Context context, Cursor c) 
	    {
	        super(context, c, 0);
			mInflater = LayoutInflater.from(context);			
	    }

	    // bindView
		@Override
		public void bindView(View row, Context context, Cursor cursor) {
			// Model
			String imageUrl = cursor.getString(cursor.getColumnIndex(CatalogColumns.IMAGE_THUMBNAIL));
			String fullImageUrl = cursor.getString(cursor.getColumnIndex(CatalogColumns.IMAGE));
	        String titleStr = cursor.getString(cursor.getColumnIndex(CatalogColumns.TITLE));
	        
	        // Genre
	        String genre = "";
	        genre = cursor.getString(cursor.getColumnIndex(CatalogColumns.GENRE));
	        if (TextUtils.isEmpty(genre)) {
		        genre = cursor.getString(cursor.getColumnIndex(CatalogColumns.CATEGORY));
		        if (!TextUtils.isEmpty(genre))
		        	genre = genre.split(",")[0];	        	
	        }
	        	        
	        // Image
	        final ImageView image = (ImageView)  row.findViewById(R.id.img);
	        image.setImageBitmap(null);
			mImageLoader.displayImage(imageUrl, image, mDisplayImageOptions, new SimpleImageLoadingListener() {
			    @Override
			    public void onLoadingComplete(Bitmap bitmap) {
			    	try {
			    		Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
			    		image.setAnimation(anim);
			    		anim.start();
			    	} catch (Exception e) {
			    		e.printStackTrace();
			    	}
			    }
			});
			
			// Cache full sized images when user views thumb-nail -> Eleiminate 2-3 second load time on detail page
			mImageLoader.loadImage(getActivity(), fullImageUrl, null);
			
	        // View
	        TextView title = (TextView) row.findViewById(R.id.txt_title);	        
	        title.setText(titleStr);
	        
	        // Category
	        TextView category = (TextView) row.findViewById(R.id.txt_category);	        
	        category.setText(genre);	        
		}

		// newView
		@Override
		public View newView(Context arg0, Cursor arg1, ViewGroup parent) {
			return mInflater.inflate(R.layout.cell_catalog, parent, false);
		}						
	}	
	
	// --- Helpers

	private static final int UI_GRID = 0;
	private static final int UI_PROGRESS = 1;
	private static final int UI_PROBLEM = 2;	

	private void setUi(int ui) {
		switch (ui) {
			// Grid
			case 0: {
				mLayout.findViewById(R.id.prb_progress).setVisibility(View.GONE);
				mLayout.findViewById(R.id.grd).setVisibility(View.VISIBLE);			
				mLayout.findViewById(R.id.txt_problem).setVisibility(View.GONE);			
				break;
			}
			// Progress
			case 1: {
				mLayout.findViewById(R.id.prb_progress).setVisibility(View.VISIBLE);
				mLayout.findViewById(R.id.grd).setVisibility(View.GONE);			
				mLayout.findViewById(R.id.txt_problem).setVisibility(View.GONE);			
				break;
			}
			// Message
			case 2: {
				mLayout.findViewById(R.id.prb_progress).setVisibility(View.GONE);
				mLayout.findViewById(R.id.grd).setVisibility(View.GONE);			
				mLayout.findViewById(R.id.txt_problem).setVisibility(View.VISIBLE);			
				break;
			}
		}
	}	
}
