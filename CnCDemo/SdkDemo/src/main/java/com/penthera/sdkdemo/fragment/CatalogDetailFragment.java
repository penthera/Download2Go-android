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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.ocpsoft.pretty.time.PrettyTime;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.penthera.sdkdemo.Extras.CatalogDetail;
import com.penthera.sdkdemo.ImageHelper;
import com.penthera.sdkdemo.R;
import com.penthera.sdkdemo.Util;
import com.penthera.sdkdemo.VirtuosoUtil;
import com.penthera.sdkdemo.catalog.Catalog.CatalogColumns;
import com.penthera.sdkdemo.catalog.CatalogContentProvider;
import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.Common.AssetStatus;
import com.penthera.virtuososdk.client.AncillaryFile;
import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.IAssetManager;
import com.penthera.virtuososdk.client.IIdentifier;
import com.penthera.virtuososdk.client.ISegmentedAsset;
import com.penthera.virtuososdk.client.QueueObserver;
import com.penthera.virtuososdk.client.Virtuoso;
import com.penthera.virtuososdk.client.database.AssetColumns;

import static com.penthera.virtuososdk.utility.logger.CnCLogger.Log;

/**
 * The catalog detail fragment -- Show detailed information about catalog item
 */
public class CatalogDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

	// --- Constants
	/** Log Tag */
	private static final String TAG = CatalogDetailFragment.class.getName();

	// --- Service
	/** The download service from the parent activity */
	private Virtuoso mService;
	
	/** The Asset Manager of the Download Service */
	private IAssetManager mAssetManager;

	/** Progress */
	private MyQueueObserver mQueueObserver = new MyQueueObserver();

	// --- Loaders
	/** The loader ID */
	private static final int LOADER_ID = CatalogDetailFragment.class.hashCode();
	/** Image Loader */
	private ImageLoader mImageLoader;

	// --- Model Data
	/** Model for the layout */
	private Cursor mCursor;
	/** The remote catalog ID */
	private String mId;

	// --- Layout
	/** The root layout */
	private View mLayout;
	/** true UI is built */
	private boolean mBuilt = false;
	/** Download (or delete) items based on file state */
	private Button mDownloadButton;
	/** Pause button */
	private Button mPauseButton;
	/** Layout containing pause button */
	private View mPauseLayout;

	private Button refreshAdsBtn;

	/** Asset corresponding to this catalog entry */
	private IAsset mAsset;
	
	// --- Constructors

	public static CatalogDetailFragment newInstance(Virtuoso service) {
		CatalogDetailFragment cdf = new CatalogDetailFragment();
		cdf.mService = service;
		cdf.mAssetManager = cdf.mService.getAssetManager();
		return cdf;
	}	
	
	public void setService(Virtuoso service) {
		mService = service;
		mAssetManager = mService.getAssetManager();
	}
	
	// --- Life Cycle methods
	
	// onCreate
	@Override
	public void onCreate(Bundle b) {
		super.onCreate(b);

		// Arguments
		Bundle args = getArguments();		
		mId = args.getString(CatalogDetail.EXTRA_ID);
		mAsset = VirtuosoUtil.getVirtuosoAsset(mAssetManager, mId);		

		
		// Images
		ImageLoaderConfiguration config = ImageHelper.getImgaeLoaderConfiguration(getActivity());
		mImageLoader = ImageLoader.getInstance();
		mImageLoader.init(config);		
	}

	// onCreateView
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		Log.i(TAG, "onCreateView()");
		mLayout = inflater.inflate(R.layout.fragment_catalog_detail, null);		
		return mLayout;
	}
	
	// onActivityCreated
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		LoaderManager lm = getActivity().getSupportLoaderManager();
		lm.initLoader(LOADER_ID, null, CatalogDetailFragment.this);
	}

	// onResume
	public void onResume() {
		super.onResume();
		mService.addObserver(mQueueObserver);	
		//already loaded can safely build
		if(mBuilt){
			//refresh the asset in case it was added
			mAsset = VirtuosoUtil.getVirtuosoAsset(mAssetManager, mId);
			update();
		}

	}
		
	// onPause
	public void onPause() {
		super.onPause();
		mService.removeObserver(mQueueObserver);
	}

	// onDestory
	public void onDestroy() {
		super.onDestroy();
	}
	
	// --- Cursor Loader 

	// onCreateLoader
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
		Log.i(TAG, "onCreateLoader");
		CursorLoader cursorLoader = null;
		if (id == LOADER_ID) {
			cursorLoader = new CursorLoader(getActivity(),	
					CatalogContentProvider.CATALOG_URI,
					null, 
					CatalogColumns._ID + "=?",
					new String[] {mId},
					null);						
		}		
		return cursorLoader;		    
	}
	
	// onLoadFinished
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		Log.i(TAG, "onCreateLoader");
		int id = loader.getId();	

		if (id == LOADER_ID) {			
			mCursor = cursor;		

			// Create UI
			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();				
				build();
			// This should never happen
			} else {
				Util.showToast(getActivity(), getString(R.string.cannot_find_item), Toast.LENGTH_SHORT);
				getActivity().finish();
			}
		} else {
			Log.e(TAG, "erorr: " + id);
		}
	}
			
	// onLoaderReset
	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		Log.d(TAG, "onLoaderReset");
	}	
		
	// --- UI methods
	private void build() {

		if(mBuilt){
			return;
		}
		mBuilt = true;
		// Background image
		mCursor.moveToFirst();

		if (mCursor == null || mCursor.getCount() == 0)
			return;		
		mCursor.moveToFirst();
		
		// Load translucent background image
		String imageUrl = mCursor.getString(mCursor.getColumnIndex(CatalogColumns.IMAGE));		
		mImageLoader.loadImage(getActivity(), imageUrl, new MyImageLoaderListener(mLayout));
				
		// Watch now
		Button watchNow = (Button) mLayout.findViewById(R.id.btn_watch);
		watchNow.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mCursor.moveToFirst();
				VirtuosoUtil.watchItem(getActivity(), mAssetManager, mCursor);
			}
		});

		// Download: {Download, Delete}
		mDownloadButton = (Button) mLayout.findViewById(R.id.btn_download);
		mDownloadButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {				
				// Button State
				Button b = (Button) v;
				String bText = b.getText().toString();
				
				// Downloaded
				if (bText.equals(getString(R.string.download))) {
					handleDownload();
				} else if (bText.equals(getString(R.string.delete))) {
					handleDelete();
				} else {
					Log.w(TAG, "Bad button");
				}
			}
		});

		mPauseLayout = mLayout.findViewById(R.id.pause_button_row);
		mPauseButton = mLayout.findViewById(R.id.btn_pause_item);
		mPauseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                Button b = (Button) v;
                String bText = b.getText().toString();

                if (bText.equals(getString(R.string.pause))) {
                    handlePause();
                } else if (bText.equals(getString(R.string.resume))) {
                    handleResume();
                } else {
                    Log.w(TAG, "Bad button");
                }

                updatePauseButton();
			}
		});

		refreshAdsBtn = mLayout.findViewById(R.id.btn_refresh_ads);
		refreshAdsBtn.setOnClickListener(v -> {

			if(mAsset != null)
				mAssetManager.getAdManager().refreshAdsForAsset(mAsset);
		});
		update();
	}
	
	private void updateDetails(){
		
		// Title
		String titleStr = mCursor.getString(mCursor.getColumnIndex(CatalogColumns.TITLE));		
		TextView title = (TextView) mLayout.findViewById(R.id.txt_title);
		title.setText(titleStr);
		
		// Parental Rating
		String contentRatingStr = mCursor.getString(mCursor.getColumnIndex(CatalogColumns.CONTENT_RATING));	
		if (!TextUtils.isEmpty(contentRatingStr)) {
			TextView contentRating = (TextView) mLayout.findViewById(R.id.txt_parental_rating);
			contentRating.setText(contentRatingStr);
		}

		// Size
		long sizeLong = mCursor.getLong(mCursor.getColumnIndex(CatalogColumns.CONTENT_SIZE));		
		TextView size = (TextView) mLayout.findViewById(R.id.txt_size);
		if (sizeLong > 0) {
			size.setVisibility(View.VISIBLE);
			size.setText(Util.readableFileSize(sizeLong));
		} else {			
			size.setVisibility(View.GONE);
		}
		
		// Expiration
		long expiryLong = getExpiry();	
		View expiryLayout = mLayout.findViewById(R.id.lyt_expiry);
		expiryLayout.setVisibility(View.GONE);
		expiryLayout.setVisibility(View.VISIBLE);
		String expiryStr = makePretty(expiryLong,getString(R.string.never));
		TextView expiry = (TextView) mLayout.findViewById(R.id.txt_expiry);
		expiry.setText(expiryStr);

		// Available
		long availableLong = getAvailable();	
		View availableLayout = mLayout.findViewById(R.id.lyt_available);
		availableLayout.setVisibility(View.VISIBLE);
		String availableStr = makePretty(availableLong,getString(R.string.always));
		TextView available = (TextView) mLayout.findViewById(R.id.txt_available);
		available.setText(availableStr);
		
		// Duration
		int durationInt = mCursor.getInt(mCursor.getColumnIndex(CatalogColumns.DURATION));		
		TextView duration = (TextView) mLayout.findViewById(R.id.txt_duration);
		if (durationInt > 0) {
			String durationString = Util.getDurationString(durationInt);
			duration.setVisibility(View.VISIBLE);
			duration.setText(durationString);
		} else {			
			duration.setVisibility(View.GONE);
		}
		
		// Description
		String descriptionStr = mCursor.getString(mCursor.getColumnIndex(CatalogColumns.DESC));		
		TextView description = (TextView) mLayout.findViewById(R.id.txt_description);
		description.setText(descriptionStr);
		
		updateDownloadButtonText();

		updateAdRefreshBtn();

		showAncillaryFiles();

		updatePauseButton();

	}

	private void showAncillaryFiles() {


		if (mAsset != null){

			mLayout.findViewById(R.id.no_ancillary_files_lbl).setVisibility(View.VISIBLE);

			if (mAsset.getType() == Common.AssetIdentifierType.SEGMENTED_ASSET_IDENTIFIER) {//segmented files can have ancillaries
				ISegmentedAsset file = (ISegmentedAsset) mAsset;

				List<AncillaryFile> ancillaries = file.getAncillaryFiles(getContext());

				if (ancillaries != null && ancillaries.size() > 0) {

					mLayout.findViewById(R.id.no_ancillary_files).setVisibility(View.GONE);
					mLayout.findViewById(R.id.ancillary_files).setVisibility(View.VISIBLE);

					ListView list = mLayout.findViewById(R.id.ancillary_files);

					list.setAdapter(new AncillaryAdapter(getContext(), ancillaries));
				} else {
					mLayout.findViewById(R.id.ancillary_files).setVisibility(View.GONE);
					mLayout.findViewById(R.id.no_ancillary_files).setVisibility(View.VISIBLE);
				}


			} else {
				mLayout.findViewById(R.id.ancillary_files).setVisibility(View.GONE);
				mLayout.findViewById(R.id.no_ancillary_files).setVisibility(View.VISIBLE);
			}
		}else {
			mLayout.findViewById(R.id.no_ancillary_files_lbl).setVisibility(View.GONE);
			mLayout.findViewById(R.id.ancillary_files).setVisibility(View.GONE);
			mLayout.findViewById(R.id.no_ancillary_files).setVisibility(View.GONE);
		}


	}

	private static class AncillaryAdapter extends BaseAdapter{

		private Context context;
		private List<AncillaryFile> files;


		public AncillaryAdapter(Context context, List<AncillaryFile> files){
			this.context = context;
			this.files = files;
		}

		@Override
		public int getCount() {
			return files != null ? files.size() : 0;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView == null){
				convertView = LayoutInflater.from(context).inflate(R.layout.ancillary_list_item, parent, false);
			}

			AncillaryFile file = files.get(position);

			((TextView)convertView.findViewById(R.id.ancillary_desc)).setText(file.description);
			((TextView)convertView.findViewById(R.id.ancillary_src)).setText(file.srcUrl.toString());
			((TextView)convertView.findViewById(R.id.ancillary_path)).setText(file.localPath);
			((TextView)convertView.findViewById(R.id.ancillary_tags)).setText(file.tags != null && file.tags.length > 0 ? TextUtils.join(",", file.tags) : "NONE");

			if(file.srcUrl.toString().endsWith(".jpg")){
				File img = new File(file.localPath);

				((ImageView)convertView.findViewById(R.id.ancillary_img)).setImageBitmap(BitmapFactory.decodeFile(img.getAbsolutePath()));
			}
			else{
				convertView.findViewById(R.id.ancillary_img).setVisibility(View.GONE);
			}

			return convertView;
		}
	}

	private void update(){
		updateDetails();
		updateItemStatus(mAsset,true);
	}
	
	/**
	 * @return expiration time in milliseconds
	 */
	private long getExpiry() {
		// Use Catalog Value
		long expiry = mCursor.getLong(mCursor.getColumnIndex(CatalogColumns.CATALOG_EXPIRY));
		// Override with SDK value if exists
		if (mAsset != null) {
			expiry = VirtuosoUtil.getExpiration(mAsset);
		}
		if (expiry > 0)
			expiry *= 1000;

		return expiry;
	}
	
	/**
     * Displays times like "2 Days from now"
     * 
     * @param value timestamp in milliseconds
     * 
     * @return Pretty time, never if -1 passeed
     */
    private String makePretty(long value,String fallback) {
    	if (value < 0) {
    		return fallback;
    	}
    	
		PrettyTime p = new PrettyTime();
		String format = p.format(new Date(value));	
		return format;
    }

	/**
	 * @return expiration time in milliseconds
	 */
	private long getAvailable() {
		// Use Catalog Value
		long available = mCursor.getLong(mCursor.getColumnIndex(CatalogColumns.AVAILABILITY_START));	

		// Override with SDK value if exists
		if (mAsset != null) {
			available = mAsset.getStartWindow();
		}
		if (available > 0)
			available *= 1000;
		return available;
	}

	// --- Event Handlers

	/**
	 * Download handler
	 */
	private void handleDownload() {		
		ContentValues cv = new ContentValues();
		mCursor.moveToFirst();
		DatabaseUtils.cursorRowToContentValues(mCursor, cv);
		if(cv.size() == 0){
			return;
		}
		// Here we are going to determine the type of download.
		// This is being done in order to show the different ways an HLS file can be generated.
		final int mediaType = cv.getAsInteger(CatalogColumns.MEDIA_TYPE);
		switch(mediaType){
		case 2: //a normal mp4
		case 4: //an hls that will be constructed by adding the fragments.
			new AddFileTask(cv).execute();	
			break;
		case 3: //shows how an HLS file is passed to Virtuoso for it to choose the best bit rate
		case 5: // shows how an HLS manifest is parsed and a play list chosen
		case 6: //dash
			//for these cases we do not use an async task as the processing is async
			VirtuosoUtil.downloadItem(getActivity(), mService, cv);
			break;
		}	
	}

	/**
	 * Delete Handler
	 */
	private void handleDelete() {
		showDeleteDialog();
	}

	/**
	 * Pause Item Handler (pauses the individual item)
	 */
	private void handlePause() {
		if (mAsset != null && mAsset.getDownloadStatus() != AssetStatus.DOWNLOAD_PAUSED) {
            mAssetManager.pauseDownload(mAsset);
        }
	}

	private void handleResume() {
        if (mAsset != null && mAsset.getDownloadStatus() == AssetStatus.DOWNLOAD_PAUSED) {
            mAssetManager.resumeDownload(mAsset);
        }
	}
	
	/**
	 * Show delete confirmation dialog
	 */
	private void showDeleteDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setTitle(getString(R.string.are_you_sure));
    	builder.setMessage(getString(R.string.delete_item));    	
    	builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) {
				doDelete();
				dialog.dismiss();
				dialog = null;
			}
    	});    	
    	builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) {
				dialog.dismiss();
				dialog = null;
			}
    	});
    	AlertDialog ad = builder.create();
    	ad.show();		
	}
	
	/**
	 * Delete item
	 */
	private void doDelete() {
		String assetId = mCursor.getString(mCursor.getColumnIndex(CatalogColumns._ID));				
		IAsset asset = VirtuosoUtil.getVirtuosoAsset(mAssetManager, assetId);
		mAssetManager.delete(asset);
		getActivity().finish();		
	}

	/**
	 * Apply effects to background image
	 */
	private static class MyImageLoaderListener implements ImageLoadingListener {
		View mLayout = null;
		
		MyImageLoaderListener(View layout) {
			mLayout = layout;
		}
		
		@Override
		public void onLoadingStarted() {
		}

		@Override
		public void onLoadingFailed(FailReason failReason) {
		}

		@SuppressWarnings("deprecation")
		@Override
		public void onLoadingComplete(Bitmap loadedImage) {
			mLayout.setBackgroundDrawable(new BitmapDrawable(ImageHelper.adjustOpacity(loadedImage, 80)));
		}

		@Override
		public void onLoadingCancelled() {
		}		
	}
		
	// --- Download Observer [ Progress]
	
	/**
	 * Download Observer
	 * 
	 * . Updates are called a couple times a second, 
	 * . Engine always send two updates for each fractionComplete 
	 * . Engine sometimes sends 4 or more updates for each fractionComplete
	 *   . Works off timer so can send X*2 progress message for same progress
	 *
	 */
	class MyQueueObserver extends QueueObserver {
		/** Engine sending two progress updates for each fractionComplete */
		int mPreviousProgress = -1;

		@Override
		public void engineStartedDownloadingAsset(IIdentifier aFile) {
			Log.i(TAG, "engineStartedDownloadingAsset");
			mPreviousProgress = -1;
			updateItem(aFile,true);
		}

		@Override
		public void enginePerformedProgressUpdateDuringDownload(final IIdentifier aFile) {
			updateItem(aFile,false);
		}

		@Override
		public void engineCompletedDownloadingAsset(IIdentifier aFile) {
			Log.i(TAG, "engineCompletedDownloadingAsset");
			updateItem(aFile,true);
		}

		@Override
		public void engineEncounteredErrorDownloadingAsset(IIdentifier aFile) {
			Log.w(TAG, "engineEncounteredErrorDownloadingAsset");
			updateItem(aFile,true);
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Util.showToast(getActivity(), "engineEncounteredErrorDownloadingAsset", Toast.LENGTH_SHORT);					
				}				
			});
			
		}

		@Override
		public void engineUpdatedQueue() {
			Log.i(TAG, "engineUpdatedQueue");
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					updateDownloadButtonText();
					updatePauseButton();
					updateAdRefreshBtn();

				}
			});
		}
		
		// --- Helpers
		
		private void updateItem(final IIdentifier aFile,final boolean forceUpdate) {

			final IAsset asset = (IAsset) aFile;
			final String assetId = asset.getAssetId();				
			
			// Progress is for catalog item
			if (!TextUtils.isEmpty(assetId) && assetId.equals(mId)) {	
				//update our asset status
				getActivity().runOnUiThread(new Runnable() {				
					@Override
					public void run() {			
						updateItemStatus(asset,forceUpdate);
					}
				});				
						
			}	
		}			
	};	
	
	private void updateItemStatus(IAsset asset, boolean forceUpdate){
		if(asset != null && asset.getAssetId().equals(mId)){
			
			//update our asset reference
			mAsset = asset;

			final int progress = (int) (mAsset.getFractionComplete() * 100.0);	
			// Not a repeated progress -- Keep context switches minimal due to frequency of messages, unless forced
			if(forceUpdate || progress != mQueueObserver.mPreviousProgress){
				String assetStatus = "";
				int fds = mAsset.getDownloadStatus();
				String value;
				boolean checkRetryingState = false;
				switch(fds){
				
				case AssetStatus.DOWNLOADING:
				case AssetStatus.EARLY_DOWNLOADING:
					assetStatus = getString(R.string.status_downloading);
					value = "downloading";
					break;
					
				case AssetStatus.DOWNLOAD_COMPLETE:
					assetStatus = getString(R.string.status_downloaded);
					value = "complete";
					break;
					
				case AssetStatus.EXPIRED:
					assetStatus = getString(R.string.status_expired);
					value = "expired";
					break;

				case AssetStatus.DOWNLOAD_DENIED_ASSET:
					assetStatus = "Queued";
					value = "DENIED : MAD";
					checkRetryingState = true;
					break;

				case AssetStatus.DOWNLOAD_DENIED_ACCOUNT :
					assetStatus = "Queued";
					value = "DENIED : MDA";
					checkRetryingState = true;
					break;

				case AssetStatus.DOWNLOAD_DENIED_EXTERNAL_POLICY:
					assetStatus = "Queued";
					value = "DENIED : EXT";
					checkRetryingState = true;
					break;

				case AssetStatus.DOWNLOAD_DENIED_MAX_DEVICE_DOWNLOADS:
					assetStatus = "Queued";
					value = "DENIED :MPD";
					checkRetryingState = true;
					break;

				case AssetStatus.DOWNLOAD_BLOCKED_AWAITING_PERMISSION:
					assetStatus = "Queued";
					value = "AWAITING PERMISSION";
					break;

				case AssetStatus.DOWNLOAD_DENIED_COPIES:
					assetStatus = "Queued";
					value = "DENIED : COPIES";
					checkRetryingState = true;
					break;

				default:
					assetStatus = getString(R.string.status_pending);
					checkRetryingState = true;
					value = "pending";
				}
				TextView tv = mLayout.findViewById(R.id.txt_assetstatus);
				tv.setVisibility(View.VISIBLE);
				tv.setText(String.format(Locale.US,getString(R.string.asset_status), assetStatus,mAsset.getErrorCount(),value));
				TextView retryTv = mLayout.findViewById(R.id.txt_retrystatus);
				boolean showRetryState = false;
				if (checkRetryingState) {
					if (mAsset.maximumRetriesExceeded()) {
						retryTv.setText(R.string.retries_exceeded);
						showRetryState = true;
					}
				}
				retryTv.setVisibility(showRetryState ? View.VISIBLE : View.GONE);
				updateProgressBar(progress);
			}
		}
	}
	
	/**
	 * Updates the progress bar for downloading item
	 * @param progress the amount of progress 0 to 100
	 */	
	private void updateProgressBar(int progress) {
		mQueueObserver.mPreviousProgress = progress;
		// Tiny Progress
		if (progress == 0) {
			progress = 1;
		}

		// Progress Bar
		ProgressBar pb = (ProgressBar) mLayout.findViewById(R.id.prg);
		if (progress > 0 && progress < 100) {					
			pb.setProgress(progress);
			pb.setVisibility(View.VISIBLE);
		} else {
			pb.setVisibility(View.GONE);
		}			
	}	
	
	/**
	 * true, item is in Q
	 * @param assetId
	 * @return true, item is in Q
	 */
	private boolean isQ(String assetId) {
		if (mAssetManager == null)
			return false;
		Cursor c = null;
		try{
			c = mAssetManager.getQueue().getCursor(new String[]{AssetColumns._ID}, AssetColumns.ASSET_ID+"=?", new String[]{assetId});
			return c != null && c.getCount() > 0;
		}
		finally {
			if(c != null && !c.isClosed())
				c.close();
			c = null;
		}
	}
	
	/**
	 * true, the item has been downloaded
	 * @param assetId
	 * @return true, the item has been downloaded
	 */
	private boolean isDownloaded(String assetId) {
		if (mAssetManager == null)
			return false;
		Cursor c = null;
		try{
			c = mAssetManager.getDownloaded().getCursor(new String[]{AssetColumns._ID}, AssetColumns.ASSET_ID+"=?", new String[]{assetId});
			return c != null && c.getCount() > 0;
		}
		finally {
			if(c != null && !c.isClosed())
				c.close();
		}
	}
	
	/**
	 * true, the item has been downloaded
	 * @param assetId
	 * @return true, the item has been downloaded
	 */
	private boolean isExpired(String assetId) {
		if (mAssetManager == null)
			return false;
		Cursor c = null;
		try{
			c = mAssetManager.getExpired().getCursor(new String[]{AssetColumns._ID}, AssetColumns.ASSET_ID+"=?", new String[]{assetId});
			return c != null && c.getCount() > 0;
		}
		finally {
			if(c != null && !c.isClosed())
				c.close();
		}
	}

	/**
	 * Test if the item is currently in the paused state
	 * @param assetId the asset id to check
	 * @return true if the item is paused.
	 */
	private boolean isPaused(String assetId) {
		if (mAssetManager == null)
			return false;
		Cursor c = null;
		try{
			c = mAssetManager.getQueue().getCursor(new String[]{AssetColumns._ID, AssetColumns.DOWNLOAD_STATUS}, AssetColumns.ASSET_ID+"=?", new String[]{assetId});
			if (c != null && c.getCount() > 0 && c.moveToFirst()){
                return c.getInt(1) == AssetStatus.DOWNLOAD_PAUSED;
            }
			return false;
		}
		finally {
			if(c != null && !c.isClosed())
				c.close();
		}
	}

	private void updateAdRefreshBtn(){
		String assetId = mCursor.getString(mCursor.getColumnIndex(CatalogColumns._ID));

		mLayout.findViewById(R.id.row_btn2).setVisibility(isDownloaded(assetId) ? View.VISIBLE : View.GONE);

	}

	/**
	 * Update download button to download or delete based on file state
	 */
	private void updateDownloadButtonText() {
		String assetId = mCursor.getString(mCursor.getColumnIndex(CatalogColumns._ID));				
		boolean known = isQ(assetId) || isDownloaded(assetId) || isExpired(assetId);

		if (!known) {					
			mDownloadButton.setText(R.string.download);			
		} else {
			mDownloadButton.setText(R.string.delete);					
		}		
	}

	private void updatePauseButton() {
		String assetId = mCursor.getString(mCursor.getColumnIndex(CatalogColumns._ID));
		boolean queued = isQ(assetId);
		if (queued) {
            if (isPaused(assetId)) {
                mPauseButton.setText(R.string.resume);
            } else {
                mPauseButton.setText(R.string.pause);
            }

			mPauseLayout.setVisibility(View.VISIBLE);
		} else {
			mPauseLayout.setVisibility(View.GONE);
		}
	}

	/**
	 * Task for adding item to the download Q
	 */
	private class AddFileTask extends AsyncTask<Void, Void, Void> {
		private ProgressDialog mProgressDialog;
		private ContentValues mContentValues;
		
		public AddFileTask(ContentValues cv) {
			mContentValues = cv;
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				VirtuosoUtil.downloadItem(getActivity(), mService, mContentValues);
			} catch (Exception e) {
				Log.e(TAG,"Could not add file", e);
				e.printStackTrace();
			}
			return null;
		}      		

		@Override
		protected void onPreExecute() {
			mProgressDialog = ProgressDialog.show(getActivity(), "Adding Item", "Adding Item...");
		}

		@Override
		protected void onProgressUpdate(Void... values) {
		}

		@Override
		protected void onPostExecute(Void result) {
			if (mProgressDialog != null) {
				try {
					mProgressDialog.dismiss();
				} catch (Exception e){}
			}
		}
	}

}
