package com.penthera.sdkdemokotlin.dialog

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.penthera.sdkdemokotlin.R
import java.util.*

internal class DateTimePicker @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : RelativeLayout(context, attrs, defStyle), View.OnClickListener, DatePicker.OnDateChangedListener, TimePicker.OnTimeChangedListener {

    // DatePicker reference
    private val datePicker: DatePicker
    // TimePicker reference
    private val timePicker: TimePicker
    // ViewSwitcher reference
    private val viewSwitcher: ViewSwitcher
    // Calendar reference
    private val mCalendar: Calendar

    var dateTimeMillis: Long
        get() = mCalendar.timeInMillis
        set(timeMillis) {
            mCalendar.timeInMillis = timeMillis
            updateDate(mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH))
            updateTime(mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE))
        }

    // Convenience wrapper for internal TimePicker instance
    // Convenience wrapper for internal TimePicker instance
    var is24HourView: Boolean
        get() = timePicker.is24HourView
        set(is24HourView) = timePicker.setIs24HourView(is24HourView)

    init {

        // Inflate myself
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.datetimepicker, this, true)

        // Inflate the date and time picker views
        val datePickerView = inflater.inflate(R.layout.datepicker, null) as LinearLayout
        val timePickerView = inflater.inflate(R.layout.timepicker, null) as LinearLayout

        mCalendar = Calendar.getInstance()
        viewSwitcher = this.findViewById<View>(R.id.DateTimePickerVS) as ViewSwitcher

        // Init date picker
        datePicker = datePickerView.findViewById<View>(R.id.DatePicker) as DatePicker
        datePicker.init(mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH), this)

        // Init time picker
        timePicker = timePickerView.findViewById<View>(R.id.TimePicker) as TimePicker
        timePicker.setOnTimeChangedListener(this)

        // Handle button clicks
        (findViewById<View>(R.id.SwitchToTime) as Button).setOnClickListener(this) // shows the time picker
        (findViewById<View>(R.id.SwitchToDate) as Button).setOnClickListener(this) // shows the date picker

        // Populate ViewSwitcher
        viewSwitcher.addView(datePickerView, 0)
        viewSwitcher.addView(timePickerView, 1)
    }

    override fun onDateChanged(view: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        // Update the internal Calendar instance
        mCalendar.set(year, monthOfYear, dayOfMonth, mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE))
    }

    override fun onTimeChanged(view: TimePicker, hourOfDay: Int, minute: Int) {
        // Update the internal Calendar instance
        mCalendar.set(mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH), hourOfDay, minute)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.SwitchToDate -> {
                v.isEnabled = false
                findViewById<View>(R.id.SwitchToTime).isEnabled = true
                viewSwitcher.showPrevious()
            }

            R.id.SwitchToTime -> {
                v.isEnabled = false
                findViewById<View>(R.id.SwitchToDate).isEnabled = true
                viewSwitcher.showNext()
            }
        }
    }

    // Convenience wrapper for internal Calendar instance
    operator fun get(field: Int): Int {
        return mCalendar.get(field)
    }

    // Convenience wrapper for internal DatePicker instance
    fun updateDate(year: Int, monthOfYear: Int, dayOfMonth: Int) {
        datePicker.updateDate(year, monthOfYear, dayOfMonth)
    }

    // Convenience wrapper for internal TimePicker instance
    fun updateTime(currentHour: Int, currentMinute: Int) {
        timePicker.hour = currentHour
        timePicker.minute = currentMinute
    }
}