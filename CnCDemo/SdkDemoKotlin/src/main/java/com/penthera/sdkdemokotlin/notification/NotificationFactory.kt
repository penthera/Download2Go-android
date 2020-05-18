package com.penthera.sdkdemokotlin.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.activity.MainActivity
import com.penthera.sdkdemokotlin.catalog.ExampleMetaData
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.IAsset
import com.penthera.virtuososdk.client.IAssetManager
import com.penthera.virtuososdk.client.IEvent
import com.penthera.virtuososdk.client.Virtuoso

/**
 *
 */
class NotificationFactory(private val applicationName: String) {

    companion object {
        fun channelId() = "VIRTUOSO_KOTLIN_DEMO_CHANNEL_ID"
        fun channelName() = "Demo Background Activity"
        fun channelDescription() = "Indicates activity this application will perform when the application is not open"

        private val TAG = NotificationFactory::class.java.simpleName
    }

    private var assetManager: IAssetManager? = null
    private var notificationBuilder: Notification.Builder? = null
    private var compatNotificationBuilder: NotificationCompat.Builder? = null

    private val PROGRESS_NOTIFICATION = 0
    private val COMPLETED_NOTIFICATION = 1
    private val STOPPED_NOTIFICATION = 2
    private val PAUSED_NOTIFICATION = 3
    private val RESTART_NOTIFICATION = 4
    private val FAILED_NOTIFICATION = 5

    /**
     * This defines a default intent for the app which can be used if none is provided in the request
     */
    private fun defaultNotificationIntent(context: Context) : Intent {

        val notificationIntent = Intent(context, MainActivity::class.java)
        notificationIntent.action = "foregroundservice.action.ForegroundServiceNotificationAction"
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        return notificationIntent
    }

    fun getNotification(context: Context, intent: Intent?): Notification? {
        // A default intent is used if no intent is delivered in the request.
        val notificationIntent = intent ?: defaultNotificationIntent(context)

        var clientReference: String?

        try {
            val ai = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val b = ai.metaData

            clientReference = b.getString(Common.CLIENT_PACKAGE)

        } catch (e: Exception) {
            throw RuntimeException("cannot retrieve client", e)
        }

        val action = notificationIntent.action
        if (clientReference == null || action == null) {
            return null
        }

        // If the intent action contains the NOTIFICATION_EVENT_TAG, then this is a log event broadcast sent from
        // the SDK analytics system.  For debugging, we might want to post status bar notifications, but in general,
        // these events are used to push SDK analytics events into a custom/3rd party analytics platform, and shouldn't
        // be sent to the status bar. For the purposes of this demo, we'll log them and return null, which prevents the
        // status bar notice from being shown.
        if (action.contains(Common.Notifications.NOTIFICATION_EVENT_TAG)) {

            val event: IEvent? = notificationIntent.getParcelableExtra(Common.Notifications.EXTRA_NOTIFICATION_EVENT)

            event?.let {
                Log.d(TAG, "Got event named(" + it.name() + ") asset(" + it.assetId() + " data(" + it.numericData() + ")")
            }
            return null
        }

        // The other broadcasts are notification broadcasts specifically sent for (optional) status bar notification delivery.
        // Determine which action we are handling and create a notification for it.
        if (assetManager == null) synchronized(this) {
            if (assetManager == null) {
                assetManager = Virtuoso(context).assetManager
            }
        }

        var notificationType : Int
        var file: IAsset? = null

        if (action == Common.START_VIRTUOSO_SERVICE) {
            notificationType = RESTART_NOTIFICATION
        } else {
            var hasInfo = false
            val extras = notificationIntent.extras
            var info = Common.Notifications.DownloadStopReason.NO_ERROR

            if (extras != null) {
                if (extras.containsKey(Common.Notifications.EXTRA_NOTIFICATION_DOWNLOAD_STOP_REASON)) {
                    hasInfo = true
                    info = extras.getInt(Common.Notifications.EXTRA_NOTIFICATION_DOWNLOAD_STOP_REASON)
                }
                file = extras.getParcelable(Common.Notifications.EXTRA_NOTIFICATION_FILE)
            }

            when(action.replace(clientReference, "")){

                Common.Notifications.INTENT_NOTIFICATION_DOWNLOAD_COMPLETE -> {
                    notificationType = COMPLETED_NOTIFICATION
                    Log.d(TAG, "DOWNLOAD COMPLETE NOTIFICATION FOR " + file?.uuid + " stat: " + if (hasInfo) info else "unknown")

                }
                Common.Notifications.INTENT_NOTIFICATION_DOWNLOAD_START ->{
                    notificationType = PROGRESS_NOTIFICATION
                    Log.d(TAG, "DOWNLOAD START NOTIFICATION FOR " + file?.uuid + " stat: " + if (hasInfo) info else "unknown")

                }

                Common.Notifications.INTENT_NOTIFICATION_DOWNLOAD_STOPPED-> {
                    if (file != null) {
                        Log.d(TAG, "DOWNLOAD STOP NOTIFICATION FOR " + file.uuid + " stat: " + if (hasInfo) info else "unknown")
                    } else {
                        Log.d(TAG, "DOWNLOAD STOP NOTIFICATION FOR UNKNOWN" + " stat: " + if (hasInfo) info else "unknown")
                    }
                    notificationType = STOPPED_NOTIFICATION
                }

                Common.Notifications.INTENT_NOTIFICATION_DOWNLOADS_PAUSED -> {
                    if (file != null) {
                        Log.d(TAG, "DOWNLOAD PAUSED NOTIFICATION FOR " + file.uuid + " stat: " + if (hasInfo) info else "unknown")
                    } else {
                        Log.d(TAG, "DOWNLOAD PAUSED NOTIFICATION FOR UNKNOWN" + " stat: " + if (hasInfo) info else "unknown")
                    }
                    notificationType = PAUSED_NOTIFICATION
                }

                Common.Notifications.INTENT_NOTIFICATION_DOWNLOAD_UPDATE -> {
                    notificationType = PROGRESS_NOTIFICATION
                    Log.d(TAG, "DOWNLOAD UPDATE NOTIFICATION FOR " + file?.uuid + " stat: " + if (hasInfo) info else "unknown")

                }

                Common.Notifications.INTENT_NOTIFICATION_MANIFEST_PARSE_FAILED-> {
                    notificationType = FAILED_NOTIFICATION
                    Log.d(TAG, "EXCEPTIONAL CIRCUMSTANCE NOTIFICATION for asset failed to be queued while in background")
                }

                else ->{
                    notificationType = RESTART_NOTIFICATION
                    Log.d(TAG, "UNHANDLED NOTIFICATION ACTION $action")
                }
            }

        }

        var notification: Notification? = null
        if (notificationType > -1) {
            notification = createNotification(notificationType, context, file)
        }

        return notification
    }

