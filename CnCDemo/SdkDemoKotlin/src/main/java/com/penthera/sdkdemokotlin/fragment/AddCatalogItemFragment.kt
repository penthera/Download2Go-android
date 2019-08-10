package com.penthera.sdkdemokotlin.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.catalog.ExampleCatalog
import com.penthera.sdkdemokotlin.catalog.ExampleCatalogItem
import kotlinx.android.synthetic.main.fragment_add_catalog_item.*
import android.R.array
import android.app.DatePickerDialog
import android.content.Context
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.widget.ArrayAdapter
import android.widget.TextView
import com.penthera.sdkdemokotlin.catalog.CatalogItemType
import com.penthera.sdkdemokotlin.dialog.DateTimeDialogFragment
import kotlinx.android.synthetic.main.datetime_dialog.*
import kotlinx.android.synthetic.main.fragment_add_catalog_content.*
import kotlinx.android.synthetic.main.fragment_add_catalog_meta.*
import kotlinx.android.synthetic.main.fragment_asset_detail.*
import java.lang.NumberFormatException


class AddCatalogItemFragment : Fragment(){

    private lateinit var imgUrl : String

    private lateinit var adapter : AddItemPagerAdapter;

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_catalog_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AddItemPagerAdapter(childFragmentManager, context)
        add_item_pager.adapter = adapter
        add_item_pager.offscreenPageLimit = 2
        add_item_tabs.setupWithViewPager(add_item_pager)


        /*

        setDateText(txt_expiration, expirationDate, "Never" )

        btn_set_expiration.setOnClickListener {
            DateTimeDialogFragment().newInstance(object :  DateTimeDialogFragment.OnDateSetListener {
                override fun onDateTimeSet(dateTime : Long){
                    expirationDate = dateTime
                }
            },expirationDate, "Asset Expiration Date")
        }*/
        add_item_save.setOnClickListener {

            val newItem  = ExampleCatalogItem(adapter.assetId(),
                                            adapter.title(),
                                            adapter.contentUrl(),
                                            adapter.catalogItemType(),
                                            adapter.mimeType(),
                                            adapter.description(),
                                            -1,0,0,
                                            adapter.rating(),
                                            adapter.durationSeconds(),
                                    adapter.imgUrl() ?: "")
            ExampleCatalog.getInstance(context!!).addAndStore(newItem)

            activity?.onBackPressed()
        }
    }

    private class AddItemPagerAdapter (fm: FragmentManager, context: Context?) : FragmentPagerAdapter(fm) {

        val context = context
        val content : AddItemContentFragment = AddItemContentFragment()
        val meta : AddItemMetadataFragment = AddItemMetadataFragment()
        val expire : AddItemExpiryFragment = AddItemExpiryFragment()

        override fun getItem(position : Int): Fragment {
            when (position){
                0 -> {
                    return content
                }

                1 -> {
                    return meta;
                }

                else -> {
                    return expire
                }
            }

        }

        override fun getCount(): Int {
            return 3
        }

        override fun getPageTitle(position: Int): CharSequence? {
            val resid = when (position) {
                0 -> R.string.content_tab_lbl
                1 -> R.string.meta_tab_lbl
                else -> R.string.expiry_tab_lbl
            }
            return context?.getString(resid)
        }


        fun catalogItemType() : CatalogItemType{
            return when(content.spinner_item_type.selectedItemPosition){
                0 -> CatalogItemType.FILE
                1 -> CatalogItemType.HLS_MANIFEST
                2 -> CatalogItemType.DASH_MANIFEST
                else -> CatalogItemType.FILE
            }


        }
        fun title() : String{
            return meta.title_input_layout.editText?.editableText.toString()
        }

        fun description() : String{
            return meta.text_input_item_desc.editText?.editableText.toString()
        }

        fun contentUrl(): String{
            return content.text_input_url.editText?.editableText.toString()
        }

        fun assetId() :String{
            return content.text_input_asset_id.editText?.editableText.toString()
        }

        fun mimeType() : String{
            return content.txt_input_item_mime_type.editText?.editableText.toString()
        }

        fun durationSeconds() : Int{
            var ret = 1000;
            try {
                ret = content.txt_input_item_duration.editText?.editableText.toString().toInt()
            }
            catch (e : NumberFormatException){

            }
            return ret;
        }

        fun rating() : String{
            return meta.text_input_item_rating.editText?.editableText.toString()
        }

        fun imgUrl() : String? {
            return meta.imgUrl;
        }

    }

}