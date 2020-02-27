package com.penthera.sdkdemokotlin.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.dialog.DateTimeDialogFragment
import kotlinx.android.synthetic.main.fragment_add_catalog_expire.*
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

class AddItemExpiryFragment : Fragment() {

    public  var expiry : Long = -1
    public var avalable : Long = -1


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_catalog_expire, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_set_expiration.setOnClickListener { setExpiry(it) }
        btn_set_available.setOnClickListener { setExpiry(it) }

    }

    fun setExpiry(it : View) {


        var dlg : DateTimeDialogFragment?
        when (it) {

            btn_set_expiration ->{
                dlg = DateTimeDialogFragment.newInstance(object : DateTimeDialogFragment.OnDateSetListener {
                    override fun onDateTimeSet(datetime: Long) {

                        expiry = datetime;

                        txt_expiration.text = PrettyTime().format(Date(expiry))

                    }

                }, System.currentTimeMillis(), "Content Expiration Date")
            }

            btn_set_available -> {
                dlg = DateTimeDialogFragment.newInstance(object : DateTimeDialogFragment.OnDateSetListener {
                    override fun onDateTimeSet(datetime: Long) {

                        avalable = datetime;

                        txt_available.text =PrettyTime().format(Date(avalable))

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