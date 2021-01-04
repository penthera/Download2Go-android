package com.penthera.sdkdemo.push;

import com.penthera.virtuososdk.client.push.ADMReceiver;

public class DemoADMReceiver extends ADMReceiver {

    public DemoADMReceiver(){
        //Pass our ADM Service to the parent class
        super(DemoADMService.class, DemoADMJobMessageHandler.class);
    }
}
