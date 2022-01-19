package com.penthera.sdkdemokotlin.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.penthera.sdkdemokotlin.databinding.FragmentMainTabsBinding
import com.penthera.sdkdemokotlin.view.MainPagerAdapter

/**
 *
 */
class MainTabsFragment  : Fragment() {

    companion object {
        fun newInstance(): MainTabsFragment {
            return MainTabsFragment()
        }
    }

    private var _binding: FragmentMainTabsBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMainTabsBinding.inflate(inflater, container, false)

        parentFragmentManager.apply {
            val fragmentAdapter = MainPagerAdapter(this, context)
            binding.viewpagerMain.adapter = fragmentAdapter
            binding.tabsMain.setupWithViewPager(binding.viewpagerMain)

        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}