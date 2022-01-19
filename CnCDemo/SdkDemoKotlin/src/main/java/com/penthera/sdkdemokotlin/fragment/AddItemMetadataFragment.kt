package com.penthera.sdkdemokotlin.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.penthera.sdkdemokotlin.databinding.FragmentAddCatalogMetaBinding
import com.penthera.sdkdemokotlin.dialog.TextInputDialog
import com.squareup.picasso.Picasso

class AddItemMetadataFragment : Fragment() {

    private var _binding: FragmentAddCatalogMetaBinding? = null

    public var imgUrl :String? = null

    val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAddCatalogMetaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imgItemTapPrompt.setOnClickListener {
            TextInputDialog.newInstance(object : TextInputDialog.TextInputObserver{
                override fun complete(value: String) {
                    imgUrl = value;

                    binding.imgItemTapPrompt.visibility = View.GONE
                    loadImage()
                }

            },"", "Image url").show(childFragmentManager,"url input")
        }
    }

    fun loadImage(){
        Picasso.get().load(imgUrl).error(android.R.drawable.ic_menu_help).into(binding.imgItemThumbnail)
    }
}