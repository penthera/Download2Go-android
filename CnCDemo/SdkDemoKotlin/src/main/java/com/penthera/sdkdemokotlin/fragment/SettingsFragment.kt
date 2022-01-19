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
import com.penthera.sdkdemokotlin.databinding.FragmentSettingsBinding
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.*
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

    private var _binding: FragmentSettingsBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val seekProgressChangeListener = object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                           fromUser: Boolean) {
                if (fromUser) {
                    binding.progressPercentLabel.text = getString(R.string.report_progress_setting, progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}

        }
        binding.progressPercentBar.setOnSeekBarChangeListener(seekProgressChangeListener)

        val seekChangeListener = object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                           fromUser: Boolean) {
                if (fromUser) {
                    binding.batteryLabel.text = getString(R.string.battery_threshold, progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }
        binding.battery.setOnSeekBarChangeListener(seekChangeListener)

        binding.btnEnableDisable.setOnClickListener {
            try {
                val downloadEnabled = backplaneSettings?.value?.downloadEnabled ?: false
                backplane?.changeDownloadEnablement(!downloadEnabled)
            } catch (e: BackplaneException) {
                e.printStackTrace()
            }
        }

        binding.btnMaxstorageReset.setOnClickListener { settings?.value?.resetMaxStorageAllowed(); }
        binding.btnHeadroomReset.setOnClickListener { settings?.value?.resetHeadroom() }
        binding.btnBatteryReset.setOnClickListener { settings?.value?.resetBatteryThreshold() }
        binding.btnDestinationReset.setOnClickListener { settings?.value?.resetDestinationPath() }
        binding.btnCellquotaReset.setOnClickListener { settings?.value?.resetCellularDataQuota() }
        binding.btnCellquotaDateReset.setOnClickListener { settings?.value?.resetCellularDataQuotaStart() }
        binding.btnProgressPercentReset.setOnClickListener { settings?.value?.resetProgressUpdateByPercent() }
        binding.btnProgressTimedReset.setOnClickListener { settings?.value?.resetProgressUpdateByTime() }
        binding.btnConnectionTimeoutReset.setOnClickListener { settings?.value?.resetHTTPConnectionTimeout() }
        binding.btnSocketTimeoutReset.setOnClickListener { settings?.value?.resetHTTPSocketTimeout() }
        binding.btnSdkAllowedCodecsReset.setOnClickListener { settings?.value?.resetAudioCodecsToDownload() }
        binding.btnAlwaysRequestPerm.setOnClickListener { settings?.value?.setAlwaysRequestPermission(binding.btnAlwaysRequestPerm.isChecked) }
        binding.btnBackgroundOnPause.setOnClickListener { settings?.value?.removeNotificationOnPause = binding.btnBackgroundOnPause.isChecked }
        binding.btnAutorenewDrmLicenses.setOnClickListener { settings?.value?.isAutomaticDrmLicenseRenewalEnabled = binding.btnAutorenewDrmLicenses.isChecked }
        binding.btnSdkAllowedCodecsReset.setOnClickListener { settings?.value?.resetAudioCodecsToDownload() }
        binding.btnApply.setOnClickListener { save() }
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
            binding.cellquotaStartDate.text = sdf.format(d)

            var batteryLevel = (it.batteryThreshold * 100).toInt()
            batteryLevel = if (batteryLevel < 0) 0 else if (batteryLevel > 100) 100 else batteryLevel

            binding.battery.progress = batteryLevel
            binding.batteryLabel.text = getString(R.string.battery_threshold, batteryLevel)
            binding.maxstorage.setText(it.maxStorageAllowed.toString())
            binding.headroomValue.setText(it.headroom.toString())
            binding.cellquota.setText(it.cellularDataQuota.toString())
            binding.destinationPathValue.setText(it.destinationPath)

            binding.progressPercentBar.progress = it.progressUpdateByPercent
            binding.progressPercentLabel.text = getString(R.string.report_progress_setting, it.progressUpdateByPercent)
            binding.progressTimed.setText(it.progressUpdateByTime.toString())
            binding.maxSegmentErrors.setText(it.maxPermittedSegmentErrors.toString())
            binding.proxySegmentErrorCode.setText(it.segmentErrorHttpCode.toString())
            binding.connectionTimeout.setText(it.httpConnectionTimeout.toString())
            binding.socketTimeout.setText(it.httpSocketTimeout.toString())
            binding.btnAlwaysRequestPerm.isChecked = it.alwaysRequestPermission()
            binding.btnBackgroundOnPause.isChecked = it.removeNotificationOnPause
            binding.btnAutorenewDrmLicenses.isChecked = it.isAutomaticDrmLicenseRenewalEnabled
            binding.sdkAllowedCodecs.setText(if (it.audioCodecsToDownload != null) TextUtils.join(",", it.audioCodecsToDownload) else "")
        }

        backplaneSettings?.value?.let {
            binding.btnEnableDisable.text = getString(if (it.downloadEnabled) R.string.disable else R.string.enable)
            binding.btnEnableDisable.isEnabled = backplane?.authenticationStatus != Common.AuthenticationStatus.NOT_AUTHENTICATED
            binding.downloadEnabledText.text =  it.downloadEnabled.toString()
        }
    }

    private fun save() {
        try {
            settings?.value?.apply {
                progressUpdateByTime = binding.progressTimed.text.toString().toLong()
                progressUpdateByPercent = binding.progressPercentBar.progress
                batteryThreshold = (binding.battery.progress.toFloat() / 100)
                destinationPath = (binding.destinationPathValue.text.toString().trim { it <= ' ' })
                cellularDataQuota = binding.cellquota.text.toString().toLong()
                headroom = binding.headroomValue.text.toString().toLong()
                maxStorageAllowed = binding.maxstorage.text.toString().toLong()
                httpConnectionTimeout = binding.connectionTimeout.text.toString().toInt()
                httpSocketTimeout = binding.socketTimeout.text.toString().toInt()
                segmentErrorHttpCode = binding.proxySegmentErrorCode.text.toString().toInt()
                maxPermittedSegmentErrors = binding.maxSegmentErrors.text.toString().toInt()
                audioCodecsToDownload = if (binding.sdkAllowedCodecs.text.toString().isNotEmpty()) binding.sdkAllowedCodecs.text.toString().split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() else null
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