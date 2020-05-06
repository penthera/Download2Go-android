package com.penthera.sdkdemokotlin.drm

import com.penthera.virtuososdk.client.drm.LicenseManager
import java.util.*

/**
 *
 */
class DemoLicenseManager : LicenseManager() {

    override fun getLicenseAcquistionUrl(): String {
        return "https://proxy.uat.widevine.com/proxy"
    }

    override fun getKeyRequestProperties(): MutableMap<String, String> {
        val props: MutableMap<String, String> = HashMap()
        props["Content-Type"] = "application/octet-stream"
        props["User-Agent"] = "virtuoso-sdk"
        return props
    }
}