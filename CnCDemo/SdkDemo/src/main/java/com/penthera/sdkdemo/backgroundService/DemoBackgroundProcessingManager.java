package com.penthera.sdkdemo.backgroundService;

import com.penthera.virtuososdk.client.IBackgroundProcessingManager;
import com.penthera.virtuososdk.client.IDASHManifestRenditionSelector;
import com.penthera.virtuososdk.client.IHLSManifestRenditionSelector;
import com.penthera.virtuososdk.client.IManifestParserObserver;
import com.penthera.virtuososdk.client.IPrepareURLObserver;
import com.penthera.virtuososdk.client.autodownload.IPlaylistAssetProvider;
import com.penthera.virtuososdk.client.ads.IClientSideAdsParserProvider;

/**
 *
 */
public class DemoBackgroundProcessingManager implements IBackgroundProcessingManager {
    @Override
    public IManifestParserObserver getManifestParserObserver() {
        return new DemoManifestParserObserver();
    }

    @Override
    public IDASHManifestRenditionSelector getDASHManifestRenditionSelector() {
        return null;
    }

    @Override
    public IHLSManifestRenditionSelector getHLSManifestRenditionSelector() {
        return null;
    }

    @Override
    public IClientSideAdsParserProvider getClientSideAdsParserProvider() {
        return null;
    }

    @Override
    public IPlaylistAssetProvider getPlaylistProvider() {
        return null;
    }

    @Override
    public IPrepareURLObserver getSegmentPrepareObserver() {
        return null;
    }
}
