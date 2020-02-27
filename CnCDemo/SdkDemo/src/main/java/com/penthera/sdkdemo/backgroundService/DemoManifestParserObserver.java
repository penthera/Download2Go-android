package com.penthera.sdkdemo.backgroundService;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.penthera.sdkdemo.catalog.Catalog;
import com.penthera.sdkdemo.catalog.CatalogContentProvider;
import com.penthera.virtuososdk.client.IAssetManager;
import com.penthera.virtuososdk.client.IManifestParserObserver;
import com.penthera.virtuososdk.client.ISegment;
import com.penthera.virtuososdk.client.ISegmentedAsset;

/**
 * This class provides a placeholder for observing and performing actions on the manifest parser.
 * This is instantiated and runs within the SDK service process in order to be called when each
 * segment is parsed and before the asset is queued.
 *
 * In this example we look up the asset and set some parameters on the asset after it has been parsed
 * and prior to it being added to the download queue. Practically these actions could be taken at another time
 * within the main process. This is simply an artificial requirement in order to demonstrate the interface.
 * @see IManifestParserObserver
 */
public class DemoManifestParserObserver implements IManifestParserObserver {
    @Override
    public String didParseSegment(ISegmentedAsset asset, ISegment segment) {
        // This demo does not include assets that require URL manipulation.  If your assets require
        // that you add or change the download URL from the manifest prior to downloading it, then
        // you would use this method to return the modified URL.
        return segment.getRemotePath();
    }

    @Override
    public void willAddToQueue(ISegmentedAsset aSegmentedAsset, IAssetManager assetManager, Context context) {

        Cursor assetCursor = null;
        try {

            assetCursor = context.getContentResolver().query(CatalogContentProvider.CATALOG_URI, null, Catalog.CatalogColumns._ID + "=?",
                    new String[] {aSegmentedAsset.getAssetId()}, null);

            if (assetCursor != null && assetCursor.moveToFirst()){

                // Fetch details for expiry overrides from the remote catalog
                long catalogExpiry = assetCursor.getLong(assetCursor.getColumnIndex(Catalog.CatalogColumns.CATALOG_EXPIRY));
                final long downloadExpiry = assetCursor.getLong(assetCursor.getColumnIndex(Catalog.CatalogColumns.DOWNLOAD_EXPIRY));
                final long expiryAfterPlay = assetCursor.getLong(assetCursor.getColumnIndex(Catalog.CatalogColumns.EXPIRY_AFTER_PLAY));
                final long availabilityStart = assetCursor.getLong(assetCursor.getColumnIndex(Catalog.CatalogColumns.AVAILABILITY_START));
                final long now = System.currentTimeMillis()/1000;

                // Set the expiry parameters on the asset and save
                aSegmentedAsset.setStartWindow(availabilityStart <= 0 ? now : availabilityStart);
                aSegmentedAsset.setEndWindow(catalogExpiry <= 0 ? Long.MAX_VALUE : catalogExpiry);
                aSegmentedAsset.setEap(expiryAfterPlay);
                aSegmentedAsset.setEad(downloadExpiry);
                assetManager.update(aSegmentedAsset);
            }

        } catch (Exception e) {
            Log.w(DemoManifestParserObserver.class.getSimpleName(),"Could not find and set the expiry parameters on a parsed manifest: " + e.getMessage());
        } finally {
            if (assetCursor != null && !assetCursor.isClosed()) {
                assetCursor.close();
            }
        }

    }
}
