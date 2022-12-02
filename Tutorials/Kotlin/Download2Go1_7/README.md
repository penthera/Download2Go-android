Tutorial - Example1.7: Push Notification Configuration
======================================================
## Summary
<br>
This sample demonstrates an integration approach which enables existing application push notifications to be handled by the application while ones for the SDK are passed through. The SDK provides a base class to be derived from to handle the SDK push notifications. The example indicates how to configure Firebase Push notifications by including the framework in the gradle configuration and defining the service in the  Android manifest. This is exactly the same as would be required for any other FCM integration without the SDK.
<br><br>
The sample app enables the user to register multiple devices to a single user and then request remote unregistration from one device of all other devices registered to that user. The push message will be processed on the other devices, causing the SDK to perform a backplane sync. This results in the device authorisation being removed. If push messaging were not configured then the unregistration would occur on the next sync, which should occur within 24 hours if the user is not actively within the application.
<br><br>
To configure the application for demonstration purposes you will need to complete the following steps:

1. Rename the demo application package
2. Create a demo application in Firebase Cloud Messaging for that package name
3. Download the google-services.json file from the Firebase Console and add it to the root of the project
4. Copy the server key from the cloud messaging tab on the Firebase Console
5. Paste the server key into the FCM/GCM configuration on the instance of the Penthera cloud server application that you are using for testing.
