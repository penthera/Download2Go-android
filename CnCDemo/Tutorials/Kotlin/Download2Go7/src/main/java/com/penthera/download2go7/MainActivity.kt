package com.penthera.download2go7

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.penthera.common.Common.AssetStatus
import com.penthera.common.Common.AuthenticationStatus
import com.penthera.download2go7.databinding.ActivityMainBinding
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.*
import com.penthera.virtuososdk.client.IService.IConnectionObserver
import com.penthera.virtuososdk.client.Observers.IEngineObserver
import com.penthera.virtuososdk.client.autodownload.PlaylistConfigBuilder
import com.penthera.virtuososdk.client.builders.HLSAssetBuilder
import java.net.URL
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var  virtuoso : Virtuoso
    var asset : IAsset? =  null
    private lateinit var queueObserver: AssetQueueObserver


    private var internalUpdate : Boolean = false
    //local reference to the Download2Go service
    private var download2GoService : IService? = null

    private var pauseRequested : Boolean = false
    private var resumeRequested : Boolean = false

    /**
     * Connection observer monitors when the service is bound
     */
     private var serviceConnectionObserver = object : IConnectionObserver {
        override fun connected() {
            download2GoService?.let{
                if(it.isBound){
                    try {
                        val status = it.status
                        internalUpdate = true
                        binding.pauseEngine.isChecked = status == Common.EngineStatus.PAUSED
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


        initVirtuosoSDK(savedInstanceState)

        binding.download.setOnClickListener { downloadAsset() }
        binding.play.setOnClickListener { playAsset()}
        binding.delete.setOnClickListener { deleteAsset() }

        binding.showPlaylist.setOnClickListener { showPlaylist() }

        binding.pauseAsset.setOnCheckedChangeListener { _, isChecked ->  pauseAsset(isChecked) }
        binding.pauseEngine.setOnCheckedChangeListener { _, isChecked -> pauseEngine(isChecked) }

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
            if(status == AuthenticationStatus.NOT_AUTHENTICATED){//if not authenticated execute sdk startup
                //here we use the simplest login with hard coded values

                virtuoso.startup(
                    URL(BACKPLANE_URL),//substitute the proper backplane url for you implementation
                    // substitute the proper backplane url for your implementation
                    UUID.randomUUID()
                        .toString(),    // provide an appropriate unique user id. A random uuid is used here for demonstration purposes only
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
        }

        //load asset if it has already been downloaded
        asset = getCurrentAsset()

    }

    private fun getCurrentAsset() : IAsset? {

        for(id in ASSET_LIST){
            val list : MutableList<IIdentifier>? = virtuoso.assetManager.getByAssetId(id)

            list?.let{
                if (it.isNotEmpty())
                    return  list[0] as IAsset
            }
        }

        return null
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

    private fun showPlaylist(){
        
        startActivity(Intent(this, PlaylistItemsActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(PlaylistItemsActivity.PLAYLIST_NAME, TEST_PLAYLIST_NAME)
        })

    }

    fun updateUI() {

        binding.download.isEnabled = asset == null
        binding.play.isEnabled = asset != null
        binding.delete.isEnabled = asset != null

        if( asset == null){
            binding.textView.text = ""
            binding.currentAsset.text = ""
            binding.pauseAsset.isEnabled = false
        }
        else{
            binding.pauseAsset.isEnabled = true
            binding.currentAsset.text = asset!!.metadata
        }
    }

    private fun downloadAsset(){

        virtuoso.assetManager.playlistManager.find(TEST_PLAYLIST_NAME)?.let{
            //clicking the download button will restart the entire autodownload process
            // so we clear the assets and history of the exisitng playlist
            virtuoso.assetManager.playlistManager.clear(TEST_PLAYLIST_NAME)
            //then add the assets we want in the playlist
            it.append(ASSET_LIST.toMutableList())
        } ?: run {
            //create playlist configuration options
            val plConfig = PlaylistConfigBuilder().apply{
                withName(TEST_PLAYLIST_NAME)
                requirePlayback(true)
                considerAssetHistory(false)
                searchFromBeginning(false)
            }.build()

            //create playlist with list of asset ids
            virtuoso.assetManager.playlistManager.create(plConfig, ASSET_LIST.toMutableList())
        }

        //create first asset in playlist
        val params = HLSAssetBuilder().apply {
            assetId(ASSET_ID)//REQUIRED PARAMETER asset ID of the new asset
            manifestUrl(URL(ASSET_URL))//REQUIRED PARAMETER  url of the new asset
            assetObserver(AssetParseObserver(this@MainActivity))//REQUIRED PARAMETER observer
            addToQueue(true)// add to the download queue after parsing complete
            desiredVideoBitrate(Int.MAX_VALUE)//specify a bitrate for desired video quality Integer.MAX_VALUE for largest available
            withMetadata(ASSET_TITLE)//user specified descriptive text for the asset.  Here we supply a title.
        }.build()

        virtuoso.assetManager.createHLSSegmentedAssetAsync(params)
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
                    mActivity.binding.textView.text = if (downloaded == 0) "Asset Deleted" else "Asset Downloaded"
                    mActivity.binding.progressBar.visibility = View.GONE
                }
            }
        }

        override fun engineEncounteredErrorParsingAsset(mAssetId: String) {}


        private fun updateItem(aFile: IIdentifier?, forceUpdate: Boolean) {

            aFile?.let {
                if(aFile is IAsset) {
                    val assetId = aFile.assetId

                    // Progress is for catalog item
                    if (assetId.isNotEmpty() && ASSET_LIST.contains(assetId)) {
                        //update our asset status
                        mActivity.runOnUiThread { updateItemStatus(aFile, forceUpdate) }

                    }
                }
            }
        }

        private fun updateItemStatus(asset: IAsset, forceUpdate: Boolean) {


            if(ASSET_LIST.contains(asset.assetId)){
                mActivity.asset = asset

                var progress  = (asset.currentSize / asset.expectedSize * 100).toInt()
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
                    val tv = mActivity.binding.textView
                    tv.visibility = View.VISIBLE
                    tv.text = String.format(mActivity.getString(R.string.asset_status), assetStatus, asset.errorCount, value)

                    lastProgress = progress
                    // Tiny Progress
                    if (progress == 0) progress = 1

                    // Progress Bar
                    val pb = mActivity.binding.progressBar
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
        const val ASSET_ID : String = "SEASON-1-EPISODE-1"
        const val ASSET_TITLE : String = "Season 1 Episode 1"
        const val ASSET_ID2 : String = "SEASON-1-EPISODE-2"
        const val ASSET_TITLE2 : String = "Season 1 Episode 2"
        const val ASSET_ID3 : String = "SEASON-1-EPISODE-3"
        const val ASSET_TITLE3 : String = "Season 1 Episode 3"
        const val ASSET_ID4 : String = "SEASON-1-EPISODE-4"
        const val ASSET_TITLE4 : String = "Season 1 Episode 4"

        const val TEST_PLAYLIST_NAME : String = "TEST_PLAYLIST"

        const val ASSET_URL: String = "http://virtuoso-demo-content.s3.amazonaws.com/bbb/season1/ep1/index.m3u8"
        const val ASSET_URL2: String = "http://virtuoso-demo-content.s3.amazonaws.com/bbb/season1/ep2/index.m3u8"
        const val ASSET_URL3: String = "http://virtuoso-demo-content.s3.amazonaws.com/bbb/season1/ep3/index.m3u8"
        const val ASSET_URL4: String = "http://virtuoso-demo-content.s3.amazonaws.com/bbb/season1/ep4/index.m3u8"

        val ASSET_MAP = mapOf(ASSET_ID to Pair(ASSET_TITLE,  ASSET_URL),
            ASSET_ID2 to Pair(ASSET_TITLE2, ASSET_URL2),
            ASSET_ID3 to Pair(ASSET_TITLE3, ASSET_URL3),
            ASSET_ID4 to Pair(ASSET_TITLE4, ASSET_URL4))

        val ASSET_LIST = listOf(ASSET_ID, ASSET_ID2, ASSET_ID3, ASSET_ID4)

        const val BACKPLANE_URL = "https://demo.penthera.com"
        const val BACKPLANE_PUBLIC_KEY =  
        const val BACKPLANE_PRIVATE_KEY = 

    }
}
