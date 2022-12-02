package com.penthera.download2go6

import com.penthera.virtuososdk.client.IAsset
import com.penthera.virtuososdk.client.ads.IVirtuosoAdUrlResolver
import java.net.MalformedURLException
import java.net.URL


/**
 * The Advertising Definitions Resolver provides urls for the client Ad definitions for assets.
 * Multiple resolvers may be registered to the SDK if required and the SDK will iterate through the
 * registered resolvers in order. Therefore a resolver may return null for the URL if it cannot
 * provide the adverts.
 *
 * The ad network name is used in generating the ad impression data for reporting.
 */
class DemoAdDefinitionsResolver : IVirtuosoAdUrlResolver {
    @Throws(MalformedURLException::class)
    override fun getUrlForAsset(iAsset: IAsset): URL {
        // Fetch the url for the VAST/VMAP advertising definitions for an asset.
        // In this case report a Google test url which will return a VMAP document containing pre-roll, mid-roll, and post-roll test adverts.
        return URL("https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ar%3Dpremidpostpod&cmsid=496&vid=short_onecue&correlator=")
    }

    override fun getAdNetworkName(): String {
        return "GoogleDemoAdNetwork"
    }
}