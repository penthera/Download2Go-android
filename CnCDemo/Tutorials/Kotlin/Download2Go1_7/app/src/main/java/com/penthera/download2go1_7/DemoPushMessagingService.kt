package com.penthera.download2go1_7

import com.google.firebase.messaging.RemoteMessage
import com.penthera.virtuososdk.client.push.FcmMessagingService


/**
 * This class provides an empty example of intercepting FCM tokens and messages to use with another
 * component which uses push messaging within the application.  If the SDK is the only user of push
 * messaging then this class is not required. The android manifest may be configured to reference the
 * `com.penthera.virtuososdk.client.subscriptions.FcmMessagingService` directly.
 */
class DemoPushMessagingService : FcmMessagingService() {
    override fun onMessageReceived(msg: RemoteMessage) {
        super.onMessageReceived(msg)
        // Message can be passed on to any other services which require to receive FCM push messaging content here
    }

    /** {@inheritDoc}  */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token can be passed to any other services which require to use the FCM push messaging
    }
}
