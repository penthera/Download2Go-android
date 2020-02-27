package com.penthera.sdkdemokotlin.catalog

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.activity.NavigationListener
import com.penthera.sdkdemokotlin.util.TextUtils
import com.penthera.sdkdemokotlin.util.inflate
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.listrow_catalog.view.*

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
        val inflatedView = parent.inflate(R.layout.listrow_catalog, false)
        return CatalogItemHolder(inflatedView)
    }

    override fun getItemCount(): Int = catalog.currentCatalog.size

    override fun onBindViewHolder(holder: CatalogItemHolder, position: Int) {
        holder.bindItem(catalog.currentCatalog.get(position), navigationListener)
    }

    /**
     * ViewHolder for catalog item view
     */
    class CatalogItemHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {

        private var view: View = v

        private var catalogItem: ExampleCatalogItem? = null

        private var navigationListener: NavigationListener? = null;

        init {
            v.setOnClickListener(this)
        }

        fun bindItem(catalogItem: ExampleCatalogItem, navigationListener: NavigationListener?) {
            this.catalogItem = catalogItem
            this.navigationListener = navigationListener

            if(!catalogItem.imageUri.isNullOrEmpty()) {
                Picasso.get()
                        .load(catalogItem.imageUri)
                        .placeholder(R.drawable.cloud)
                        .error(R.drawable.no_image)
                        .into(view.catalogImage)
            }
            view.catalogTitle.text = catalogItem.title
            view.catalogRating.text = catalogItem.contentRating
            view.catalogDuration.text = TextUtils().getDurationString(catalogItem.durationSeconds)
        }

        override fun onClick(v: View?) {
            navigationListener?.showCatalogDetailView(catalogItem!!)
        }
    }

}