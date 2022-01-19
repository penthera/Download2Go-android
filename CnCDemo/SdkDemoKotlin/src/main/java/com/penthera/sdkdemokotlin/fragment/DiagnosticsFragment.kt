package com.penthera.sdkdemokotlin.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.penthera.VirtuosoSDK
import com.penthera.sdkdemokotlin.activity.OfflineVideoProvider
import com.penthera.sdkdemokotlin.databinding.FragmentDiagnosticsBinding
import com.penthera.sdkdemokotlin.engine.VirtuosoEngineState
import com.penthera.sdkdemokotlin.engine.VirtuosoServiceModelFactory
import com.penthera.sdkdemokotlin.engine.VirtuosoServiceViewModel
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.IBackplane
import com.penthera.virtuososdk.client.IBackplaneSettings
import com.penthera.virtuososdk.client.ISettings
import com.penthera.virtuososdk.client.Virtuoso
import java.text.SimpleDateFormat
import java.util.*

/**
 *
 */
class DiagnosticsFragment : Fragment(), LifecycleObserver {

    companion object {
        private val sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    }

    /** Handle to main virtuoso object used for checking overall status */
    private var virtuoso: Virtuoso? = null

    /** Handle to the settings interface  */
    private var settings: LiveData<ISettings>? = null

    /** Handle to the backplane settings interface  */
    private var backplaneSettings: LiveData<IBackplaneSettings>? = null

    /** Handle to the backplane interface  */
    private var backplane: IBackplane? = null

    /** Service viewmodel exposes the current state and throughput */
    private lateinit var serviceViewModel: VirtuosoServiceViewModel

    /** The Battery level  */
    private var currentBatteryLevel = -1

    /** true, the device is charging  */
    private var isCharging = false

    private val handler = Handler()

    private var _binding: FragmentDiagnosticsBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentDiagnosticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.version.text = VirtuosoSDK.BUILD_VERSION
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val offlineVideoProvider = activity as OfflineVideoProvider
        virtuoso = offlineVideoProvider.getOfflineEngine().getVirtuoso()
        backplane = offlineVideoProvider.getOfflineEngine().getVirtuoso().backplane
        settings = offlineVideoProvider.getOfflineEngine().getSettings()
        settings?.observe(viewLifecycleOwner, androidx.lifecycle.Observer { handler.post(settingsValuesUpdater) })
        backplaneSettings = offlineVideoProvider.getOfflineEngine().getBackplaneSettings()
        backplaneSettings?.observe(viewLifecycleOwner, androidx.lifecycle.Observer{ handler.post(backplaneSettingsValuesUpdater)})

