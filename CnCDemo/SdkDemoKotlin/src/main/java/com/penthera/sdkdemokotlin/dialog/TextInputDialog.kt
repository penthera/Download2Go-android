package com.penthera.sdkdemokotlin.dialog

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.penthera.sdkdemokotlin.databinding.DialogTextInputBinding

class TextInputDialog : DialogFragment() {

    interface TextInputObserver{
        fun complete(value : String)
    }

    private lateinit var hint : String
    private lateinit var value : String
    private lateinit var observer: TextInputObserver

    private var _binding: DialogTextInputBinding? = null

    private val binding get() = _binding!!

    companion object {
        fun newInstance(observer: TextInputObserver , value : String, hint : String) : TextInputDialog{
            return TextInputDialog().apply{
                this.hint = hint
                this.value = value
                this.observer = observer
            }
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogTextInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.txtInputValue.hint = hint
        binding.txtInputValue.editText?.setText( value)

        binding.inputBtnDone.setOnClickListener() {
            value = binding.txtInputValue.editText?.editableText.toString()
            observer.complete(value)
            dismiss()
        }
    }

}