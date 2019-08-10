package com.penthera.sdkdemokotlin.view

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.engine.AssetsRecyclerAdapter
import com.penthera.sdkdemokotlin.util.inflate
import kotlinx.android.synthetic.main.listrow_header.view.*

/**
 * A simple header adapter for use within the merge adapter. Shows a header row only in the
 * associated adapter contains rows.
 */
open class HeaderRecyclerAdapter(headerTitle: String, adapter: AssetsRecyclerAdapter) : RecyclerView.Adapter<HeaderRecyclerAdapter.HeaderHolder>() {

    private val title: String = headerTitle

    private val adapter: AssetsRecyclerAdapter = adapter

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderRecyclerAdapter.HeaderHolder {
        val inflatedView = parent.inflate(R.layout.listrow_header, false)
        return HeaderHolder(inflatedView, title)
    }

    override fun getItemCount(): Int = if(adapter.itemCount > 0) 1 else 0

    override fun onBindViewHolder(holder: HeaderRecyclerAdapter.HeaderHolder, position: Int) {
        holder.bindItem()
    }

    /**
     * ViewHolder for catalog item view
     */
    class HeaderHolder(v: View, title: String) : RecyclerView.ViewHolder(v) {

        private var view: View = v

        private val title: String = title

        fun bindItem() {
            view.headerText.text = title
        }

    }
}