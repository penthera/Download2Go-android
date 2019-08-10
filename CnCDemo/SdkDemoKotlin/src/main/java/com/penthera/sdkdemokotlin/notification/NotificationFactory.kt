package com.penthera.sdkdemokotlin.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.util.Log
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
class NotificationFactory(applicationName: String) {

    companion object {
        fun channelId() = "VIRTUOSO_KOTLIN_DEMO_CHANNEL_ID"
        fun channelName() = "Demo Background Activity"
        fun channelDescription() = "Indicates activity this application will perform when the application is not open"

        private val TAG = NotificationFactory::class.java.simpleName
    }

    private val applicationName: String = applicationName
    private var assetManager: IAssetManager? = null
    private var notificationBuilder: Notification.Builder? = null
    private var compatNotificationBuilder: NotificationCompat.Builder? = null

    internal val PROGRESS_NOTIFICATION = 0
    internal val COMPLETED_NOTIFICATION = 1
    internal val STOPPED_NOTIFICATION = 2
    internal val PAUSED_NOTIFICATION = 3
    internal val RESTART_NOTIFICATION = 4
    internal val FAILED_NOTIFICATION = 5

    /**
     * This defines a default intent for the app which can be used if none is provided in the request
     */
    fun defaultNotificationIntent(context: Context) : Intent {

        val notificationIntent = android.content.Intent(context, MainActivity::class.java)
        notificationIntent.action = "foregroundservice.action.ForegroundServiceNotificationAction"
        notificationIntent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        return notificationIntent;
    }

    fun getNotification(context: Context, intent: Intent?): Notification? {
        // A default intent is used if no intent is delivered in the request.
        val notificationIntent = intent ?: defaultNotificationIntent(context)

        var clientReference: String?

        try {
            val ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA)
            val b = ai.metaData

            clientReference = b.getString(Common.CLIENT_PACKAGE)

        } catch (e: Exception) {
            throw RuntimeException("cannot retrieve client", e)
        }

        val action = notificationIntent.getAction()
        if (clientReference == null || action == null) {
            return null
        }

        // If the intent action contains the NOTIFICATION_EVENT_TAG, then this is a log event broadcast sent from
        // the SDK analytics system.  For debugging, we might want to post status bar notifications, but in general,
        // these events are used to push SDK analytics events into a custom/3rd party analytics platform, and shouldn't
        // be sent to the status bar. For the purposes of this demo, we'll log them and return null, which prevents the
        // status bar notice from being shown.
        if (action.contains(Common.Notifications.NOTIFICATION_EVENT_TAG)) {

            val event: IEvent = notificationIntent.getParcelableExtra<IEvent>(Common.Notifications.EXTRA_NOTIFICATION_EVENT)

            Log.d(TAG, "Got event named(" + event.name() + ") asset(" + event.assetId() + " data(" + event.numericData() + ")")
            return null
        }

        // The other broadcasts are notification broadcasts specifically sent for (optional) status bar notification delivery.
        // Determine which action we are handling and create a notification for it.
        if (assetManager == null) {
            synchronized(this) {
                if (assetManager == null) {
                    assetManager = Virtuoso(context).assetManager;
                }
            }
        }

        var notification_type = -1
        var file: IAsset? = null

