package com.penthera.download2goX1_6.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.penthera.download2goX1_6.AssetWrapper;
import com.penthera.download2goX1_6.R;
import com.penthera.virtuososdk.androidxsupport.SegmentedAssetLiveData;
import com.penthera.virtuososdk.androidxsupport.VirtuosoLiveDataFactory;
import com.penthera.virtuososdk.client.ISegmentedAsset;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Very simple ViewModel for completeness of the component model architecture example.
 * This contains the single asset LiveData from the SDK plus a number of transformations which
 * are used to create content for the UI. These might better be transformed in a presenter component
 * within a real application.
 */
public class AssetViewModel extends AndroidViewModel {

    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a", Locale.US);

    private SegmentedAssetLiveData segmentedAssetLiveData;

    public LiveData<String> fileType;

    public LiveData<String> duration;

    public LiveData<String> status;

    public LiveData<String> expectedSize;

    public LiveData<String> currentSize;

    public LiveData<String> firstPlay;

    public LiveData<String> playbackUrl;

    public AssetViewModel(@NonNull Application application,
                          @NonNull VirtuosoLiveDataFactory dataFactory,
                          String assetId) {
        super(application);

        segmentedAssetLiveData = dataFactory.getAssetStatus(assetId);
        fileType = Transformations.map(segmentedAssetLiveData, asset -> getFileType(asset));
        duration = Transformations.map(segmentedAssetLiveData, asset -> formatDuration(asset.getDuration()));
        status = Transformations.map(segmentedAssetLiveData, asset -> AssetWrapper.getStatusText(getApplication(), asset.getDownloadStatus()));
        expectedSize = Transformations.map(segmentedAssetLiveData, asset -> formatMBSize(asset.getExpectedSize()));
        currentSize = Transformations.map(segmentedAssetLiveData, asset -> formatMBSize(asset.getCurrentSize()));
        firstPlay = Transformations.map(segmentedAssetLiveData, asset -> getFirstPlayTime(asset.getFirstPlayTime()));
        playbackUrl = Transformations.map(segmentedAssetLiveData, asset -> getPlaybackUrl(asset));
    }

    public SegmentedAssetLiveData getAssetLiveData() {
        return segmentedAssetLiveData;
    }

    private String getFileType(ISegmentedAsset asset) {
        String fileTypeText = "";
        if (asset != null) {
            switch (asset.segmentedFileType()) {
                case ISegmentedAsset.SEG_FILE_TYPE_HLS:
                    fileTypeText = getApplication().getString(R.string.hls_type);
                    break;
                case ISegmentedAsset.SEG_FILE_TYPE_MPD:
                    fileTypeText = getApplication().getString(R.string.mpd_type);
                    break;
                default:
                    fileTypeText = getApplication().getString(R.string.unknown_type);
            }
        }
        return fileTypeText;
    }

    public String formatMBSize(double size) {
        return String.format(Locale.getDefault(),"%.2f MB", size/1048576.00);
    }

    public String formatDuration(long duration) {
        return String.format(Locale.getDefault(),"%d seconds", duration);
    }

    public String getFirstPlayTime(long firstPlayTime) {
        String result = getApplication().getString(R.string.not_yet_played);
        if (firstPlayTime > 0) {
            result = dateFormatter.format(new Date(firstPlayTime * 1000));
        }
        return result;
    }

    public String getPlaybackUrl(ISegmentedAsset asset) {
        String result = getApplication().getString(R.string.unavailable);
        try {
            URL playbackUrl = asset.getPlaybackURL();
            if (playbackUrl != null) {
                result = playbackUrl.toString();
            }
        } catch (MalformedURLException mue) {}
        return result;
    }

    /**
     * View model factory, used to create the AssetViewModel for a specified asset id.
     */
    public static class Factory extends ViewModelProvider.NewInstanceFactory {

        @NonNull
        private final Application application;

        @NonNull
        private final VirtuosoLiveDataFactory dataFactory;

        @NonNull
        private final String assetId;

        public Factory(@NonNull Application application, @NonNull VirtuosoLiveDataFactory dataFactory,
                       String assetId) {
            this.application = application;
            this.dataFactory = dataFactory;
            this.assetId = assetId;
        }

        @Override
        @NonNull
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new AssetViewModel(application, dataFactory, assetId);
        }

    }
}
