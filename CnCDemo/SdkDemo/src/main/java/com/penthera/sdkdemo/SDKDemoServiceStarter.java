package com.penthera.sdkdemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;

import com.penthera.sdkdemo.notification.NotificationFactory;
import com.penthera.sdkdemo.activity.SplashActivity;
import com.penthera.sdkdemo.notification.ServiceForegroundNotificationProvider;
import com.penthera.virtuososdk.client.Virtuoso;
import com.penthera.virtuososdk.service.VirtuosoServiceStarter;
import static com.penthera.virtuososdk.utility.logger.CnCLogger.Log;

public class SDKDemoServiceStarter extends VirtuosoServiceStarter {


    public static String foregroundServiceNotificationAction = "foregroundservice.action.ForegroundServiceNotificationAction";

    private static NotificationChannel notificationChannel = null;
    private static Notification currentNotification = null;

    public final static String CHANNEL_ID = "VIRTUOSO_DEMO_CHANNEL_ID";
    public final static String CHANNEL_NAME = "SdkDemo Background Activity";
    public final static String CHANNEL_DESCRIPTION = "Indicates activity this application will perform when the application is not open";


	public static void updateNotification(Context aContext, Intent aIntent ) {
		updateNotification( aContext, aIntent,SDKDemoServiceStarter.class);
	}

    @Override
    public Notification getForegroundServiceNotification(Context context, @Nullable Intent forIntent) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationChannel == null) {
            notificationChannel = new NotificationChannel(CHANNEL_ID,CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            notificationChannel.setDescription(CHANNEL_DESCRIPTION);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
        }

        Intent notificationIntent = new Intent(context,SplashActivity.class);
        notificationIntent.setAction(foregroundServiceNotificationAction);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        Notification notification = NotificationFactory.getNotification(context, (forIntent!=null)?forIntent:notificationIntent, CHANNEL_ID, "SDKDemo");
        if(currentNotification != null && notification != null){
            currentNotification.contentView = notification.contentView;
        }
        else if( notification != null ) {
            currentNotification = notification;
        }

        return currentNotification;
    }

    @Override
    public Class getForegroundServiceNotificationProvider(){
	    // Returning this class definition causes the service to instantiate and use the class
        // from within the service process to generate all notifications relating to asset downloads.
        // Returning null results in the classic SDK behaviour where all notifications are generated
        // and delivered to the service via the service starter.
        return ServiceForegroundNotificationProvider.class;
    }

}
