package com.penthera.download2go1_3

import com.penthera.virtuososdk.database.impl.provider.VirtuosoSDKContentProvider

/**
 * This class sets the authority within the content provider. It also enables the Virtuoso service
 * to discover the clients database via the content provider framework.
 *
 * The authority defined in this class must match the authority defined in the content provider
 * entry in the manifest. It should also match the manifest meta-data item com.penthera.virtuososdk.client.pckg
 * which is used to build Uris to connect to the content provider in the client libraries.
 */
class DemoContentProvider : VirtuosoSDKContentProvider() {

    /**
     * Upon initialization the authority must be set on the content provider.
     */
    init {
        setAuthority("com.penthera.virtuososdk.provider.download2go1_3")
    }

    /**
     * This returns the authority.
     */
    override fun getAuthority(): String {
        return "com.penthera.virtuososdk.provider.download2go1_3"
    }
}