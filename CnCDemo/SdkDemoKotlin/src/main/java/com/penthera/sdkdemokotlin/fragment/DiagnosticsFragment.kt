package com.penthera.sdkdemokotlin.fragment

import android.arch.lifecycle.*
import android.arch.lifecycle.Observer
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.penthera.VirtuosoSDK
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.activity.OfflineVideoProvider
import com.penthera.sdkdemokotlin.engine.VirtuosoEngineState
import com.penthera.sdkdemokotlin.engine.VirtuosoServiceModelFactory
import com.penthera.sdkdemokotlin.engine.VirtuosoServiceViewModel
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.IBackplane
import com.penthera.virtuososdk.client.IBackplaneSettings
import com.penthera.virtuososdk.client.ISettings
import com.penthera.virtuososdk.client.Virtuoso
import kotlinx.android.synthetic.main.fragment_diagnostics.*
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_diagnostics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        version.text = VirtuosoSDK.BUILD_VERSION
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val offlineVideoProvider = activity as OfflineVideoProvider
        virtuoso = offlineVideoProvider.getOfflineEngine().getVirtuoso()
        backplane = offlineVideoProvider.getOfflineEngine().getVirtuoso().backplane
        settings = offlineVideoProvider.getOfflineEngine().getSettings()
        settings?.observe(this, android.arch.lifecycle.Observer { handler.post(settingsValuesUpdater) })
        backplaneSettings = offlineVideoProvider.getOfflineEngine().getBackplaneSettings()
        backplaneSettings?.observe(this, android.arch.lifecycle.Observer{ handler.post(backplaneSettingsValuesUpdater)})

        serviceViewModel = ViewModelProviders.of(this, VirtuosoServiceModelFactory(offlineVideoProvider.getOfflineEngine()))
                .get(VirtuosoServiceViewModel::class.java)
        serviceViewModel.getEngineState().observe(this, Observer<VirtuosoEngineState>{
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
            cellQuota.text = it.cellularDataQuota.toString()
            val d = Date(it.cellularDataQuotaStart * 1000)
            quotaStartDate.text = sdf.format(d)
            maxStorage.text = it.maxStorageAllowed.toString()
            headroom.text = it.headroom.toString()
            batteryThreshold.text = it.batteryThreshold.toString()
        }
    }

    private val backplaneSettingsValuesUpdater = Runnable {
        backplaneSettings?.value?.let {
            mdd.text = it.maxDevicesAllowedForDownload.toString()
            usedMdd.text = it.usedDownloadQuota.toString()
            ead.text = it.expiryAfterDownload.toString()
            eap.text = it.expiryAfterPlay.toString()
            maxOffline.text = it.maxOffline.toString()
            downloadEnabled.text = it.downloadEnabled.toString()
            maxDownloads.text = it.maxPermittedDownloads.toString()
            mad.text = it.maxDownloadsPerAsset.toString()
            mda.text = it.maxDownloadsPerAccount.toString()
            mac.text = it.maxCopiesPerAsset.toString()
            deviceName.text = it.deviceNickname
            backplaneUser.text = it.userId
            backplaneUrl.text = it.url?.let{ it.toString() } ?: "https://demo.penthera.com/"
        }
    }

    private val valuesUpdater = Runnable {

        engineState.text = serviceViewModel.getCurrentEngineStatusString()

        backplane?.let {
            val d = Date(it.lastAuthentication * 1000)
            lastAuth.text = sdf.format(d)
            authState.text = when(it.authenticationStatus) {
                Common.AuthenticationStatus.AUTHENTICATED -> "AUTHENTICATED"
                Common.AuthenticationStatus.NOT_AUTHENTICATED -> "NOT_AUTHENTICATED"
                Common.AuthenticationStatus.AUTHENTICATION_EXPIRED -> "AUTHENTICATION_EXPIRED"
                Common.AuthenticationStatus.INVALID_LICENSE -> "INVALID_LICENSE"
                Common.AuthenticationStatus.SHUTDOWN -> "LOGGED OUT"
                else -> "Unknown"
            }
        }

        virtuoso?.let {
            usedStorage.text = it.storageUsed.toString()
            availableStorage.text = it.allowableStorageRemaining.toString()
            powerStatus.text = if (it.isPowerStatusOK()) "OKAY" else "NOT OKAY"
            networkStatus.text = if (it.isNetworkStatusOK()) "OKAY" else "NOT OKAY"
            cellQuotaStatus.text = if (it.isCellularDataQuotaOK()) "OKAY" else "NOT OKAY"
            usedCellQuota.text = it.utilizedCellularDataQuota.toString()
            diskStatus.text = if (it.isDiskStatusOK()) "OKAY" else "NOT OKAY"
        }
        charging.text = isCharging.toString()
        batteryLevel.text = currentBatteryLevel.toString()
    }

    private fun downloadThroughputUpdater(state: VirtuosoEngineState) {
        engineState.text = serviceViewModel.getCurrentEngineStatusString()
        overallThroughput.text = String.format("%.2f", state.overallThroughput)
        currentThroughput.text = String.format("%.2f", state.currentThroughput)
        windowThroughput.text = String.format("%.2f", state.windowedThroughput)
    }
}