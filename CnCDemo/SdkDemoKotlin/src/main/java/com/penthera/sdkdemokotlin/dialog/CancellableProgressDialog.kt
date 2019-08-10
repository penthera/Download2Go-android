package com.penthera.sdkdemokotlin.dialog

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.penthera.sdkdemokotlin.R
import kotlinx.android.synthetic.main.dialog_progress.view.*

/**
 *
 */
class CancellableProgressDialog : DialogFragment() {

    interface CancelDialogListener {
        fun cancel()
    }

    private var listener: CancelDialogListener? = null
    private lateinit var title: String

    companion object {
        fun newInstance(listener: CancelDialogListener?, title: String): CancellableProgressDialog {
            val dialog = CancellableProgressDialog()
            dialog.listener = listener
            dialog.title = title
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(android.support.v4.app.DialogFragment.STYLE_NORMAL, 0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_progress, container, false)
        view.progressTitle.text = title
        view.btnCancelProgress.setOnClickListener(View.OnClickListener {
            listener?.cancel()
            dismiss()
        })
        return view;
    }

}