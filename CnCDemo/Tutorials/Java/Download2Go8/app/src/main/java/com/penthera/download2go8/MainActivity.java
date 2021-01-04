/*
    Created by Penthera.
    Copyright © 2020 penthera. All rights reserved.

    This source file contains a very basic example showing how to use the Penthera Download2Go SDK.
    Pay close attention to code comments marked IMPORTANT
*/
package com.penthera.download2go8;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.client.EngineObserver;
import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.IIdentifier;
import com.penthera.virtuososdk.client.ISegmentedAsset;
import com.penthera.virtuososdk.client.ISegmentedAssetFromParserObserver;
import com.penthera.virtuososdk.client.Observers;
import com.penthera.virtuososdk.client.Virtuoso;
import com.penthera.virtuososdk.client.builders.MPDAssetBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // DEMO Server details
    private static final String BACKPLANE_URL = "https://demo.penthera.com/";
    private static final String BACKPLANE_PUBLIC_KEY = ;
    private static final String BACKPLANE_PRIVATE_KEY = ;

    // This is the test asset which will be downloaded
    // Important: Asset ID should be unique across your video catalog
    private static final String ASSET_ID = "TEST_ASSET_ID";
    private static final String ASSET_TITLE = "TEST ASSET";
    private static final String ASSET_URL = "https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears_sd.mpd";

    private Virtuoso virtuoso;
    private IAsset asset = null;
    private AssetQueueObserver queueObserver;
    private LicenseObserver licenseObserver;

    // The three buttons
    private Button dlBtn;
    private Button plBtn;
    private Button delBtn;

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

        dlBtn.setOnClickListener(v -> downloadAsset());
        plBtn.setOnClickListener(v -> playAsset());
        delBtn.setOnClickListener(v -> deleteAsset());

        initVirtuosoSDK(savedInstanceState);
        updateUI();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Resume the Download2Go SDK on activity resume
        virtuoso.onResume();
        virtuoso.addObserver(queueObserver);
        virtuoso.addObserver(licenseObserver);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Pause the Download2Go SDK on activity pause
        virtuoso.onPause();
        virtuoso.removeObserver(queueObserver);
        virtuoso.removeObserver(licenseObserver);
    }

    public void initVirtuosoSDK(Bundle savedInstanceState) {

        virtuoso = new Virtuoso(this);
        queueObserver = new AssetQueueObserver(this);
        licenseObserver = new LicenseObserver(this);

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
        asset = null;
        updateUI();
    }

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

    static class LicenseObserver extends EngineObserver {

        private AppCompatActivity activity;

        public LicenseObserver(AppCompatActivity activity) {
            this.activity = activity;
        }

        @Override
        public void assetLicenseRetrieved(final IIdentifier aItem, final boolean aSuccess) {
            activity.runOnUiThread(() -> {
                ISegmentedAsset asset = (ISegmentedAsset)aItem;
                Toast.makeText(activity, activity.getString(aSuccess ? R.string.license_fetch_success : R.string.license_fetch_failure, asset.getAssetId()), Toast.LENGTH_LONG).show();
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
            // The base implementation does nothing.  See class documentation.
        }

        @Override
        public void engineEncounteredErrorParsingAsset(String s) {
            // The base implementation does nothing.  See class documentation.
        }

        private void updateItem(final IIdentifier identifier, boolean forceUpdate) {
            if ( identifier instanceof IAsset) {
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
