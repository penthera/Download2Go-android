package com.penthera.sdkdemokotlin.engine

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
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


    /*
     * Download item checking permissions and informing user of problems
     *
     * @param context Activity Context
     * @param permObserver
     */
//    fun downloadItem(remoteId: String, url: String, mimetype: String,
//                     catalogExpiry: Long, downloadExpiry: Long, expiryAfterPlay: Long,
//                     availabilityStart: Long, metaData: ExampleMetaData,
//                     permObserver: IQueue.IQueuedAssetPermissionObserver): Boolean {
//        Log.i(TAG, "Downloading item")
//        var success = false
//
//        getVirtuoso().assetManager?.apply {
//            val json = metaData.toJson()
//            val now = System.currentTimeMillis() / 1000
//
//            // Create an asset
//            var file: IFile? = this.createFileAsset(url, remoteId, mimetype, json)
//            file!!.startWindow = if (availabilityStart <= 0) now else availabilityStart
//            file.endWindow = if (catalogExpiry <= 0) java.lang.Long.MAX_VALUE else catalogExpiry
//            file.eap = expiryAfterPlay
//            file.ead = downloadExpiry
//
//            // Add file to the Queue
//            this.queue.add(file, permObserver)
//
//            success = true
//        }
//        return success
//    }
//
//
//    internal class HlsResult {
//        var error = 0
//        var queued = false
//    }

//    fun downloadDashItem(context: Context, service: Virtuoso,
//                         downloadEnabledContent: Boolean, remoteId: String,
//                         url: String, catalogExpiry: Long,
//                         downloadExpiry: Long, expiryAfterPlay: Long,
//                         availabilityStart: Long, title: String,
//                         thumbnail: String, permObserver: IQueue.IQueuedAssetPermissionObserver) {
//
//        val pm = PermissionManager()
//        val authorized = pm.canDownload(service.backplane.settings.downloadEnabled,
//                downloadEnabledContent, catalogExpiry)
//        val now = System.currentTimeMillis() / 1000
//        if (authorized == Permission.EAccessAllowed) {
//
//            // Create meta data for later display of download list
//            val json = MetaData.toJson(title, thumbnail)
//            val manager = service.assetManager
//
//            //note we would not be able to use the progress dialog if running from doBackground in an async task
//            val pdlg = ProgressDialog.show(context, "Processing dash manifest", "Adding fragments...")
//            val observer = object : ISegmentedAssetFromParserObserver {
//                override fun willAddToQueue(aSegmentedAsset: ISegmentedAsset?) {
//                    if (aSegmentedAsset != null) {
//                        aSegmentedAsset.startWindow = if (availabilityStart <= 0) now else availabilityStart
//                        aSegmentedAsset.endWindow = if (catalogExpiry <= 0) java.lang.Long.MAX_VALUE else catalogExpiry
//                        aSegmentedAsset.eap = expiryAfterPlay
//                        aSegmentedAsset.ead = downloadExpiry
//                        manager.update(aSegmentedAsset)
//                    }
//                }
//
//                override fun complete(aSegmentedAsset: ISegmentedAsset?, aError: Int, addedToQueue: Boolean) {
//
//                    try {
//                        pdlg.dismiss()
//                    } catch (e: Exception) {
//                    }
//
//                    if (aSegmentedAsset == null) {
//                        val builder1 = AlertDialog.Builder(context)
//                        builder1.setTitle("Could Not Create Asset")
//                        builder1.setMessage("Encountered error(" + Integer.toString(aError) + ") while creating asset.  This could happen if the device is currently offline, or if the asset manifest was not accessible.  Please try again later.")
//                        builder1.setCancelable(false)
//                        builder1.setPositiveButton("OK"
//                        ) { dialog, id -> dialog.cancel() }
//
//                        val alert11 = builder1.create()
//                        alert11.show()
//                    }
//                    Log.i(TAG, "Finished procesing dash file addedToQueue:$addedToQueue error:$aError")
//                }
//
//                override fun didParseSegment(segment: ISegment): String {
//                    return segment.remotePath
//                }
//            }
//
//            try {
//                manager.createMPDSegmentedAssetAsync(observer, URL(url), 0, 0, remoteId, json, true, permObserver)
//            } catch (e: MalformedURLException) {
//                Log.e(TAG, "Problem with dash url.", e)
//            }
//
//        }
//
//    }
}