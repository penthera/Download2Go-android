package com.penthera.sdkdemokotlin.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.penthera.sdkdemokotlin.databinding.DialogProgressBinding

/**
 *
 */
class CancellableProgressDialog : DialogFragment() {

    interface CancelDialogListener {
        fun cancel()
    }

    private var listener: CancelDialogListener? = null
    private lateinit var title: String

    private var _binding: DialogProgressBinding? = null

    companion object {
        fun newInstance(listener: CancelDialogListener?, title: String): CancellableProgressDialog {
           return  CancellableProgressDialog().apply {
               this.listener = listener
               this.title = title
           }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, 0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogProgressBinding.inflate(inflater, container, false)
        val binding = _binding!!
        binding.progressTitle.text = title
        binding.btnCancelProgress.setOnClickListener {
            listener?.cancel()
            dismiss()
        }
        return binding.root;
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}