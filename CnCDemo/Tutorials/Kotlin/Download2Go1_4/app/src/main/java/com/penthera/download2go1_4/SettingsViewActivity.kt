package com.penthera.download2go1_4

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.penthera.download2go1_4.databinding.ActivityMainBinding
import com.penthera.download2go1_4.databinding.SettingsActivityBinding
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

    private lateinit var binding: SettingsActivityBinding

    /** Handle to the settings interface  */
    private lateinit var settings: ISettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        virtuoso = Virtuoso(this)
        settings = virtuoso.settings

        val seekProgressChangeListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.progressPercentLabel.text = getString(R.string.report_progress_percent, progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }
        binding.progressPercentValue.setOnSeekBarChangeListener(seekProgressChangeListener)
        val seekChangeListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.batteryLabel.text = getString(R.string.battery_threshold, progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }
        binding.batteryValue.setOnSeekBarChangeListener(seekChangeListener)

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

    fun onGlobalReset(view: View) {
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

    fun onApplySettings(view: View) {
        try {
            settings.apply {
                maxStorageAllowed = binding.maxStorageValue.text.toString().toLong()
                headroom = binding.headroomValue.text.toString().toLong()
                batteryThreshold =  binding.batteryValue.progress.toFloat() / 100
                cellularDataQuota = binding.cellquotaValue.text.toString().toLong()
                httpConnectionTimeout = binding.edtConnectionTimeout.text.toString().toInt()
                httpSocketTimeout = binding.edtSocketTimeout.text.toString().toInt()
                maxDownloadConnections = binding.edtMaxConnections.text.toString().toInt()
                progressUpdateByPercent = binding.progressPercentValue.progress
                progressUpdateByTime = binding.progressTimedValue.text.toString().toLong()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateSettingsValues() {
        binding.maxStorageValue.setText(String.format(Locale.getDefault(), "%d", settings.maxStorageAllowed))

        binding.headroomValue.setText(String.format(Locale.getDefault(), "%d", settings.headroom))

        var value = (settings.batteryThreshold * 100).toInt()
        value = if (value < 0) 0 else if (value > 100) 100 else value
        binding.batteryValue.progress = value

        binding.batteryLabel.text = getString(R.string.battery_threshold, value)

        binding.cellquotaValue.setText(String.format(Locale.getDefault(), "%d", settings.cellularDataQuota))

        val quotaStart = settings.cellularDataQuotaStart
        binding.cellquotaDateValue.text = dateFormatter.format(Date(quotaStart * 1000))

        binding.edtConnectionTimeout.setText(String.format(Locale.getDefault(), "%d", settings.httpConnectionTimeout))

        binding.edtSocketTimeout.setText(String.format(Locale.getDefault(), "%d", settings.httpSocketTimeout))

        binding.edtMaxConnections.setText(String.format(Locale.getDefault(), "%d", settings.maxDownloadConnections))

        binding.progressPercentValue.progress = settings.progressUpdateByPercent

        binding.progressPercentLabel.text = getString(R.string.report_progress_percent, settings.progressUpdateByPercent)

        binding.progressTimedValue.setText(String.format(Locale.getDefault(), "%d", settings.progressUpdateByTime))
    }

    // Reset Methods
    fun onMaxStorageReset(view: View) {
        settings.resetMaxStorageAllowed()
        updateSettingsValues()
    }

    fun onHeadroomReset(view: View) {
        settings.resetHeadroom()
        updateSettingsValues()
    }

    fun onBatteryReset(view: View) {
        settings.resetBatteryThreshold()
        updateSettingsValues()
    }

    fun onCellQuotaReset(view: View) {
        settings.resetCellularDataQuota()
        updateSettingsValues()
    }

    fun onCellQuotaDateReset(view: View) {
        settings.resetCellularDataQuotaStart()
        updateSettingsValues()
    }

    fun onConnectionTimeoutReset(view: View) {
        settings.resetHTTPConnectionTimeout()
        updateSettingsValues()
    }

    fun onSocketTimeoutReset(view: View) {
        settings.resetHTTPSocketTimeout()
        updateSettingsValues()
    }

    fun onMaxConnectionReset(view: View) {
        settings.resetMaxDownloadConnections()
        updateSettingsValues()
    }

    fun onProgressPercentReset(view: View) {
        settings.resetProgressUpdateByPercent()
        updateSettingsValues()
    }

    fun onProgressTimedReset(view: View) {
        settings.resetProgressUpdateByTime()
        updateSettingsValues()
    }

    companion object {
        @SuppressLint("SimpleDateFormat")
        private val dateFormatter = SimpleDateFormat("MM/dd/yyyy HH:mm:ss a")
    }
}