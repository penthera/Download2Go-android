package com.penthera.sdkdemokotlin.view


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.penthera.sdkdemokotlin.databinding.ListrowHeaderBinding
import com.penthera.sdkdemokotlin.engine.AssetsRecyclerAdapter

/**
 * A simple header adapter for use within the merge adapter. Shows a header row only in the
 * associated adapter contains rows.
 */
open class HeaderRecyclerAdapter(headerTitle: String, adapter: AssetsRecyclerAdapter) : RecyclerView.Adapter<HeaderRecyclerAdapter.HeaderHolder>() {

    private val title: String = headerTitle

    private val adapter: AssetsRecyclerAdapter = adapter

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderRecyclerAdapter.HeaderHolder {
        val itemBinding = ListrowHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HeaderHolder(itemBinding, title)
    }

    override fun getItemCount(): Int = if(adapter.itemCount > 0) 1 else 0

    override fun onBindViewHolder(holder: HeaderRecyclerAdapter.HeaderHolder, position: Int) {
        holder.bindItem()
    }

    /**
     * ViewHolder for catalog item view
     */
    class HeaderHolder(private val itemBinding: ListrowHeaderBinding, private val title: String) : RecyclerView.ViewHolder(itemBinding.root) {

        fun bindItem() {
            itemBinding.headerText.text = title
        }

    }
}