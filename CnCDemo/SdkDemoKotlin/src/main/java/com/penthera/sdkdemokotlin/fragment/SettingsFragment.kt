package com.penthera.sdkdemokotlin.fragment


import android.content.Context
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
                    batteryLabel.setText(getString(R.string.battery_threshold, progress))
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }
        battery.setOnSeekBarChangeListener(seekChangeListener)

        btnEnableDisable.setOnClickListener(View.OnClickListener {
            try {
                var downloadEnabled = backplaneSettings?.value?.downloadEnabled ?: false
                backplane?.changeDownloadEnablement(!downloadEnabled)
            } catch (e: BackplaneException) {
                e.printStackTrace()
            }
        })

        btnMaxstorageReset.setOnClickListener(View.OnClickListener { settings?.value?.resetMaxStorageAllowed()?.save(); })
        btnHeadroomReset.setOnClickListener(View.OnClickListener { settings?.value?.resetHeadroom()?.save() })
        btnBatteryReset.setOnClickListener(View.OnClickListener { settings?.value?.resetBatteryThreshold()?.save() })
        btnDestinationReset.setOnClickListener(View.OnClickListener { settings?.value?.resetDestinationPath()?.save() })
        btnCellquotaReset.setOnClickListener(View.OnClickListener { settings?.value?.resetCellularDataQuota()?.save() })
        btnCellquotaDateReset.setOnClickListener(View.OnClickListener { settings?.value?.resetCellularDataQuotaStart()?.save() })
        btnProgressPercentReset.setOnClickListener(View.OnClickListener { settings?.value?.resetProgressUpdateByPercent()?.save() })
        btnProgressTimedReset.setOnClickListener(View.OnClickListener { settings?.value?.resetProgressUpdateByTime()?.save() })
        btnConnectionTimeoutReset.setOnClickListener(View.OnClickListener { settings?.value?.resetHTTPConnectionTimeout()?.save() })
        btnSocketTimeoutReset.setOnClickListener(View.OnClickListener { settings?.value?.resetHTTPSocketTimeout()?.save() })
        btnSdkAllowedCodecsReset.setOnClickListener(View.OnClickListener { settings?.value?.resetAudioCodecsToDownload() })
        btnAlwaysRequestPerm.setOnClickListener(View.OnClickListener { settings?.value?.setAlwaysRequestPermission(btnAlwaysRequestPerm.isChecked)?.save() })
        btnBackgroundOnPause.setOnClickListener(View.OnClickListener { settings?.value?.setRemoveNotificationOnPause(btnBackgroundOnPause.isChecked)?.save() })
        btnAutorenewDrmLicenses.setOnClickListener(View.OnClickListener { settings?.value?.setAutomaticDrmLicenseRenewalEnabled(btnAutorenewDrmLicenses.isChecked)?.save() })
        btnSdkAllowedCodecsReset.setOnClickListener(View.OnClickListener { settings?.value?.resetAudioCodecsToDownload()?.save() })
        btnApply.setOnClickListener(View.OnClickListener { save() })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val offlineVideoProvider = activity as OfflineVideoProvider
        backplane = offlineVideoProvider.getOfflineEngine().getVirtuoso().backplane
        settings = offlineVideoProvider.getOfflineEngine().getSettings()
        settings?.observe(this, androidx.lifecycle.Observer { handler.post(valuesUpdater) })
        backplaneSettings = offlineVideoProvider.getOfflineEngine().getBackplaneSettings()
        backplaneSettings?.observe(this, androidx.lifecycle.Observer{ handler.post(valuesUpdater)})
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
            val quotaStart = it.getCellularDataQuotaStart()

            val d = Date(quotaStart * 1000)
            cellquotaStartDate.text = sdf.format(d)

            var batteryLevel = (it.getBatteryThreshold() * 100).toInt()
            batteryLevel = if (batteryLevel < 0) 0 else if (batteryLevel > 100) 100 else batteryLevel

            battery.progress = batteryLevel
            batteryLabel.text = getString(R.string.battery_threshold, batteryLevel)
            maxstorage.setText(it.maxStorageAllowed.toString())
            headroom.setText(it.headroom.toString())
            cellquota.setText(it.cellularDataQuota.toString())
            destinationPath.setText(it.destinationPath)

            progressPercentBar.progress = it.progressUpdateByPercent
            progressPercentLabel.text = getString(R.string.report_progress_setting, it.progressUpdateByPercent)
            progressTimed.setText(it.progressUpdateByTime.toString())
            maxSegmentErrors.setText(it.maxPermittedSegmentErrors.toString())
            proxySegmentErrorCode.setText(it.segmentErrorHttpCode.toString())
            connectionTimeout.setText(it.httpConnectionTimeout.toString())
            socketTimeout.setText(it.httpSocketTimeout.toString())
            btnAlwaysRequestPerm.setChecked(it.alwaysRequestPermission())
            btnBackgroundOnPause.setChecked(it.removeNotificationOnPause)
            btnAutorenewDrmLicenses.setChecked(it.isAutomaticDrmLicenseRenewalEnabled())
            sdkAllowedCodecs.setText(if (it.getAudioCodecsToDownload() != null) TextUtils.join(",", it.getAudioCodecsToDownload()) else "")
        }

        backplaneSettings?.value?.let {
            btnEnableDisable.setText(getString(if (it.getDownloadEnabled()) R.string.disable else R.string.enable))
            btnEnableDisable.setEnabled(backplane?.getAuthenticationStatus() != Common.AuthenticationStatus.NOT_AUTHENTICATED)
            downloadEnabledText.setText("" + it.getDownloadEnabled())
        }
    }

    private fun save() {
        try {
            settings?.value?.let {
                it.setProgressUpdateByTime(java.lang.Long.parseLong(progressTimed.getText().toString()))
                        .setProgressUpdateByPercent(progressPercentBar.getProgress())
                        .setBatteryThreshold(battery.getProgress().toFloat() / 100)
                        .setDestinationPath(destinationPath.getText().toString().trim({ it <= ' ' }))
                        .setCellularDataQuota(java.lang.Long.parseLong(cellquota.getText().toString()))
                        .setHeadroom(java.lang.Long.parseLong(headroom.getText().toString()))
                        .setMaxStorageAllowed(java.lang.Long.parseLong(maxstorage.getText().toString()))
                        .setHTTPConnectionTimeout(Integer.parseInt(connectionTimeout.getText().toString()))
                        .setHTTPSocketTimeout(Integer.parseInt(socketTimeout.getText().toString()))
                        .setSegmentErrorHttpCode(Integer.parseInt(proxySegmentErrorCode.getText().toString()))
                        .setMaxPermittedSegmentErrors(Integer.parseInt(maxSegmentErrors.getText().toString()))
                        .setAudioCodecsToDownload(if (sdkAllowedCodecs.getText().toString().length > 0) sdkAllowedCodecs.getText().toString().split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray() else null)
                        .save()
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
            handler.post(Runnable { Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show() })
        }
    }
}