package com.penthera.sdkdemokotlin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.penthera.sdkdemokotlin.activity.MainActivity
import com.penthera.sdkdemokotlin.notification.NotificationFactory
import com.penthera.sdkdemokotlin.notification.ServiceForegroundNotificationProvider
import com.penthera.virtuososdk.service.VirtuosoServiceStarter

/**
 *
 */
class ServiceStarter : VirtuosoServiceStarter() {

    companion object {

        private var notificationChannel: NotificationChannel? = null
        private var currentNotification: Notification? = null

        val CHANNEL_ID = "VIRTUOSO_DEMO_CHANNEL_ID"
        val CHANNEL_NAME = "SdkDemo Background Activity"
        val CHANNEL_DESCRIPTION = "Indicates activity this application will perform when the application is not open"


        /**
         * This convenience method shows how to call the underlying method to force the SDK
         * to update the foreground notification stored within the download service.
         * This method causes an asynchronous process which will call back into the
         * getForegroundServiceNotification() method and then send the resulting notification
         * into the download service process.
         */
        fun updateNotification(aContext: Context, aIntent: Intent) {
            updateNotification(aContext, aIntent, ServiceStarter::class.java)
        }
    }

    // This is a helper class which is used in the demo for creating the notifications
    private val notificationFactory: NotificationFactory

    init {
        notificationFactory = NotificationFactory("Penthera Demo")
    }

    /**
     * This method will be called by the framework to request the generation of a notification,
     * to be displayed as the foreground notification for the download service. The notification
     * returned from this method will be parcelled and passed over the process boundary into the
     * service. This imposes some limitations on the size of the notification returned, as it must
     * be suitable to pass over the boundary.
     *
     * In this demo we use a separate notification factory class to create foreground notifications
     * for both these notifications and the ones created directly in the service
     * (see ServiceForegroundNotificationProvider). In a more complex application this may not be shared.
     */
    override fun getForegroundServiceNotification(context: Context?, forIntent: Intent?): Notification {

        val notification = notificationFactory.getNotification(context!!, forIntent)
        if (notification != null) {
            currentNotification = notification
        }

        return currentNotification!!
    }

    override fun getForegroundServiceNotificationProvider(): Class<*> {
        // This method is deprecated. The class used from within the service process to generate
        // all notifications relating to asset downloads should be registered in the Android Manifest
        // using the meta-data name "com.penthera.virtuososdk.notification.provider.impl"
        return ServiceForegroundNotificationProvider::class.java
    }

}