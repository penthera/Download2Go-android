package com.penthera.sdkdemo.push;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.penthera.virtuososdk.client.push.ADMJobMessageHandler;

public class DemoADMJobMessageHandler extends ADMJobMessageHandler {

    private static final String TAG = DemoADMJobMessageHandler.class.getName();

    @Override
    protected void onMessage(Context context, Intent intent) {
        //always call super so that the SDK works correctly
        Log.d(TAG,"onMessage");
        super.onMessage(context,intent);
    }

    @Override
    protected void onRegistrationError(Context context, String s) {
        //always call super so that the SDK works correctly
        Log.d(TAG,"onRegistrationError");
        super.onRegistrationError(context,s);
    }

    @Override
    protected void onRegistered(Context context, String s) {
        //always call super so that the SDK works correctly
        Log.d(TAG,"onRegistered");
        super.onUnregistered(context,s);
    }

    @Override
    protected void onUnregistered(Context context, String s) {
        //always call super so that the SDK works correctly
        Log.d(TAG,"onUnregistered");
        super.onUnregistered(context,s);
    }

}