    /**
     * Create the notification for the specified type.
     * @param type The notification type.
     * @param context The context to be used
     * @param asset the asset (may be null)
     * @return the notification.
     */
    private fun  createNotification(type: Int, context: Context, asset: IAsset?): Notification? {
        var title = "$applicationName: "
        var contentText = ""
        var progress = -1


        when (type) {
            PROGRESS_NOTIFICATION -> {
                progress = getDownloadProgress(asset)
                title += getAssetTitle(asset)
                contentText += progress.toString() + "%" + " : " + String.format(" ( %1$,.0f)", asset?.currentSize)
            }

            COMPLETED_NOTIFICATION -> {
                progress = 100
                title += getAssetTitle(asset) + " complete."
            }

            STOPPED_NOTIFICATION -> title += "stopped downloads."

            PAUSED_NOTIFICATION -> title += "paused downloads."

            RESTART_NOTIFICATION -> title += "is starting up..."

            FAILED_NOTIFICATION -> title += " asset could not be queued"
        }

        val pendingIntent = PendingIntent.getActivity(context, 0, createIntent(context), PendingIntent.FLAG_CANCEL_CURRENT)

        var notification: Notification?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (notificationBuilder == null) {
                synchronized(this){
                    if (notificationBuilder == null){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            notificationBuilder = Notification.Builder(context, channelId())
                        } else {
                            @Suppress("DEPRECATION")
                            notificationBuilder = Notification.Builder(context)
                        }
                        notificationBuilder!!.setOnlyAlertOnce(true)
                    }
                }
            }
            var nb = notificationBuilder!!
                    .setTicker(applicationName)
                    .setSmallIcon(R.drawable.small_logo)
                    .setContentTitle(title)
                    .setContentIntent(pendingIntent)
                    .setColor(context.getColor(android.R.color.holo_blue_bright))
                    .setContentText(contentText)

            if (progress >= 0) {
                nb = nb.setProgress(100, progress, false)
            }
            notification =
                    nb.setWhen(System.currentTimeMillis())
                    .setOngoing(true).build()
        } else {
            if (compatNotificationBuilder == null) {
                synchronized(this){
                    if (compatNotificationBuilder == null) {
                        compatNotificationBuilder = NotificationCompat.Builder(context, channelId())
                        compatNotificationBuilder!!.setOnlyAlertOnce(true)
                    }
                }
            }
            var nb = compatNotificationBuilder!!
                    .setTicker(applicationName)
                    .setSmallIcon(R.drawable.small_logo)
                    .setContentTitle(title)
                    .setContentIntent(pendingIntent)
                    .setColor(context.getColor(android.R.color.holo_blue_bright))
                    .setContentText(contentText)

            if (progress >= 0) {
                nb = nb.setProgress(100, progress, false)
            }
            notification =
                    nb.setWhen(System.currentTimeMillis())
                            .setOngoing(true).build()
        }

        return notification
    }

    /**
    * calculates the progress of the current download.
    * @param asset the current asset downloading
    * @return progress
    */
    private fun getDownloadProgress(asset: IAsset?): Int {
        if (asset == null)
            return 0

        var fractionComplete = asset.fractionComplete
        fractionComplete *= 100
        if (fractionComplete > 99.0) {
            fractionComplete = 99.0
        }

        return fractionComplete.toInt()
    }

    /**
     * Get the assets title or an empty string
     * @param asset the asset
     * @return
     */
    private fun getAssetTitle(asset: IAsset?): String {

        val title = asset?.let {
            var title = ""
            val json: String = it.metadata
            if (json.isNotEmpty()) {
               title = ExampleMetaData().fromJson(json).title
            }
            title
        }

        return title ?: ""
    }

    /**
     * create an intent for opening the application when the user clicks on the notification.
     * @param aContext used to get the package name
     * @return the intent
     */
    private fun createIntent(aContext: Context): Intent {
        val intent = Intent(aContext.packageName + ".DEMO_NOTIFICATION")
        intent.component = ComponentName(aContext.packageName, MainActivity::class.java.name)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.flags = Intent.FLAG_FROM_BACKGROUND
        return intent
    }
}