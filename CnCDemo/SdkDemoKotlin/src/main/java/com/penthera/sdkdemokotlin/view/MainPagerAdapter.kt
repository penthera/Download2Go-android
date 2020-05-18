package com.penthera.sdkdemokotlin.view

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.fragment.CatalogFragment
import com.penthera.sdkdemokotlin.fragment.InboxFragment
import com.penthera.sdkdemokotlin.fragment.OtherViewFragment

/**
 *
 */
class MainPagerAdapter (fm:FragmentManager, val context: Context?) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {

        val ret : Fragment
        when (position) {
            0 -> ret =  InboxFragment.newInstance()
            1 -> ret = CatalogFragment()
            else -> ret =  OtherViewFragment()
        }

        return ret
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