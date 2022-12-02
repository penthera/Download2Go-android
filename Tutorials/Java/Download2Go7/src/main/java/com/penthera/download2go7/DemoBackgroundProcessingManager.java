package com.penthera.download2go7;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.client.AssetNoLongerAvailableException;
import com.penthera.virtuososdk.client.IAssetManager;
import com.penthera.virtuososdk.client.IBackgroundProcessingManager;
import com.penthera.virtuososdk.client.IDASHManifestRenditionSelector;
import com.penthera.virtuososdk.client.IHLSManifestRenditionSelector;
import com.penthera.virtuososdk.client.IIdentifier;
import com.penthera.virtuososdk.client.IManifestParserObserver;
import com.penthera.virtuososdk.client.IPrepareURLObserver;
import com.penthera.virtuososdk.client.ISegment;
import com.penthera.virtuososdk.client.ISegmentedAsset;
import com.penthera.virtuososdk.client.ads.IClientSideAdsParserProvider;
import com.penthera.virtuososdk.client.ads.IVirtuosoAdParserObserver;
import com.penthera.virtuososdk.client.ads.IVirtuosoAdUrlResolver;
import com.penthera.virtuososdk.client.autodownload.IPlaylistAssetProvider;
import com.penthera.virtuososdk.client.autodownload.VirtuosoPlaylistAssetItem;
import com.penthera.virtuososdk.client.builders.HLSAssetBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
        return new DemoAdsParserProvider();
    }

    @Override
    public IPlaylistAssetProvider getPlaylistProvider() {
        return new DemoPlaylistAssetProvider();
    }

	@Override
    public IPrepareURLObserver getSegmentPrepareObserver() {
        return null;
    } 

    private static class DemoPlaylistAssetProvider implements IPlaylistAssetProvider{

        @Override
        public VirtuosoPlaylistAssetItem getAssetParamsForAssetId(String assetId) throws AssetNoLongerAvailableException {

            if(!TextUtils.isEmpty(assetId)){

                Pair<String,String> assetInfo = MainActivity.ASSET_DETAIL_MAP.get(assetId);
                URL assetUrl;



                if(assetInfo != null){

                    try{
                        assetUrl = new URL(assetInfo.second);
                    } catch (MalformedURLException e) {
                       throw new AssetNoLongerAvailableException(assetId);
                    }
                    HLSAssetBuilder.HLSAssetParams params = new HLSAssetBuilder()
                            .assetId(assetId)
                            .addToQueue(true)
                            .manifestUrl(assetUrl)
                            .withMetadata(assetInfo.first)
                            .desiredVideoBitrate(Integer.MAX_VALUE)
                            .build();

                    return new VirtuosoPlaylistAssetItem(Common.PlaylistDownloadOption.DOWNLOAD, params);
                }


            }
            //item not found  try again later
            return new VirtuosoPlaylistAssetItem(Common.PlaylistDownloadOption.TRY_AGAIN_LATER, null);
        }

        @Override
        public IIdentifier didProcessAssetForPlaylist(IIdentifier iIdentifier, String playlistsName) {
            return iIdentifier;
        }
    }



    private static class DemoManifestParserObserver implements IManifestParserObserver{

        @Override
        public String didParseSegment(ISegmentedAsset iSegmentedAsset, ISegment iSegment) {
            //this method can be used to modify the url for each segment prior to download.
            //A default implementation will return the existing remote path of the segment
            return iSegment.getRemotePath();
        }

        @Override
        public void willAddToQueue(ISegmentedAsset iSegmentedAsset, IAssetManager iAssetManager, Context context) {

        }
    }

    private static class DemoAdsParserProvider implements IClientSideAdsParserProvider{

        @NonNull
        @Override
        public List<IVirtuosoAdParserObserver> getAdParserObservers() {
            return new ArrayList<>();
        }

        @NonNull
        @Override
        public List<IVirtuosoAdUrlResolver> getAdUrlResolvers() {
            return new ArrayList<>();
        }
    }
}
