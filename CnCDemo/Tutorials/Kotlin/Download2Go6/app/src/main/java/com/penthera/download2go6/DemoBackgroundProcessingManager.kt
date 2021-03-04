package com.penthera.download2go6

import com.penthera.virtuososdk.client.IBackgroundProcessingManager
import com.penthera.virtuososdk.client.IManifestParserObserver
import com.penthera.virtuososdk.client.IPlaylistAssetProvider
import com.penthera.virtuososdk.client.ads.IClientSideAdsParserProvider
import com.penthera.virtuososdk.client.subscriptions.ISubscriptionsProvider


/**
 * An example background processing manager.
 * This is a factory class which is responsible for providing instances of provider classes to
 * the download service within the service process. The provider classes can be called to fetch
 * details of client side advert definitions, subscriptions and playlists.
 *
 * This class is registered in the android manifest using meta-data 'com.penthera.virtuososdk.background.manager.impl'
 */
class DemoBackgroundProcessingManager :
    IBackgroundProcessingManager {
    override fun getManifestParserObserver(): IManifestParserObserver? {
        return null
    }

    override fun getClientSideAdsParserProvider(): IClientSideAdsParserProvider {
        return DemoClientAdsProvider.getInstance()
    }

    override fun getSubscriptionsProvider(): ISubscriptionsProvider? {
        return null
    }

    override fun getPlaylistProvider(): IPlaylistAssetProvider? {
        return null
    }
	
	override fun getSegmentPrepareObserver(): IPrepareURLObserver? {
        return null;
    }
}
