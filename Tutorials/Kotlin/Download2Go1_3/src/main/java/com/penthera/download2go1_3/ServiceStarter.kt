package com.penthera.download2go1_3

import android.app.Notification
import android.content.Context
import android.content.Intent
import com.penthera.virtuososdk.service.VirtuosoServiceStarter

/**
 *
 */
class ServiceStarter : VirtuosoServiceStarter() {

    companion object {

        private var currentNotification: Notification? = null

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
    private val notificationFactory: NotificationFactory = NotificationFactory("Download2Go1.3")

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
     *
     * @param context A context which can be used in generating the notification
     * @param forIntent The intent for which to create a notification
     * @return The new notification
     */
    override fun getForegroundServiceNotification(context: Context?, forIntent: Intent?): Notification {

        val notification = notificationFactory.getNotification(context!!, forIntent)
        if (notification != null) {
            currentNotification = notification
        }

        return currentNotification!!
    }

    override fun getForegroundServiceNotificationProvider(): Class<*> {
        // Returning this class definition causes the service to instantiate and use the class
        // from within the service process to generate all notifications relating to asset downloads.
        // Returning null results in the classic SDK behaviour where all notifications are generated
        // and delivered to the service via the service starter.
        return ServiceForegroundNotificationProvider::class.java
    }

}