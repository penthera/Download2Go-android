/*
    Created by Penthera.
    Copyright © 2020 penthera. All rights reserved.

    This source file contains a very basic example showing how to use the Penthera Download2Go SDK.
    Pay close attention to code comments marked IMPORTANT
*/
package com.penthera.download2go8;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This component receives progress events in the client app whilst files are downloading.
 * Notifications can be received for events and for basic downloader operations like starting and
 * stopping downloads.
 *
 * Filter the notifications required here by adding them to the intent-filter for this
 * component in the application manifest.
 *
 * Here we demonstrate using the received notifications to send a sample foreground notification
 * back to display via the service as the default foreground notification. This approach can be used
 * for all foreground notification updates, but a more efficient method is to register a notification
 * provider with the service which will enable generation of the foreground notifications within the
 * service process directly. That approach is also demonstrated within the ServiceStarter class.
 */
public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context aContext, Intent aIntent) {
        ServiceStarter.updateNotification( aContext, aIntent);
    }
}
