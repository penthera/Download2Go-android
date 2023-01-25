package com.penthera.download2go6

import android.util.Log
import com.penthera.virtuososdk.client.ads.IClientSideAdsParserProvider
import com.penthera.virtuososdk.client.ads.IVirtuosoAdParserObserver
import com.penthera.virtuososdk.client.ads.IVirtuosoAdUrlResolver
import java.lang.ref.WeakReference


/**
 *
 */
class DemoClientAdsProvider : IClientSideAdsParserProvider {
    override fun getAdParserObservers(): List<IVirtuosoAdParserObserver> {
        // The AdParserObserver will receive a parser error call if the parser cannot successfully complete
        // parsing of the ad definitions.
        // This provider is being instantiated within the SDK Service process, so it cannot directly inform
        // the user or action the UI. It could be used however for error tracking, ad correction, immediate
        // refresh, or using android OS cross-process messaging to inform the client application.
        return listOf(IVirtuosoAdParserObserver { asset, error, message ->
            Log.e(
                "ClientAdsProvider",
                "Failed to process advertising definitions for asset"
            )
        })
    }

    override fun getAdUrlResolvers(): List<IVirtuosoAdUrlResolver> {
        // The SDK can be provided with multiple advert resolvers which will provide adverts definitions from
        // alternative networks. It will iterate the resolvers in order until one provides a URL for adverts.
        // In this case we only provide one.
        return listOf(DemoAdDefinitionsResolver())
    }

    companion object {
        private var instance: WeakReference<DemoClientAdsProvider>? = null

        @Synchronized
        fun getInstance(): DemoClientAdsProvider {
            var i: DemoClientAdsProvider? = null

            if (instance != null) {
                i = instance!!.get()
            }
            if (i == null) {
                i = DemoClientAdsProvider()
                instance =
                    WeakReference(i)
            }
            return i
        }
    }
}