package com.penthera.download2go1_3

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.penthera.virtuososdk.Common
import com.penthera.common.Common.AuthenticationStatus
import com.penthera.common.Common.AssetStatus
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
    private lateinit var dlBtn : Button
    private lateinit var plBtn : Button
    private lateinit var delBtn : Button
    private lateinit var pauseAsset : Switch
    private lateinit var pauseEngine : Switch
    private lateinit var ancillaryImage : ImageView

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
                        pauseEngine.isChecked = it.status == Common.EngineStatus.PAUSED
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
        setContentView(R.layout.activity_main)

        virtuoso = Virtuoso(this)
        queueObserver = AssetQueueObserver(this)

        download2GoService = virtuoso.service

        dlBtn = findViewById(R.id.download)
        dlBtn.setOnClickListener { downloadAsset() }
        plBtn= findViewById(R.id.play)
        plBtn.setOnClickListener { playAsset()}
        delBtn = findViewById(R.id.delete)
        delBtn.setOnClickListener { deleteAsset() }
        pauseAsset = findViewById(R.id.pauseAsset)
        pauseAsset.setOnCheckedChangeListener { _, isChecked ->  pauseAsset(isChecked) }
        pauseEngine = findViewById(R.id.pauseEngine)
        pauseEngine.setOnCheckedChangeListener { _, isChecked -> pauseEngine(isChecked) }
        ancillaryImage = findViewById(R.id.ancillaryImage)

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

    private fun initVirtuosoSDK() {
        //this is the current best practice for initializing the SDK
            val status = virtuoso.backplane?.authenticationStatus
            if(status != AuthenticationStatus.AUTHENTICATED){//if not authenticated execute sdk startup
                //here we use the simplest login with hard coded values

                virtuoso.startup(
                    URL(BACKPLANE_URL),//substitute the proper backplane url for you implementation
                    virtuoso.backplane?.settings?.deviceId,//provide an appropriate unique user id.  Virtuoso SDK device id used here for convenience
                    null, //Optional additional device id to be associated with the user account.  This is not the device id generated by the virtuoso SDK
                    BACKPLANE_PUBLIC_KEY,//Penthera demo public key.  Substitute the correct one.
                    BACKPLANE_PRIVATE_KEY,
                    object : IPushRegistrationObserver {
                        override fun onServiceAvailabilityResponse(
                            pushService: Int,
                            errorCode: Int
                        ) {
                            //callback for push registration.  this will be detailed in subsequent tutorials
                        }
                    }
                )
            }

        //load asset if it has already been downloaded
        val list : MutableList<IIdentifier>? = virtuoso.assetManager.getByAssetId(ASSET_ID)

        list?.let{
            if (it.isNotEmpty())
                asset = list[0] as IAsset
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.status_view -> {
                val i = Intent(this, StatusViewActivity::class.java)
                startActivity(i)
                true
            }
            else -> super.onOptionsItemSelected(item)
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

        dlBtn.isEnabled = asset == null
        plBtn.isEnabled = asset != null
        delBtn.isEnabled = asset != null

        if(asset == null){
            findViewById<TextView>(R.id.textView).text = ""
            pauseAsset.isEnabled = false
            ancillaryImage.visibility = View.INVISIBLE
        }
        else{
            internalUpdate = true
            if(pauseAsset.isChecked != (asset?.downloadStatus == AssetStatus.DOWNLOAD_PAUSED)){
                pauseAsset.isChecked = true
            }
            internalUpdate = false

            pauseAsset.isEnabled = true
        }

        showAncillaryImage(asset)
    }


    private fun showAncillaryImage(asset: IAsset?) {
        asset?.let{
            if(it is ISegmentedAsset){
                val fileList = it.getAncillaryFiles(this)
                if(!fileList.isNullOrEmpty()){
                    val fileLocalPath = fileList[0].localPath

                    fileLocalPath?.let{filepath ->
                        val imgFile = File(filepath)
                        if (imgFile.exists()) {
                            ancillaryImage.setImageURI(Uri.fromFile(imgFile))
                            ancillaryImage.visibility = View.VISIBLE
                        }
                    }

                }
            }
        }
    }
    private fun downloadAsset(){
        initVirtuosoSDK()

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
                assetManager.get(curAsset.id)?.let {
                    val asset = it as IAsset
                    if (asset.downloadStatus != curAsset.downloadStatus) {
                        mActivity.asset = asset
                        updateItem(asset, true)
                    }
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


        private fun updateItem(aFile: IIdentifier, forceUpdate: Boolean) {

            val updateAsset = aFile as IAsset
            val assetId = updateAsset.assetId

            // Progress is for catalog item
            if (assetId.isNotEmpty() && ASSET_ID == assetId) {
                //update our asset status
                mActivity.runOnUiThread{ updateItemStatus(updateAsset, forceUpdate) }

            }
        }

        private fun updateItemStatus(asset: IAsset?, forceUpdate: Boolean) {

            asset?.let{
                if(ASSET_ID == it.assetId){
                    mActivity.asset = asset

                    var progress = (asset.fractionComplete * 100.0).toInt()
                    // Not a repeated progress -- Keep context switches minimal due to frequency of messages, unless forced
                    if (forceUpdate || progress != lastProgress) {
                        val assetStatus : String
                        val fds = asset.downloadStatus
                        val value: String
                        when (fds) {

                            AssetStatus.DOWNLOADING -> {
                                assetStatus = mActivity.getString(R.string.status_downloading)
                                value = "downloading"
                            }

                            AssetStatus.DOWNLOAD_COMPLETE -> {
                                assetStatus = mActivity.getString(R.string.status_downloaded)
                                value = "complete"
                                mActivity.updateUI()
                            }

                            AssetStatus.EXPIRED -> {
                                assetStatus = mActivity.getString(R.string.status_expired)
                                value = "expired"
                            }

                            AssetStatus.DOWNLOAD_DENIED_ASSET -> {
                                assetStatus = "Queued"
                                value = "DENIED : MAD"
                            }

                            AssetStatus.DOWNLOAD_DENIED_ACCOUNT -> {
                                assetStatus = "Queued"
                                value = "DENIED : MDA"
                            }

                            AssetStatus.DOWNLOAD_DENIED_EXTERNAL_POLICY -> {
                                assetStatus = "Queued"
                                value = "DENIED : EXT"
                            }

                            AssetStatus.DOWNLOAD_DENIED_MAX_DEVICE_DOWNLOADS -> {
                                assetStatus = "Queued"
                                value = "DENIED :MPD"
                            }

                            AssetStatus.DOWNLOAD_BLOCKED_AWAITING_PERMISSION -> {
                                assetStatus = "Queued"
                                value = "AWAITING PERMISSION"
                            }

                            AssetStatus.DOWNLOAD_DENIED_COPIES -> {
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
