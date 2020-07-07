package com.penthera.download2go6;

import android.util.Log;

import androidx.annotation.NonNull;

import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.ads.IClientSideAdsParserProvider;
import com.penthera.virtuososdk.client.ads.IVirtuosoAdParserObserver;
import com.penthera.virtuososdk.client.ads.IVirtuosoAdUrlResolver;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class DemoClientAdsProvider implements IClientSideAdsParserProvider {

    private static WeakReference<DemoClientAdsProvider> instance = null;

    public static synchronized DemoClientAdsProvider getInstance() {
        DemoClientAdsProvider i = null;
        if (instance != null) {
            i = instance.get();
        }
        if (i == null) {
            i = new DemoClientAdsProvider();
            instance = new WeakReference<>(i);
        }
        return i;
    }

    @NonNull
    @Override
    public List<IVirtuosoAdParserObserver> getAdParserObservers() {
        // The AdParserObserver will receive a parser error call if the parser cannot successfully complete
        // parsing of the ad definitions.
        // This provider is being instantiated within the SDK Service process, so it cannot directly inform
        // the user or action the UI. It could be used however for error tracking, ad correction, immediate
        // refresh, or using android OS cross-process messaging to inform the client application.
        return Collections.singletonList(new IVirtuosoAdParserObserver() {
            @Override
            public void onParserError(IAsset asset, int error, String message) {
                Log.e("ClientAdsProvider", "Failed to process advertising definitions for asset");
            }
        });
    }

    @NonNull
    @Override
    public List<IVirtuosoAdUrlResolver> getAdUrlResolvers() {
        // The SDK can be provided with multiple advert resolvers which will provide adverts definitions from
        // alternative networks. It will iterate the resolvers in order until one provides a URL for adverts.
        // In this case we only provide one.
        return Collections.singletonList(new DemoAdDefinitionsResolver());
    }
}
