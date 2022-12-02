package com.penthera.download2go1_7;

import com.google.firebase.messaging.RemoteMessage;
import com.penthera.virtuososdk.client.push.FcmMessagingService;

/**
 * This class provides an empty example of intercepting FCM tokens and messages to use with another
 * component which uses push messaging within the application.  If the SDK is the only user of push
 * messaging then this class is not required. The android manifest may be configured to reference the
 * <code>com.penthera.virtuososdk.client.subscriptions.FcmMessagingService</code> directly.
 */
public class DemoPushMessagingService extends FcmMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage msg) {
        super.onMessageReceived(msg);
        // Message can be passed on to any other services which require to receive FCM push messaging content here
    }

    /** {@inheritDoc} */
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Token can be passed to any other services which require to use the FCM push messaging
    }

}
