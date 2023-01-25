package com.penthera.bitmovindemo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.penthera.common.Common.Notifications.NOTIFICATION_EVENT_TAG
import com.penthera.virtuososdk.client.IAsset
import com.penthera.virtuososdk.client.IForegroundNotificationProvider

/**
 * This implements the IForegroundNotificationProvider interface, which enables notifications to be built
 * within the service process so they do not need to cross the process boundary. Notifications created in the service starter
 * would be passed in an intent over the process boundary into the service process, and this introduces limitations due to the
 * size of object that can be passed over that boundary. This object is instantiated within the service process instead,
 * so no limitations exist on the way the notifications are used, but the process should be considered. Any resources which
 * are required to generate the notification will need to be loaded within this process as they cannot be shared with the process
 * which contains the UI.
 *
 * This example uses the same Factory class to generate notifications. Please remember this is a
 * separate instance of the NotificationFactory, running within a different process than the traditional one in ServiceStarter.
 */
class ServiceForegroundNotificationProvider : IForegroundNotificationProvider{

    private var context: Context? = null
    private var notificationChannel: NotificationChannel? = null
    private var channelId: String? = null
    private var currentNotification: Notification? = null


    /**
     * This method provides a place where you can setup any necessary resources when the provider is instantiated.
     * This is called directly after construction.
     * @param context A context from the service which can be used for access to Android resources.
     */
    override fun prepareNotificationProvider(context: Context?) {
        this.context = context
    }

    /**
     * This method allows the client to select for which intents the service will request a new notification.
     * @param context A context object
     * @param reasonIntent The updated intent from the service
     * @return true if the notification should be updated, false otherwise.
     */
    override fun shouldUpdateForegroundServiceNotificationOnIntent(context: Context?, reasonIntent: Intent?): Boolean {

        if(context != null) {
            reasonIntent?.let {
                val action = it.action
                Log.d("ForegroundNotification", "got action: $action")
                action?.let{// Do not update progress for events
                    return !action.contains(NOTIFICATION_EVENT_TAG)
                }
            }
        }
        return false
    }

    /**
     * This method provides the notification provider with a copy of the original launch notification which was
     * passed to the service upon startup via the ServiceStarter. This can be used to retrieve the channel details and
     * notification ID, which enables the manipulation of the same notification on each request.
     * @param notification The existing notification
     */
    override fun setExistingNotificationForReuse(notification: Notification?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = notification?.channelId
            if (currentNotification != null && notificationChannel != null && currentNotification?.channelId == channelId) {
                return
            }
            val manager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationChannel = manager.getNotificationChannel(channelId)
            this.channelId = channelId
            currentNotification = notification
        } else {
            currentNotification = notification
        }
    }

    /**
     * Return a new notification, given the file and intent. Always return a notification.
     * @param context A context to use in processing
     * @param file The asset for which the notification is being created
     * @param reasonIntent The intent for which the notification is being created
     * @return The notification
     */
    override fun getForegroundServiceNotification(context: Context?, file: IAsset?, reasonIntent: Intent?): Notification {
        if (reasonIntent == null) return currentNotification!!

        currentNotification = NotificationFactory("Download2Go 8").getNotification(context!!, reasonIntent )

        return currentNotification!!
    }
}