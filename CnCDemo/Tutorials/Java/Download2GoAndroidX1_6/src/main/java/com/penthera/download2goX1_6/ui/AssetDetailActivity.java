/*
    Created by Penthera on 26/03/21.
    Copyright © 2021 penthera. All rights reserved.

    This source file contains a very basic example showing how to use the Penthera Download2Go SDK.
    Pay close attention to code comments marked IMPORTANT
*/
package com.penthera.download2goX1_6.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.penthera.download2goX1_6.R;
import com.penthera.download2goX1_6.databinding.DetailActivityBinding;
import com.penthera.download2goX1_6.viewmodel.AssetViewModel;
import com.penthera.virtuososdk.androidxsupport.VirtuosoLiveDataFactory;
import com.penthera.virtuososdk.client.ISegmentedAsset;
import com.penthera.virtuososdk.client.Virtuoso;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * AssetDetailActivity shows a basic detail view of an asset, primarily demonstrating internal
 * properties of the asset that you would not directly show a user. It is populated and updated
 * using a LiveData component with a very basic ViewModel and data binding.
 */
public class AssetDetailActivity extends AppCompatActivity implements AssetItemCallback {

    public final static String ASSET_ID_KEY = "asset_id";

    // The data binding to the layout
    private DetailActivityBinding activityBinding;

    // Important: The LiveData Factory object from the Penthera SDK enables the SDK to be lifecycle aware. It contains
    // a reference to the underlying Virtuoso object and provides all of the LiveData objects for the SDK.
    private VirtuosoLiveDataFactory virtuosoLiveDataFactory;

    // The Virtuoso object provides the top level interface to the Penthera SDK for all aspects apart from LiveData.
    private Virtuoso virtuoso;

    // The asset Id of the asset to show from the SDK.
    String assetId = null;

    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityBinding = DataBindingUtil.setContentView(this, R.layout.detail_activity);
        activityBinding.setLifecycleOwner(this);
        activityBinding.setCallbacks(this);

        // Initialise the SDK
        initVirtuosoSDK();

        if (savedInstanceState != null) {
            assetId = savedInstanceState.getString(ASSET_ID_KEY);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ASSET_ID_KEY, assetId);
    }

    /**
     * Initialize the Penthera SDK and the ViewModel with SDK LiveData objects
     */
    public void initVirtuosoSDK() {

        // The live data factory must be created and then initialized with a lifecycle.
        virtuosoLiveDataFactory = VirtuosoLiveDataFactory.getInstance();
        virtuoso = virtuosoLiveDataFactory.createVirtuosoWithLifecycle(this, this);

        if (assetId == null) {
            Intent intent = getIntent();
            assetId = intent.getStringExtra(ASSET_ID_KEY);
        }

        AssetViewModel.Factory factory = new AssetViewModel.Factory(getApplication(), virtuosoLiveDataFactory, assetId);

        AssetViewModel model = new ViewModelProvider(this, factory).get(AssetViewModel.class);

        activityBinding.setAssetViewModel(model);
    }

    @Override
    public void onPlay(ISegmentedAsset asset) {
        VideoPlayerActivity.playVideoDownload(this, asset);
    }

    @Override
    public void onDelete(ISegmentedAsset asset) {
        virtuoso.getAssetManager().delete(asset);
        finish();
    }

    @Override
    public void onOpen(ISegmentedAsset asset) {
        // Intentionally blank
    }
}
