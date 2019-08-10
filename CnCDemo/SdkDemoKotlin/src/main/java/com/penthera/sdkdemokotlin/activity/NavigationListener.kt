package com.penthera.sdkdemokotlin.activity

import android.support.v4.app.Fragment
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