        serviceViewModel = ViewModelProvider(this, VirtuosoServiceModelFactory(offlineVideoProvider.getOfflineEngine()))
                .get(VirtuosoServiceViewModel::class.java)
        serviceViewModel.getEngineState().observe(viewLifecycleOwner, androidx.lifecycle.Observer<VirtuosoEngineState>{
            it?.let {downloadThroughputUpdater(it)}
        })
        lifecycle.addObserver(this)
        handler.post(valuesUpdater)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        val TAG = "Diagnostics-ClientMessageReceiver"
        override fun onReceive(context: Context, aIntent: Intent) {
            val action = aIntent.action
            if (action == null) {
                Log.e(TAG, "onReceive(): null action")
                return
            }

            Log.d(TAG, "Diagnostics Broadcast Receiver: received - $action")

            if (action == Intent.ACTION_POWER_CONNECTED) {
                isCharging = true
                handler.post(valuesUpdater)
            } else if (action == Intent.ACTION_POWER_DISCONNECTED) {
                isCharging = false
                handler.post(valuesUpdater)
            } else if (action == Intent.ACTION_BATTERY_CHANGED) {
                val plugged = aIntent.getIntExtra("plugged", 0)
                val charge = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB
                if (isCharging != charge) {
                    isCharging = charge
                }
                val raw_level = aIntent.getIntExtra("level", -1)
                val scale = aIntent.getIntExtra("scale", -1)

                if (raw_level >= 0 && scale > 0) currentBatteryLevel = raw_level * 100 / scale
                handler.post(valuesUpdater)
            } else {
                Log.w(TAG, "onReceive(): unknown action: $action")
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun registerApiReceiver() {
        context?.registerReceiver(broadcastReceiver, IntentFilter(Intent.ACTION_POWER_CONNECTED))
        context?.registerReceiver(broadcastReceiver, IntentFilter(Intent.ACTION_POWER_DISCONNECTED))
        context?.registerReceiver(broadcastReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun unregisterApiReceiver() {
        context?.unregisterReceiver(broadcastReceiver)
    }

    private val settingsValuesUpdater = Runnable {
        settings?.value?.let {
            binding.cellQuota.text = it.cellularDataQuota.toString()
            val d = Date(it.cellularDataQuotaStart * 1000)
            binding.quotaStartDate.text = sdf.format(d)
            binding.maxStorage.text = it.maxStorageAllowed.toString()
            binding.headroom.text = it.headroom.toString()
            binding.batteryThreshold.text = it.batteryThreshold.toString()
        }
    }

    private val backplaneSettingsValuesUpdater = Runnable {
        backplaneSettings?.value?.let {
            binding.mdd.text = it.maxDevicesAllowedForDownload.toString()
            binding.usedMdd.text = it.usedDownloadQuota.toString()
            binding.ead.text = it.expiryAfterDownload.toString()
            binding.eap.text = it.expiryAfterPlay.toString()
            binding.maxOffline.text = it.maxOffline.toString()
            binding.downloadEnabled.text = it.downloadEnabled.toString()
            binding.maxDownloads.text = it.maxPermittedDownloads.toString()
            binding.mad.text = it.maxDownloadsPerAsset.toString()
            binding.mda.text = it.maxDownloadsPerAccount.toString()
            binding.mac.text = it.maxCopiesPerAsset.toString()
            binding.deviceName.text = it.deviceNickname
            binding.backplaneUser.text = it.userId
            binding.backplaneUrl.text = it.url?.let{ it.toString() } ?: "https://demo.penthera.com/"
        }
    }

    private val valuesUpdater = Runnable {

        binding.engineState.text = serviceViewModel.getCurrentEngineStatusString()

        backplane?.let {
            val d = Date(it.lastAuthentication * 1000)
            binding.lastAuth.text = sdf.format(d)
            binding.authState.text = when(it.authenticationStatus) {
                Common.AuthenticationStatus.AUTHENTICATED -> "AUTHENTICATED"
                Common.AuthenticationStatus.NOT_AUTHENTICATED -> "NOT_AUTHENTICATED"
                Common.AuthenticationStatus.AUTHENTICATION_EXPIRED -> "AUTHENTICATION_EXPIRED"
                Common.AuthenticationStatus.INVALID_LICENSE -> "INVALID_LICENSE"
                Common.AuthenticationStatus.SHUTDOWN -> "LOGGED OUT"
                else -> "Unknown"
            }
        }

        virtuoso?.let {
            binding.usedStorage.text = it.storageUsed.toString()
            binding.availableStorage.text = it.allowableStorageRemaining.toString()
            binding.powerStatus.text = if (it.isPowerStatusOK()) "OKAY" else "NOT OKAY"
            binding.networkStatus.text = if (it.isNetworkStatusOK()) "OKAY" else "NOT OKAY"
            binding.cellQuotaStatus.text = if (it.isCellularDataQuotaOK()) "OKAY" else "NOT OKAY"
            binding.usedCellQuota.text = it.utilizedCellularDataQuota.toString()
            binding.diskStatus.text = if (it.isDiskStatusOK()) "OKAY" else "NOT OKAY"
        }
        binding.charging.text = isCharging.toString()
        binding.batteryLevel.text = currentBatteryLevel.toString()
    }

    private fun downloadThroughputUpdater(state: VirtuosoEngineState) {
        binding.engineState.text = serviceViewModel.getCurrentEngineStatusString()
        binding.overallThroughput.text = String.format("%.2f", state.overallThroughput)
        binding.currentThroughput.text = String.format("%.2f", state.currentThroughput)
        binding.windowThroughput.text = String.format("%.2f", state.windowedThroughput)
    }
}