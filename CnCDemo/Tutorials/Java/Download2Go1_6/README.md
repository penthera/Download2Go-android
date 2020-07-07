Tutorial - Example1.6: Multi-asset view using cursor
==================================================
## Summary
This sample demonstrates how to fetch a cursor from the SDK to view the all the items currently within the SDK. This includes assets which are downloaded, in the download queue, or in the deferred queue which is for items that have been created but not queued for download. In this example we wish to show all of these three cursors in a single recyclerview, so we use the highest level cursor. That cursor does not receive notifications for all of the different queue activities, because more commonly you would use one of the more specialised cursors such as the download queue cursor whose notification receives updates for download progress. In order to receive all of those notifications, because the cursor can only be assigned a single notification uri, we create a ContentObserver and register the three important notification uris to that observer, then set the cursor notification to a uri posted by that observer.
<p>This sample also demonstrates a second view which loads the details for a single asset which is selected from the recyclerview.</p>
</br>
</br>