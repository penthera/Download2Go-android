package com.penthera.sdkdemokotlin.push


import com.penthera.virtuososdk.client.push.ADMReceiver

class DemoADMReceiver : ADMReceiver(
        DemoADMService::class.java, DemoADMJobMessageHandler::class.java)