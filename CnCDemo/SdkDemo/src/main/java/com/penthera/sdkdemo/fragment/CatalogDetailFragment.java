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

import java.util.Date;
import junit.framework.Assert;

import org.ocpsoft.pretty.time.PrettyTime;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v4.app.Fragment;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.penthera.sdkdemo.Extras.CatalogDetail;
import com.penthera.sdkdemo.ImageHelper;
import com.penthera.sdkdemo.R;
import com.penthera.sdkdemo.Util;
import com.penthera.sdkdemo.VirtuosoUtil;
import com.penthera.sdkdemo.catalog.Catalog;
import com.penthera.sdkdemo.catalog.Catalog.CatalogColumns;
import com.penthera.sdkdemo.catalog.CatalogContentProvider;
import com.penthera.virtuososdk.Common.AssetStatus;
import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.IAssetManager;
import com.penthera.virtuososdk.client.IIdentifier;
import com.penthera.virtuososdk.client.QueueObserver;
import com.penthera.virtuososdk.client.Virtuoso;
import com.penthera.virtuososdk.client.database.AssetColumns;

import static com.penthera.sdkdemo.catalog.Catalog.MediaType.MP4;
import static com.penthera.sdkdemo.catalog.CatalogContentProvider.CATALOG_URI;

/**
 * The catalog detail fragment -- Show detailed information about catalog item
 */
