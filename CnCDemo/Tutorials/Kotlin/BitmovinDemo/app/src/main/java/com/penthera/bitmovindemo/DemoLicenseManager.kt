/*
    Created by Penthera.
    Copyright © 2020 penthera. All rights reserved.

    This source file contains a very basic example showing how to use the Penthera Download2Go SDK.
    Pay close attention to code comments marked IMPORTANT
*/
package com.penthera.bitmovindemo

import com.penthera.virtuososdk.client.drm.LicenseManager

// Used if implementing executeKeyRequest
/**
 * A basic license manager implementation which provides the widevine url.
 *
 * Note: For this LicenseManager implementation to be used the class must be specified in the manifest metadata
 * e.g.:
 * <meta-data tools:replace="android:value" android:name="com.penthera.virtuososdk.license.manager.impl" android:value="com.penthera.download2go8.DemoLicenseManager"></meta-data>
 * for the above to work you will need to add the tools name space
 * xmlns:tools="http://schemas.android.com/tools"
 */
class DemoLicenseManager : LicenseManager() {
    override fun getLicenseAcquistionUrl(): String {
        // This demonstration uses the Google UAT server
        return "https://proxy.uat.widevine.com/proxy"
    }



    // Override the following to perform more complex manipulation of the request and response,
    // for instance if you need to parse a json response to retrieve the key response in base64
    // You will also need to implement a post request method to support this implementation.
    /*
    public byte[] executeKeyRequest(UUID uuid, MediaDrm.KeyRequest request) throws IOException {
        String url = getLicenseAcquistionUrl();
        return post(url, request.getData(), getKeyRequestProperties());
    }
    */
}