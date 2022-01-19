package com.penthera.sdkdemokotlin.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.databinding.FragmentAddCatalogContentBinding

class AddItemContentFragment : Fragment() {

    private var _binding: FragmentAddCatalogContentBinding? = null

    val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        _binding = FragmentAddCatalogContentBinding.inflate(inflater, container, false)

        val adapter = ArrayAdapter.createFromResource(requireContext(), R.array.add_item_content_type, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerItemType.adapter = adapter

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}