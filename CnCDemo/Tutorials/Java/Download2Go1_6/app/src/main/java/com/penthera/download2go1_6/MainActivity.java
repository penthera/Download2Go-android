/*
    Created by Penthera on 10/10/19.
    Copyright © 2019 penthera. All rights reserved.

    This source file contains a very basic example showing how to use the Penthera Download2Go SDK.
    Pay close attention to code comments marked IMPORTANT
*/
package com.penthera.download2go1_6;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.client.IAssetManager;
import com.penthera.virtuososdk.client.ISegmentedAsset;
import com.penthera.virtuososdk.client.ISegmentedAssetFromParserObserver;
import com.penthera.virtuososdk.client.Virtuoso;
import com.penthera.virtuososdk.client.builders.HLSAssetBuilder;
import com.penthera.virtuososdk.client.builders.MPDAssetBuilder;
import com.penthera.virtuososdk.client.database.AssetColumns;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import static android.content.Intent.ACTION_VIEW;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // DEMO Server details
    private static final String BACKPLANE_URL = "https://demo.penthera.com/";
    private static final String BACKPLANE_PUBLIC_KEY = ;
    private static final String BACKPLANE_PRIVATE_KEY = ;

    // This is the first test asset which will be downloaded
    // Important: Asset ID should be unique across your video catalog
    private static final String ASSET_ID_1 = "TEST_ASSET_ID_1";
    private String ASSET_TITLE_1;
    private static final String ASSET_URL_1 = "https://storage.googleapis.com/wvmedia/clear/h264/tears/tears_sd.mpd";

    // This is the first test asset which will be downloaded
    // Important: Asset ID should be unique across your video catalog
    private static final String ASSET_ID_2 = "TEST_ASSET_ID_2";
    private String ASSET_TITLE_2;
    private static final String ASSET_URL_2 = "https://virtuoso-demo-content.s3.amazonaws.com/Steve/steve.m3u8";

    // This is the first test asset which will be downloaded
    // Important: Asset ID should be unique across your video catalog
    private static final String ASSET_ID_3 = "TEST_ASSET_ID_3";
    private String ASSET_TITLE_3;
    private static final String ASSET_URL_3 = "https://virtuoso-demo-content.s3.amazonaws.com/College/college.m3u8";

    // We only use one loader here, so this is of little significance.
    private int LOADER_ID = 1;

    // Projection for asset cursor
    static final String[] PROJECTION = new String[]{
            AssetColumns._ID
            ,AssetColumns.UUID
            ,AssetColumns.ASSET_ID
            ,AssetColumns.DOWNLOAD_STATUS
            ,AssetColumns.METADATA
            ,AssetColumns.CURRENT_SIZE
            ,AssetColumns.EXPECTED_SIZE
            ,AssetColumns.DURATION_SECONDS
            ,AssetColumns.FRACTION_COMPLETE
    };

    private Virtuoso virtuoso;
    IAssetManager assetManager;

    // The three download buttons
    private Button download1;
    private Button download2;
    private Button download3;

    private RecyclerView recyclerView;

    private AssetRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ASSET_TITLE_1 = getString(R.string.download1_name);
        ASSET_TITLE_2 = getString(R.string.download2_name);
        ASSET_TITLE_3 = getString(R.string.download3_name);

        // Set up the three download buttons
        download1 = findViewById(R.id.download_1);
        download2 = findViewById(R.id.download_2);
        download3 = findViewById(R.id.download_3);

        download1.setOnClickListener(v -> downloadAsset(0));
        download2.setOnClickListener(v -> downloadAsset(1));
        download3.setOnClickListener(v -> downloadAsset(2));

        // Initialise the SDK
        initVirtuosoSDK(savedInstanceState);

        // Set up the recyclerview
        recyclerView = findViewById(R.id.downloads_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, RecyclerView.VERTICAL));
        adapter = new AssetRecyclerAdapter(this);
        recyclerView.setAdapter(adapter);

        // Initialize the loader which will get a cursor and update our recyclerview
        LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Resume the Download2Go SDK on activity resume
        virtuoso.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Pause the Download2Go SDK on activity pause
        virtuoso.onPause();
    }


    public void initVirtuosoSDK(Bundle savedInstanceState) {

        virtuoso = new Virtuoso(this);

        assetManager = virtuoso.getAssetManager();

        // This is current best practice for initializing the SDK
        if (savedInstanceState == null) {
            try {
                int status = virtuoso.getBackplane().getAuthenticationStatus();
                if (status == Common.AuthenticationStatus.NOT_AUTHENTICATED) { // If not authenticated then execute sdk startup

                    // Here we use the simplest login with hard coded values
                    URL backplaneUrl = new URL(BACKPLANE_URL);

                    // This starts the SDK by registering it with the backplane. It is asynchronous. Success can be observed using the IEngineObserver interface.
                    virtuoso.startup(backplaneUrl,                              // substitute the proper backplane url for your implementation
                            virtuoso.getBackplane().getSettings().getDeviceId(),    // provide an appropriate unique user id. Virtuoso SDK device id is used here for demonstration purposes only
                            null,                                           // Optional additional device id to be associated with the user account. This is not the device id generated by the SDK
                            BACKPLANE_PUBLIC_KEY,                               // Substitute the real public backplane key here
                            BACKPLANE_PRIVATE_KEY,                              // Substitute the real private backplane key here
                            null                         // Push registration callback, this will be detailed in subsequent tutorials.
                                                                                // This callback does not indicate that SDK startup is complete
                    );
                }
            } catch (MalformedURLException mue) {
                Log.e("MainActivity", "Error with backplane url", mue);
            }
        }

    }


    private void downloadAsset(int index) {

        String url;
        String title;
        String assetId;
        switch (index) {
            case 0:
                url = ASSET_URL_1;
                title = ASSET_TITLE_1;
                assetId = ASSET_ID_1;
                break;
            case 1:
                url = ASSET_URL_2;
                title = ASSET_TITLE_2;
                assetId = ASSET_ID_2;
                break;
            case 2:
            default:
                url = ASSET_URL_3;
                title = ASSET_TITLE_3;
                assetId = ASSET_ID_3;
                break;
        }

        URL assetUrl;
        try {
            assetUrl = new URL(url);
        } catch (MalformedURLException mue) {
            Toast.makeText(this, "Problem with asset or ancillary URL", Toast.LENGTH_LONG).show();
            return;
        }

        // Simple switch between MPEG-DASH and HLS asset creation for demonstration purposes
        if (url.endsWith("mpd")) {
            // Creation of assets follows a builder pattern.
            // Create the parameters for the new asset.
            MPDAssetBuilder.MPDAssetParams params = new MPDAssetBuilder()
                    .assetId(assetId)          // REQUIRED PARAMETER unique asset ID of the new asset
                    .manifestUrl(assetUrl)
                    .assetObserver(new AssetParseObserver(this))
                    .addToQueue(true)
                    .desiredVideoBitrate(Integer.MAX_VALUE)
                    .withMetadata(title)
                    .build();

            // Add to the SDK
            virtuoso.getAssetManager().createMPDSegmentedAssetAsync(params);
        } else {
            HLSAssetBuilder.HLSAssetParams params = new HLSAssetBuilder()
                    .assetId(assetId)          // REQUIRED PARAMETER unique asset ID of the new asset
                    .manifestUrl(assetUrl)
                    .assetObserver(new AssetParseObserver(this))
                    .addToQueue(true)
                    .desiredVideoBitrate(Integer.MAX_VALUE)
                    .withMetadata(title)
                    .build();

            // Add to the SDK
            virtuoso.getAssetManager().createHLSSegmentedAssetAsync(params);
        }
    }

    private void deleteAsset(int assetId) {
        virtuoso.getAssetManager().delete(assetId);
    }

    private void openAsset(String assetId) {
        Intent intent = new Intent(this, AssetDetailActivity.class)
                .setAction(ACTION_VIEW)
                .putExtra(AssetDetailActivity.ASSET_ID_KEY, assetId);
        startActivity(intent);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int loader, Bundle arg1) {
        Uri uri = assetManager.CONTENT_URI();
        return new CursorLoader(this,uri,PROJECTION,null,null,null);
    }

    // onLoadFinished
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null) {
            cursor.setNotificationUri(getContentResolver(), assetManager.CONTENT_URI());
            adapter.setCursor(cursor);
        }
    }

    // onLoaderReset
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            adapter.setCursor(null);
    }

    /**
     * This class observes when an asset parse is complete
     */
    static class AssetParseObserver implements ISegmentedAssetFromParserObserver {

        private AppCompatActivity activity;

        public AssetParseObserver(AppCompatActivity activity) {
            this.activity = activity;
        }

        @Override
        public void complete(final ISegmentedAsset asset, int error, boolean addedToQueue) {
            activity.runOnUiThread(() -> {

                if(asset != null && error == 0) {
                    Toast.makeText(activity, "Asset parsed and " + (addedToQueue ? "added" : "not added") + " to download queue", Toast.LENGTH_LONG  ).show();
                }
                else{
                    Toast.makeText(activity, "Error " + error + " while parsing asset", Toast.LENGTH_LONG).show();
                }
            });
        }
    }


    private class AssetRecyclerAdapter extends RecyclerView.Adapter<AssetRecyclerAdapter.AssetHolder> {

        private Cursor cursor = null;

        private final Context context;

        public AssetRecyclerAdapter(Context context) {
            this.context = context;
        }

        public void setCursor(Cursor cursor) {
            this.cursor = cursor;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public AssetHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new AssetHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.asset_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull AssetHolder holder, int position) {
            if (cursor != null) {
                cursor.moveToPosition(position);
                holder.bindItem(cursor);
            }
        }

        @Override
        public int getItemCount() {
            if (cursor != null) {
                return cursor.getCount();
            }
            return 0;
        }

        class AssetHolder extends RecyclerView.ViewHolder {

            private TextView titleTextView;
            private TextView progressTextView;
            private TextView idTextView;
            private TextView statusTextView;
            private TextView sizeTextView;
            private ProgressBar progressBar;

            private int titleIndex = -1;
            private int index;
            private int idIndex;
            private int statusIndex;
            private int currentSizeIndex;
            private int estimatedSizeIndex;
            private int progressIndex;

            private int itemId;
            private String assetId;

            public AssetHolder(@NonNull View itemView) {
                super(itemView);

                titleTextView = itemView.findViewById(R.id.titleTextView);
                progressTextView = itemView.findViewById(R.id.progressTextView);
                idTextView = itemView.findViewById(R.id.idTextView);
                statusTextView = itemView.findViewById(R.id.statusTextView);
                sizeTextView = itemView.findViewById(R.id.sizeTextView);
                progressBar = itemView.findViewById(R.id.progressBar);

                itemView.findViewById(R.id.delete).setOnClickListener(v -> deleteAsset(itemId));

                itemView.setOnClickListener(v -> openAsset(assetId));
            }

            public void bindItem(Cursor cursor) {

                if (titleIndex < 0) {
                    fetchCursorIndexes(cursor);
                }

                itemId = cursor.getInt(index);
                assetId = cursor.getString(idIndex);

                titleTextView.setText(cursor.getString(titleIndex));
                idTextView.setText(assetId);
                statusTextView.setText(getStatusText(context, cursor.getInt(statusIndex)));

                long currentSize = cursor.getLong(currentSizeIndex);
                long expectedSize = cursor.getLong(estimatedSizeIndex);
                sizeTextView.setText(context.getString(R.string.asset_size, String.format(Locale.getDefault(),"%.2f MB", currentSize/1048576.00), String.format(Locale.getDefault(),"%.2f MB", expectedSize/1048576.00)));

                double progressPercent = cursor.getDouble(progressIndex);
                progressTextView.setText(String.format(Locale.getDefault(),"(%.2f)", progressPercent));

                progressBar.setProgress((int)(progressPercent * 100));
            }

            // For efficiency, fetch all the cursor indexes on the first execution and reuse
            public void fetchCursorIndexes(Cursor cursor) {
                index = cursor.getColumnIndex(AssetColumns._ID);
                titleIndex = cursor.getColumnIndex(AssetColumns.METADATA);
                idIndex = cursor.getColumnIndex(AssetColumns.ASSET_ID);
                statusIndex = cursor.getColumnIndex(AssetColumns.DOWNLOAD_STATUS);
                currentSizeIndex = cursor.getColumnIndex(AssetColumns.CURRENT_SIZE);
                estimatedSizeIndex = cursor.getColumnIndex(AssetColumns.EXPECTED_SIZE);
                progressIndex = cursor.getColumnIndex(AssetColumns.FRACTION_COMPLETE);
            }
        }

    }

    public static String getStatusText(Context context,  int downloadStatus) {
        String value;

        switch (downloadStatus) {
            case Common.AssetStatus.DOWNLOADING:
                value = context.getString(R.string.asset_status_downloading);
                break;

            case Common.AssetStatus.DOWNLOAD_COMPLETE:
                value = context.getString(R.string.asset_status_complete);
                break;

            case Common.AssetStatus.DOWNLOAD_PAUSED:
                value = context.getString(R.string.asset_status_paused);
                break;

            case Common.AssetStatus.EXPIRED:
                value = context.getString(R.string.asset_status_expired);
                break;

            case Common.AssetStatus.DOWNLOAD_DENIED_ASSET:
                value = context.getString(R.string.asset_status_denied_mad);
                break;

            case Common.AssetStatus.DOWNLOAD_DENIED_ACCOUNT:
                value = context.getString(R.string.asset_status_denied_mda);
                break;

            case Common.AssetStatus.DOWNLOAD_DENIED_EXTERNAL_POLICY:
                value = context.getString(R.string.asset_status_denied_ext);
                break;

            case Common.AssetStatus.DOWNLOAD_DENIED_MAX_DEVICE_DOWNLOADS:
                value = context.getString(R.string.asset_status_denied_mpd);
                break;

            case Common.AssetStatus.DOWNLOAD_DENIED_COPIES:
                value = context.getString(R.string.asset_status_denied_copies);
                break;

            case Common.AssetStatus.DOWNLOAD_BLOCKED_AWAITING_PERMISSION:
                value = context.getString(R.string.asset_status_await_permission);
                break;

            default:
                value = context.getString(R.string.asset_status_pending);
        }
        return value;
    }
}
