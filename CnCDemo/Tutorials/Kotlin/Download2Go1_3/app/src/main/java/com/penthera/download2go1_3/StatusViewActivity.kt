package com.penthera.download2go1_3

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.penthera.VirtuosoSDK
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.*
import com.penthera.virtuososdk.client.IService.IConnectionObserver
import com.penthera.virtuososdk.client.Observers.IEngineObserver
import com.penthera.virtuososdk.client.Observers.IQueueObserver
import java.text.SimpleDateFormat
import java.util.*


/**
 *
 */
class StatusViewActivity : AppCompatActivity() {
    // We can keep a single instance of the Virtuoso object in an application class, or create a
    // new copy in each activity.
    private lateinit var virtuoso: Virtuoso
    private var download2GoService: IService? = null
    private var statusList: RecyclerView? = null
    private lateinit var statusArrayAdapter: StatusArrayAdapter
    private lateinit var statusValues: MutableList<StatusValue>
    private var engineStatus = 0
    private var currentThroughput = 0.0
    private var overallThroughput = 0.0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.status_activity)

        statusValues = ArrayList()
        statusArrayAdapter = StatusArrayAdapter(statusValues)
        statusList = findViewById(R.id.status_list)
        statusList?.apply {
            layoutManager = LinearLayoutManager(this@StatusViewActivity, RecyclerView.VERTICAL, false)
            addItemDecoration(DividerItemDecoration(this@StatusViewActivity, RecyclerView.VERTICAL))
            adapter = statusArrayAdapter
        }
        
        virtuoso = Virtuoso(this)

        // This assigns a new instance of the service client, it is a thin wrapper around an Android service binding.
        download2GoService = virtuoso.service
    }

    public override fun onResume() {
        super.onResume()

        // Resume the Download2Go SDK on activity resume
        virtuoso.onResume()
        virtuoso.addObserver(engineStatusObserver)
        virtuoso.addObserver(throughputObserver)
        download2GoService?.setConnectionObserver(serviceConnectionObserver)
        download2GoService?.bind()
        updateStatusDetails()
    }

    public override fun onPause() {
        super.onPause()

        // Pause the Download2Go SDK on activity pause
        virtuoso.removeObserver(engineStatusObserver)
        virtuoso.removeObserver(throughputObserver)
        download2GoService?.unbind()
        download2GoService?.setConnectionObserver(null)
        virtuoso.onPause()
    }

    fun updateStatusDetails() {
        // populate values into statusValues
        var valueSetChanged = false
        if(statusValues.size == 0){
            populateKeys()
            valueSetChanged = true
        }

        updateValues()

        // force list to update
        statusArrayAdapter.apply {
            if(valueSetChanged)
                this.notifyDataSetChanged()
            else
                this.notifyItemRangeChanged(0, 11)
        }
    }

    private fun populateKeys() {

        statusValues.apply{

            // 0. SDK Version
            val sdkVersionValue = StatusValue()
            sdkVersionValue.key = getString(R.string.sdk_version)
            sdkVersionValue.value = VirtuosoSDK.FULL_VERSION
            add(sdkVersionValue)

            // 1. Engine Status
            val engineStatusValue = StatusValue()
            engineStatusValue.key = getString(R.string.engine_status)
            engineStatusValue.value = ""
            add(engineStatusValue)

            // 2. Network Status
            val networkStatusValue = StatusValue()
            networkStatusValue.key = getString(R.string.network_status)
            networkStatusValue.value = ""
            add(networkStatusValue)

            // 3. Storage Status
            val storageStatusValue = StatusValue()
            storageStatusValue.key = getString(R.string.storage_status)
            storageStatusValue.value = ""
            add(storageStatusValue)

            // 4. Power Status
            val powerStatusValue = StatusValue()
            powerStatusValue.key = getString(R.string.power_status)
            powerStatusValue.value = ""
            add(powerStatusValue)

            // 5. Cell Quota Status
            val cellStatusValue = StatusValue()
            cellStatusValue.key = getString(R.string.cell_quota_status)
            cellStatusValue.value = ""
            add(cellStatusValue)

            // 6. Disk Usage
            val diskUsageValue = StatusValue()
            diskUsageValue.key = getString(R.string.disk_usage)
            diskUsageValue.value = ""
            add(diskUsageValue)

            // 7. Available Storage
            val availableStorageValue = StatusValue()
            availableStorageValue.key = getString(R.string.available_storage)
            availableStorageValue.value = ""
            add(availableStorageValue)

            // 8. Authentication Status
            val authenticationValue = StatusValue()
            authenticationValue.key = getString(R.string.authentication_status)
            authenticationValue.value = ""
            add(authenticationValue)

            // 9. Current Throughput
            val currentThroughputValue = StatusValue()
            currentThroughputValue.key = getString(R.string.current_throughput)
            currentThroughputValue.value = ""
            add(currentThroughputValue)

            // 10. Overall Throughput
            val overallThroughputValue = StatusValue()
            overallThroughputValue.key = getString(R.string.overall_throughput)
            overallThroughputValue.value = ""
            add(overallThroughputValue)

            // 11. Secure Time
            val secureTimeValue = StatusValue()
            secureTimeValue.key = getString(R.string.secure_time)
            secureTimeValue.value = ""
            add(secureTimeValue)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateValues() {
        
        download2GoService?.let{
            if(it.isBound){
                try{
                    engineStatus = it.status
                    currentThroughput = it.currentThroughput
                    overallThroughput = it.overallThroughput
                }catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        statusValues.apply {
            this[EngineStatusIdx].value = getStatusString(engineStatus)
            this[NetworkStatusIdx].value = getOkBlockedString(virtuoso.isNetworkStatusOK)
            this[StorageStatusIdx].value = getOkBlockedString(virtuoso.isDiskStatusOK)
            this[PowerStatusIdx].value = getOkBlockedString(virtuoso.isPowerStatusOK)
            this[CellQuotaIdx].value = getOkBlockedString(virtuoso.isCellularDataQuotaOK)
            this[DiskUsageIdx].value = String.format("%d MB", virtuoso.storageUsed)
            this[AvailStorageIdx].value = String.format("%d MB", virtuoso.allowableStorageRemaining)
            this[AuthenticationIdx].value = authenticationString(virtuoso.backplane.authenticationStatus)
            this[CurrentThroughputIdx].value = String.format("%.2f", currentThroughput)
            this[OverallThroughputIdx].value = String.format("%.2f", overallThroughput)
            this[SecureTimeIdx].value = dateFormatter.format(virtuoso.currentVirtuosoTime)
        }
       
    }

    private fun updateThroughput() {

        download2GoService?.let{
            if(it.isBound){
                try {
                    currentThroughput = it.currentThroughput
                    overallThroughput = it.overallThroughput
                }catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        statusValues.apply{
            this[CurrentThroughputIdx].value  = String.format("%.2f", currentThroughput)
            this[OverallThroughputIdx].value = String.format("%.2f", overallThroughput)
        }


        statusArrayAdapter.notifyItemRangeChanged(9, 2)
    }

    private fun getOkBlockedString(ok: Boolean): String {
        return getString(if (ok) R.string.status_ok else R.string.status_blocked)
    }

    private fun getStatusString(engineStatus: Int): String {
        return when (engineStatus) {
            Common.EngineStatus.AUTH_FAILURE -> "AUTH_FAILURE"
            Common.EngineStatus.ERROR -> "ERROR"
            Common.EngineStatus.BLOCKED -> {
                "BLOCKED"
            }
            Common.EngineStatus.DISABLED -> "DISABLED"
            Common.EngineStatus.PAUSED -> "PAUSED"
            else -> "OKAY"
        }
    }

    private fun authenticationString(status: Int): String {
        var statusString = "UNSET"
        when (status) {
            Common.AuthenticationStatus.AUTHENTICATED -> statusString =
                "AUTHENTICATED"
            Common.AuthenticationStatus.NOT_AUTHENTICATED -> statusString =
                "NOT_AUTHENTICATED"
            Common.AuthenticationStatus.AUTHENTICATION_EXPIRED -> statusString =
                "AUTHENTICATION_EXPIRED"
            Common.AuthenticationStatus.INVALID_LICENSE -> statusString =
                "INVALID_LICENSE"
            Common.AuthenticationStatus.SHUTDOWN -> statusString =
                "LOGGED OUT"
        }
        return statusString
    }

    /**
     * Connection observer monitors when the service is bound
     */
    private var serviceConnectionObserver: IConnectionObserver = object : IConnectionObserver {
        override fun connected() {
            // Update UI once we know connection is bound.
            download2GoService?.let{
                if(it.isBound){
                    updateStatusDetails()
                }
            }
        }

        override fun disconnected() {}
    }

    /**
     * This class observes the SDK engine status changes. Note that callbacks are not on
     * the main thread.
     */
    private val engineStatusObserver: IEngineObserver = object : EngineObserver() {
        override fun engineStatusChanged(status: Int) {
            runOnUiThread { updateStatusDetails() }
        }
    }

    /**
     * This simple extension of the queue observer is used for demonstration purposes as a simple
     * way to update the throughput values as a download progresses. Note that callbacks are not on
     * the main thread.
     */
    private val throughputObserver: IQueueObserver = object : QueueObserver() {
        override fun enginePerformedProgressUpdateDuringDownload(aAsset: IIdentifier) {
            runOnUiThread { updateThroughput() }
        }

        override fun engineCompletedDownloadingAsset(aAsset: IIdentifier) {}
        override fun engineEncounteredErrorDownloadingAsset(aAsset: IIdentifier) {}
    }

    internal class StatusValue {
        var key: String? = null
        var value: String? = null
    }

    // A very simple adapter for demonstration purposes which populates a number of key-value status rows into
    // a recyclerview
    internal inner class StatusArrayAdapter(private val values: List<StatusValue>) :
        RecyclerView.Adapter<StatusArrayAdapter.StatusArrayAdapterViewHolder?>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusArrayAdapterViewHolder {
            return StatusArrayAdapterViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.status_list_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: StatusArrayAdapterViewHolder, position: Int) {
            holder.bind(values[position])
        }
        override fun getItemCount() : Int{
            return values.size
        }

        inner class StatusArrayAdapterViewHolder(itemView: View) :
             RecyclerView.ViewHolder(itemView) {
            private var keyTextView = itemView.findViewById<TextView>(R.id.status_key)
            private var valueTextView = itemView.findViewById<TextView>(R.id.status_value)
            fun bind(value: StatusValue) {
                keyTextView.text = value.key
                valueTextView.text = value.value
            }
        }
    }

    companion object {
        @SuppressLint("SimpleDateFormat")
        private val dateFormatter = SimpleDateFormat("MM/dd/yyyy HH:mm:ss a")
        const val EngineStatusIdx = 1
        const val NetworkStatusIdx = 2
        const val StorageStatusIdx = 3
        const val PowerStatusIdx = 4
        const val CellQuotaIdx = 5
        const val DiskUsageIdx = 6
        const val AvailStorageIdx = 7
        const val AuthenticationIdx = 8
        const val CurrentThroughputIdx = 9
        const val OverallThroughputIdx = 10
        const val SecureTimeIdx = 11

    }
}