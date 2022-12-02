package com.penthera.download2go1_7;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.penthera.virtuososdk.client.push.VirtuosoFCMMessageHandler;


/**
 * This class provides an empty example of integrating the Virtusos SDK FCM messaging
 * functionality in cases where it is not practical to use or extend the included
 *<code>com.penthera.virtuososdk.client.subscriptions.FcmMessagingService</code>.
 * In these cases, applications can implement thier own custom FCM messaging components and
 * forward messages intended to be consumed by the Virtuoso SDK via
 * the <code>com.penthera.virtuososdk.client.push.VirtuosoFCMMessageHandler</code>.
 */
public class DemoCustomPushMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {

        if (new VirtuosoFCMMessageHandler(getApplicationContext()).handleMessage(remoteMessage)){
            return;//the message has been consumed by the virtuoso SDK
        }

        //add custom message handling here

    }

    @Override
    public void onNewToken(@NonNull String s) {

        try {
            new VirtuosoFCMMessageHandler(getApplicationContext()).handleNewToken(s);
        } catch (VirtuosoFCMMessageHandler.InvalidFCMTokenException e) {
            e.printStackTrace();
        }
    }
}
