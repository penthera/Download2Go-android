package com.penthera.sdkdemokotlin.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.penthera.sdkdemokotlin.databinding.DialogNicknameBinding
import com.penthera.virtuososdk.client.IBackplaneDevice

/**
 * A simple dialog which enables editing a string field and applying or cancelling.
 */
class ChangeNicknameDialogFragment : DialogFragment() {

    interface ChangeNicknameObserver {
        fun onChanged(device: IBackplaneDevice, nickname: String)
    }

    private lateinit var listener: ChangeNicknameObserver
    private lateinit var device: IBackplaneDevice

    private var _binding: DialogNicknameBinding? = null

    companion object {
        fun newInstance(listener: ChangeNicknameObserver, device: IBackplaneDevice) : ChangeNicknameDialogFragment {
            return ChangeNicknameDialogFragment().apply{
                this.listener = listener
                this.device = device
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, 0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogNicknameBinding.inflate(inflater, container, false)
        val binding = _binding!!

        binding.nickname.setText(device.nickname())
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnApply.setOnClickListener {
            listener.onChanged(device, binding.nickname.text.toString())
            dismiss()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}