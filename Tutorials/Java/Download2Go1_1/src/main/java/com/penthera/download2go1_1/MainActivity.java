/*
    Created by Penthera on 10/10/19.
    Copyright © 2019 penthera. All rights reserved.

    This source file contains a very basic example showing how to use the Penthera Download2Go SDK.
    Pay close attention to code comments marked IMPORTANT
*/
package com.penthera.download2go1_1;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.penthera.virtuososdk.Common;
import com.penthera.common.Common.AuthenticationStatus;
import com.penthera.virtuososdk.client.EngineObserver;
import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.IAssetManager;
import com.penthera.virtuososdk.client.IIdentifier;
import com.penthera.virtuososdk.client.ISegmentedAsset;
import com.penthera.virtuososdk.client.ISegmentedAssetFromParserObserver;
import com.penthera.virtuososdk.client.IService;
import com.penthera.virtuososdk.client.Observers;
import com.penthera.virtuososdk.client.ServiceException;
import com.penthera.virtuososdk.client.Virtuoso;
import com.penthera.virtuososdk.client.builders.MPDAssetBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // DEMO Server details
    private static final String BACKPLANE_URL = "https://demo.penthera.com/";
    private static final String BACKPLANE_PUBLIC_KEY = ;
    private static final String BACKPLANE_PRIVATE_KEY = ;

    // This is the test asset which will be downloaded
    // Important: Asset ID should be unique across your video catalog
    private static final String ASSET_ID = "TEST_ASSET_ID";
    private static final String ASSET_TITLE = "TEST ASSET";
    private static final String ASSET_URL = "https://storage.googleapis.com/wvmedia/clear/h264/tears/tears_sd.mpd";

    private Virtuoso virtuoso;
    private IAsset asset = null;
    private AssetQueueObserver queueObserver;

    private IService download2GoService;


    // The three buttons
    private Button dlBtn;
    private Button plBtn;
    private Button delBtn;

    // The pause switches
    private Switch pauseAssetSwitch;
    private Switch pauseEngineSwitch;

    // Engine pause is an asynchronous request so we keep track of our current states to update the UI
    private boolean pauseRequested = false;
    private boolean resumeRequested = false;

    private boolean applyingInternalUpdate = false;

    private TextView textView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dlBtn = findViewById(R.id.download);
        plBtn = findViewById(R.id.play);
        delBtn = findViewById(R.id.delete);
        textView = findViewById(R.id.textView);
        progressBar = findViewById(R.id.progressBar);
        pauseAssetSwitch = findViewById(R.id.pauseAsset);
        pauseEngineSwitch = findViewById(R.id.pauseEngine);

        dlBtn.setOnClickListener(v -> downloadAsset());
        plBtn.setOnClickListener(v -> playAsset());
        delBtn.setOnClickListener(v -> deleteAsset());

        pauseAssetSwitch.setOnCheckedChangeListener((v, checked) -> {if(!applyingInternalUpdate) pauseAsset(checked);});
        pauseEngineSwitch.setOnCheckedChangeListener((v, checked) -> {if(!applyingInternalUpdate) pauseEngine(checked);});

        initVirtuosoSDK(savedInstanceState);
        updateUI();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Resume the Download2Go SDK on activity resume
        virtuoso.onResume();
        virtuoso.addObserver(queueObserver);
        virtuoso.addObserver(enginePauseObserver);
        download2GoService.setConnectionObserver(serviceConnectionObserver);
        download2GoService.bind();

        forceUpdateAssetDetails();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Pause the Download2Go SDK on activity pause
        virtuoso.onPause();
        virtuoso.removeObserver(queueObserver);
        virtuoso.removeObserver(enginePauseObserver);
        download2GoService.unbind();
        download2GoService.setConnectionObserver(null);
    }

    public void initVirtuosoSDK(Bundle savedInstanceState) {

        virtuoso = new Virtuoso(this);
        // This assigns a new instance of the service client, it is a thin wrapper around an Android service binding.
        download2GoService = virtuoso.getService();

        queueObserver = new AssetQueueObserver(this);

        // This is current best practice for initializing the SDK
        if (savedInstanceState == null) {
            try {
                int status = virtuoso.getBackplane().getAuthenticationStatus();
                if (status == AuthenticationStatus.NOT_AUTHENTICATED) { // If not authenticated then execute sdk startup

                    // Here we use the simplest login with hard coded values
                    URL backplaneUrl = new URL(BACKPLANE_URL);

                    // This starts the SDK by registering it with the backplane. It is asynchronous. Success can be observed using the IEngineObserver interface.
                    virtuoso.startup(backplaneUrl,                              // substitute the proper backplane url for your implementation
                            UUID.randomUUID().toString(),    // provide an appropriate unique user id. A random uuid is used here for demonstration purposes only
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

        // Load asset if it has already been downloaded
        List<IIdentifier> list = virtuoso.getAssetManager().getByAssetId(ASSET_ID);
        if (list != null && list.size() > 0) {
            asset = (IAsset)list.get(0);
        }
    }

    private void updateUI() {
        dlBtn.setEnabled(asset == null);
        plBtn.setEnabled(asset != null);
        delBtn.setEnabled(asset != null);

        if (asset == null) {
            textView.setText("");
            pauseAssetSwitch.setEnabled(false);
        } else {
            pauseAssetSwitch.setEnabled(true);
        }
    }

    private void forceUpdateAssetDetails() {
        // Find any current asset. Commonly this should be only one matching the external ID
        List<IIdentifier> assets =  virtuoso.getAssetManager().getByAssetId(ASSET_ID);
        if (assets.size() > 0) {
            asset = (IAsset)assets.get(0);
        }

        if (asset != null) {
            // update UI
            queueObserver.updateItem(asset, true);
            applyingInternalUpdate = true; // prevent updating the switch from calling the SDK
            boolean assetPaused = asset.getDownloadStatus() == Common.AssetStatus.DOWNLOAD_PAUSED;
            if (pauseAssetSwitch.isChecked() != assetPaused) {
                pauseAssetSwitch.setChecked(assetPaused);
            }
            applyingInternalUpdate = false;
        }
    }

    private void downloadAsset() {

        URL assetUrl;
        try {
            assetUrl = new URL(ASSET_URL);
        } catch (MalformedURLException mue) {
            Toast.makeText(this, "Problem with asset URL", Toast.LENGTH_LONG).show();
            return;
        }

        // Creation of assets follows a builder pattern.
        // Create the parameters for the new asset.
        MPDAssetBuilder.MPDAssetParams params = new MPDAssetBuilder()
                .assetId(ASSET_ID)          // REQUIRED PARAMETER unique asset ID of the new asset
                .manifestUrl(assetUrl)
                .assetObserver(new AssetParseObserver(this))
                .addToQueue(true)
                .desiredVideoBitrate(Integer.MAX_VALUE)
                .withMetadata(ASSET_TITLE)
                .build();

        // Add to the SDK
        virtuoso.getAssetManager().createMPDSegmentedAssetAsync(params);
    }

    private void playAsset() {
        if (asset != null) {
            VideoPlayerActivity.playVideoDownload(this, asset);
        }
    }

    private void deleteAsset() {

        virtuoso.getAssetManager().delete(asset);
        textView.setText(R.string.deleting_asset);
        asset = null;
        updateUI();
    }

    private void pauseAsset(boolean pause) {
        if (asset != null) {
            if (pause) {
                virtuoso.getAssetManager().pauseDownload(asset);
            } else {
                virtuoso.getAssetManager().resumeDownload(asset);
            }
        }
    }

    private void pauseEngine(boolean pause) {
        if (download2GoService != null && download2GoService.isBound()){
            try {
                if (pause && download2GoService.getStatus() != Common.EngineStatus.PAUSED){
                    pauseRequested = true;
                    download2GoService.pauseDownloads();
                } else if (!pause && download2GoService.getStatus() == Common.EngineStatus.PAUSED) {
                    resumeRequested = true;
                    download2GoService.resumeDownloads();
                }
            } catch (ServiceException se) {
                Log.e("MainActivity", "Error connecting to Virtuoso service", se);
            }
        }
    }

    /**
     * Connection observer monitors when the service is bound
     */
    protected IService.IConnectionObserver serviceConnectionObserver = new IService.IConnectionObserver(){

        @Override
        public void connected() {
            // Update UI once we know connection is bound.
            if (download2GoService != null && download2GoService.isBound()) {
                try {
                    final int status = download2GoService.getStatus();
                    applyingInternalUpdate = true;
                    pauseEngineSwitch.setChecked(status == Common.EngineStatus.PAUSED);
                    applyingInternalUpdate = false;

                } catch (ServiceException se) {
                    Log.d(MainActivity.class.getName(), "Service Exception on getting service status");
                }
            }
        }

        @Override
        public void disconnected() {
        }

    };

    /**
     * This class observes the SDK engine status changes.
     */
    private Observers.IEngineObserver enginePauseObserver = new EngineObserver() {
        @Override
        public void engineStatusChanged(int status) {
            if(pauseRequested){
                if(status == Common.EngineStatus.PAUSED) pauseRequested = false;
                forceUpdateAssetDetails();
            }
            if(resumeRequested){
                if(status != Common.EngineStatus.PAUSED) resumeRequested = false;
                forceUpdateAssetDetails();
            }
        }
    };

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

    /**
     * This class observes the SDK download queue and provides updates during the download process.
     */
    static class AssetQueueObserver implements Observers.IQueueObserver {

        private final MainActivity mainActivity;
        private int lastProgress = -1;

        public AssetQueueObserver(MainActivity activity){
            mainActivity = activity;
        }

        @Override
        public void engineStartedDownloadingAsset(IIdentifier asset) {
            lastProgress = -1;
            updateItem(asset, true);
        }

        @Override
        public void enginePerformedProgressUpdateDuringDownload(IIdentifier asset) {
            updateItem(asset, false);
        }

        @Override
        public void engineCompletedDownloadingAsset(IIdentifier asset) {
            updateItem(asset, true);
        }

        @Override
        public void engineEncounteredErrorDownloadingAsset(IIdentifier asset) {
            // The base implementation does nothing.  See class documentation.
        }

        @Override
        public void engineUpdatedQueue() {
            // This indicates a change to the download queue - meaning either we added or removed something
            IAssetManager assetManager = mainActivity.virtuoso.getAssetManager();
            int queued = assetManager.getQueue().size();
            final int downloaded = assetManager.getDownloaded().getCursor().getCount();
            if (mainActivity.asset != null && (queued > 0 || downloaded > 0)) {
                IAsset asset = (IAsset)assetManager.get(mainActivity.asset.getId());
                if (asset.getDownloadStatus() != mainActivity.asset.getDownloadStatus()){
                    mainActivity.asset = asset;
                    updateItem(asset, true);
                }
            }
            if (queued == 0) {
                // The asset has been deleted or downloaded
                mainActivity.runOnUiThread(() -> {
                    mainActivity.textView.setText(downloaded == 0 ? "Asset Deleted" : "Asset Downloaded");
                    mainActivity.progressBar.setVisibility(View.GONE);
                });
            }
        }

        @Override
        public void engineEncounteredErrorParsingAsset(String s) {
            // The base implementation does nothing.  See class documentation.
        }

        private void updateItem(final IIdentifier identifier, boolean forceUpdate) {
            if (identifier instanceof IAsset) {
                final IAsset asset = (IAsset) identifier;
                String assetId = asset.getAssetId();

                // Ensure progress is for our catalog item
                if (!TextUtils.isEmpty(assetId) && assetId.equals(ASSET_ID)){
                    mainActivity.runOnUiThread(() -> updateItemStatus(asset, forceUpdate));
                }
            }
        }

        private void updateItemStatus(final IAsset asset, boolean forceUpdate) {
            mainActivity.asset = asset;

            // Calculate progress as a percentage
            int progress = (int) (asset.getCurrentSize() / asset.getExpectedSize() * 100); // Sizes in doubles, percentage cast to an int for reporting

            // Keep context switches minimal due to frequency of messages, unless forced
            if (forceUpdate || progress != lastProgress) {
                lastProgress = progress;

                String assetStatus;
                int downloadStatus = asset.getDownloadStatus();
                String value;

                switch (downloadStatus) {
                    case Common.AssetStatus.DOWNLOADING:
                        assetStatus = mainActivity.getString(R.string.status_downloading);
                        value = mainActivity.getString(R.string.asset_status_downloading);
                        break;

                    case Common.AssetStatus.DOWNLOAD_COMPLETE:
                        assetStatus = mainActivity.getString(R.string.status_downloaded);
                        value = mainActivity.getString(R.string.asset_status_complete);
                        break;

                    case Common.AssetStatus.DOWNLOAD_PAUSED:
                        assetStatus = mainActivity.getString(R.string.status_queued);
                        value = mainActivity.getString(R.string.asset_status_paused);
                        break;

                    case Common.AssetStatus.EXPIRED:
                        assetStatus = mainActivity.getString(R.string.status_expired);
                        value = mainActivity.getString(R.string.asset_status_expired);
                        break;

                    case Common.AssetStatus.DOWNLOAD_DENIED_ASSET:
                        assetStatus = mainActivity.getString(R.string.status_queued);
                        value = mainActivity.getString(R.string.asset_status_denied_mad);
                        break;

                    case Common.AssetStatus.DOWNLOAD_DENIED_ACCOUNT:
                        assetStatus = mainActivity.getString(R.string.status_queued);
                        value = mainActivity.getString(R.string.asset_status_denied_mda);
                        break;

                    case Common.AssetStatus.DOWNLOAD_DENIED_EXTERNAL_POLICY:
                        assetStatus = mainActivity.getString(R.string.status_queued);
                        value = mainActivity.getString(R.string.asset_status_denied_ext);
                        break;

                    case Common.AssetStatus.DOWNLOAD_DENIED_MAX_DEVICE_DOWNLOADS:
                        assetStatus = mainActivity.getString(R.string.status_queued);
                        value = mainActivity.getString(R.string.asset_status_denied_mpd);
                        break;

                    case Common.AssetStatus.DOWNLOAD_DENIED_COPIES:
                        assetStatus = mainActivity.getString(R.string.status_queued);
                        value = mainActivity.getString(R.string.asset_status_denied_copies);
                        break;

                    case Common.AssetStatus.DOWNLOAD_BLOCKED_AWAITING_PERMISSION:
                        assetStatus = mainActivity.getString(R.string.status_queued);
                        value = mainActivity.getString(R.string.asset_status_await_permission);
                        break;

                    default:
                        assetStatus = mainActivity.getString(R.string.status_pending);
                        value = mainActivity.getString(R.string.asset_status_pending);
                }

                mainActivity.updateUI();
                mainActivity.textView.setVisibility(View.VISIBLE);
                mainActivity.textView.setText(mainActivity.getString(R.string.asset_status, assetStatus, asset.getErrorCount(), value));

                // Show a small amount of progress on bar
                if (progress == 0) {
                    progress = 1;
                }

                if (progress > 0 && progress < 99) {
                    mainActivity.progressBar.setVisibility(View.VISIBLE);
                    mainActivity.progressBar.setProgress(progress);
                } else {
                    mainActivity.progressBar.setVisibility(View.GONE);
                }
            }
        }

    }
}
