package com.penthera.sdkdemokotlin.dialog

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.penthera.sdkdemokotlin.R


class DateTimeDialogFragment : DialogFragment() {


    private var mDateSetListener: OnDateSetListener? = null
    private var mDateTimePicker: DateTimePicker? = null
    private var mLayout: View? = null
    private var mTitle: String? = null
    private var mInitialTime: Long = 0

    /**
     * The callback used to indicate the user is done filling in the date.
     */
    interface OnDateSetListener {

        /**
         * @param datetime The date / time set in ms
         */
        fun onDateTimeSet(datetime: Long)
    }

    companion object {
        fun newInstance(listener: OnDateSetListener, currentTime: Long, title: String): DateTimeDialogFragment {
            val f = DateTimeDialogFragment()
            f.mDateSetListener = listener
            f.mTitle = title
            f.mInitialTime = if (currentTime != java.lang.Long.MAX_VALUE) currentTime else System.currentTimeMillis()
            return f
        }

    }

    // onCreateView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        mLayout = inflater.inflate(R.layout.datetime_dialog, container, false)

        getDialog().setTitle(mTitle)

        mDateTimePicker = mLayout!!.findViewById(R.id.DateTimePicker)
        mDateTimePicker!!.is24HourView = true
        mDateTimePicker!!.dateTimeMillis = mInitialTime

        mLayout!!.findViewById<View>(R.id.SetDateTime).setOnClickListener {
            if (mDateSetListener != null) {
                mDateSetListener!!.onDateTimeSet(mDateTimePicker!!.dateTimeMillis)
            }
            dismiss()
        }

        mLayout!!.findViewById<View>(R.id.ResetDateTime).setOnClickListener { mDateTimePicker!!.dateTimeMillis = mInitialTime }

        mLayout!!.findViewById<View>(R.id.CancelDialog).setOnClickListener { dismiss() }

        return mLayout
    }
}