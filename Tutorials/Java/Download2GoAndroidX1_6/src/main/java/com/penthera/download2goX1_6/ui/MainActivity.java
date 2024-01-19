/*
    Created by Penthera on 26/03/21.
    Copyright © 2021 penthera. All rights reserved.

    This source file contains a very basic example showing how to use the Penthera Download2Go SDK.
*/
package com.penthera.download2goX1_6.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.penthera.download2goX1_6.AssetsAdapter;
import com.penthera.download2goX1_6.viewmodel.AssetListViewModel;
import com.penthera.download2goX1_6.R;
import com.penthera.common.Common.AuthenticationStatus;
import com.penthera.virtuososdk.androidxsupport.VirtuosoLiveDataFactory;
import com.penthera.virtuososdk.client.ISegmentedAsset;
import com.penthera.virtuososdk.client.ISegmentedAssetFromParserObserver;
import com.penthera.virtuososdk.client.Virtuoso;
import com.penthera.virtuososdk.client.builders.HLSAssetBuilder;
import com.penthera.virtuososdk.client.builders.MPDAssetBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import static android.content.Intent.ACTION_VIEW;

/**
 * A simple activity containing buttons to create three assets and a RecyclerView to show all stored assets.
 * The RecyclerView is populated using a basic ViewModel, which uses LiveData provided by the Penthera SDK.
 */
public class MainActivity extends AppCompatActivity {

    // DEMO Server details
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

    // Important: The LiveData Factory object from the Penthera SDK enables the SDK to be lifecycle aware. It contains
    // a reference to the underlying Virtuoso object and provides all of the LiveData objects for the SDK.
    private VirtuosoLiveDataFactory virtuosoLiveDataFactory;

    // The Virtuoso object provides the top level interface to the Penthera SDK for all aspects apart from LiveData.
    private Virtuoso virtuoso;

    // The three download buttons
    private Button download1;
    private Button download2;
    private Button download3;

    private RecyclerView recyclerView;
    private AssetsAdapter adapter;

    // Callback interface used for binding to the list items
    private final AssetItemCallback assetCallback = new AssetItemCallback() {
        @Override
        public void onOpen(ISegmentedAsset asset) {
            Intent intent = new Intent(MainActivity.this, AssetDetailActivity.class)
                    .setAction(ACTION_VIEW)
                    .putExtra(AssetDetailActivity.ASSET_ID_KEY, asset.getAssetId());
            startActivity(intent);
        }

        @Override
        public void onDelete(ISegmentedAsset asset) {
            virtuoso.getAssetManager().delete(asset.getId());
        }

        @Override
        public void onPlay(ISegmentedAsset asset){
            // Unused from list view
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ASSET_TITLE_1 = getString(R.string.download1_name);
        ASSET_TITLE_2 = getString(R.string.download2_name);
        ASSET_TITLE_3 = getString(R.string.download3_name);

        // The live data factory must be created and then initialized with a lifecycle.
        virtuosoLiveDataFactory = VirtuosoLiveDataFactory.getInstance();
        virtuoso = virtuosoLiveDataFactory.createVirtuosoWithLifecycle(this, this);

        AssetListViewModel.Factory factory = new AssetListViewModel.Factory(getApplication(), virtuosoLiveDataFactory);
        AssetListViewModel model = new ViewModelProvider(this, factory).get(AssetListViewModel.class);

        model.getUiAssets().observe(this, assets -> {
            if (assets != null) {
                adapter.setAssetList(assets);
            }
        });

        // Set up the three download buttons
        download1 = findViewById(R.id.download_1);
        download2 = findViewById(R.id.download_2);
        download3 = findViewById(R.id.download_3);

        download1.setOnClickListener(v -> downloadAsset(0));
        download2.setOnClickListener(v -> downloadAsset(1));
        download3.setOnClickListener(v -> downloadAsset(2));

        // Set up the recyclerview
        recyclerView = findViewById(R.id.downloads_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, RecyclerView.VERTICAL));
        adapter = new AssetsAdapter(assetCallback);
        recyclerView.setAdapter(adapter);
        // This prevents the button on the list items flickering due to the default animation
        recyclerView.getItemAnimator().setChangeDuration(0);
    }

    /**
     * Initialize the Penthera SDK and the ViewModel with SDK LiveData objects
     **/
    public void initVirtuosoSDK() {
        // This is current best practice for starting the SDK
        int status = virtuoso.getBackplane().getAuthenticationStatus();
        if (status != AuthenticationStatus.AUTHENTICATED) { // If not authenticated then execute sdk startup

            // Here we use the simplest login with hard coded values
            // This starts the SDK by registering it with the backplane. It is asynchronous. Success can be observed using the IEngineObserver interface.
            virtuoso.startup(
                    UUID.randomUUID().toString(),    // provide an appropriate unique user id. A random uuid is used here for demonstration purposes only
                    null,                                           // Optional additional device id to be associated with the user account. This is not the device id generated by the SDK
                    BACKPLANE_PUBLIC_KEY,                               // Substitute the real public backplane key here
                    BACKPLANE_PRIVATE_KEY,                              // Substitute the real private backplane key here
                    null                         // Push registration callback, this will be detailed in subsequent tutorials.
                    // This callback does not indicate that SDK startup is complete
            );
        }
    }

    /**
     * A simple method to download an asset from the three assets statically defined for the test app.
     * Demonstrates adding a basic MPG DASH or HLS asset.
     * @param index Index of asset to add to the download queue.
     */
    private void downloadAsset(int index) {
        initVirtuosoSDK();

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

    /**
     * This class observes when an asset parse is complete.
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
}