        if (action == Common.START_VIRTUOSO_SERVICE) {
            notification_type = RESTART_NOTIFICATION
        } else {
            var hasInfo = false
            val extras = notificationIntent.getExtras()
            var info = Common.Notifications.DownloadStopReason.NO_ERROR

            if (extras != null) {
                if (extras.containsKey(Common.Notifications.EXTRA_NOTIFICATION_DOWNLOAD_STOP_REASON)) {
                    hasInfo = true
                    info = extras.getInt(Common.Notifications.EXTRA_NOTIFICATION_DOWNLOAD_STOP_REASON)
                }
                file = extras.getParcelable(Common.Notifications.EXTRA_NOTIFICATION_FILE)
            }
            val INTENT_ACTION = action.replace(clientReference, "")

            if (INTENT_ACTION == Common.Notifications.INTENT_NOTIFICATION_DOWNLOAD_COMPLETE) {

                notification_type = COMPLETED_NOTIFICATION
                Log.d(TAG, "DOWNLOAD COMPLETE NOTIFICATION FOR " + file?.getUuid() + " stat: " + if (hasInfo) info else "unknown")

            } else if (INTENT_ACTION == Common.Notifications.INTENT_NOTIFICATION_DOWNLOAD_START) {

                notification_type = PROGRESS_NOTIFICATION
                Log.d(TAG, "DOWNLOAD START NOTIFICATION FOR " + file?.getUuid() + " stat: " + if (hasInfo) info else "unknown")

            } else if (INTENT_ACTION == Common.Notifications.INTENT_NOTIFICATION_DOWNLOAD_STOPPED) {

                if (file != null) {
                    Log.d(TAG, "DOWNLOAD STOP NOTIFICATION FOR " + file.getUuid() + " stat: " + if (hasInfo) info else "unknown")
                } else {
                    Log.d(TAG, "DOWNLOAD STOP NOTIFICATION FOR UNKNOWN" + " stat: " + if (hasInfo) info else "unknown")
                }
                notification_type = STOPPED_NOTIFICATION

            } else if (INTENT_ACTION == Common.Notifications.INTENT_NOTIFICATION_DOWNLOADS_PAUSED) {

                if (file != null) {
                    Log.d(TAG, "DOWNLOAD PAUSED NOTIFICATION FOR " + file.getUuid() + " stat: " + if (hasInfo) info else "unknown")
                } else {
                    Log.d(TAG, "DOWNLOAD PAUSED NOTIFICATION FOR UNKNOWN" + " stat: " + if (hasInfo) info else "unknown")
                }
                notification_type = PAUSED_NOTIFICATION

            } else if (INTENT_ACTION == Common.Notifications.INTENT_NOTIFICATION_DOWNLOAD_UPDATE) {

                notification_type = PROGRESS_NOTIFICATION
                Log.d(TAG, "DOWNLOAD UPDATE NOTIFICATION FOR " + file?.getUuid() + " stat: " + if (hasInfo) info else "unknown")

            } else if (INTENT_ACTION == Common.Notifications.INTENT_NOTIFICATION_MANIFEST_PARSE_FAILED) {

                notification_type = FAILED_NOTIFICATION
                Log.d(TAG, "EXCEPTIONAL CIRCUMSTANCE NOTIFICATION for asset failed to be queued while in background")
            } else {
                notification_type = RESTART_NOTIFICATION
                Log.d(TAG, "UNHANDLED NOTIFICATION ACTION $action")
            }
        }

        var notification: Notification? = null
        if (notification_type > -1) {
            notification = createNotification(notification_type, context, file)
        }

        return notification
    }

    /**
     * Create the notification for the specified type.
     * @param type The notification type.
     * @param aContext The context to be used
     * @param aNotificationChannelID The notification channel to use.  May be null if not using notification channels.
     * @param aAsset the asset (may be null)
     * @param aAppName name of the application - this is used in the notification title
     * @return the notification.
     */
    fun  createNotification(type: Int, context: Context, asset: IAsset?): Notification? {
        var title = applicationName + ": "
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

        var notification: Notification? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (notificationBuilder == null) {
                synchronized(this){
                    if (notificationBuilder == null){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            notificationBuilder = Notification.Builder(context, channelId())
                        } else {
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
                        compatNotificationBuilder = NotificationCompat.Builder(context)
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
    * @param aAsset the current asset downloading
    * @return progress
    */
    fun getDownloadProgress(asset: IAsset?): Int {
        if (asset == null)
            return 0

        var fractionComplete = asset.getFractionComplete();
        fractionComplete *= 100;
        if (fractionComplete > 99.0) {
            fractionComplete = 99.0;
        }

        return fractionComplete.toInt()
    }

    /**
     * Get the assets title or an empty string
     * @param aAsset the asset
     * @return
     */
    internal fun getAssetTitle(asset: IAsset?): String {

        var title = asset?.let {
            var title : String = ""
            val json: String = it.metadata
            if (json.isNotEmpty()) {
               title = ExampleMetaData().fromJson(json).title
            }
            title ?: ""
        }

        return title ?: ""
    }

    /**
     * create an intent for opening the application when the user clicks on the notification.
     * @param aContext used to get the package name
     * @return the intent
     */
    fun createIntent(aContext: Context): Intent {
        val intent = Intent(aContext.packageName + ".DEMO_NOTIFICATION")
        intent.component = ComponentName(aContext.packageName, MainActivity::class.java.getName())
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.flags = Intent.FLAG_FROM_BACKGROUND
        return intent
    }
}