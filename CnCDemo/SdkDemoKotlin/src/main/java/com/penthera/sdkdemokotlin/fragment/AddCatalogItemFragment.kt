package com.penthera.sdkdemokotlin.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.catalog.ExampleCatalog
import com.penthera.sdkdemokotlin.catalog.ExampleCatalogItem
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.penthera.sdkdemokotlin.catalog.CatalogItemType
import com.penthera.sdkdemokotlin.databinding.FragmentAddCatalogItemBinding
import java.lang.NumberFormatException


class AddCatalogItemFragment : Fragment(){

    private var _binding: FragmentAddCatalogItemBinding? = null

    private lateinit var imgUrl : String

    private lateinit var adapter : AddItemPagerAdapter;

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAddCatalogItemBinding.inflate(inflater, container, false);
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AddItemPagerAdapter(childFragmentManager, context)
        binding.addItemPager.adapter = adapter
        binding.addItemPager.offscreenPageLimit = 2
        binding.addItemTabs.setupWithViewPager(binding.addItemPager)


        /*

        setDateText(txt_expiration, expirationDate, "Never" )

        btn_set_expiration.setOnClickListener {
            DateTimeDialogFragment().newInstance(object :  DateTimeDialogFragment.OnDateSetListener {
                override fun onDateTimeSet(dateTime : Long){
                    expirationDate = dateTime
                }
            },expirationDate, "Asset Expiration Date")
        }*/
        binding.addItemSave.setOnClickListener {

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
            ExampleCatalog.getInstance(requireContext()).addAndStore(newItem)

            activity?.onBackPressed()
        }
    }

    private class AddItemPagerAdapter (fm: FragmentManager, context: Context?) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

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
            return when(content.binding.spinnerItemType.selectedItemPosition){
                0 -> CatalogItemType.FILE
                1 -> CatalogItemType.HLS_MANIFEST
                2 -> CatalogItemType.DASH_MANIFEST
                else -> CatalogItemType.FILE
            }


        }
        fun title() : String{
            return meta.binding.titleInputLayout.editText?.editableText.toString()
        }

        fun description() : String{
            return meta.binding.textInputItemDesc.editText?.editableText.toString()
        }

        fun contentUrl(): String{
            return content.binding.textInputUrl.editText?.editableText.toString()
        }

        fun assetId() :String{
            return content.binding.textInputAssetId.editText?.editableText.toString()
        }

        fun mimeType() : String{
            return content.binding.txtInputItemMimeType.editText?.editableText.toString()
        }

        fun durationSeconds() : Int{
            var ret = 1000;
            try {
                ret = content.binding.txtInputItemDuration.editText?.editableText.toString().toInt()
            }
            catch (e : NumberFormatException){

            }
            return ret;
        }

        fun rating() : String{
            return meta.binding.textInputItemRating.editText?.editableText.toString()
        }

        fun imgUrl() : String? {
            return meta.imgUrl;
        }

    }

}