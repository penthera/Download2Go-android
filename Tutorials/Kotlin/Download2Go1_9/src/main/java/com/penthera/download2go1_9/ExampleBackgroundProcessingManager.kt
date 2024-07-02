package com.penthera.download2go1_9

import com.penthera.virtuososdk.client.IBackgroundProcessingManager
import com.penthera.virtuososdk.client.IDASHManifestRenditionSelector
import com.penthera.virtuososdk.client.IHLSManifestRenditionSelector
import com.penthera.virtuososdk.client.IManifestParserObserver
import com.penthera.virtuososdk.client.IPrepareURLObserver
import com.penthera.virtuososdk.client.ads.IClientSideAdsParserProvider
import com.penthera.virtuososdk.client.autodownload.IPlaylistAssetProvider

/**
 * The background processing manager is the registration point for a number of hooks that
 * can be used to customize the download process. Note that this is loaded into the manifest
 * parser and downloader which are both located within the service process. If you wish to
 * debug any of these methods then you will need to connect the debugger to that process.
 */
class ExampleBackgroundProcessingManager : IBackgroundProcessingManager {

    override fun getDASHManifestRenditionSelector(): IDASHManifestRenditionSelector? {
        return ExampleCustomDASHTrackSelector.getInstance()
    }

    // Unused in this example, enables altering segment urls during the parsing stage
    override fun getManifestParserObserver(): IManifestParserObserver? {
        return null
    }

    // Unused in this example, enables altering track selection for HLS manifests in the same manner
    // as the DASH example above
    override fun getHLSManifestRenditionSelector(): IHLSManifestRenditionSelector? {
        return null
    }

    // Unused in this example, enables providing parser observers and url resolvers for client side
    // Ad document parsing (VAST/VMAP)
    override fun getClientSideAdsParserProvider(): IClientSideAdsParserProvider? {
        return null
    }

    // Unused in this example, provides a playlist provider which implements the logic for scheduling
    // playlist assets upon the deletion of previous assets.
    override fun getPlaylistProvider(): IPlaylistAssetProvider? {
        return null
    }

    // Unused in this example, provides a hook to alter segment urls just ahead of segment download.
    override fun getSegmentPrepareObserver(): IPrepareURLObserver? {
        return null
    }
}