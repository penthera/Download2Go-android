package com.penthera.sdkdemo.push;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.penthera.virtuososdk.client.push.VirtuosoFCMMessageHandler;

/**
 * Demonstrate how to integrate Virtuoso SDK FCM messaging functionality when it is not feasible to
 * subclass the provided SDK FCM messaging classes.
 */
public class DemoCustomFCMService extends FirebaseMessagingService {

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


