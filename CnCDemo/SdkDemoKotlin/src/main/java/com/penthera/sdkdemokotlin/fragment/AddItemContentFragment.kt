package com.penthera.sdkdemokotlin.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.penthera.sdkdemokotlin.R
import kotlinx.android.synthetic.main.fragment_add_catalog_content.*

class AddItemContentFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val ret : View =  inflater.inflate(R.layout.fragment_add_catalog_content, container, false)


        val adapter = ArrayAdapter.createFromResource(context!!, R.array.add_item_content_type, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_item_type.setAdapter(adapter)

        return ret
    }
}