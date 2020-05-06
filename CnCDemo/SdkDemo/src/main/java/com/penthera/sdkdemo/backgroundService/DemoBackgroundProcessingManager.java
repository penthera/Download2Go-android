package com.penthera.sdkdemo.backgroundService;

import com.penthera.virtuososdk.client.IBackgroundProcessingManager;
import com.penthera.virtuososdk.client.IManifestParserObserver;
import com.penthera.virtuososdk.client.IPlaylistAssetProvider;
import com.penthera.virtuososdk.client.ads.IClientSideAdsParserProvider;
import com.penthera.virtuososdk.client.subscriptions.ISubscriptionsProvider;

/**
 *
 */
public class DemoBackgroundProcessingManager implements IBackgroundProcessingManager {
    @Override
    public IManifestParserObserver getManifestParserObserver() {
        return new DemoManifestParserObserver();
    }

    @Override
    public IClientSideAdsParserProvider getClientSideAdsParserProvider() {
        return null;
    }

    @Override
    public ISubscriptionsProvider getSubscriptionsProvider() {
        return null;
    }

    @Override
    public IPlaylistAssetProvider getPlaylistProvider() {
        return null;
    }
}
