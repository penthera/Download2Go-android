package com.penthera.sdkdemokotlin.activity

import androidx.fragment.app.Fragment
import com.penthera.sdkdemokotlin.catalog.ExampleCatalogItem
import com.penthera.virtuososdk.client.IAsset

/**
 *
 */
interface NavigationListener {

    fun hideLogin()

    fun showInboxDetailsView(asset: IAsset)

    fun showCatalogDetailView(item: ExampleCatalogItem)

    fun addFragment(fragment: Fragment, backStackName: String)

    fun showAddCatalogItemView()

}