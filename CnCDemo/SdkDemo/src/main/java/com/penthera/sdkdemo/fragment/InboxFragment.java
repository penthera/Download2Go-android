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

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Set;
import java.util.UUID;

import org.json.JSONObject;
import org.ocpsoft.pretty.time.PrettyTime;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;

import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import android.support.v4.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.commonsware.cwac.merge.MergeAdapter;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.penthera.sdkdemo.Config;
import com.penthera.sdkdemo.Extras;
import com.penthera.sdkdemo.ImageHelper;
import com.penthera.sdkdemo.MetaData;
import com.penthera.sdkdemo.R;
import com.penthera.sdkdemo.Util;
import com.penthera.sdkdemo.VirtuosoUtil;
import com.penthera.sdkdemo.activity.CatalogDetailActivity;
import com.penthera.sdkdemo.activity.MainActivity.DemoTabListener;
import com.penthera.sdkdemo.customviews.InboxRow;
import com.penthera.virtuososdk.Common.AssetStatus;
import com.penthera.virtuososdk.Common.EngineStatus;
import com.penthera.virtuososdk.client.EngineObserver;
import com.penthera.virtuososdk.client.IAssetManager;
import com.penthera.virtuososdk.client.IService;
import com.penthera.virtuososdk.client.ServiceException;
import com.penthera.virtuososdk.client.Virtuoso;
import com.penthera.virtuososdk.client.database.AssetColumns;

/**
 * Display the inbox Q
 */
public class InboxFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>{

	/** Log tag */
	private static final String TAG = InboxFragment.class.getName();

	// --- Service
	/** The download service */
	private Virtuoso mService;
	/** The assets manager */
	private IAssetManager mAssetManager;

	// --- Adapters
	/** The main list adapter */
	private MergeAdapter mAdapter = new MergeAdapter();
	
	/** The Queue Adapter */
	private ContentAdapter mQueueAdapter;
	private HeaderAdapter mQueueHeaderAdapter;

	/** The Downloaded Adapter */
	private ContentAdapter mCompletedAdapter;
	private HeaderAdapter mCompletedHeaderAdapter;

	/** The Expired Adapter */
	private ContentAdapter mExpiredAdapter;
	private HeaderAdapter mExpiredHeaderAdapter;
	
	/** The empty list adapter */
	private EmptyAdapter mEmptyAdapter;

	/** The loader ID */
	private static final int LOADER_ID_QUEUED = 1;
	private static final int LOADER_ID_DOWNLOADED = 2;
	private static final int LOADER_ID_EXPIRED = 3;
	
	/** Action mode */
	private ActionMode mActionMode;

	/** Status widget */
	private TextView mFooter;
	/** Status widget */
	private View mFooterLayout;

	/** Status widget */
	private TextView mStatus;
	/** Virtuoso reported status */
	int mVirtuosoStatus;

