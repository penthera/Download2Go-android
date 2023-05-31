package com.penthera.bitmovindemo

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.penthera.bitmovindemo.databinding.ActivityMainBinding

import com.penthera.virtuososdk.Common
import com.penthera.common.Common.AuthenticationStatus
import com.penthera.common.Common.AssetStatus
import com.penthera.virtuososdk.client.*
import com.penthera.virtuososdk.client.builders.MPDAssetBuilder


import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var  virtuoso : Virtuoso
    var asset : IAsset? =  null
    private lateinit var queueObserver: AssetQueueObserver
    private lateinit var licenseObserver: LicenseObserver




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        virtuoso = Virtuoso(this)
        queueObserver = AssetQueueObserver(this)
        licenseObserver = LicenseObserver(this)

        binding.download.setOnClickListener { downloadAsset() }
        binding.play.setOnClickListener { playAsset()}
        binding.delete.setOnClickListener { deleteAsset() }

        updateUI()
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
        virtuoso.addObserver(licenseObserver)
    }

    override fun onPause() {
        super.onPause()

        //pause the VirtuosoSDK on activity pause
        virtuoso.onPause()
        virtuoso.removeObserver(queueObserver)
        virtuoso.removeObserver(licenseObserver)
    }

    private fun playAsset() {

        if(asset != null) {
            VideoPlayerActivity.playVideoDownload(asset!!, this)
        }

    }

    private fun deleteAsset() {
        virtuoso.assetManager.delete(asset)
        asset = null
        updateUI()
    }

    fun updateUI() {

        binding.download.isEnabled = asset == null
        binding.play.isEnabled = asset != null
        binding.delete.isEnabled = asset != null

        if(asset == null){
            binding.textView.text = ""
        }
    }

    private fun downloadAsset(){
        initVirtuosoSDK()

        val params = MPDAssetBuilder().apply {
            assetId(ASSET_ID) //REQUIRED PARAMETER asset ID of the new asset
            manifestUrl(URL(ASSET_URL)) //REQUIRED PARAMETER  url of the new asset
            assetObserver(AssetParseObserver(this@MainActivity))//REQUIRED PARAMETER observer that will be notified of parsing status
            addToQueue(true) // add to the download queue after parsing complete
            desiredVideoBitrate(Integer.MAX_VALUE)//specify a bitrate for desired video quality Integer.MAX_VALUE for largest available
            withMetadata(ASSET_TITLE)//user specified descriptive text for the asset.  Here we supply a title.
        }.build()

        virtuoso.assetManager.createMPDSegmentedAssetAsync(params)
    }

    class AssetParseObserver(activity : AppCompatActivity) : ISegmentedAssetFromParserObserver{

        private var mActivty : AppCompatActivity = activity

        @SuppressLint("ShowToast")
        override fun complete(asset: ISegmentedAsset?, error : Int, addedToQueue : Boolean) {

            mActivty.runOnUiThread {
                if(asset != null){
                    Toast.makeText(mActivty, "Asset parsed and " + if(addedToQueue) "added" else "not added" + "to download queue", Toast.LENGTH_LONG  ).show()

                }
                else{
                    Toast.makeText(mActivty, "Error $error while parsing asset", Toast.LENGTH_LONG).show()
                }
            }
            SegmentedParserError.CANCEL_ERROR


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
            // The base implementation does nothing.  See class documentation.
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
                        val value: String
                        when (asset.downloadStatus) {

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
    }
    internal class LicenseObserver(private val activity: AppCompatActivity) :
        EngineObserver() {
        override fun assetLicenseRetrieved(
            aItem: IIdentifier,
            aSuccess: Boolean
        ) {
            activity.runOnUiThread {
                val asset = aItem as ISegmentedAsset
                Toast.makeText(
                    activity,
                    activity.getString(
                        if (aSuccess) R.string.license_fetch_success else R.string.license_fetch_failure,
                        asset.assetId
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    }

    companion object{
        // Important: Asset ID should be unique across your video catalog
        const val ASSET_ID : String = "TEST_ASSET_ID"
        const val ASSET_TITLE : String = "TEST ASSET"
        const val ASSET_URL: String = "https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears_sd.mpd";

        const val BACKPLANE_URL = "https://qa.penthera.com/"
        const val BACKPLANE_PUBLIC_KEY =  "c9adba5e6ceeed7d7a5bfc9ac24197971bbb4b2c34813dd5c674061a961a899e"
        const val BACKPLANE_PRIVATE_KEY = "41cc269275e04dcb4f2527b0af6e0ea11d227319fa743e4364255d07d7ed2830"

    }
}
