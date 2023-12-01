package com.penthera.download2go1_7;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.penthera.virtuososdk.Common;
import com.penthera.common.Common.Notifications;
import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.IEvent;

/**
 * This notification factory is a helper class for creating our notifications throughout the tutorial series.
 * Nothing in this class is specific to the Download2Go SDK, it encapsulates all of the standard methods that
 * may be required to generate a simple notification on Android which contains some text and a progress bar.
 */
public class NotificationFactory {

    // Log Tag
    static final String LOG_TAG = NotificationFactory.class.getName();

    private static final String channelID = "DOWNLOAD2GO_HELLO_WORLD_CHANNEL_ID";
    private static final String channelName = "Download2GoHelloWorld Background Activity";
    private static final String channelDescription = "Indicates activity this application will perform when the application is not open";

    /** Internal list of types of notifications in this factory */
    final static int PROGRESS_NOTIFICATION		= 0;
    final static int COMPLETED_NOTIFICATION		= 1;
    final static int STOPPED_NOTIFICATION		= 2;
    final static int PAUSED_NOTIFICATION		= 3;
    final static int RESTART_NOTIFICATION		= 4;
    final static int FAILED_NOTIFICATION		= 5;

    /** Name of the application, used in the notification */
    private String applicationName;

    /** It is more efficient to keep the notification builder and notification channel between calls, where this object
     * may be created and destroyed on multiple occassions by the OS */
    private static NotificationCompat.Builder compatNotificationBuilder = null;
    private static NotificationChannel notificationChannel = null;

    public NotificationFactory(String applicationName) {
        this.applicationName = applicationName;
    }

    public static void setChannel(NotificationChannel channel){
        notificationChannel = channel;
    }

    /**
     * This returns the new notification to represent the current state of the asset / intent.
     * @param context The context to use for managing the notification
     * @param intent The intent for which to create a notification
     * @return The notification
     */
    public Notification getNotification(Context context, Intent intent) {

        // A default intent is used if no intent is delivered in the request.
        Intent notificationIntent = intent != null ? intent : defaultNotificationIntent(context);

        // Get package name for use in modifying actions
        String clientReference;
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle b =ai.metaData;

            clientReference = b.getString(Common.CLIENT_PACKAGE);
        } catch(Exception e){
            throw new RuntimeException("cannot retrieve client",e);
        }

        String action = notificationIntent.getAction();
        if(clientReference == null || action == null){
            return null;
        }

        // If the intent action contains the NOTIFICATION_EVENT_TAG, then this is a log event broadcast sent from
        // the SDK analytics system.  For debugging, we might want to post status bar notifications, but in general,
        // these events are used to push SDK analytics events into a custom/3rd party analytics platform, and shouldn't
        // be sent to the status bar. For the purposes of this demo, we'll log them and return null, which prevents the
        // status bar notice from being shown.
        if (action.contains(Notifications.NOTIFICATION_EVENT_TAG)) {

            IEvent event = notificationIntent.getParcelableExtra(Notifications.EXTRA_NOTIFICATION_EVENT);

            if(event != null)
                Log.d(LOG_TAG, "Got event named(" + event.name() + ") asset(" + event.assetId() + " data(" + event.numericData() + ")");
            return null;
        }

        // The other broadcasts are notification broadcasts specifically sent for (optional) status bar notification delivery.
        // Determine which action we are handling and create a notification for it.
        int notificationType = -1;
        IAsset asset = null;

        if (action.equals( Common.START_VIRTUOSO_SERVICE)) {
            notificationType = RESTART_NOTIFICATION;
        } else {
            boolean hasInfo = false;
            Bundle extras = notificationIntent.getExtras();
            int info = Common.Notifications.DownloadStopReason.NO_ERROR;

            if(extras != null) {
                if(extras.containsKey(Common.Notifications.EXTRA_NOTIFICATION_DOWNLOAD_STOP_REASON)){
                    hasInfo = true;
                    info = extras.getInt(Common.Notifications.EXTRA_NOTIFICATION_DOWNLOAD_STOP_REASON);
                }
                asset = extras.getParcelable(Common.Notifications.EXTRA_NOTIFICATION_FILE);
            }
            final String intentAction = action.replace(clientReference, "");

            switch(intentAction) {
                case Common.Notifications.INTENT_NOTIFICATION_DOWNLOAD_COMPLETE:
                    notificationType = COMPLETED_NOTIFICATION;
                    if (asset != null) {
                        Log.d(LOG_TAG, "DOWNLOAD COMPLETE NOTIFICATION FOR " + asset.getUuid() + " stat: " + (hasInfo ? info : "unknown"));
                    }else {
                        Log.d(LOG_TAG, "DOWNLOAD COMPLETE NOTIFICATION FOR UNKNOWN" + " stat: " + (hasInfo ? info : "unknown"));
                    }
                    break;

                case Common.Notifications.INTENT_NOTIFICATION_DOWNLOAD_START:
                    notificationType = PROGRESS_NOTIFICATION;
                    if (asset != null) {
                        Log.d(LOG_TAG, "DOWNLOAD START NOTIFICATION FOR " + asset.getUuid() + " stat: " + (hasInfo ? info : "unknown"));
                    }else {
                        Log.d(LOG_TAG, "DOWNLOAD START NOTIFICATION FOR UNKNOWN" + " stat: " + (hasInfo ? info : "unknown"));
                    }
                    break;

                case Common.Notifications.INTENT_NOTIFICATION_DOWNLOAD_STOPPED:
                    if (asset != null) {
                        Log.d(LOG_TAG, "DOWNLOAD STOP NOTIFICATION FOR " + asset.getUuid() + " stat: " + (hasInfo ? info : "unknown"));
                    } else {
                        Log.d(LOG_TAG, "DOWNLOAD STOP NOTIFICATION FOR UNKNOWN" + " stat: " + (hasInfo ? info : "unknown"));
                    }
                    notificationType = STOPPED_NOTIFICATION;
                    break;

                case Common.Notifications.INTENT_NOTIFICATION_DOWNLOADS_PAUSED:
                    if (asset != null) {
                        Log.d(LOG_TAG, "DOWNLOAD PAUSED NOTIFICATION FOR " + asset.getUuid()  + " stat: " + (hasInfo ? info : "unknown"));
                    } else {
                        Log.d(LOG_TAG, "DOWNLOAD PAUSED NOTIFICATION FOR UNKNOWN" + " stat: " + (hasInfo ? info : "unknown"));
                    }
                    notificationType = PAUSED_NOTIFICATION;
                    break;

                case Common.Notifications.INTENT_NOTIFICATION_DOWNLOAD_UPDATE:
                    notificationType = PROGRESS_NOTIFICATION;
                    if (asset != null) {
                        Log.d(LOG_TAG, "DOWNLOAD UPDATE NOTIFICATION FOR " + asset.getUuid() + " stat: " + (hasInfo ? info : "unknown"));
                    }else {
                        Log.d(LOG_TAG, "DOWNLOAD UPDATE NOTIFICATION FOR UNKNOWN" + " stat: " + (hasInfo ? info : "unknown"));
                    }
                    break;

                case Common.Notifications.INTENT_NOTIFICATION_MANIFEST_PARSE_FAILED:
                    notificationType = FAILED_NOTIFICATION;
                    Log.d(LOG_TAG, "EXCEPTIONAL CIRCUMSTANCE NOTIFICATION for asset failed to be queued while in background");
                    break;

                default:
                    notificationType = RESTART_NOTIFICATION;
                    Log.d(LOG_TAG, "UNHANDLED NOTIFICATION ACTION " + intentAction);
            }
        }


