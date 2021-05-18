package com.penthera.download2goX1_6;

import android.content.Context;

import androidx.annotation.NonNull;

import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.client.ISegment;
import com.penthera.virtuososdk.client.ISegmentedAsset;

import java.util.Locale;

/**
 * Illustrates wrapping the ISegmentedAsset provided from the SDK in order to provide a model object
 * that can be mapped or bound directly to UI using data binding or view binding.
 */
public class AssetWrapper {

    // The wrapped segmented asset from the SDK.
    @NonNull private final ISegmentedAsset segmentedAsset;

    private final Context context;

    public AssetWrapper(Context context, @NonNull ISegmentedAsset segmentedAsset) {
        this.segmentedAsset = segmentedAsset;
        this.context = context;
    }

    public ISegmentedAsset getSegmentedAsset() {
        return segmentedAsset;
    }

    public int getDBId() {
        return segmentedAsset.getId();
    }

    public String title() {
        return segmentedAsset.getMetadata();
    }

    public String assetId() {
        return segmentedAsset.getAssetId();
    }

    public String getProgressString() {
        return String.format(Locale.US,"(%.2f)", segmentedAsset.getFractionComplete());
    }

    public int getProgressPercentage() {
        return (int)(segmentedAsset.getFractionComplete() * 100);
    }

    public String getSize(){
        return context.getString(R.string.asset_size, String.format(Locale.US,"%.2f MB", segmentedAsset.getCurrentSize()/1048576.00), String.format(Locale.US,"%.2f MB", segmentedAsset.getExpectedSize()/1048576.00));
    }

    public int getStatus() {
        return segmentedAsset.getDownloadStatus();
    }

    public String getStatusText(){
        return getStatusText(context, segmentedAsset.getDownloadStatus());
    }

    public static String getStatusText(Context context, int downloadStatus) {
        String value;

        switch (downloadStatus) {
            case Common.AssetStatus.DOWNLOADING:
                value = context.getString(R.string.asset_status_downloading);
                break;

            case Common.AssetStatus.DOWNLOAD_COMPLETE:
                value = context.getString(R.string.asset_status_complete);
                break;

            case Common.AssetStatus.DOWNLOAD_PAUSED:
                value = context.getString(R.string.asset_status_paused);
                break;

            case Common.AssetStatus.EXPIRED:
                value = context.getString(R.string.asset_status_expired);
                break;

            case Common.AssetStatus.DOWNLOAD_DENIED_ASSET:
                value = context.getString(R.string.asset_status_denied_mad);
                break;

            case Common.AssetStatus.DOWNLOAD_DENIED_ACCOUNT:
                value = context.getString(R.string.asset_status_denied_mda);
                break;

            case Common.AssetStatus.DOWNLOAD_DENIED_EXTERNAL_POLICY:
                value = context.getString(R.string.asset_status_denied_ext);
                break;

            case Common.AssetStatus.DOWNLOAD_DENIED_MAX_DEVICE_DOWNLOADS:
                value = context.getString(R.string.asset_status_denied_mpd);
                break;

            case Common.AssetStatus.DOWNLOAD_DENIED_COPIES:
                value = context.getString(R.string.asset_status_denied_copies);
                break;

            case Common.AssetStatus.DOWNLOAD_BLOCKED_AWAITING_PERMISSION:
                value = context.getString(R.string.asset_status_await_permission);
                break;

            default:
                value = context.getString(R.string.asset_status_pending);
        }
        return value;
    }
}
