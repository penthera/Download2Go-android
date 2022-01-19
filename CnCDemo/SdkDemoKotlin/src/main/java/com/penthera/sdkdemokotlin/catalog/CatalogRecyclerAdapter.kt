package com.penthera.sdkdemokotlin.catalog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.activity.NavigationListener
import com.penthera.sdkdemokotlin.databinding.ListrowCatalogBinding
import com.penthera.sdkdemokotlin.util.TextUtils
import com.penthera.sdkdemokotlin.util.inflate
import com.squareup.picasso.Picasso

/**
 *
 */
class CatalogRecyclerAdapter(context: Context) : RecyclerView.Adapter<CatalogRecyclerAdapter.CatalogItemHolder>() {

    private var catalog: ExampleCatalog

    var navigationListener: NavigationListener? = null;

    init {
        catalog = ExampleCatalog.getInstance(context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatalogItemHolder {
        val itemBinding = ListrowCatalogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CatalogItemHolder(itemBinding)
    }

    override fun getItemCount(): Int = catalog.currentCatalog.size

    override fun onBindViewHolder(holder: CatalogItemHolder, position: Int) {
        holder.bindItem(catalog.currentCatalog.get(position), navigationListener)
    }

    /**
     * ViewHolder for catalog item view
     */
    class CatalogItemHolder(private val itemBinding: ListrowCatalogBinding) : RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {

        private var catalogItem: ExampleCatalogItem? = null

        private var navigationListener: NavigationListener? = null;

        init {
            itemBinding.root.setOnClickListener(this)
        }

        fun bindItem(catalogItem: ExampleCatalogItem, navigationListener: NavigationListener?) {
            this.catalogItem = catalogItem
            this.navigationListener = navigationListener

            if(!catalogItem.imageUri.isNullOrEmpty()) {
                Picasso.get()
                        .load(catalogItem.imageUri)
                        .placeholder(R.drawable.cloud)
                        .error(R.drawable.no_image)
                        .into(itemBinding.catalogImage)
            }
            itemBinding.catalogTitle.text = catalogItem.title
            itemBinding.catalogRating.text = catalogItem.contentRating
            itemBinding.catalogDuration.text = TextUtils().getDurationString(catalogItem.durationSeconds)
        }

        override fun onClick(v: View?) {
            navigationListener?.showCatalogDetailView(catalogItem!!)
        }
    }

}