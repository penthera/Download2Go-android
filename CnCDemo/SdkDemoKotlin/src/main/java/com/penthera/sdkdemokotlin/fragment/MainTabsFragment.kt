package com.penthera.sdkdemokotlin.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.view.MainPagerAdapter
import kotlinx.android.synthetic.main.fragment_main_tabs.view.*

/**
 *
 */
class MainTabsFragment  : Fragment() {

    companion object {
        fun newInstance(): MainTabsFragment {
            return MainTabsFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.fragment_main_tabs, container, false)

        fragmentManager?.apply {
            val fragmentAdapter = MainPagerAdapter(this, context)
            view.viewpager_main.adapter = fragmentAdapter
            view.tabs_main.setupWithViewPager(view.viewpager_main)

        }
        return view
    }
}