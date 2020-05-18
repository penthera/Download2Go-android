package com.penthera.sdkdemokotlin.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.activity.NavigationListener
import com.penthera.sdkdemokotlin.catalog.CatalogRecyclerAdapter
import com.penthera.sdkdemokotlin.catalog.ExampleCatalog
import kotlinx.android.synthetic.main.fragment_catalog.*

/**
 * A fragment containing the top level catalog tab, which contains a simple list of the
 * available catalog items.
 */
class CatalogFragment : Fragment() , ExampleCatalog.CatalogObserver {


    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var itemDecoration: DividerItemDecoration
    private lateinit var adapter: CatalogRecyclerAdapter
    private var navigationListener: NavigationListener? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_catalog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        catalogList.layoutManager = linearLayoutManager
        itemDecoration = DividerItemDecoration(catalogList.context, linearLayoutManager.orientation)
        catalogList.addItemDecoration(itemDecoration)
        catalogList.adapter = adapter

        catalog_list_add_item.setOnClickListener() {
            navigationListener?.showAddCatalogItemView()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        linearLayoutManager = LinearLayoutManager(context)
        adapter = CatalogRecyclerAdapter(context)

        navigationListener = activity as NavigationListener
        adapter.navigationListener = navigationListener


        ExampleCatalog.getInstance(context).registerObserver(this)

    }

    override fun onDetach() {
        super.onDetach()

        ExampleCatalog.getInstance(requireContext()).unregisterObserver(this)
    }

    override fun onStart() {
        super.onStart()
        adapter.notifyDataSetChanged()
    }

    override fun catalogChanged() {
       adapter.notifyDataSetChanged()
    }
}