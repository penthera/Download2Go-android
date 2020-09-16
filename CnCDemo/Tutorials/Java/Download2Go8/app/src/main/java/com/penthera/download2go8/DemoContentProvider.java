/*
    Created by Penthera.
    Copyright © 2020 penthera. All rights reserved.

    This source file contains a very basic example showing how to use the Penthera Download2Go SDK.
    Pay close attention to code comments marked IMPORTANT
*/
package com.penthera.download2go8;

import com.penthera.virtuososdk.database.impl.provider.VirtuosoSDKContentProvider;

/**
 * This class sets the authority within the content provider. It also enables the Virtuoso service
 * to discover the clients database via the content provider framework.
 *
 * The authority defined in this class must match the authority defined in the content provider
 * entry in the manifest. It should also match the manifest meta-data item com.penthera.virtuososdk.client.pckg
 * which is used to build Uris to connect to the content provider in the client libraries.
 */
public class DemoContentProvider extends VirtuosoSDKContentProvider {

    /**
     * Upon initialization the authority must be set on the content provider.
     */
    static{
        setAuthority("com.penthera.virtuososdk.provider.download2go8");
    }

    /**
     * This returns the authority.
     */
    @Override
    protected String getAuthority() {
        return "com.penthera.virtuososdk.provider.download2go8";
    }
}
