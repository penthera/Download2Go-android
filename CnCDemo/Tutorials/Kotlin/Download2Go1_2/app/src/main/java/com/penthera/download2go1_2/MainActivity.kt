package com.penthera.download2go1_2

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.penthera.download2go1_2.databinding.ActivityMainBinding

import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.*
import com.penthera.virtuososdk.client.IService.IConnectionObserver
import com.penthera.virtuososdk.client.Observers.IEngineObserver
import com.penthera.virtuososdk.client.builders.MPDAssetBuilder
import java.io.File
import java.net.URL
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var  virtuoso : Virtuoso
    var asset : IAsset? =  null
    private lateinit var queueObserver: AssetQueueObserver

    private lateinit var binding: ActivityMainBinding

    private var internalUpdate : Boolean = false
    //local reference to the Download2Go service
    private  var download2GoService : IService? = null

    private var pauseRequested : Boolean = false
    private var resumeRequested : Boolean = false

    /**
     * Connection observer monitors when the service is bound
     */
    private var serviceConnectionObserver: IConnectionObserver = object : IConnectionObserver {
        override fun connected() {
            // Update UI once we know connection is bound.
            download2GoService?.let {
                if(it.isBound){
                    try {
                        internalUpdate = true
                        binding.pauseEngine.isChecked = it.status == Common.EngineStatus.PAUSED
                        internalUpdate = false
                    } catch (se: ServiceException) {
                        Log.d(
                            MainActivity::class.java.name,
                            "Service Exception on getting service status"
                        )
                    }
                }
            }
        }

        override fun disconnected() {}
    }

    private val enginePauseObserver: IEngineObserver = object : EngineObserver() {
        override fun engineStatusChanged(status: Int) {
            if (pauseRequested) {
                pauseRequested = !(status == Common.EngineStatus.PAUSED)
                updateUI()
            }
            if (resumeRequested) {
                resumeRequested = !(status != Common.EngineStatus.PAUSED)
                updateUI()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)


        binding.download.setOnClickListener { downloadAsset() }
        binding.play.setOnClickListener { playAsset()}
        binding.delete.setOnClickListener { deleteAsset() }
        binding.pauseAsset.setOnCheckedChangeListener { _, isChecked ->  pauseAsset(isChecked) }
        binding.pauseEngine.setOnCheckedChangeListener { _, isChecked -> pauseEngine(isChecked) }

        initVirtuosoSDK(savedInstanceState)
        updateUI()
    }

    private fun pauseAsset(pause : Boolean){
        asset?.let{
            if(pause){
                virtuoso.assetManager.pauseDownload(it)
            }
            else{
                virtuoso.assetManager.resumeDownload(it)
            }
        }
    }

    private fun pauseEngine(pause: Boolean){
        download2GoService?.let{
            if(it.isBound){
                if(pause && it.status != Common.EngineStatus.PAUSED){
                    pauseRequested = true
                    it.pauseDownloads()
                }
                else if(!pause && it.status == Common.EngineStatus.PAUSED){
                    resumeRequested = true
                    it.resumeDownloads()
                }
            }
        }
    }

    private fun initVirtuosoSDK(savedInstanceState: Bundle?) {

        virtuoso = Virtuoso(this)
        queueObserver = AssetQueueObserver(this)

        download2GoService = virtuoso.service

        //this is the current best practice for initializing the SDK
        if(savedInstanceState == null){//initial start of activity will have null saved instance state
            val status = virtuoso.backplane?.authenticationStatus
            if(status == Common.AuthenticationStatus.NOT_AUTHENTICATED){//if not authenticated execute sdk startup
                //here we use the simplest login with hard coded values

                virtuoso.startup(
                    URL(BACKPLANE_URL),//substitute the proper backplane url for you implementation
                    virtuoso.backplane?.settings?.deviceId,//provide an appropriate unique user id.  Virtuoso SDK device id used here for convenience
                    null, //Optional additional device id to be associated with the user account.  This is not the device id generated by the virtuoso SDK
                    BACKPLANE_PUBLIC_KEY,//Penthera demo public key.  Substitute the correct one.
                    BACKPLANE_PRIVATE_KEY
                ) { _, _ ->}//callback lambda for push registration.  this will be detailed in subsequent tutorials

            }

        }

        //load asset if it has already been downloaded
        val list : MutableList<IIdentifier>? = virtuoso.assetManager.getByAssetId(ASSET_ID)

        list?.let{
            if (it.isNotEmpty())
                asset = list[0] as IAsset
        }
    }

    override fun onResume() {
        super.onResume()

        //resume the VirtuosoSDK on activity resume
        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.P){
            //this is the recommended workaround for issuetracker.google.com/issues/110237673
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningAppProcessInfo = activityManager.runningAppProcesses?.first { it.pid == android.os.Process.myPid() }
            if (runningAppProcessInfo != null && runningAppProcessInfo.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                virtuoso.onResume()
            }
        }
        else{
            virtuoso.onResume()
        }
        virtuoso.addObserver(queueObserver)
        virtuoso.addObserver(enginePauseObserver)
        download2GoService?.setConnectionObserver(serviceConnectionObserver)
        download2GoService?.bind()


    }

    override fun onPause() {
        super.onPause()

        //pause the VirtuosoSDK on activity pause
        virtuoso.onPause()
        virtuoso.removeObserver(queueObserver)
        virtuoso.removeObserver(enginePauseObserver)
        download2GoService?.unbind()
        download2GoService?.setConnectionObserver(null)
    }

    private fun playAsset() {

        if(asset != null) {
            VideoPlayerActivity.playVideoDownload(asset!!, this)
        }


    }

    private fun deleteAsset() {
        virtuoso.assetManager.delete(asset)
        asset = null

        findViewById<TextView>(R.id.textView).text = "Deleting asset"
        val pb = findViewById<ProgressBar>(R.id.progressBar)
        pb.visibility = View.INVISIBLE
        pb.progress = 0
        updateUI()
    }

    fun updateUI() {

        binding.download.isEnabled = asset == null
        binding.play.isEnabled = asset != null
        binding.delete.isEnabled = asset != null

        if(asset == null){
            findViewById<TextView>(R.id.textView).text = ""
            binding.ancillaryImage.visibility = View.INVISIBLE
            binding.pauseAsset.isEnabled = false
        }
        else{
            internalUpdate = true
            if(binding.pauseAsset.isChecked != (asset?.downloadStatus == Common.AssetStatus.DOWNLOAD_PAUSED)){
                binding.pauseAsset.isChecked = true
            }
            internalUpdate = false

            binding.pauseAsset.isEnabled = true
        }

        showAncillaryImage(asset)
    }


    private fun showAncillaryImage(asset: IAsset?) {
        asset?.let{ iAsset ->
            if(iAsset is ISegmentedAsset){
                var fileList : List<AncillaryFile>? = iAsset.getAncillaryFiles(this)
                fileList?.let{ ancillaries ->

                    val fileLocalPath = ancillaries[0].localPath

                    fileLocalPath?.let{filePath->
                        val imgFile = File(filePath)
                        if (imgFile.exists()) {
                            binding.ancillaryImage.apply {
                                setImageURI(Uri.fromFile(imgFile))
                                visibility = View.VISIBLE
                            }
                        }
                    }

                }
            }
        }
    }
    private fun downloadAsset(){

        // One or more tags can be saved with the images
        val tags = arrayOf(ANCILLARY_IMAGE_TAG)

        // Set up the list of ancillary files to download with the asset
        val fileList: MutableList<AncillaryFile> = ArrayList()
        fileList.add(AncillaryFile(URL(ANCILLARY_IMAGE_URL), "Movie Image", tags))

        val params = MPDAssetBuilder().apply {
            assetId(ASSET_ID)
            manifestUrl(URL(ASSET_URL))
            assetObserver(AssetParseObserver(this@MainActivity))
            addToQueue(true)
            desiredVideoBitrate(Int.MAX_VALUE)
            withMetadata(ASSET_TITLE)
            withAncillaryFiles(fileList)
        }.build()

        virtuoso.assetManager.createMPDSegmentedAssetAsync(params)
    }

    class AssetParseObserver(activity : AppCompatActivity) : ISegmentedAssetFromParserObserver{

        private var mActivty : AppCompatActivity = activity

        @SuppressLint("ShowToast")
        override fun complete(asset: ISegmentedAsset?, error : Int, addedToQueue : Boolean) {

            if(asset != null){
               Toast.makeText(mActivty, "Asset parsed and " + if(addedToQueue) "added" else "not added" + "to download queue", Toast.LENGTH_LONG  ).show()

            }
            else{
                Toast.makeText(mActivty, "Error $error while parsing asset", Toast.LENGTH_LONG).show()
            }
        }
    }

    class AssetQueueObserver(activity: MainActivity) : Observers.IQueueObserver {


        private var lastProgress : Int = -1
        private var  mActivity : MainActivity = activity



        override fun engineStartedDownloadingAsset(aAsset: IIdentifier) {
            lastProgress = -1
            updateItem(aAsset, true)
        }

        override fun enginePerformedProgressUpdateDuringDownload(aAsset: IIdentifier) {
            updateItem(aAsset, true)
        }

        override fun engineCompletedDownloadingAsset(aAsset: IIdentifier) {
            updateItem(aAsset, true)
        }

        override fun engineEncounteredErrorDownloadingAsset(aAsset: IIdentifier) {
            // The base implementation does nothing.  See class documentation.
        }

        override fun engineUpdatedQueue() {
            // This indicates a change to the download queue - meaning either we added or removed something
            val assetManager: IAssetManager = mActivity.virtuoso.assetManager
            val queued = assetManager.queue.size()
            val downloaded = assetManager.downloaded.cursor.count
            val curAsset = mActivity.asset
            if ( curAsset != null && (queued > 0 || downloaded > 0)) {
                val asset = assetManager.get(curAsset.id) as IAsset
                if (asset.downloadStatus != curAsset.downloadStatus) {
                    mActivity.asset = asset
                    updateItem(asset, true)
                }
            }
            if (queued == 0) {
                // The asset has been deleted or downloaded
                mActivity.runOnUiThread {
                    val tv = mActivity.findViewById(R.id.textView) as TextView
                    tv.text = if (downloaded == 0) "Asset Deleted" else "Asset Downloaded"
                    val pb = mActivity.findViewById(R.id.progressBar) as ProgressBar
                    pb.visibility = View.GONE
                }
            }
        }

        override fun engineEncounteredErrorParsingAsset(mAssetId: String) {}


        private fun updateItem(aFile: IIdentifier?, forceUpdate: Boolean) {

            aFile?.let {
                if(aFile is IAsset) {
                    val assetId = aFile.assetId

                    // Progress is for catalog item
                    if (assetId.isNotEmpty() && ASSET_ID == assetId) {
                        //update our asset status
                        mActivity.runOnUiThread { updateItemStatus(aFile, forceUpdate) }

                    }
                }
            }
        }

        private fun updateItemStatus(asset: IAsset, forceUpdate: Boolean) {


            if(ASSET_ID == asset.assetId){
                mActivity.asset = asset

                var progress = (asset.fractionComplete * 100.0).toInt()
                // Not a repeated progress -- Keep context switches minimal due to frequency of messages, unless forced
                if (forceUpdate || progress != lastProgress) {
                    val assetStatus : String
                    val fds = asset.downloadStatus
                    val value: String
                    when (fds) {

                        Common.AssetStatus.DOWNLOADING -> {
                            assetStatus = mActivity.getString(R.string.status_downloading)
                            value = "downloading"
                        }

                        Common.AssetStatus.DOWNLOAD_COMPLETE -> {
                            assetStatus = mActivity.getString(R.string.status_downloaded)
                            value = "complete"
                            mActivity.updateUI()
                        }

                        Common.AssetStatus.EXPIRED -> {
                            assetStatus = mActivity.getString(R.string.status_expired)
                            value = "expired"
                        }

                        Common.AssetStatus.DOWNLOAD_DENIED_ASSET -> {
                            assetStatus = "Queued"
                            value = "DENIED : MAD"
                        }

                        Common.AssetStatus.DOWNLOAD_DENIED_ACCOUNT -> {
                            assetStatus = "Queued"
                            value = "DENIED : MDA"
                        }

                        Common.AssetStatus.DOWNLOAD_DENIED_EXTERNAL_POLICY -> {
                            assetStatus = "Queued"
                            value = "DENIED : EXT"
                        }

                        Common.AssetStatus.DOWNLOAD_DENIED_MAX_DEVICE_DOWNLOADS -> {
                            assetStatus = "Queued"
                            value = "DENIED :MPD"
                        }

                        Common.AssetStatus.DOWNLOAD_BLOCKED_AWAITING_PERMISSION -> {
                            assetStatus = "Queued"
                            value = "AWAITING PERMISSION"
                        }

                        Common.AssetStatus.DOWNLOAD_DENIED_COPIES -> {
                            assetStatus = "Queued"
                            value = "DENIED : COPIES"
                        }

                        else -> {
                            assetStatus = mActivity.getString(R.string.status_pending)
                            value = "pending"
                        }
                    }


                    mActivity.updateUI()
                    val tv = mActivity.findViewById(R.id.textView) as TextView
                    tv.visibility = View.VISIBLE
                    tv.text = String.format(mActivity.getString(R.string.asset_status), assetStatus, asset.errorCount, value)

                    lastProgress = progress
                    // Tiny Progress
                    if (progress == 0) progress = 1

                    // Progress Bar
                    val pb = mActivity.findViewById(R.id.progressBar) as ProgressBar
                    if (progress in 1..99) {
                        pb.progress = progress
                        pb.visibility = View.VISIBLE
                    } else {
                        pb.visibility = View.GONE
                    }
                }
            }

        }
    }

    companion object{
        // Important: Asset ID should be unique across your video catalog
        const val ASSET_ID : String = "TEST_ASSET_ID"
        const val ASSET_TITLE : String = "TEST ASSET"
        const val ASSET_URL: String = "https://storage.googleapis.com/wvmedia/clear/h264/tears/tears_sd.mpd"
        const val ANCILLARY_IMAGE_URL: String  = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/70/Tos-poster.png/440px-Tos-poster.png"
        const val ANCILLARY_IMAGE_TAG: String  = "movie-posters"
        
        const val BACKPLANE_URL = "https://demo.penthera.com"
        const val BACKPLANE_PUBLIC_KEY =  
        const val BACKPLANE_PRIVATE_KEY = 

    }
}
