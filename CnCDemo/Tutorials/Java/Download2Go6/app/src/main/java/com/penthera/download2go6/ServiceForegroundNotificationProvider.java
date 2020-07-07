package com.penthera.download2go6;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.IForegroundNotificationProvider;


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
public class ServiceForegroundNotificationProvider implements IForegroundNotificationProvider {

    private Context context;
    private NotificationChannel notificationChannel = null;
    private String channelId = null;
    private Notification currentNotification = null;

    // This is a helper class which is used in the demo for creating the notifications
    private NotificationFactory notificationFactory;

    /**
     * This method provides a place where you can setup any necessary resources when the provider is instantiated.
     * This is called directly after construction.
     * @param context A context from the service which can be used for access to Android resources.
     */
    @Override
    public void prepareNotificationProvider(Context context) {
        this.context = context;
        notificationFactory = new NotificationFactory("Download2GoHelloWorld");
    }

    /**
     * This method allows the client to select for which intents the service will request a new notification.
     * @param context A context object
     * @param reasonIntent The updated intent from the service
     * @return true if the notification should be updated, false otherwise.
     */
    @Override
    public boolean shouldUpdateForegroundServiceNotificationOnIntent(Context context, Intent reasonIntent){

        String action = reasonIntent.getAction();
        if(context == null || reasonIntent == null || action == null){
            return false;
        }

        // Do not update progress for events
        if( action.contains(Common.Notifications.NOTIFICATION_EVENT_TAG) ){
            return false;
        }

        return true;
    }

    /**
     * This method provides the notification provider with a copy of the original launch notification which was
     * passed to the service upon startup via the ServiceStarter. This can be used to retrieve the channel details and
     * notification ID, which enables the manipulation of the same notification on each request.
     * @param notification The existing notification
     */
    @Override
    public void setExistingNotificationForReuse(Notification notification) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = notification.getChannelId();
            if (currentNotification != null && notificationChannel != null && currentNotification.getChannelId().equals(channelId)) {
                return;
            }
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationChannel = manager.getNotificationChannel(channelId);
            NotificationFactory.setChannel(notificationChannel);
            this.channelId = channelId;
            currentNotification = notification;
        } else {
            currentNotification = notification;
        }
    }

    /**
     * Return a new notification, given the file and intent. Always return a notification.
     * @param context A context to use in processing
     * @param asset The asset for which the notification is being created
     * @param reasonIntent The intent for which the notificaation is being created
     * @return The notification
     */
    @Override
    public Notification getForegroundServiceNotification(Context context, IAsset asset, Intent reasonIntent) {
        if (reasonIntent == null) return currentNotification;

        currentNotification = notificationFactory.getNotification(context, reasonIntent);

        return currentNotification;
    }
}
