package com.penthera.download2goX1_6.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.penthera.download2goX1_6.AssetWrapper;
import com.penthera.virtuososdk.androidxsupport.AllAssetsLiveData;
import com.penthera.virtuososdk.androidxsupport.VirtuosoLiveDataFactory;
import com.penthera.virtuososdk.client.ISegmentedAsset;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple view model to contain and transform the SDK provided LiveData for all current assets.
 * Other LiveData objects are available from the factory for assets in the download queue, downloaded assets,
 * or expired assets.
 */
public class AssetListViewModel extends AndroidViewModel {

    private final AllAssetsLiveData assets;
    private final LiveData<List<AssetWrapper>> uiAssets;

    public AssetListViewModel(@NonNull Application application, @NonNull VirtuosoLiveDataFactory dataFactory) {
        super(application);
        assets = dataFactory.getAssetList();
        uiAssets = Transformations.map(assets, assetVals -> {
            ArrayList<AssetWrapper> resultList = new ArrayList<>();
            for(ISegmentedAsset a : assetVals){
                resultList.add(new AssetWrapper(application, a));
            }
            return resultList;
        });
    }

    /**
     * Get the original assets LiveData for the segmented assets list.
     * @return The asset live data direct from the SDK
     */
    public AllAssetsLiveData getAssets() {
        return assets;
    }

    /**
     * Get assets which have been wrapped in a concrete class (instead of the SDK interface) so
     * that they can be used directly within data binding to the UI.
     * @return Live Data containing a list of AssetWrapper objects suitable for UI data binding
     */
    public LiveData<List<AssetWrapper>> getUiAssets() {
        return uiAssets;
    }

    /**
     * View model factory, used to create the AssetListViewModel.
     */
    public static class Factory extends ViewModelProvider.NewInstanceFactory {

        @NonNull
        private final Application application;

        @NonNull
        private final VirtuosoLiveDataFactory dataFactory;

        public Factory(@NonNull Application application, @NonNull VirtuosoLiveDataFactory dataFactory) {
            this.application = application;
            this.dataFactory = dataFactory;
        }

        @Override
        @NonNull
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new AssetListViewModel(application, dataFactory);
        }

    }
}
