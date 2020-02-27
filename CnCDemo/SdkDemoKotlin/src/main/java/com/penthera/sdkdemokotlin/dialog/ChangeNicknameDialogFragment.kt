package com.penthera.sdkdemokotlin.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.penthera.sdkdemokotlin.R
import com.penthera.virtuososdk.client.IBackplaneDevice
import kotlinx.android.synthetic.main.dialog_nickname.view.*

/**
 * A simple dialog which enables editing a string field and applying or cancelling.
 */
class ChangeNicknameDialogFragment : DialogFragment() {

    interface ChangeNicknameObserver {
        fun onChanged(device: IBackplaneDevice, nickname: String)
    }

    private lateinit var listener: ChangeNicknameObserver
    private lateinit var device: IBackplaneDevice

    companion object {
        fun newInstance(listener: ChangeNicknameObserver, device: IBackplaneDevice) : ChangeNicknameDialogFragment {
            var dialog = ChangeNicknameDialogFragment()
            dialog.listener = listener
            dialog.device = device
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, 0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_nickname, container, false)

        view.nickname.setText(device.nickname())
        view.btnCancel.setOnClickListener(View.OnClickListener { dismiss() })
        view.btnApply.setOnClickListener(View.OnClickListener {
            listener.onChanged(device, view.nickname.text.toString())
            dismiss()
        })

        return view;
    }
}