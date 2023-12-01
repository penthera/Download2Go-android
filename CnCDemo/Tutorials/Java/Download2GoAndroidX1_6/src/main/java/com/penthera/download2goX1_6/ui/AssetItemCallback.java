package com.penthera.download2goX1_6.ui;

import com.penthera.virtuososdk.client.ISegmentedAsset;

/**
 * A simple interface for binding the UI button clicks to the activities.
 */
public interface AssetItemCallback {

    /**
     * Play button click
     * @param asset The asset to play
     */
    void onPlay(ISegmentedAsset asset);

    /**
     * Delete button click
     * @param asset The asset to delete
     */
    void onDelete(ISegmentedAsset asset);

    /**
     * Open button click - open asset in detail view
     * @param asset The asset to open
     */
    void onOpen(ISegmentedAsset asset);
}
