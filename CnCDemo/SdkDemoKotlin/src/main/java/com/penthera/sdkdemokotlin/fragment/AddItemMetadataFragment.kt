package com.penthera.sdkdemokotlin.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.dialog.TextInputDialog
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_add_catalog_meta.*

class AddItemMetadataFragment : Fragment() {

    public var imgUrl :String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_catalog_meta, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        img_item_tap_prompt.setOnClickListener {
            TextInputDialog.newInstance(object : TextInputDialog.TextInputObserver{
                override fun complete(value: String) {
                    imgUrl = value;

                    img_item_tap_prompt.visibility = View.GONE
                    loadImage()
                }

            },"", "Image url").show(childFragmentManager,"url input")
        }
    }

    fun loadImage(){
        Picasso.get().load(imgUrl).error(android.R.drawable.ic_menu_help).into(img_item_thumbnail)
    }
}