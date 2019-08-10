package com.penthera.sdkdemokotlin.view

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.fragment.CatalogFragment
import com.penthera.sdkdemokotlin.fragment.InboxFragment
import com.penthera.sdkdemokotlin.fragment.OtherViewFragment

/**
 *
 */
class MainPagerAdapter (fm:FragmentManager, context: Context?) : FragmentPagerAdapter(fm) {

    val context = context

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> {
                return InboxFragment.newInstance()
            }
            1 -> {
                return CatalogFragment()
            }
            else -> {
                return OtherViewFragment();
            }
        }
    }

    override fun getCount(): Int = 3

    override fun getPageTitle(position: Int): CharSequence? {
        val resid = when (position) {
            0 -> R.string.inbox
            1 -> R.string.catalog
            else -> R.string.other
        }
        return context?.getString(resid)
    }
}