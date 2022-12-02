package com.penthera.download2go1_7

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.penthera.virtuososdk.client.push.VirtuosoFCMMessageHandler


/**
 * This class provides an empty example of integrating the Virtuso SDK FCM messaging
 * functionality in cases where it is not practical to use or extend the included
 * `com.penthera.virtuososdk.client.subscriptions.FcmMessagingService`.
 * In these cases, applications can implement their own custom FCM messaging components and
 * forward messages intended to be consumed by the Virtuoso SDK via
 * the `com.penthera.virtuososdk.client.push.VirtuosoFCMMessageHandler`.
 */
class DemoCustomPushMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {


        //check to see if push should be handled by the VirtusosSDK and hand the message off
        if(VirtuosoFCMMessageHandler(applicationContext).handleMessage(remoteMessage)) {
            return //the message has been consumed by the virtuoso SDK
        }

        //add custom message handling here
    }

    override fun onNewToken(s: String) {

        //Do any custom token registration appropriate for this app

        //provide new registration token to the VirtuosoSDK
        try {
            VirtuosoFCMMessageHandler(applicationContext).handleNewToken(s)
        }
        catch(e: VirtuosoFCMMessageHandler.InvalidFCMTokenException){
            e.printStackTrace()
        }
    }
}
