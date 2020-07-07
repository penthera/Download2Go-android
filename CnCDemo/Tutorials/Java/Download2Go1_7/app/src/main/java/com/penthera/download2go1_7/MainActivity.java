/*
    Created by Penthera on 10/10/19.
    Copyright © 2019 penthera. All rights reserved.

    This source file contains a very basic example showing how to use the Penthera Download2Go SDK.
    Pay close attention to code comments marked IMPORTANT
*/
package com.penthera.download2go1_7;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.client.BackplaneException;
import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.IAssetManager;
import com.penthera.virtuososdk.client.IBackplaneDevice;
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
    private static String BACKPLANE_URL = "https://demo.penthera.com/";
    private static String BACKPLANE_PUBLIC_KEY = ;
    private static String BACKPLANE_PRIVATE_KEY = ;



    // This is the test asset which will be downloaded
    // Important: Asset ID should be unique across your video catalog
    private static String ASSET_ID = "TEST_ASSET_ID";
    private static String ASSET_TITLE = "TEST ASSET";
    private static String ASSET_URL = "https://storage.googleapis.com/wvmedia/clear/h264/tears/tears_sd.mpd";

    private Virtuoso virtuoso;
    private IAsset asset = null;
    private AssetQueueObserver queueObserver;
    private boolean registering = false;

    // Register / unregister buttons
    private Button registerBtn;
    private Button unregisterDevicesBtn;
    private EditText accountName;

    // The three asset download buttons
    private Button dlBtn;
    private Button plBtn;
    private Button delBtn;

    private TextView textView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerBtn = findViewById(R.id.register);
        unregisterDevicesBtn = findViewById(R.id.unregister);
        accountName = findViewById(R.id.account_name);

        dlBtn = findViewById(R.id.download);
        plBtn = findViewById(R.id.play);
        delBtn = findViewById(R.id.delete);
        textView = findViewById(R.id.textView);
        progressBar = findViewById(R.id.progressBar);

        registerBtn.setOnClickListener(v -> {if (!registering) doRegister(false);});
        unregisterDevicesBtn.setOnClickListener(v -> doUnregister());
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
        virtuoso.addObserver(backplaneObserver);

        forceUpdateAssetDetails();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Pause the Download2Go SDK on activity pause
        virtuoso.onPause();
        virtuoso.removeObserver(queueObserver);
        virtuoso.removeObserver(backplaneObserver);
    }

    public void initVirtuosoSDK(Bundle savedInstanceState) {

        virtuoso = new Virtuoso(this);

        queueObserver = new AssetQueueObserver(this);

        updateRegisterButtons();

        // Load asset if it has already been downloaded
        List<IIdentifier> list = virtuoso.getAssetManager().getByAssetId(ASSET_ID);
        if (list != null && list.size() > 0) {
            asset = (IAsset)list.get(0);
        }
    }

    private void updateRegisterButtons() {
        int status = virtuoso.getBackplane().getAuthenticationStatus();
        if (status == Common.AuthenticationStatus.NOT_AUTHENTICATED) { // If not authenticated then execute sdk startup
            registerBtn.setEnabled(true);
        } else {
            registerBtn.setEnabled(false);
            String registeredUserId = virtuoso.getBackplane().getSettings().getUserId();
            accountName.setText(registeredUserId);
        }
    }

    private void doRegister(boolean retry) {

        int status = virtuoso.getBackplane().getAuthenticationStatus();
        if (status == Common.AuthenticationStatus.NOT_AUTHENTICATED) { // If not authenticated then execute sdk startup
            String name = accountName.getText().toString();
            if (!TextUtils.isEmpty(name)) {

                URL backplaneUrl = null;
                try {
                    // Here we use the simplest login with hard coded values
                    backplaneUrl = new URL(BACKPLANE_URL);
                } catch (MalformedURLException mue) {
                    // In a real app we would handle this error
                    return;
                }

                registering = true;

                // This starts the SDK by registering it with the backplane. It is asynchronous. Success can be observed using the IEngineObserver interface.
                // Push registration callback,
                virtuoso.startup(backplaneUrl,                              // substitute the proper backplane url for your implementation
                        name,    // provide an appropriate unique user id. Virtuoso SDK device id is used here for demonstration purposes only
                        null,                                           // Optional additional device id to be associated with the user account. This is not the device id generated by the SDK
                        BACKPLANE_PUBLIC_KEY,                               // Substitute the real public backplane key here
                        BACKPLANE_PRIVATE_KEY,                              // Substitute the real private backplane key here
                        (pushService, connectionResponse) -> {                 // Push registration observer (IPushRegistrationObserver)
                            // This observer can be used for both FCM and ADM. Only FCM is demonstrated here
                            // This callback does not indicate that SDK startup is complete, only that push is registered
                            if (pushService == Common.PushService.FCM_PUSH)

                                // Check for failure
                                if (connectionResponse != 0) {

                                    // If FCM registration fails on older devices it is most likely due to lack
                                    // of the appropriate support library. Check to see if it can be made available.
                                    final GoogleApiAvailability gApi = GoogleApiAvailability.getInstance();
                                    if (!retry && gApi.isUserResolvableError(connectionResponse)) {

                                        runOnUiThread(new Runnable() {

                                            @Override
                                            public void run() {
                                                gApi.makeGooglePlayServicesAvailable(MainActivity.this)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                Log.d("Push Registration", "makeGooglePlayServicesAvailable complete");

                                                                if (task.isSuccessful()) {
                                                                    Log.d("Push Registration", "makeGooglePlayServicesAvailable completed successfully");
                                                                    doRegister(true);
                                                                } else {
                                                                    Exception e = task.getException();
                                                                    Log.e("Push Registration", "makeGooglePlayServicesAvailable completed with exception " + e.getMessage(), e);
                                                                }
                                                            }
                                                        });
                                            }
                                        });

                                    }
                                } // on success we do not take any action in this demonstration
                        });

            } else {
                Toast.makeText(this, R.string.missing_account_name, Toast.LENGTH_LONG);
            }
        }
    }

    private void doUnregister() {

        unregisterDevicesBtn.setEnabled(false);

        // Set up request to get account devices
        int status = virtuoso.getBackplane().getAuthenticationStatus();
        if (status == Common.AuthenticationStatus.AUTHENTICATED) {
            try {
                virtuoso.getBackplane().getDevices(iBackplaneDevices -> {
                    if (iBackplaneDevices.length > 0) {
                        // There should always be at least one device registered - ourselves!
                        // Iterate to identify all other devices registered to user
                        for (IBackplaneDevice device : iBackplaneDevices) {
                            if (!device.isCurrentDevice()) {
                                // When we request the device to deregister, this should send a push
                                // notification to that device via the backplane. It may not be a common
                                // approach to deregister devices directly from another device,
                                // but is useful for demonstration purposes.

                                try {
                                    virtuoso.getBackplane().unregisterDevice(device);
                                } catch (BackplaneException e) {
                                    Log.e("Unregister", "Caught exception requesting unregister on device",e);
                                }
                            }
                        }
                    }
                });
            } catch (BackplaneException be) {
                Log.e("Unregister", "Caught exception requesting devices",be);
            };
        }

        unregisterDevicesBtn.setEnabled(true);
    }


    private void updateUI() {
        dlBtn.setEnabled(asset == null);
        plBtn.setEnabled(asset != null);
        delBtn.setEnabled(asset != null);

        if (asset == null) {
            textView.setText("");
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
        textView.setText("Deleting asset");
        asset = null;
        updateUI();
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

                if(asset != null && error == 0) {    //TODO: we should have a success constant!
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
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.textView.setText(downloaded == 0 ? "Asset Deleted" : "Asset Downloaded");
                        mainActivity.progressBar.setVisibility(View.GONE);
                    }
                });
            }
        }

        @Override
        public void engineEncounteredErrorParsingAsset(String s) {
            // The base implementation does nothing.  See class documentation.
        }

        private void updateItem(final IIdentifier identifier, boolean forceUpdate) {
            if (identifier != null && identifier instanceof IAsset) {
                final IAsset asset = (IAsset) identifier;
                String assetId = asset.getAssetId();

                // Ensure progress is for our catalog item
                if (!TextUtils.isEmpty(assetId) && assetId.equals(ASSET_ID)){
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateItemStatus(asset, forceUpdate);
                        }
                    });
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

    // Observe backplane request changes, enabling the UI to react to actions such as SDK startup and unregister.
    private Observers.IBackplaneObserver backplaneObserver = new Observers.IBackplaneObserver(){
        @Override
        public void requestComplete(final int request, final int result, final String errorMessage) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (request == Common.BackplaneCallbackType.SYNC) {
                        // Push messages result in a sync. That sync response may contain instructions such as to
                        // unregister the device, disable downloads, or remote wipe.
                        if (result == Common.BackplaneResult.SUCCESS) {
                            updateRegisterButtons();
                        }
                    }
                    if (request == Common.BackplaneCallbackType.REGISTER || request == Common.BackplaneCallbackType.UNREGISTER) {
                        registering = false;
                        if (result == Common.BackplaneResult.SUCCESS) {
                            updateRegisterButtons();
                        } else {
                            Toast.makeText(getBaseContext(), "Backplane register / unregister failed", Toast.LENGTH_SHORT).show();
                        }
                    } else if (request == Common.BackplaneCallbackType.DEVICE_UNREGISTERED) {
                        // Unregister a different device
                        Toast.makeText(getBaseContext(),  (result == Common.BackplaneResult.SUCCESS) ? R.string.unregister_success : R.string.unregister_fail, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    };
}
