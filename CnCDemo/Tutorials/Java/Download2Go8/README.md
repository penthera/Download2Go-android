Tutorial - Example8: Widevine DRM integration example
=======================================
## Summary
This example demonstrates how to use the SDK to manage offline licenses for Widevine DRM. This is split into two parts:
1. To setup the SDK to download offline licenses it is necessary to provide a license manager class which configures the HTTP request to fetch a license.
2. The easiest method to manage DRM during playback is to wrap the SDK DRM session manager with a suitable class to work with your player implementation. The session manager will load the offline key using the android MediaDrm and return a drm session containing the key for use by a player.
</br>
</br>
