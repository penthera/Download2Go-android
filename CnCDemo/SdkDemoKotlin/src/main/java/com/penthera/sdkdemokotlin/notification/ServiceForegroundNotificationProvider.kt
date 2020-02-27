package com.penthera.sdkdemokotlin.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.penthera.sdkdemokotlin.ServiceStarter
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.IAsset
import com.penthera.virtuososdk.client.IForegroundNotificationProvider

/**
 *
 */
class ServiceForegroundNotificationProvider : IForegroundNotificationProvider{

    private var context: Context? = null
    private var notificationChannel: NotificationChannel? = null
    private var channelId: String? = null
    private var currentNotification: Notification? = null



    override fun prepareNotificationProvider(context: Context?) {
        this.context = context
    }

    override fun shouldUpdateForegroundServiceNotificationOnIntent(context: Context?, reasonIntent: Intent?): Boolean {
        val action = reasonIntent!!.action
        Log.d("ForegroundNotification" , "got action: $action")
        if (context == null ||  action == null) {
            return false
        }

        // Do not update progress for events
        return !action.contains(Common.Notifications.NOTIFICATION_EVENT_TAG)
    }

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

    override fun getForegroundServiceNotification(context: Context?, file: IAsset?, reasonIntent: Intent?): Notification {
        if (reasonIntent == null) return currentNotification!!

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && channelId == null) {
            val manager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationChannel = NotificationChannel(ServiceStarter.CHANNEL_ID, ServiceStarter.CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            notificationChannel?.description = ServiceStarter.CHANNEL_DESCRIPTION
            notificationChannel?.enableLights(false)
            notificationChannel?.enableVibration(false)
            manager.createNotificationChannel(notificationChannel!!)
            channelId = notificationChannel?.id
        }

        currentNotification = NotificationFactory("SdkKotlinDemo").getNotification(context!!, reasonIntent )

        return currentNotification!!
    }
}