	//determine when the service is connected
	private IService.IConnectionObserver mConnectionObserver = new IService.IConnectionObserver(){

		@Override
		public void connected() {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					setStatus();				
				}
			});
		}

		@Override
		public void disconnected() {
		}
		
	};
	/** handle on a connected Service. Used to update the status. */
	private IService mConnectedService;
	
	/** The fields we want for the Assets */
	static final String[] PROJECTION = new String[]{
		AssetColumns._ID
		,AssetColumns.UUID
		,AssetColumns.TYPE		
		,AssetColumns.ASSET_ID
		,AssetColumns.DOWNLOAD_STATUS
		,AssetColumns.ERROR_COUNT
		,AssetColumns.METADATA
		,AssetColumns.EAP
		,AssetColumns.EAD
		,AssetColumns.END_WINDOW
		,AssetColumns.FIRST_PLAY_TIME
		,AssetColumns.COMPLETION_TIME
		,AssetColumns.CURRENT_SIZE
		,AssetColumns.EXPECTED_SIZE
		,AssetColumns.CONTENT_LENGTH
	};

	/**
	 * Create a new instance of the fragment with the service from the parent activity
	 * 
	 * @param service the Vrituoso service
	 * @return
	 */
	DemoTabListener mListener;

	public static InboxFragment newInstance(Virtuoso service, DemoTabListener l) {
		InboxFragment inf = new InboxFragment();
		inf.mService = service;
		inf.mAssetManager = service.getAssetManager();
		inf.mListener = l;
		return inf;
	}		
	
	
	// onCreate
	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
				
		// Menu
		setHasOptionsMenu(true);		
	}

	// onCreateView
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View layout = inflater.inflate(R.layout.list, null);

		layout.findViewById(R.id.row_status).setVisibility(View.VISIBLE);

		//view gets created when it becomes available.
		//this can be created of screen.
		//if content has been updated before the view is created then it can be lost.
		//ensure status is not lost when returning
		mStatus = (TextView) layout.findViewById(R.id.txt_status);
		setStatus();

		mFooterLayout = layout.findViewById(R.id.row_footer);
		mFooter = (TextView) layout.findViewById(R.id.txt_footer);
		return layout;
	}
	
	// onActivityCreated
	//this will get called when the view comes in to play in the ViewPager of the main Activity
	//we have to be careful here not to re-instantiate the list adapters otherwise messages can get lost from the observers.
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		LoaderManager lm = getActivity().getSupportLoaderManager();
		lm.initLoader(LOADER_ID_QUEUED, null, InboxFragment.this);
		lm.initLoader(LOADER_ID_DOWNLOADED, null, InboxFragment.this);
		lm.initLoader(LOADER_ID_EXPIRED, null, InboxFragment.this);
		
		//queued adapter
		if(mQueueAdapter == null){
			mQueueAdapter = new ContentAdapter(getActivity(),null);
			mQueueHeaderAdapter = new HeaderAdapter(getActivity(), getString(R.string.queued), mQueueAdapter);
			mQueueAdapter.setAdapterType(ContentAdapter.ADAPTER_TYPE_QUEUE);
			mAdapter.addAdapter(mQueueHeaderAdapter);
			mAdapter.addAdapter(mQueueAdapter);
		}
		//downloaded adapter
		if(mCompletedAdapter == null){
			mCompletedAdapter = new ContentAdapter(getActivity(),null);
			mCompletedHeaderAdapter = new HeaderAdapter(getActivity(), getString(R.string.downloaded), mCompletedAdapter);
			mCompletedAdapter.setAdapterType(ContentAdapter.ADAPTER_TYPE_DOWNLOADED);
			mAdapter.addAdapter(mCompletedHeaderAdapter);
			mAdapter.addAdapter(mCompletedAdapter);
		}

		//expired adapter
		if(mExpiredAdapter == null){
			mExpiredAdapter = new ContentAdapter(getActivity(),null);
			mExpiredHeaderAdapter = new HeaderAdapter(getActivity(), getString(R.string.expired), mExpiredAdapter);
			mExpiredAdapter.setAdapterType(ContentAdapter.ADAPTER_TYPE_EXPIRED);
			mAdapter.addAdapter(mExpiredHeaderAdapter);
			mAdapter.addAdapter(mExpiredAdapter);
		}

		// Empty
		if(mEmptyAdapter == null){
			mEmptyAdapter = new EmptyAdapter(getActivity(),new BaseAdapter[]{ mQueueAdapter, mCompletedAdapter,mExpiredAdapter});
			mAdapter.addAdapter(mEmptyAdapter);
		}

		// Set the list adapter to use the merge adapter
		setListAdapter(mAdapter);	
		
		if( mService != null )
			mConnectedService = mService.getService();
		else 
			mConnectedService = null;
	}
	
	// onResume
	@Override
	public void onResume() {
		super.onResume();
		if (mService != null) {
			mService.addObserver(mEngineObserver);
		} else {
			Log.w(TAG, "problem");
		}
		
		if( mConnectedService != null )
		{
			mConnectedService.setConnectionObserver(mConnectionObserver);
			mConnectedService.bind();
		}
	}

	private class MyEngineObserver extends EngineObserver {
		@Override
		public void engineStatusChanged(final int arg0) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					setStatus(arg0);		
				}				
			});
		}
	}
	private MyEngineObserver mEngineObserver = new MyEngineObserver();

	// onPause
	@Override
	public void onPause() {
		super.onPause();
		if (mService != null) {
			mService.removeObserver(mEngineObserver);
		}

		if( mConnectedService != null )
		{
			mConnectedService.setConnectionObserver(null);
			mConnectedService.unbind();
		}
	}
	
	// onDestroy
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	// setUserVisibleHint
	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
	    super.setUserVisibleHint(isVisibleToUser);
	}
	
	// setMenuVisibility
	@Override
    public void setMenuVisibility(final boolean visible) {
        super.setMenuVisibility(visible);
	}
	
	// onCreateOptionsMenu
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_inbox, menu);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		mPauseResumeMenuItem = menu.findItem(R.id.menu_pause_resume);
		//member data of the fragment could have been updated whilst the view was out of bounds
		//Ensure that view creation does not override the title.
		setPauseResumeMenuItem();
		mAdapter.notifyDataSetChanged();
	}	

	// onOptionsItemSelected
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_add_test_item: {
				handleAddTestItem();
				break;
			}
			case R.id.menu_pause_resume: {
				handlePauseResume(item);
				break;
			}
	        case R.id.menu_sync: {
	        	handleSync();
	        	return true;
	        }			
		}	
		return super.onOptionsItemSelected(item);
	}
	
	// --- Empty Adapter 

	/**
	 * Display empty message when both adapters are empty
	 * 
	 * Note: Bit over the top: Could replace the ListView with an empty view in which case you would not need an adapter.
	 * This adapter was used on another project which still showed other row content with an empty Q
	 */
	public static class EmptyAdapter extends ArrayAdapter<Object> {
		private BaseAdapter[] mAdapters;


		public EmptyAdapter(Context context, BaseAdapter[] adapters) {
			super(context, R.layout.download_empty, new ArrayList<Object> ());
			mAdapters = adapters;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {	
			View row = convertView;	       
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(R.layout.download_empty, null);
			row.setId(R.id.row_empty);
			return row;
		}

		@Override
		public int getCount() {
			if(mAdapters != null){

				for(BaseAdapter adapter : mAdapters){
					if(adapter.getCount() > 0)
						return 0;
				}
			}
			return 1;
		}
	}

	/**
	 * Only display header when adapter is populated
	 */	
	public static class HeaderAdapter extends ArrayAdapter<Object> {
		private BaseAdapter mAdapter;
		private String mTitle;
		
		public HeaderAdapter(Context context, String title, BaseAdapter a) {
			super(context, R.layout.download_empty, new ArrayList<Object> ());
			mTitle = title;
			mAdapter = a;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {	
			View row = convertView;	        
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(R.layout.row_header, null);

			// Title
			TextView tv = (TextView) row.findViewById(R.id.txt_title);
			tv.setText(mTitle);
			
			return row;
		}

		@Override
		public int getCount() {
			if (mAdapter.getCount() > 0) {
				return 1;
			}
			return 0;
		}
	}
	// --- Cursor Loader 

	// onCreateLoader
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
		Log.i(TAG, "onCreateLoader " + id);
		CursorLoader cursorLoader = null;
		if (id == LOADER_ID_QUEUED && mAssetManager != null) {
			Log.d(TAG, "Creating Queued Loader");
			Uri uri = mAssetManager.getQueue().CONTENT_URI();
			cursorLoader = new CursorLoader(getActivity(),uri,PROJECTION,null,null,null);
		}	
		else if (id == LOADER_ID_DOWNLOADED && mAssetManager != null){
			Log.d(TAG, "Creating Downloaded Loader");
			Uri uri = mAssetManager.getDownloaded().CONTENT_URI();
			cursorLoader = new CursorLoader(getActivity(),uri,PROJECTION,null,null,null);
		}	
		else if (id == LOADER_ID_EXPIRED && mAssetManager != null){
			Log.d(TAG, "Creating Expired Loader");
			Uri uri = mAssetManager.getExpired().CONTENT_URI();
			cursorLoader = new CursorLoader(getActivity(),uri,PROJECTION,null,null,null);
		}
		return cursorLoader;		    
	}
	
	// onLoadFinished
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		Log.i(TAG, "onLoadFinished");
		final int id = loader.getId();	
		//do we need to notify the adapter notifyDataSetChanged?
		if (id == LOADER_ID_QUEUED && mAssetManager != null) {
			Log.d(TAG, "Finish Queued Loader");
			cursor.setNotificationUri(getActivity().getContentResolver(), mAssetManager.getQueue().CONTENT_URI());
			mQueueAdapter.swapCursor(cursor);
		}	
		else if (id == LOADER_ID_DOWNLOADED && mAssetManager != null){
			Log.d(TAG, "Finish Downloaded Loader");
			cursor.setNotificationUri(getActivity().getContentResolver(), mAssetManager.getDownloaded().CONTENT_URI());
			mCompletedAdapter.swapCursor(cursor);
		}	
		else if (id == LOADER_ID_EXPIRED && mAssetManager != null){
			Log.d(TAG, "Finish Expired Loader");
			cursor.setNotificationUri(getActivity().getContentResolver(), mAssetManager.getExpired().CONTENT_URI());
			mExpiredAdapter.swapCursor(cursor);
		}
		mEmptyAdapter.notifyDataSetChanged();		
		setFooter();
	}
			
	// onLoaderReset
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.d(TAG, "onLoaderReset");
		if(loader != null){
			final int id = loader.getId();	
			if (id == LOADER_ID_QUEUED && mQueueAdapter != null) {
				Log.d(TAG, "Resetting Queue Loader");
				mQueueAdapter.swapCursor(null);
			}	
			else if (id == LOADER_ID_DOWNLOADED && mCompletedAdapter != null){
				Log.d(TAG, "Resetting Downloaded Loader");
				mCompletedAdapter.swapCursor(null);
			}	
			else if (id == LOADER_ID_EXPIRED && mExpiredAdapter != null){
				Log.d(TAG, "Resetting Expired Loader");
				mExpiredAdapter.swapCursor(null);
			}
		}
	}	
	
	// --- Content Adapter
	public class ViewTag {
		final String mAssetId;
		final int mId;
		final int mPosition;
		final int mCursorCount;
		ViewTag(int id, int position, String assetId, int count){
			mId = id;
			mPosition = position;
			mAssetId = assetId;
			mCursorCount = count;
		}	
	}
	
	public class ContentAdapter extends CursorAdapter {
		// --- Image Loading	    
		/** The image Loader */
		private ImageLoader mImageLoader;
		/** The image loader options */
		private DisplayImageOptions mDisplayImageOptions;
		
		final static int ADAPTER_TYPE_QUEUE = LOADER_ID_QUEUED;
		final static int ADAPTER_TYPE_DOWNLOADED = LOADER_ID_DOWNLOADED;
		final static int ADAPTER_TYPE_EXPIRED = LOADER_ID_EXPIRED;
		private int mAdapterType = ADAPTER_TYPE_QUEUE;
		
		/** Keep track of checks */
		public Hashtable<Integer, Integer> mChecked = new Hashtable<Integer, Integer>();
	    
	    /** The inflater */
		private LayoutInflater mInflater;

	    /** Up listener */
	    private UpListener mUpListener = new UpListener();
	    /** Down listener */
	    private DownListener mDownListener = new DownListener();
	    
		public ContentAdapter(Context context, Cursor c) {
			super(context, c, 0);
			mInflater = LayoutInflater.from(context);

			// Images
		    ImageLoaderConfiguration config = ImageHelper.getImgaeLoaderConfiguration(context);
			mImageLoader = ImageLoader.getInstance();
			mImageLoader.init(config);
			mDisplayImageOptions = ImageHelper.getDisplayImageOptionsBuilder().build();	
		}

		@Override
		public void bindView(View row, Context context, final Cursor cursor) {
			
			try {
				final int position = cursor.getPosition();
				int count = cursor.getCount();

				// 0 maps to AssetColumns._ID from projection
				final int id = cursor.getInt(0);
				
				// 1 maps to AssetColumns.UUID from projection
				String uuid = cursor.getString(1);
				// 3 maps to AssetColumns.ASSET_ID from projection
				String assetID = cursor.getString(3);
				
				//create the views tag
				ViewTag viewTag = new ViewTag(id,position,assetID,count);
				row.setTag(viewTag);
				
				// Process meta data
				String titleStr = assetID;
				// 6 maps to AssetColumns.METADATA from projection
				String jsonStr = cursor.getString(6);

				
				if(!TextUtils.isEmpty(jsonStr)){
					JSONObject obj = new JSONObject(jsonStr);
					String imageUrl = obj.getString(MetaData.IMAGE_THUMBNAIL_URL);	        
					titleStr = obj.getString(MetaData.TITLE);			
			        // Image
			        ImageView image = (ImageView)  row.findViewById(R.id.img);	        
					mImageLoader.displayImage(imageUrl, image, mDisplayImageOptions);
				}
				// Title
		        TextView title = (TextView) row.findViewById(R.id.txt_title);
		        title.setText(titleStr);
		        
		     // Up & Down Buttons
				View up = row.findViewById(R.id.btn_up);
				View down = row.findViewById(R.id.btn_down);
	        	
				if (mAdapterType == ADAPTER_TYPE_QUEUE) {					
					// Up
					if (showUp(count,position)) {
						up.setVisibility(View.VISIBLE);
						up.setTag(viewTag);
						up.setOnClickListener(mUpListener);						
					} else {
						up.setVisibility(View.GONE);
					}
					
					// Down
					if (showDown(count,position)) {
						down.setVisibility(View.VISIBLE);
						down.setTag(viewTag);
						down.setOnClickListener(mDownListener);						
					} else {
						down.setVisibility(View.GONE);
					}
						
				} else {
					up.setVisibility(View.GONE);					
					down.setVisibility(View.GONE);
				}

		        

		        // TODO: Best to create one instance not allocate here
		        row.setOnLongClickListener(new OnLongClickListener() {
		        	@Override
		        	public boolean onLongClick(View v) {
		        		ViewTag viewTag = (ViewTag) v.getTag();
		        		toggleCheck(viewTag.mId);

		        		if (mActionMode == null) {
		        			mActionMode = getActivity().startActionMode(mCabCallback);
		        		}

		        		int numChecked = mCompletedAdapter.mChecked.keySet().size() + 
		        				mQueueAdapter.mChecked.keySet().size() + mExpiredAdapter.mChecked.keySet().size();

		        		String title = String.format(getString(R.string.num_selected), numChecked);
		        		mActionMode.setTitle(title);

		        		return true;
		        	}			
		        });

		        // TODO: Best to create one instance not allocate here
		        row.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						// Asset
		        		ViewTag viewTag = (ViewTag) v.getTag();
	        	        // Args
	        	        Bundle args = new Bundle();
	        			args.putString(Extras.CatalogDetail.EXTRA_ID, viewTag.mAssetId);
	        			
	        			// Catalog Detail
	        			Util.startActivity(getActivity(), CatalogDetailActivity.class, args);
					}		        	
		        });
		        

        		View rowDownloadStatus = row.findViewById(R.id.row_download_status);
        		View rowErrorCount = row.findViewById(R.id.row_error_count);

        		ProgressBar pb = (ProgressBar) row.findViewById(R.id.prg);
		        if (mAdapterType == ADAPTER_TYPE_QUEUE) {	
					// 4 maps to AssetColumns.DOWNLOAD_STATUS from projection
					final int download_status = cursor.getInt(4);

	        		TextView downloadStatus = (TextView) row.findViewById(R.id.txt_download_status);
	        		TextView errorCount = (TextView) row.findViewById(R.id.txt_error_count);
	        		
	        		// Download status
	        		String value;
	        		switch(download_status){
						case AssetStatus.DOWNLOADING:
							value = "downloading";
							break;
							
						case AssetStatus.DOWNLOAD_COMPLETE:
							value = "complete";
							break;
							
						case AssetStatus.EXPIRED:
							value = "expired";
							break;

						case AssetStatus.DOWNLOAD_FILE_COPY_ERROR:
							value = "io error";
							break;

						case AssetStatus.DOWNLOAD_FILE_MIME_MISMATCH:
							value = "error on mime";
							break;

						case AssetStatus.DOWNLOAD_FILE_SIZE_MISMATCH:
							value = "error on size";
							break;

						case AssetStatus.DOWNLOAD_NETWORK_ERROR:
							value = "network error";
							break;

						case AssetStatus.DOWNLOAD_REACHABILITY_ERROR:
							value = "unreachable";
							break;

						case AssetStatus.DOWNLOAD_DENIED_ASSET:
							value = "DENIED : MAD";
							break;

						case AssetStatus.DOWNLOAD_DENIED_ACCOUNT :
							value = "DENIED : MDA";
							break;
						case AssetStatus.DOWNLOAD_DENIED_MAX_DEVICE_DOWNLOADS:
							value = "DENIED :MPD";
							break;
							
						default:
							value = "pending";
	        		}
		        	downloadStatus.setText(value);
		        	
		        	// Error Count

					// 5 maps to AssetColumns.ERROR_COUNT from projection
					final int error_count = cursor.getInt(5);
        			errorCount.setText(""+error_count);
	        			        		
	        		// Visibility
	        		rowDownloadStatus.setVisibility(View.VISIBLE);
	        		rowErrorCount.setVisibility(View.VISIBLE);
	        		
	        		// 12 maps to AssetColumns.CURRENT_SIZE from projection
					// 13 maps to AssetColumns.EXPECTED_SIZE from projection
					// 14 maps to AssetColumns.CONTENT_LENGTH from projection
					long size = cursor.getLong(13);
					if(size <= 0){
						size = cursor.getLong(14);
					}
					long currentSize = cursor.getLong(12);
					int val = (size<=0) ? 0 : (int) ( ( ((double) currentSize)/(size)) * 100.0);
					if((val > 0 || (val == 0 && download_status == AssetStatus.DOWNLOADING))  && val != 100){
			        	pb.setVisibility(View.VISIBLE);
			        	pb.setProgress(val);
			        } else {
			        	pb.setVisibility(View.GONE);
			        }
		        } else {
	        		rowDownloadStatus.setVisibility(View.GONE);
	        		rowErrorCount.setVisibility(View.GONE);
		        	pb.setVisibility(View.GONE);
		        }

		        // Expiration
		        // 7 maps to AssetColumns.EAP from projection
		        // 8 maps to AssetColumns.EAD from projection
		        // 9 maps to AssetColumns.END_WINDOW from projection
		        // 10 maps to AssetColumns.FIRST_PLAY_TIME from projection
		        // 11 maps to AssetColumns.COMPLETION_TIME from projection
        		TextView expiration = (TextView) row.findViewById(R.id.txt_expiration);
		        long expirationLong = VirtuosoUtil.getExpiration(cursor.getLong(11),
		        													cursor.getLong(9),
		        													cursor.getLong(10),
		        													cursor.getLong(7),
		        													cursor.getLong(8));
		        String date = makePretty(expirationLong);
		        expiration.setText(date);

				// Update the check-marks for the CAB
				boolean checked = mChecked.containsKey(uuid);
				((InboxRow)row).setChecked(checked);
				row.refreshDrawableState();	
			} catch (Exception e) {
				e.printStackTrace();
			}
	        row.setId(R.id.row_inbox);
		}

		@Override
		public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
			return mInflater.inflate(R.layout.row_inbox_layout, arg2 ,false);
		}

	    private boolean showUp(int count,int position) {
	    	return (count > 0 && position != 0);
	    }
	    
	    private boolean showDown(int count,int position) {
	    	return (count > 0 && position != count - 1);	    	
	    }
	    
	    /**
	     * Displays times like "2 Days from now"
	     * 
	     * @param value timestamp in milliseconds
	     * 
	     * @return Pretty time, never if -1 passeed
	     */
	    private String makePretty(long value) {
	    	if (value == -1) {
	    		return getString(R.string.never);
	    	}
	    	value *= 1000;
			PrettyTime p = new PrettyTime();
			String format = p.format(new Date(value));	
			return format;
	    }
	    
	    //listeners
	    /**
		 * Up listener
		 */
	    private class UpListener implements View.OnClickListener {
			@Override
			public void onClick(View v) {
        		ViewTag viewTag = (ViewTag) v.getTag();				
				if (viewTag.mPosition == 0) return;
				mAssetManager.getQueue().move(viewTag.mId, viewTag.mPosition - 1);
			}
	    }

	    /**
	     * Down listener
	     */
	    private class DownListener implements View.OnClickListener {
	    	@Override
	    	public void onClick(View v) {
        		ViewTag viewTag = (ViewTag) v.getTag();	
    			if (viewTag.mPosition == viewTag.mCursorCount -1) return;
				mAssetManager.getQueue().move(viewTag.mId, viewTag.mPosition + 1);
	    	}
	    }
	    
	    /**
		 * Toggle checked rows during a long press
		 * 
		 * @param id the UUID
		 */
		public void toggleCheck(int id) {
			boolean contains = mChecked.containsKey(id);
			
			if (!contains) {
				mChecked.put(id, Integer.valueOf(1));
			} else {	
				mChecked.remove(id);
			}
			notifyDataSetChanged();
		}
		

	    /**
	     * Set mode: downloaded or downloading
	     */
		public void setAdapterType(int type) {
			mAdapterType = type;
		}
	}

	// --- UI Events

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Log.i(TAG, "onListItemClick");
		if (v.getId() == R.id.row_empty) {
			if (mListener != null)
				mListener.onChange(1);
		}		
	}
	
	/**
	 * A wee command to add a test item
	 */	
	private static int mCount = 0;
	private void handleAddTestItem() {
		VirtuosoUtil.downloadItem(getActivity(),mService, true, "" + UUID.randomUUID().getMostSignificantBits(), Config.SMALL_DOWNLOAD, null, Long.MAX_VALUE,-1,-1,-1, "Test Download: " + mCount , "http://www.freegreatdesign.com/files/images/7/3193-assorted-cool-icon-png-2.jpg");
		++mCount;
	}
	
	/**
	 * Action mode callback
	 */
	private ActionMode.Callback mCabCallback = new ActionMode.Callback(){

		// oncreateActionMode
	    @Override 
	    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
	          MenuInflater inflater = mode.getMenuInflater();
	          inflater.inflate(R.menu.context_inbox, menu);
	          return true;
	        }

	    // onDestroyActionMode
	    @Override
	    public void onDestroyActionMode(ActionMode mode) {
	    	if (mActionMode == mode) {
	    		mActionMode = null;
	    	}
	    	mQueueAdapter.mChecked.clear();
	    	mCompletedAdapter.mChecked.clear();
	    	mExpiredAdapter.mChecked.clear();
	    }

	    // onActionItemClicked
	    @Override
	    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
	        if (item.getItemId() == R.id.menu_delete) {
				Log.i(TAG, "delete");
				try {
					handleOp(DELETE);
				} catch (Exception e) {
					e.printStackTrace();
				}
	        } else if (item.getItemId() == R.id.menu_reset) {
				Log.i(TAG, "reset");
				try {
					handleOp(RESET);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				return false;
			}

	        mode.finish();
	        return true;
	    }

	    // onPrepareActionMode
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			mAdapter.notifyDataSetChanged();
			return false;
		}

		private static final int DELETE = 0;
		private static final int RESET = 1;
		
		/**
		 * Handle Delete
		 */
	    private void handleOp(int op) {
	    	// Downloaded
	    	Set<Integer> idSet = mCompletedAdapter.mChecked.keySet();
	    	if (op == DELETE) {
		    	deleteSet(idSet);	    		
	    	} else if (op == RESET) {
	    		resetSet(idSet);
	    	}
	    	mCompletedAdapter.mChecked.clear();
	    	
	    	// Downloading
	    	idSet = mQueueAdapter.mChecked.keySet();
	    	if (op == DELETE) {
		    	deleteSet(idSet);	    		
	    	} else if (op == RESET) {
	    		resetSet(idSet);
	    	}
	    	mQueueAdapter.mChecked.clear();

	    	// Expired
	    	idSet = mExpiredAdapter.mChecked.keySet();
	    	if (op == DELETE) {
		    	deleteSet(idSet);	    		
	    	} else if (op == RESET) {
	    		resetSet(idSet);
	    	}
	    	mExpiredAdapter.mChecked.clear();
	    }

	    private void deleteSet(Set<Integer> idSet) {
	    	for (Integer id : idSet) {
	    		mAssetManager.delete(id);
	    	}
	    }

	    private void resetSet(Set<Integer> idSet) {
	    	for (Integer id : idSet) {
	    		mAssetManager.getQueue().resetErrors(id);   		
	    	}
	    }
	};
	
	// TODO: Doesn't seem to be showing up?
	private void setFooter() {
		if (mQueueAdapter.getCount() > 0 || mCompletedAdapter.getCount() > 0 || mExpiredAdapter.getCount() > 0) {
			mFooterLayout.setVisibility(View.VISIBLE);
			mFooter.setText(R.string.long_press_cab);
		} else {
			mFooterLayout.setVisibility(View.GONE);			
		}
	}
	
	private void setStatus(Integer status) {	
		if (status == null)
			return;
		
		mVirtuosoStatus = status;
		
		if (mStatus == null)
			return;
		
		// there is no callback for when an item is paused.
		// we may need to update the UI with the download state of a file having changed.
		// we can do this in a couple of ways. One would be to execute the DownloadTask and the other
		// would be to just get the asset from Virtuoso and get the file status as we do for progress.
		// we could just use a notify on the queue uri if we are paused.
		
		setPauseResumeMenuItem();
		mStatus.setText("Engine Status: " + Util.virtuosoStateToString(mVirtuosoStatus));
	}
	
	private void setStatus() {	
		Integer status = null; // unknown status
		try {
			if(mConnectedService != null && mConnectedService.isBound())
				status = mConnectedService.getStatus();
		} catch (ServiceException e) {
			e.printStackTrace();
		}
		setStatus(status);
	}

	/** The pause resume menu item */
	private MenuItem mPauseResumeMenuItem;
	
	/**
	 * Set pause or resume in the menu based on engine state
	 */
	private void setPauseResumeMenuItem() {
		if (mPauseResumeMenuItem == null)
			return;
		
		if(mVirtuosoStatus == EngineStatus.PAUSED) {
			mPauseResumeMenuItem.setTitle(R.string.resume);
		} else if(mVirtuosoStatus != EngineStatus.PAUSED) {
			mPauseResumeMenuItem.setTitle(R.string.pause);
		}
	}		
	
	/**
	 * Pause or resume downloads
	 * 
	 * @param item the menu item
	 */
	private void handlePauseResume(MenuItem item) {
    	if(getString(R.string.pause).equalsIgnoreCase(item.getTitle().toString())){
    		try {
    			if( mConnectedService != null )
    				mConnectedService.pauseDownloads();
			} catch (ServiceException e) {
				e.printStackTrace();
			}
    	}
    	else if(getString(R.string.resume).equalsIgnoreCase(item.getTitle().toString())){
    		try {
    			if( mConnectedService != null )
    				mConnectedService.resumeDownloads();
			} catch (ServiceException e) {
				e.printStackTrace();
			}
    	}		
	}

	/**
	 * Perform a sync with the server
	 */
	private void handleSync() {
		try {
			if( mService != null )
				mService.getBackplane().sync();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
}
