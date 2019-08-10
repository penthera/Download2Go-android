package com.penthera.sdkdemo.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.penthera.sdkdemo.SDKDemoServiceStarter;
import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.IForegroundNotificationProvider;

import static com.penthera.virtuososdk.utility.logger.CnCLogger.Log;

/**
 *  This implements the IForegroundNotificationProvider interface, which enables notifications to be built
 *  within the service process so they do not need to cross the process boundary.
 *
 *  In this example it uses the same Factory class to generate notifications. Please remember this is a
 *  separate instance of the NotificationFactory, running within a different process than the traditional one in ServiceStarter.
 */
public class ServiceForegroundNotificationProvider implements IForegroundNotificationProvider {

    private Context context;
    private NotificationChannel notificationChannel = null;
    private String channelId = null;
    private Notification currentNotification = null;


    @Override
    public void prepareNotificationProvider(Context context) {
        Log.d("Preparing Notification Provider");
        this.context = context;
    }

    @Override
    public boolean shouldUpdateForegroundServiceNotificationOnIntent(Context context, Intent reasonIntent){

        String action = reasonIntent.getAction();
        Log.d("got action: " + action);
        if(context == null || reasonIntent == null || action == null){
            return false;
        }

        // Do not update progress for events
        if( action.contains(Common.Notifications.NOTIFICATION_EVENT_TAG) ){
            return false;
        }

        return true;
    }

    @Override
    public void setExistingNotificationForReuse(Notification notification) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = notification.getChannelId();
            if (currentNotification != null && notificationChannel != null && currentNotification.getChannelId().equals(channelId)) {
                return;
            }
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationChannel = manager.getNotificationChannel(channelId);
            this.channelId = channelId;
            currentNotification = notification;
        } else {
            currentNotification = notification;
        }
    }

    @Override
    public Notification getForegroundServiceNotification(Context context, IAsset file, Intent reasonIntent) {
        if (reasonIntent == null) return currentNotification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && channelId == null) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationChannel = new NotificationChannel(SDKDemoServiceStarter.CHANNEL_ID, SDKDemoServiceStarter.CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            notificationChannel.setDescription(SDKDemoServiceStarter.CHANNEL_DESCRIPTION);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            manager.createNotificationChannel(notificationChannel);
            channelId = notificationChannel.getId();
        }

        currentNotification = NotificationFactory.getNotification(context, reasonIntent, channelId, "Harness");

        return currentNotification;
    }
}