        Notification notification = null;
        if (notificationType > -1) {
            notification = createNotification(notificationType, context, asset);
        }
        return notification;
    }

    /**
     * This defines a default intent for the app which can be used if none is provided in the request
     */
    private Intent defaultNotificationIntent(Context context) {

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setAction("foregroundservice.action.ForegroundServiceNotificationAction");
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return notificationIntent;
    }

    /**
     * Create the notification for the specified type.
     * @param type The notification type.
     * @param context The context to be used
     * @param asset the asset (may be null)
     * @return the notification.
     */
    @SuppressWarnings("deprecation")    // We use a deprecated method of constructing notifications for old API versions pre channelId.
    Notification createNotification(final int type, final Context context, final IAsset asset){

        String title = applicationName + ": ";
        String contentText = "";
        int progress = -1;

        switch(type){
            case PROGRESS_NOTIFICATION:
                progress = getDownloadProgress(asset);
                title += asset.getMetadata();
                break;

            case COMPLETED_NOTIFICATION:
                progress = 100;
                title += asset.getMetadata() + " complete.";
                break;

            case STOPPED_NOTIFICATION:
                title += "stopped downloads.";
                break;

            case PAUSED_NOTIFICATION:
                title += "paused downloads.";
                break;

            case RESTART_NOTIFICATION:
                title += "is starting up...";
                break;

            case FAILED_NOTIFICATION:
                title += " asset could not be queued";
                break;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, createIntent(context), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification ;
        if (compatNotificationBuilder == null) {
            synchronized (this) {
                if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Above SDK API 26 We use notification channels
                    notificationChannel = new NotificationChannel(channelID,channelName, NotificationManager.IMPORTANCE_LOW);
                    notificationChannel.setDescription(channelDescription);
                    notificationChannel.enableLights(false);
                    notificationChannel.enableVibration(false);
                    NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (manager != null) {
                        manager.createNotificationChannel(notificationChannel);
                    }
                    compatNotificationBuilder = new NotificationCompat.Builder(context, channelID);
                } else {
                    // Below API26 there are no notification channels.
                    compatNotificationBuilder = new NotificationCompat.Builder(context);
                    compatNotificationBuilder.setOnlyAlertOnce(true);
                }
            }
        }
        NotificationCompat.Builder nb = compatNotificationBuilder
                .setTicker(applicationName)
                .setSmallIcon(R.drawable.small_logo)
                .setContentTitle(title)
                .setContentIntent(pendingIntent)
                .setColor(context.getColor(android.R.color.holo_blue_bright))
                .setContentText(contentText);

        if (progress >= 0) {
            nb = nb.setProgress(100, progress, false);
        }

        notification = nb.setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .build();

        return notification;
    }

    /**
     * calculates the progress of the current download.
     * @param aAsset the current asset downloading
     * @return progress
     */
    static int getDownloadProgress(final IAsset aAsset){
        if(aAsset == null)
            return 0;

        double expected = aAsset.getExpectedSize();
        if (expected <= .0001) {
            expected = aAsset.getContentLength();
        }
        return (int)  (aAsset.getCurrentSize()/expected * 100.0);
    }

    /**
     * create an intent for opening the application when the user clicks on the notification.
     * @param aContext used to get the package name
     * @return the intent
     */
    static Intent createIntent(Context aContext) {
        Intent intent = new Intent(aContext.getPackageName() + ".DEMO_NOTIFICATION");
        intent.setComponent(new ComponentName(aContext.getPackageName(), MainActivity.class.getName()));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_FROM_BACKGROUND);
        return intent;
    }
}