public class CatalogDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

	// --- Constants
	/** Log Tag */
	private static final String TAG = CatalogDetailFragment.class.getName();

	/** Custom genre, used to check if this item is editable */
	private static final String CUSTOM_GENRE = "Custom";

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
	
	/** Asset corresponding to this catalog entry */
	private IAsset mAsset;

	/** Editable */
	private boolean mEditable = false;

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
					CATALOG_URI,
					null, 
					CatalogColumns.ASSET_ID + "=?",
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
					Assert.assertEquals(false, "bad button");
				}
			}
		});

        // Editable
        String genre = mCursor.getString(mCursor.getColumnIndex(CatalogColumns.GENRE));
        mEditable = genre.contentEquals(CUSTOM_GENRE);

        Button editButton = mLayout.findViewById(R.id.btn_detail_edit);
        editButton.setVisibility(mEditable ? View.VISIBLE : View.GONE);
        if (mEditable) {
            editButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    enterEditMode(true);
                }
            });
            Button saveButton = mLayout.findViewById(R.id.btn_save_details);
            saveButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveDetails();
                }
            });
            String titleStr = mCursor.getString(mCursor.getColumnIndex(CatalogColumns.TITLE));
            if (TextUtils.isEmpty(titleStr) || titleStr.equalsIgnoreCase("Custom Title")){
                enterEditMode(true);
            }
        } else {
            enterEditMode(false);
        }

		update();
        mBuilt = true;
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

		if (mEditable) {
		    EditText titleEdit = mLayout.findViewById(R.id.edt_title);
		    titleEdit.setText(titleStr);

            EditText assetIdEdit = mLayout.findViewById(R.id.edt_asset_id);
            String assetIdText = mCursor.getString(mCursor.getColumnIndex(CatalogColumns.ASSET_ID));
            assetIdEdit.setText(assetIdText);

            EditText manifestEdit = mLayout.findViewById(R.id.edt_manifest_url);
            String manifestText = mCursor.getString(mCursor.getColumnIndex(CatalogColumns.CONTENT_URL));
            manifestEdit.setText(manifestText);

            EditText descriptionEdit = mLayout.findViewById(R.id.edt_description);
            descriptionEdit.setText(descriptionStr);

            Spinner mediaTypeDropdown = mLayout.findViewById(R.id.spnr_type);

            Resources res = getContext().getResources();
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_dropdown_item,res.getStringArray(R.array.media_types));
            mediaTypeDropdown.setAdapter(adapter);

            int mediaTypeId = mCursor.getInt(mCursor.getColumnIndex(CatalogColumns.MEDIA_TYPE));
            int pos;
            switch(mediaTypeId){
                case Catalog.MediaType.HSS:
                    pos = 0;
                    break;
                case Catalog.MediaType.HLS:
                    pos = 1; // HLS
                    break;
                case Catalog.MediaType.MPD:
                    pos = 2; // MPD
                    break;
                default:
                    pos = 3;  // MP4
            }
            mediaTypeDropdown.setSelection(pos);
        }

		updateDownloadButtonText();
	}
	
	private void update(){
		updateDetails();
		updateItemStatus(mAsset,true);
	}

	private void enterEditMode(boolean enter) {
	    mLayout.findViewById(R.id.detail_edit_container).setVisibility(enter ? View.VISIBLE : View.GONE);
	    mLayout.findViewById(R.id.details_container).setVisibility(enter ? View.GONE : View.VISIBLE);
	    mLayout.findViewById(R.id.btn_watch).setEnabled(!enter);
	    mLayout.findViewById(R.id.btn_download).setEnabled(!enter);
    }

    private void saveDetails(){
        EditText titleEdit = mLayout.findViewById(R.id.edt_title);
        String titleStr = titleEdit.getText().toString();

        EditText assetIdEdit = mLayout.findViewById(R.id.edt_asset_id);
        String assetIdStr = assetIdEdit.getText().toString();

        EditText manifestEdit = mLayout.findViewById(R.id.edt_manifest_url);
        String manifestStr = manifestEdit.getText().toString();

        EditText descriptionEdit = mLayout.findViewById(R.id.edt_description);
        String descriptionStr = descriptionEdit.getText().toString();

        Spinner mediaTypeDropdown = mLayout.findViewById(R.id.spnr_type);
        int pos = mediaTypeDropdown.getSelectedItemPosition();
        int mediaTypeId;
        String mime;
        switch(pos){
            case 0:
                mediaTypeId = Catalog.MediaType.HSS; // HSS
                mime = "";
                break;
            case 1:
                mediaTypeId = Catalog.MediaType.HLS; // HLS
                mime = "application/x-mpegurl";
                break;
            case 2:
                mediaTypeId = Catalog.MediaType.MPD; // MPD
                mime = "application/octet-stream";
                break;
            default:
                mediaTypeId = Catalog.MediaType.MP4;  // MP4
                mime = "video/mp4";
        }

        if (TextUtils.isEmpty(titleStr) || TextUtils.isEmpty(assetIdStr)
                || TextUtils.isEmpty(manifestStr) || TextUtils.isEmpty(descriptionStr)) {

            Toast.makeText(getContext(), R.string.missing_field,Toast.LENGTH_LONG).show();
            return;
        }

        boolean valid = false;
        if (manifestStr.startsWith("http") && manifestStr.contains("://")) {
            try {
                Uri test = Uri.parse(manifestStr);
                valid = (test != null);
            } catch (Exception e) {}
        }

        if (!valid) {
            Toast.makeText(getContext(), R.string.invalid_url, Toast.LENGTH_LONG).show();
            return;
        }

        // Save to db
        ContentValues cv = new ContentValues();
        cv.put(CatalogColumns.TITLE, titleStr);
        cv.put(CatalogColumns.ASSET_ID, assetIdStr);
        cv.put(CatalogColumns.CONTENT_URL, manifestStr);
        cv.put(CatalogColumns.DESC, descriptionStr);
        cv.put(CatalogColumns.MEDIA_TYPE, mediaTypeId);
        cv.put(CatalogColumns.MIME, mime);

        int id = mCursor.getInt(mCursor.getColumnIndex(CatalogColumns._ID));
        Uri updateUri = Uri.parse("content://" + CatalogContentProvider.AUTHORITY + "/catalog/");
        getContext().getContentResolver().update(updateUri, cv, CatalogColumns._ID + " = " + id, null);

        // Update cursor
        getLoaderManager().restartLoader(LOADER_ID, null, this);

        enterEditMode(false);
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
		if (expiry != -1) 
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
    	if (value == -1) {
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
		if (available != -1) 
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
		case Catalog.MediaType.MP4: //a normal mp4
		case Catalog.MediaType.HLS_ADD_FRAGMENT: //an hls that will be constructed by adding the fragments.
			new AddFileTask(cv).execute();	
			break;
		case Catalog.MediaType.HLS: //shows how an HLS file is passed to Virtuoso for it to choose the best bit rate
		case Catalog.MediaType.HLS_MANIFEST: // shows how an HLS manifest is parsed and a play list chosen
		case Catalog.MediaType.MPD: //dash
		case Catalog.MediaType.HSS:
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
		String assetId = mCursor.getString(mCursor.getColumnIndex(CatalogColumns.ASSET_ID));
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
				switch(fds){
				
				case AssetStatus.DOWNLOADING:
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
					break;

				case AssetStatus.DOWNLOAD_DENIED_ACCOUNT :
					assetStatus = "Queued";
					value = "DENIED : MDA";
					break;

				case AssetStatus.DOWNLOAD_DENIED_EXTERNAL_POLICY:
					assetStatus = "Queued";
					value = "DENIED : EXT";
					break;

				case AssetStatus.DOWNLOAD_DENIED_MAX_DEVICE_DOWNLOADS:
					assetStatus = "Queued";
					value = "DENIED :MPD";
					break;

				default:
					assetStatus = getString(R.string.status_pending);
					value = "pending";
				}
				TextView tv = (TextView)mLayout.findViewById(R.id.txt_assetstatus);
				tv.setVisibility(View.VISIBLE);
				tv.setText(String.format(getString(R.string.asset_status), assetStatus,mAsset.getErrorCount(),value));
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
			c = null;
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
			c = null;
		}
	}

	/**
	 * Update download button to download or delete based on file state
	 */
	private void updateDownloadButtonText() {
		String assetId = mCursor.getString(mCursor.getColumnIndex(CatalogColumns.ASSET_ID));
		boolean known = isQ(assetId) || isDownloaded(assetId) || isExpired(assetId);

		if (!known) {					
			mDownloadButton.setText(R.string.download);			
		} else {
			mDownloadButton.setText(R.string.delete);					
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
