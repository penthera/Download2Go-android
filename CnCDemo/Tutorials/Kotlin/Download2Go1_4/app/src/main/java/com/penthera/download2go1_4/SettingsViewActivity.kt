package com.penthera.download2go1_4

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.EditText
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.penthera.virtuososdk.client.ISettings
import com.penthera.virtuososdk.client.Virtuoso
import java.text.SimpleDateFormat
import java.util.*

/**
 *
 */
class SettingsViewActivity : AppCompatActivity() {
    // We can keep a single instance of the Virtuoso object in an application class, or create a
    // new copy in each activity.
    private lateinit var virtuoso: Virtuoso

    /** Handle to the settings interface  */
    private lateinit var settings: ISettings
    // Settings controls
    /** Change the current setting for max storage  */
    private var maxstorage: EditText? = null

    /** Change the current value for headroom  */
    private var headroomEdit: EditText? = null

    /** The battery level text  */
    private var batteryDetail: TextView? = null

    /** The current value for battery threshold  */
    private var batterythreshold: SeekBar? = null

    /** Change the current vlaue for cell quota  */
    private var cellquota: EditText? = null

    /** The time the cell quota started from  */
    private var cellquotastart: TextView? = null

    /** The connection timeout for HTTP transactions  */
    private var connectionTimeout: EditText? = null

    /** The socket timeout for HTTP transactions  */
    private var socketTimeout: EditText? = null

    /** The maximum number of concurrent download connections used by the downloader  */
    private var maxConcurrentConnections: EditText? = null

    /** The current percent based progress rate used by Virtuoso  */
    private var progressPercent: SeekBar? = null

    /** The percent level  */
    private var progressPercentDetails: TextView? = null

    /** The current time based progress rate used by Virtuoso  */
    private var progressTimed: EditText? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        virtuoso = Virtuoso(this)
        settings = virtuoso.settings
        maxstorage = findViewById(R.id.max_storage_value)
        headroomEdit = findViewById(R.id.headroom_value)
        batterythreshold = findViewById(R.id.battery_value)
        cellquota = findViewById(R.id.cellquota_value)
        cellquotastart = findViewById(R.id.cellquota_date_value)
        connectionTimeout = findViewById(R.id.edt_connection_timeout)
        socketTimeout = findViewById(R.id.edt_socket_timeout)
        maxConcurrentConnections = findViewById(R.id.edt_max_connections)
        progressPercent = findViewById(R.id.progress_percent_value)
        progressTimed = findViewById(R.id.progress_timed_value)
        batteryDetail = findViewById(R.id.battery_label)
        progressPercentDetails = findViewById(R.id.progress_percent_label)
        val seekProgressChangeListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    progressPercentDetails?.text = getString(R.string.report_progress_percent, progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }
        progressPercent?.setOnSeekBarChangeListener(seekProgressChangeListener)
        val seekChangeListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    batteryDetail?.text = getString(R.string.battery_threshold, progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }
        batterythreshold?.setOnSeekBarChangeListener(seekChangeListener)
    }

    public override fun onResume() {
        super.onResume()

        // Resume the Download2Go SDK on activity resume
        virtuoso.onResume()
        updateSettingsValues()
    }

    public override fun onPause() {
        super.onPause()

        // Pause the Download2Go SDK on activity pause
        virtuoso.onPause()
    }

    fun onGlobalReset() {
        settings.apply {
            resetMaxStorageAllowed()
            resetHeadroom()
            resetBatteryThreshold()
            resetCellularDataQuota()
            resetCellularDataQuotaStart()
            resetHTTPConnectionTimeout()
            resetHTTPSocketTimeout()
            resetMaxDownloadConnections()
            resetProgressUpdateByPercent()
            resetProgressUpdateByTime()
        }
    }

    fun onApplySettings() {
        try {
            settings.apply {
                maxStorageAllowed = maxstorage!!.text.toString().toLong()
                headroom = headroomEdit!!.text.toString().toLong()
                batteryThreshold =  batterythreshold!!.progress.toFloat() / 100
                cellularDataQuota = cellquota!!.text.toString().toLong()
                httpConnectionTimeout = connectionTimeout!!.text.toString().toInt()
                httpSocketTimeout = socketTimeout!!.text.toString().toInt()
                maxDownloadConnections = maxConcurrentConnections!!.text.toString().toInt()
                progressUpdateByPercent = progressPercent!!.progress
                progressUpdateByTime = progressTimed!!.text.toString().toLong()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateSettingsValues() {
        maxstorage!!.setText(String.format(Locale.getDefault(), "%d", settings.maxStorageAllowed))

        headroomEdit!!.setText(String.format(Locale.getDefault(), "%d", settings.headroom))

        var value = (settings.batteryThreshold * 100).toInt()
        value = if (value < 0) 0 else if (value > 100) 100 else value
        batterythreshold!!.progress = value

        batteryDetail!!.text = getString(R.string.battery_threshold, value)

        cellquota!!.setText(String.format(Locale.getDefault(), "%d", settings.cellularDataQuota))

        val quotaStart = settings.cellularDataQuotaStart
        cellquotastart!!.text = dateFormatter.format(Date(quotaStart * 1000))

        connectionTimeout!!.setText(String.format(Locale.getDefault(), "%d", settings.httpConnectionTimeout))

        socketTimeout!!.setText(String.format(Locale.getDefault(), "%d", settings.httpSocketTimeout))

        maxConcurrentConnections!!.setText(String.format(Locale.getDefault(), "%d", settings.maxDownloadConnections))

        progressPercent!!.progress = settings.progressUpdateByPercent

        progressPercentDetails!!.text = getString(R.string.report_progress_percent, settings.progressUpdateByPercent)

        progressTimed!!.setText(String.format(Locale.getDefault(), "%d", settings.progressUpdateByTime))
    }

    // Reset Methods
    fun onMaxStorageReset() {
        settings.resetMaxStorageAllowed()
        updateSettingsValues()
    }

    fun onHeadroomReset() {
        settings.resetHeadroom()
        updateSettingsValues()
    }

    fun onBatteryReset() {
        settings.resetBatteryThreshold()
        updateSettingsValues()
    }

    fun onCellQuotaReset() {
        settings.resetCellularDataQuota()
        updateSettingsValues()
    }

    fun onCellQuotaDateReset() {
        settings.resetCellularDataQuotaStart()
        updateSettingsValues()
    }

    fun onConnectionTimeoutReset() {
        settings.resetHTTPConnectionTimeout()
        updateSettingsValues()
    }

    fun onSocketTimeoutReset() {
        settings.resetHTTPSocketTimeout()
        updateSettingsValues()
    }

    fun onMaxConnectionReset() {
        settings.resetMaxDownloadConnections()
        updateSettingsValues()
    }

    fun onProgressPercentReset() {
        settings.resetProgressUpdateByPercent()
        updateSettingsValues()
    }

    fun onProgressTimedReset() {
        settings.resetProgressUpdateByTime()
        updateSettingsValues()
    }

    companion object {
        @SuppressLint("SimpleDateFormat")
        private val dateFormatter = SimpleDateFormat("MM/dd/yyyy HH:mm:ss a")
    }
}