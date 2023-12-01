/*
    Created by Penthera on 10/10/19.
    Copyright © 2019 penthera. All rights reserved.

    This source file contains a very basic example showing how to use the Penthera Download2Go SDK.
    Pay close attention to code comments marked IMPORTANT
*/
package com.penthera.download2go1_6;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.penthera.virtuososdk.client.IAssetManager;
import com.penthera.virtuososdk.client.IIdentifier;
import com.penthera.virtuososdk.client.ISegmentedAsset;
import com.penthera.virtuososdk.client.Observers;
import com.penthera.virtuososdk.client.Virtuoso;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AssetDetailActivity extends AppCompatActivity {

    public final static String ASSET_ID_KEY = "asset_id";

    private Virtuoso virtuoso;
    IAssetManager assetManager;

    private AssetQueueObserver assetQueueObserver;

    ISegmentedAsset asset;

    String assetId = null;

    private TextView titleView;
    private TextView uuidView;
    private TextView idView;
    private TextView fileTypeView;
    private TextView expectedSizeView;
    private TextView currentSizeView;
    private TextView durationView;
    private TextView statusView;
    private TextView pathView;
    private TextView playbackURLView;
    private TextView segmentCountView;
    private TextView firstPlayView;

    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_activity);

        titleView = findViewById(R.id.assetTitleValue);
        uuidView = findViewById(R.id.uuidValue);
        idView = findViewById(R.id.idValue);
        fileTypeView = findViewById(R.id.fileTypeValue);
        expectedSizeView = findViewById(R.id.expectedSizeValue);
        currentSizeView = findViewById(R.id.currentSizeValue);
        durationView = findViewById(R.id.durationValue);
        statusView = findViewById(R.id.statusValue);
        pathView = findViewById(R.id.pathValue);
        playbackURLView = findViewById(R.id.playbackValue);
        segmentCountView = findViewById(R.id.segmentCountValue);
        firstPlayView = findViewById(R.id.firstPlayValue);

        findViewById(R.id.btn_play).setOnClickListener(v -> playAsset());

        findViewById(R.id.btn_delete).setOnClickListener(v -> {
            if (asset != null) {
                assetManager.delete(asset);
            }
            finish();
        });

        // Initialise the SDK
        initVirtuosoSDK(savedInstanceState);

        if (savedInstanceState != null) {
            assetId = savedInstanceState.getString(ASSET_ID_KEY);
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        // Resume the Download2Go SDK on activity resume
        virtuoso.onResume();
        virtuoso.addObserver(assetQueueObserver);


        if (assetId != null && assetManager != null) {
            List<IIdentifier> assets = assetManager.getByAssetId(assetId);
            if (assets != null && assets.size() > 0) {
                asset = (ISegmentedAsset) assets.get(0);
            }
        }
        updateUI();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Pause the Download2Go SDK on activity pause
        virtuoso.onPause();
        virtuoso.removeObserver(assetQueueObserver);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ASSET_ID_KEY, assetId);
    }

    public void initVirtuosoSDK(Bundle savedInstanceState) {
        // An alternative here is to create a singleton instance somewhere like the Application class
        // and share it between Activities.
        virtuoso = new Virtuoso(this);
        assetManager = virtuoso.getAssetManager();

        if (assetId == null) {
            Intent intent = getIntent();
            assetId = intent.getStringExtra(ASSET_ID_KEY);
        }

        List<IIdentifier> assets = assetManager.getByAssetId(assetId);
        if (assets != null && assets.size() > 0) {
            asset = (ISegmentedAsset) assets.get(0);
        }

        // set up observer
        assetQueueObserver = new AssetQueueObserver(this);
    }

    private void updateUI() {
        if (asset != null) {
            titleView.setText(asset.getMetadata());
            uuidView.setText(asset.getUuid());
            idView.setText(asset.getAssetId());
            fileTypeView.setText(fileTypeFromId(asset.segmentedFileType()));
            expectedSizeView.setText(String.format(Locale.getDefault(),"%.2f MB", asset.getExpectedSize()/1048576.00));
            currentSizeView.setText(String.format(Locale.getDefault(),"%.2f MB", asset.getCurrentSize()/1048576.00));
            durationView.setText(String.format(Locale.getDefault(),"%d seconds", asset.getDuration()));
            statusView.setText(MainActivity.getStatusText(this,asset.getDownloadStatus()));
            pathView.setText(asset.getLocalBaseDir());
            try {
                URL url = asset.getPlaybackURL();
                if (url != null) {
                    playbackURLView.setText(url.toString());
                } else {
                    playbackURLView.setText(R.string.unavailable);
                }
            } catch (MalformedURLException mue) {
                playbackURLView.setText(R.string.unavailable);
            }
            segmentCountView.setText(Integer.toString(asset.getTotalSegments()));
            long firstPlayTime = asset.getFirstPlayTime();
            if (firstPlayTime > 0) {
                firstPlayView.setText(dateFormatter.format(new Date(firstPlayTime*1000)));
            } else {
                firstPlayView.setText(R.string.not_yet_played);
            }
        }
    }

    private String fileTypeFromId(int fileTypeCode) {
        String fileTypeText = getString(R.string.unknown_type);
        switch (fileTypeCode) {
            case ISegmentedAsset.SEG_FILE_TYPE_HLS:
                fileTypeText = getString(R.string.hls_type);
                break;
            case ISegmentedAsset.SEG_FILE_TYPE_MPD:
                fileTypeText = getString(R.string.mpd_type);
                break;
        }
        return fileTypeText;
    }

    private void playAsset() {
        if (asset != null) {
            VideoPlayerActivity.playVideoDownload(this, asset);
        }
    }

    /**
     * This class observes the SDK download queue and provides updates during the download process.
     */
    static class AssetQueueObserver implements Observers.IQueueObserver {

        private AssetDetailActivity activity;

        public AssetQueueObserver(AssetDetailActivity activity) {
            this.activity = activity;
        }

        @Override
        public void engineStartedDownloadingAsset(IIdentifier asset) {
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
            if (identifier instanceof ISegmentedAsset) {
                final ISegmentedAsset asset = (ISegmentedAsset) identifier;
                String assetId = asset.getAssetId();

                if (!TextUtils.isEmpty(assetId) && assetId.equals(activity.assetId)) {
                    activity.runOnUiThread(() -> {
                        activity.asset = asset;
                        activity.updateUI();
                    });
                }
            }
        }
    }
}
