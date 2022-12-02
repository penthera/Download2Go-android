package com.penthera.download2goX1_6;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.penthera.download2goX1_6.databinding.AssetListItemBinding;
import com.penthera.download2goX1_6.ui.AssetItemCallback;

import java.util.List;

/**
 * A RecyclerView Adapter to display the list of assets in the recyclerview on the main activity.
 * Uses data binding to the list items.
 */
public class AssetsAdapter extends RecyclerView.Adapter<AssetsAdapter.AssetViewHolder> {

    List<AssetWrapper> assetList;

    @NonNull final AssetItemCallback callback;

    public AssetsAdapter(@NonNull AssetItemCallback callback) {
        this.callback = callback;
        setHasStableIds(true);
    }

    public void setAssetList(final List<AssetWrapper> assets) {
        if (assetList == null) {
            assetList = assets;
            notifyItemRangeInserted(0, assetList.size());
        } else {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return assetList.size();
                }

                @Override
                public int getNewListSize() {
                    return assets.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return assetList.get(oldItemPosition).getDBId() ==
                            assets.get(newItemPosition).getDBId();
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    AssetWrapper newProduct = assets.get(newItemPosition);
                    AssetWrapper oldProduct = assetList.get(oldItemPosition);
                    return newProduct.getDBId() == oldProduct.getDBId()
                            && newProduct.getStatus() == oldProduct.getStatus()
                            && newProduct.getProgressPercentage() == oldProduct.getProgressPercentage();
                }
            });
            assetList = assets;
            result.dispatchUpdatesTo(this);
        }
    }

    @Override
    public int getItemCount() {
        return assetList == null ? 0 : assetList.size();
    }

    @Override
    public long getItemId(int position) {
        return assetList.get(position).getDBId();
    }

    @Override
    @NonNull
    public AssetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AssetListItemBinding binding = DataBindingUtil
                .inflate(LayoutInflater.from(parent.getContext()), R.layout.asset_list_item,
                        parent, false);
        binding.setCallbacks(callback);
        return new AssetViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AssetViewHolder holder, int position) {
        holder.binding.setAsset(assetList.get(position));
        holder.binding.executePendingBindings();
    }

    static class AssetViewHolder extends RecyclerView.ViewHolder {

        final AssetListItemBinding binding;

        public AssetViewHolder(AssetListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
