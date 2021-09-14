package com.penthera.download2go6;

import com.penthera.virtuososdk.client.IBackgroundProcessingManager;
import com.penthera.virtuososdk.client.IManifestParserObserver;
import com.penthera.virtuososdk.client.IPrepareURLObserver;
import com.penthera.virtuososdk.client.ads.IClientSideAdsParserProvider;
import com.penthera.virtuososdk.client.autodownload.IPlaylistAssetProvider;

/**
 * An example background processing manager.
 * This is a factory class which is responsible for providing instances of provider classes to
 * the download service within the service process. The provider classes can be called to fetch
 * details of client side advert definitions, subscriptions and playlists.
 *
 * This class is registered in the android manifest using meta-data 'com.penthera.virtuososdk.background.manager.impl'
 */
public class DemoBackgroundProcessingManager implements IBackgroundProcessingManager {

    @Override
    public IManifestParserObserver getManifestParserObserver() {
        return null;
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
        return DemoClientAdsProvider.getInstance();
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
