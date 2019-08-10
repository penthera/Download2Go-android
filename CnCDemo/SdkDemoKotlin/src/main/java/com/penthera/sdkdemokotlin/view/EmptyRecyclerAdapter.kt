package com.penthera.sdkdemokotlin.view

import com.penthera.sdkdemokotlin.engine.AssetsRecyclerAdapter

/**
 *
 */
class EmptyRecyclerAdapter (headerTitle: String, adapters: List<AssetsRecyclerAdapter>) : HeaderRecyclerAdapter(headerTitle, adapters.get(0)) {

    private val adapters: List<AssetsRecyclerAdapter> = adapters

    override fun getItemCount(): Int {
        var count = 0;
        adapters.forEach { count += it.itemCount }
        return if (count == 0) 1 else 0
    }
}