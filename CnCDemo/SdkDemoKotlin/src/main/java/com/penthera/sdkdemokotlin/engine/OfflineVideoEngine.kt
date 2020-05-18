package com.penthera.sdkdemokotlin.engine

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.penthera.sdkdemokotlin.Config
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.*

/**
 *
 */
class OfflineVideoEngine(lifeCycleOwner: LifecycleOwner, context: Context) : LifecycleObserver,
        IService.IConnectionObserver, EngineObserver(), Observers.IQueueObserver, IBackplane.IBackplaneDevicesObserver {

    companion object {
        @JvmField val TAG: String = OfflineVideoEngine::class.java.simpleName
    }

    /** The Context used to create virtuoso class */
    var mContext: Context

    private var mLifeCycleOwner: LifecycleOwner

    /** The Virtuoso class */
    private var virtuoso: Virtuoso? = null

    private var engineStateLiveData: VirtuosoEngineStateLiveData? = null

    private var settingsLiveData: MutableLiveData<ISettings> = MutableLiveData()

    private var backplaneSettingsLiveData: MutableLiveData<IBackplaneSettings> = MutableLiveData()

    private var backplaneDevicesData: MutableLiveData<Array<IBackplaneDevice>> = MutableLiveData()

    var pauseRequested = false

    var resumeRequested = false

    init {
        mLifeCycleOwner = lifeCycleOwner
        mLifeCycleOwner.lifecycle.addObserver(this)
        mContext = context
    }

    fun getVirtuoso(): Virtuoso {
        if(virtuoso == null) {
            virtuoso = Virtuoso(mContext)
            settingsLiveData.postValue(virtuoso!!.settings)
            backplaneSettingsLiveData.postValue(virtuoso!!.backplane.settings)
        }
        return virtuoso!!
    }

    fun getEngineState() : VirtuosoEngineStateLiveData {
        if (engineStateLiveData == null) {
            synchronized(this){
                if (engineStateLiveData == null) {
                    engineStateLiveData = VirtuosoEngineStateLiveData(getVirtuoso().service)
                }
            }
        }
        return engineStateLiveData!!
    }

    fun getSettings() : LiveData<ISettings> {
        return settingsLiveData
    }

    fun getBackplaneSettings() : LiveData<IBackplaneSettings> {
        return backplaneSettingsLiveData
    }

    fun getBackplaneDevices() : LiveData<Array<IBackplaneDevice>> {
        // fetch devices
        getVirtuoso().backplane.getDevices(this)
        return backplaneDevicesData
    }

    override fun backplaneDevicesComplete(devices: Array<IBackplaneDevice>) {
        backplaneDevicesData.postValue(devices)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun addEngineListeners() {
        getVirtuoso().onResume()
        getVirtuoso().service?.setConnectionObserver(this)
        // Note: this is going to be added as an EngineObserver, QueueObserver
        getVirtuoso().addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun removeEngineListeners() {
        getVirtuoso().onPause()
        getVirtuoso().removeObserver(this)
    }

    /**
     * Get asset from asset ID
     *
     * @param assetId
     * @return
     */
    fun getVirtuosoAsset(assetId: String): IAsset? {
        getVirtuoso().assetManager?.apply{
            val ls = this.getByAssetId(assetId)
            return if (ls == null || ls.size == 0) {
                null
            } else ls[0] as IAsset
        }
        return null
    }

    fun pauseResumeDownloads(pause: Boolean) : Boolean {
        engineStateLiveData?.virtuosoService?.let {
            if (it.isBound) {
                try {
                    if (pause && !pauseRequested) {
                        pauseRequested = true
                        it.pauseDownloads()
                    } else if (!resumeRequested){
                        resumeRequested = true
                        it.resumeDownloads()
                    }
                    return true
                } catch (ex: ServiceException) {
                    Log.w(TAG, "Service not connected for pause/resume")
                    if (pause) pauseRequested = false else resumeRequested = false
                }
            }
        }
        return false
    }

    // This login is only used to restart the engine after a shutdown.
    fun loginAccount() {
        virtuoso?.backplane?.settings?.let{
            virtuoso?.startup(it.url, it.userId, it.externalDeviceId, Config.BACKPLANE_PUBLIC_KEY, Config.BACKPLANE_PRIVATE_KEY) { _, _ ->
                // THIS IS WHERE WE WOULD SET UP PUSH MESSAGING
            }
        }
    }

    fun shutdownEngine() {
        virtuoso?.shutdown()
    }

    fun unregisterAccount() {
        virtuoso?.backplane?.unregister()
    }

    // EngineObserver
    override fun engineDidNotStart(reason: String) {
     //   runOnUiThread { Toast.makeText(this@MainActivity, R.string.error_start_service, Toast.LENGTH_LONG).show() }
    }

    override fun engineStatusChanged(arg0: Int) {
        if (pauseRequested) {
            if (arg0 == Common.EngineStatus.PAUSED) pauseRequested = false
        }
        if (resumeRequested) {
            if (arg0 != Common.EngineStatus.PAUSED) resumeRequested = false
        }
        engineStateLiveData?.doUpdate()
    }

    override fun settingChanged(aFlags: Int) {
        virtuoso?.let{ settingsLiveData.postValue(it.settings) }
    }

    override fun settingsError(aFlags: Int) {
        virtuoso?.let{ settingsLiveData.postValue(it.settings) }
    }

    override fun backplaneSettingChanged(aFlags: Int) {
        virtuoso?.let{ backplaneSettingsLiveData.postValue(it.backplane.settings)}
    }

    // Queue Observer
    override fun engineStartedDownloadingAsset(aAsset: IIdentifier) {

    }

    override fun enginePerformedProgressUpdateDuringDownload(aAsset: IIdentifier) {

    }

    override fun engineCompletedDownloadingAsset(aAsset: IIdentifier) {

    }

    override fun engineEncounteredErrorDownloadingAsset(aAsset: IIdentifier) {

    }

    override fun engineUpdatedQueue() {

    }

    override fun engineEncounteredErrorParsingAsset(mAssetId: String) {

    }

    // Connection observer
    override fun connected() {
        engineStateLiveData?.doUpdate()
    }

    override fun disconnected() {}
}