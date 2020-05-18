package com.penthera.sdkdemokotlin.fragment

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.OnLifecycleEvent
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.activity.OfflineVideoProvider
import com.penthera.sdkdemokotlin.dialog.CancellableProgressDialog
import com.penthera.sdkdemokotlin.dialog.ChangeNicknameDialogFragment
import com.penthera.sdkdemokotlin.engine.OfflineVideoEngine
import com.penthera.sdkdemokotlin.util.inflate
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.BackplaneException
import com.penthera.virtuososdk.client.IBackplane
import com.penthera.virtuososdk.client.IBackplaneDevice
import com.penthera.virtuososdk.client.Observers
import kotlinx.android.synthetic.main.device_row.view.*
import kotlinx.android.synthetic.main.fragment_devices.*
import java.text.SimpleDateFormat
import java.util.*

/**
 *
 */
class DevicesFragment : Fragment(), CancellableProgressDialog.CancelDialogListener,
        ChangeNicknameDialogFragment.ChangeNicknameObserver, Observers.IBackplaneObserver, LifecycleObserver {

    companion object {
        private val sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        private val TAG: String = DevicesFragment::class.java.simpleName
    }

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var itemDecoration: DividerItemDecoration
    private var offlineVideoEngine: OfflineVideoEngine? = null
    private var backplane: IBackplane? = null
    private var devices : LiveData<Array<IBackplaneDevice>>? = null

    private var progressDialog: CancellableProgressDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_devices, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        linearLayoutManager = LinearLayoutManager(context)
        deviceList.layoutManager = linearLayoutManager
        itemDecoration = DividerItemDecoration(deviceList.context, linearLayoutManager.orientation)
        deviceList.addItemDecoration(itemDecoration)
        deviceList.adapter = DevicesRecyclerAdapter(context, this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val offlineVideoProvider = activity as OfflineVideoProvider
        offlineVideoEngine = offlineVideoProvider.getOfflineEngine()
        backplane = offlineVideoEngine?.getVirtuoso()?.backplane
        lifecycle.addObserver(this)
        handleRefresh()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun addEngineListeners() {
        offlineVideoEngine?.getVirtuoso()?.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun removeEngineListeners() {
        offlineVideoEngine?.getVirtuoso()?.removeObserver(this)
    }

    private fun handleRefresh() {
        try {
            dismissProgressDialog()
            showProgressDialog(getString(R.string.fetching_devices))
            devices = offlineVideoEngine?.getBackplaneDevices()
            devices?.observe(this, androidx.lifecycle.Observer<Array<IBackplaneDevice>> {
                dismissProgressDialog()
                if (it?.size == 0){
                    // warn error
                    Toast.makeText(context, R.string.fetching_devices_failed, Toast.LENGTH_SHORT).show()
                    Log.w(TAG, "Devices call failed: unable to retrive devices from server")
                }
                val adapter = deviceList.adapter as DevicesRecyclerAdapter
                adapter.devices = it?: arrayOf()
                adapter.notifyDataSetChanged()
            })
        } catch (be: BackplaneException) {
            Log.e(TAG, "Caught exception requesting devices", be)
            dismissProgressDialog()
        }

    }

    private fun showProgressDialog(title: String) {
        if (progressDialog == null){
            progressDialog = CancellableProgressDialog.newInstance(this, title)
        }
        val ft = parentFragmentManager.beginTransaction()
        progressDialog?.show(ft, "PROGRESS")
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun cancel() {
        // Ignore in this case. Action will complete silently
    }

    private fun showNicknameDialog(device: IBackplaneDevice) {
        val ft = parentFragmentManager.beginTransaction()
        try {
            // Create Dialog
            val dialog = ChangeNicknameDialogFragment.newInstance(this, device)

            // Show
            dialog.show(ft, "nickname")
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    private fun enableDisableDevice(device: IBackplaneDevice) {
        try {
            showProgressDialog(getString(R.string.changing_enablement))
            backplane?.changeDownloadEnablement(!device.downloadEnabled(), device)
        } catch (e: BackplaneException) {
            Log.e(TAG, "Caught exception changing enablement", e)
            dismissProgressDialog()
        }
    }

    private fun deregisterDevice(device: IBackplaneDevice) {
        try {
            showProgressDialog(getString(R.string.unregistering_device))
            backplane?.unregisterDevice(device)
        } catch (e: BackplaneException) {
            Log.e(TAG, "Caught exception unregistering device", e)
            dismissProgressDialog()
        }
    }

    // Dialog observer
    override fun onChanged(device: IBackplaneDevice, nickname: String) {
        if (!TextUtils.isEmpty(nickname)){
            try {
                showProgressDialog(getString(R.string.changing_nickname))
                backplane?.changeNickname(nickname, device)
            } catch (e: BackplaneException) {
                Log.e(TAG, "Caught exception requesting nickname change", e)
                dismissProgressDialog()
            }
        }
    }

    // IBackplane observer
    override fun requestComplete(callbackType: Int, result: Int, errorMessage: String?) {
        activity?.runOnUiThread {
            if (callbackType == Common.BackplaneCallbackType.DOWNLOAD_ENABLEMENT_CHANGE ||
                    callbackType == Common.BackplaneCallbackType.DEVICE_UNREGISTERED ||
                    callbackType == Common.BackplaneCallbackType.NAME_CHANGE) {

                dismissProgressDialog()
                when (result) {
                    Common.BackplaneResult.SUCCESS -> handleRefresh()
                    Common.BackplaneResult.MAXIMUM_ENABLEMENT_REACHED -> {
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        Log.w(TAG, "Problem changing device enablement:  $errorMessage")
                    }
                    else -> {
                        Toast.makeText(context, R.string.error_backplane_comms, Toast.LENGTH_LONG).show()
                        Log.w(TAG, "Problem communicating wih the backplane result = $result")
                    }
                }
            }
        }
    }

    class DevicesRecyclerAdapter(private val context: Context?, private val fragment: DevicesFragment) : RecyclerView.Adapter<DevicesRecyclerAdapter.DeviceViewHolder>() {

        var devices : Array<IBackplaneDevice> = arrayOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val inflatedView = parent.inflate(R.layout.device_row, false)
            return DeviceViewHolder(inflatedView, context, fragment)
        }

        override fun getItemCount(): Int = devices.size

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            holder.bind(devices[position])
        }

        class DeviceViewHolder(private val v: View, private val context: Context?, private val fragment: DevicesFragment) : RecyclerView.ViewHolder(v), View.OnClickListener {

            private var device: IBackplaneDevice? = null

            init {
                v.btnChangeNickname.setOnClickListener(this)
                v.btnEnableDisable.setOnClickListener(this)
                v.btnDeregister.setOnClickListener(this)
            }

            fun bind(device : IBackplaneDevice) {
                this.device = device

                v.deviceId.text = device.id()
                v.deviceNickname.text = device.nickname()
                v.currentDevice.text =  context?.getString(if (device.isCurrentDevice) R.string.yes else R.string.no)
                v.lastSync.text = device.lastSync()?.let{ sdf.format(it) } ?: context?.getString(R.string.no_sync)
                v.lastModified.text = device.lastModified()?.let{ sdf.format(it) } ?: context?.getString(R.string.no_sync)
                v.btnEnableDisable.text = (context?.getString(if (device.downloadEnabled()) R.string.disable else R.string.enable))
                v.btnDeregister.isEnabled = !device.isCurrentDevice
            }

            override fun onClick(v: View?) {
                when (v?.id) {
                    R.id.btnEnableDisable -> {
                        device?.let {fragment.enableDisableDevice(it)}
                    }
                    R.id.btnDeregister -> {
                        device?.let {fragment.deregisterDevice(it)}
                    }
                    R.id.btnChangeNickname -> {
                        device?.let {fragment.showNicknameDialog(it)}
                    }
                }
            }
        }

    }
}