package com.penthera.download2go7

import android.content.Context
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.*
import com.penthera.virtuososdk.client.ads.IClientSideAdsParserProvider
import com.penthera.virtuososdk.client.ads.IVirtuosoAdParserObserver
import com.penthera.virtuososdk.client.ads.IVirtuosoAdUrlResolver
import com.penthera.virtuososdk.client.autodownload.IPlaylistAssetProvider
import com.penthera.virtuososdk.client.autodownload.VirtuosoPlaylistAssetItem
import com.penthera.virtuososdk.client.builders.HLSAssetBuilder
import java.net.URL

class BackgroundProcessingManager : IBackgroundProcessingManager {
    override fun getManifestParserObserver(): IManifestParserObserver {
        return ManifestParseObserver()
    }

	override fun getDASHManifestRenditionSelector(): IDASHManifestRenditionSelector? {
		return null
	}
	
	override fun getHLSManifestRenditionSelector(): IHLSManifestRenditionSelector? {
		return null
	}
	
    override fun getClientSideAdsParserProvider(): IClientSideAdsParserProvider {
        return AdsProvider();
    }

    override fun getPlaylistProvider(): IPlaylistAssetProvider {
        return PlaylistProvider()
    }

	override fun getSegmentPrepareObserver(): IPrepareURLObserver? {
        return null;
    }

    class ManifestParseObserver : IManifestParserObserver{
        override fun willAddToQueue(asset: ISegmentedAsset?, assetManager: IAssetManager?, context: Context?) {
            //this implementation does nothing in this callback
        }

        override fun didParseSegment(asset: ISegmentedAsset?, segment: ISegment?): String {
            //this method can be used to modify the url for each segment prior to download.
            //A default implementation will return the existing remote path of the segment
           return segment!!.remotePath
        }

    }

    //the IClientSideAdsParserProvider is not used in this Tutorial so an empty implementation is used here
    class AdsProvider : IClientSideAdsParserProvider{
        override fun getAdUrlResolvers(): MutableList<IVirtuosoAdUrlResolver> {
           return mutableListOf()
        }

        override fun getAdParserObservers(): MutableList<IVirtuosoAdParserObserver> {
            return mutableListOf()
        }
    }

    class PlaylistProvider : IPlaylistAssetProvider{

        override fun getAssetParamsForAssetId(assetId: String): VirtuosoPlaylistAssetItem {
            //this method is called by the SDK to request the asset parameters for an asset ID that is associated with a playlist
            //this is called when preparing to add a new item to the download queue for a playlist

            var ret :VirtuosoPlaylistAssetItem
            val assetInfo = MainActivity.ASSET_MAP[assetId];

            assetInfo?.let{
                val params = HLSAssetBuilder().apply {
                    assetId(assetId)
                    manifestUrl(URL(it.second))
                    addToQueue(true)
                    desiredVideoBitrate(Int.MAX_VALUE)//specify a bitrate for desired video quality Integer.MAX_VALUE for largest available
                    withMetadata(it.first)//user specified descriptive text for the asset.  Here we supply a title.
                }.build()
                return VirtuosoPlaylistAssetItem(Common.PlaylistDownloadOption.DOWNLOAD,params)

            }

            return VirtuosoPlaylistAssetItem(Common.PlaylistDownloadOption.TRY_AGAIN_LATER, null)
        }

        override fun didProcessAssetForPlaylist(asset: IIdentifier?, playlistName: String?): IIdentifier {
            //this method notifies the app that an asset has been downloaded for a playlist
            return asset!!;
        }

    }
}