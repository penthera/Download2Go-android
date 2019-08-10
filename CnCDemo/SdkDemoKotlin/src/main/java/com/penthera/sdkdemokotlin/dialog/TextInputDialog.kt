package com.penthera.sdkdemokotlin.dialog

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.penthera.sdkdemokotlin.R
import kotlinx.android.synthetic.main.dialog_text_input.*

class TextInputDialog : DialogFragment() {

    interface TextInputObserver{
        fun complete(value : String)
    }

    private lateinit var hint : String
    private lateinit var value : String
    private lateinit var observer: TextInputObserver


    companion object {
        fun newInstance(observer: TextInputObserver , value : String, hint : String) : TextInputDialog{
            var dialog = TextInputDialog()
            dialog.hint = hint
            dialog.value = value
            dialog.observer = observer

            return dialog

        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_text_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txt_input_value.hint = hint
        txt_input_value.editText?.setText( value)

        input_btn_done.setOnClickListener() {
            value = txt_input_value.editText?.editableText.toString()
            observer.complete(value)
            dismiss()
        }
    }

}