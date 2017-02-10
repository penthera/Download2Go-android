package com.penthera.sdkdemo.drm;

import com.penthera.virtuososdk.client.drm.LicenseManager;

import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of the Licensemanager. Just shows how methods could be overridden.
 *
 * Note: For this LicenseManager implementation to be used the class must be specified in the manifest metadata
 * e.g.:
 *   <meta-data tools:replace="android:value" android:name="com.penthera.virtuososdk.license.manager.impl" android:value="com.penthera.sdkdemo.drm.HarnessLicenseManager"/>
 * for the above to work you will need to add the tools name space
 * xmlns:tools="http://schemas.android.com/tools"
 */

public class DemoLicenseManager extends LicenseManager {
    @Override
    public String getLicenseAcquistionUrl() {
        String license_server_url = "https://proxy.uat.widevine.com/proxy";
        /*
         Here you can examine the mAsset and mAssetId member variables and modify the
         license server url if needed:
         Remember that the mAsset and mAssetId could be null if the License Manager was not built
         with the values.
         Example:
         String video_id = mAsset != null ? mAsset.getAssetId() : mAssetId != null ? mAssetId : null;
         if(!TextUtils.isEmpty(video_id){
            license_server_url += "?video_id="+video_id;
         }
         */

        return license_server_url;
    }

    @Override
    public Map<String, String> getKeyRequestProperties() {
        Map<String,String> props = new HashMap<>();
        props.put("Content-Type", "application/octet-stream");
        props.put("User-Agent","virtuoso-sdk");
        return props;
    }
}
