Tutorial - Example1.6: Multi-asset view using cursor
==================================================
## Summary
This sample demonstrates how to fetch a LiveData based list of assets from the SDK and AndroidX support library to view all the items currently within the SDK. This includes assets which are downloaded, in the download queue, or in the deferred queue which is for items that have been created but not queued for download. In this example we wish to show all of these assets in a single recyclerview, so we use the AllAssetsLiveData. We transform the assets list in our ViewModel to a simple AssetWrapper class which can be used with DataBinding to populate the list items in the UI.
<p>This sample also demonstrates a second view which loads the details for a single asset which is selected from the recyclerview.</p>
</br>
</br>