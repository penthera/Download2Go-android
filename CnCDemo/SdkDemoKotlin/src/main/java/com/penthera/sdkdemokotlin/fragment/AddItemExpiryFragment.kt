package com.penthera.sdkdemokotlin.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.penthera.sdkdemokotlin.databinding.FragmentAddCatalogExpireBinding
import com.penthera.sdkdemokotlin.dialog.DateTimeDialogFragment
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

class AddItemExpiryFragment : Fragment() {

    public  var expiry : Long = -1
    public var avalable : Long = -1

    private var _binding: FragmentAddCatalogExpireBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAddCatalogExpireBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSetExpiration.setOnClickListener { setExpiry(it) }
        binding.btnSetAvailable.setOnClickListener { setExpiry(it) }

    }

    fun setExpiry(it : View) {


        var dlg : DateTimeDialogFragment?
        when (it) {

            binding.btnSetExpiration ->{
                dlg = DateTimeDialogFragment.newInstance(object : DateTimeDialogFragment.OnDateSetListener {
                    override fun onDateTimeSet(datetime: Long) {

                        expiry = datetime;

                        binding.txtExpiration.text = PrettyTime().format(Date(expiry))

                    }

                }, System.currentTimeMillis(), "Content Expiration Date")
            }

            binding.btnSetAvailable -> {
                dlg = DateTimeDialogFragment.newInstance(object : DateTimeDialogFragment.OnDateSetListener {
                    override fun onDateTimeSet(datetime: Long) {

                        avalable = datetime;

                        binding.txtAvailable.text =PrettyTime().format(Date(avalable))

                    }

                }, System.currentTimeMillis(), "Content available date")

            }
            else -> {
                dlg = null
            }

        }

        dlg?.show(childFragmentManager, "DATE")

    }


}