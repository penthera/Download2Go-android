package com.penthera.sdkdemokotlin.fragment


import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.activity.OfflineVideoProvider
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.*
import kotlinx.android.synthetic.main.fragment_settings.*
import java.text.SimpleDateFormat
import java.util.*

/**
 *
 */
class SettingsFragment : Fragment() {

    companion object {
        private val sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    }

    /** Handle to the settings interface  */
    private var settings: LiveData<ISettings>? = null

    /** Handle to the backplane settings interface  */
    private var backplaneSettings: LiveData<IBackplaneSettings>? = null

    /** Handle to the backplane interface  */
    private var backplane: IBackplane? = null

    private val handler = Handler()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val seekProgressChangeListener = object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                           fromUser: Boolean) {
                if (fromUser) {
                    progressPercentLabel.text = getString(R.string.report_progress_setting, progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}

        }
        progressPercentBar.setOnSeekBarChangeListener(seekProgressChangeListener)

        val seekChangeListener = object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                           fromUser: Boolean) {
                if (fromUser) {
                    batteryLabel.text = getString(R.string.battery_threshold, progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }
        battery.setOnSeekBarChangeListener(seekChangeListener)

        btnEnableDisable.setOnClickListener {
            try {
                val downloadEnabled = backplaneSettings?.value?.downloadEnabled ?: false
                backplane?.changeDownloadEnablement(!downloadEnabled)
            } catch (e: BackplaneException) {
                e.printStackTrace()
            }
        }

        btnMaxstorageReset.setOnClickListener { settings?.value?.resetMaxStorageAllowed(); }
        btnHeadroomReset.setOnClickListener { settings?.value?.resetHeadroom() }
        btnBatteryReset.setOnClickListener { settings?.value?.resetBatteryThreshold() }
        btnDestinationReset.setOnClickListener { settings?.value?.resetDestinationPath() }
        btnCellquotaReset.setOnClickListener { settings?.value?.resetCellularDataQuota() }
        btnCellquotaDateReset.setOnClickListener { settings?.value?.resetCellularDataQuotaStart() }
        btnProgressPercentReset.setOnClickListener { settings?.value?.resetProgressUpdateByPercent() }
        btnProgressTimedReset.setOnClickListener { settings?.value?.resetProgressUpdateByTime() }
        btnConnectionTimeoutReset.setOnClickListener { settings?.value?.resetHTTPConnectionTimeout() }
        btnSocketTimeoutReset.setOnClickListener { settings?.value?.resetHTTPSocketTimeout() }
        btnSdkAllowedCodecsReset.setOnClickListener { settings?.value?.resetAudioCodecsToDownload() }
        btnAlwaysRequestPerm.setOnClickListener { settings?.value?.setAlwaysRequestPermission(btnAlwaysRequestPerm.isChecked) }
        btnBackgroundOnPause.setOnClickListener { settings?.value?.removeNotificationOnPause = btnBackgroundOnPause.isChecked }
        btnAutorenewDrmLicenses.setOnClickListener { settings?.value?.isAutomaticDrmLicenseRenewalEnabled = btnAutorenewDrmLicenses.isChecked }
        btnSdkAllowedCodecsReset.setOnClickListener { settings?.value?.resetAudioCodecsToDownload() }
        btnApply.setOnClickListener { save() }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val offlineVideoProvider = activity as OfflineVideoProvider
        backplane = offlineVideoProvider.getOfflineEngine().getVirtuoso().backplane
        settings = offlineVideoProvider.getOfflineEngine().getSettings()
        settings?.observe(viewLifecycleOwner, androidx.lifecycle.Observer { handler.post(valuesUpdater) })
        backplaneSettings = offlineVideoProvider.getOfflineEngine().getBackplaneSettings()
        backplaneSettings?.observe(viewLifecycleOwner, androidx.lifecycle.Observer{ handler.post(valuesUpdater)})
        handler.post(valuesUpdater)
        offlineVideoProvider.getOfflineEngine().getVirtuoso().addObserver(backplaneObserver)
    }

    override fun onDetach() {
        super.onDetach()
        val offlineVideoProvider = activity as OfflineVideoProvider
        offlineVideoProvider.getOfflineEngine().getVirtuoso().removeObserver(backplaneObserver)

    }

    private val valuesUpdater = Runnable {
        settings?.value?.let {
            val quotaStart = it.cellularDataQuotaStart

            val d = Date(quotaStart * 1000)
            cellquotaStartDate.text = sdf.format(d)

            var batteryLevel = (it.batteryThreshold * 100).toInt()
            batteryLevel = if (batteryLevel < 0) 0 else if (batteryLevel > 100) 100 else batteryLevel

            battery.progress = batteryLevel
            batteryLabel.text = getString(R.string.battery_threshold, batteryLevel)
            maxstorage.setText(it.maxStorageAllowed.toString())
            headroomValue.setText(it.headroom.toString())
            cellquota.setText(it.cellularDataQuota.toString())
            destinationPathValue.setText(it.destinationPath)

            progressPercentBar.progress = it.progressUpdateByPercent
            progressPercentLabel.text = getString(R.string.report_progress_setting, it.progressUpdateByPercent)
            progressTimed.setText(it.progressUpdateByTime.toString())
            maxSegmentErrors.setText(it.maxPermittedSegmentErrors.toString())
            proxySegmentErrorCode.setText(it.segmentErrorHttpCode.toString())
            connectionTimeout.setText(it.httpConnectionTimeout.toString())
            socketTimeout.setText(it.httpSocketTimeout.toString())
            btnAlwaysRequestPerm.isChecked = it.alwaysRequestPermission()
            btnBackgroundOnPause.isChecked = it.removeNotificationOnPause
            btnAutorenewDrmLicenses.isChecked = it.isAutomaticDrmLicenseRenewalEnabled
            sdkAllowedCodecs.setText(if (it.audioCodecsToDownload != null) TextUtils.join(",", it.audioCodecsToDownload) else "")
        }

        backplaneSettings?.value?.let {
            btnEnableDisable.text = getString(if (it.downloadEnabled) R.string.disable else R.string.enable)
            btnEnableDisable.isEnabled = backplane?.authenticationStatus != Common.AuthenticationStatus.NOT_AUTHENTICATED
            downloadEnabledText.text =  it.downloadEnabled.toString()
        }
    }

    private fun save() {
        try {
            settings?.value?.apply {
                progressUpdateByTime = progressTimed.text.toString().toLong()
                progressUpdateByPercent = progressPercentBar.progress
                batteryThreshold = (battery.progress.toFloat() / 100)
                destinationPath = (destinationPathValue.text.toString().trim { it <= ' ' })
                cellularDataQuota = cellquota.text.toString().toLong()
                headroom = headroomValue.text.toString().toLong()
                maxStorageAllowed = maxstorage.text.toString().toLong()
                httpConnectionTimeout =connectionTimeout.text.toString().toInt()
                httpSocketTimeout = socketTimeout.text.toString().toInt()
                segmentErrorHttpCode =  proxySegmentErrorCode.text.toString().toInt()
                maxPermittedSegmentErrors = maxSegmentErrors.text.toString().toInt()
                audioCodecsToDownload = if (sdkAllowedCodecs.text.toString().isNotEmpty()) sdkAllowedCodecs.text.toString().split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val backplaneObserver = Observers.IBackplaneObserver { callbackType, result, errorMessage ->
        handler.post(valuesUpdater)

        if (callbackType == Common.BackplaneCallbackType.DOWNLOAD_ENABLEMENT_CHANGE && result == Common.BackplaneResult.MAXIMUM_ENABLEMENT_REACHED) {
            // Show a warning message - in this case we show the server response directly,
            // but this would not be the case in a production application.
            handler.post { Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show() }
        }
    }
}