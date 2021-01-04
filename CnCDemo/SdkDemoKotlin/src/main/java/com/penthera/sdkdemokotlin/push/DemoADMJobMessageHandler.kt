package com.penthera.sdkdemokotlin.push

import android.content.Context
import android.content.Intent
import android.util.Log
import com.penthera.virtuososdk.client.push.ADMJobMessageHandler

class DemoADMJobMessageHandler : ADMJobMessageHandler() {
    override fun onMessage(context: Context, intent: Intent) {
        //always call super so that the SDK works correctly
        Log.d(TAG, "onMessage")
        super.onMessage(context, intent)
    }

    override fun onRegistrationError(context: Context, s: String) {
        //always call super so that the SDK works correctly
        Log.d(TAG, "onRegistrationError")
        super.onRegistrationError(context, s)
    }

    override fun onRegistered(context: Context, s: String) {
        //always call super so that the SDK works correctly
        Log.d(TAG, "onRegistered")
        super.onUnregistered(context, s)
    }

    override fun onUnregistered(context: Context, s: String) {
        //always call super so that the SDK works correctly
        Log.d(TAG, "onUnregistered")
        super.onUnregistered(context, s)
    }

    companion object {
        private val TAG = DemoADMJobMessageHandler::class.java.name
    }
}