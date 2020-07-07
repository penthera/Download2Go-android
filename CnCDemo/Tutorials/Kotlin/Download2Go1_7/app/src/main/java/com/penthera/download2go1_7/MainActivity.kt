package com.penthera.download2go1_7

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.GoogleApiAvailability
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.*
import com.penthera.virtuososdk.client.Observers.IBackplaneObserver
import com.penthera.virtuososdk.client.builders.HLSAssetBuilder
import java.net.MalformedURLException
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var  virtuoso : Virtuoso
    var asset : IAsset? =  null
    private lateinit var queueObserver: AssetQueueObserver
    private lateinit var dlBtn : Button
    private lateinit var plBtn : Button
    private lateinit var delBtn : Button

    private var registering: Boolean = false

    private lateinit var registerBtn : Button
    private lateinit var unregisterBtn : Button
    private lateinit var accountName : EditText

    private lateinit var progress : ProgressBar
    private lateinit var text : TextView

    // Observe backplane request changes, enabling the UI to react to actions such as SDK startup and unregister.
    private val backplaneObserver =
        IBackplaneObserver { request, result, _ ->
            runOnUiThread {
                if (request == Common.BackplaneCallbackType.SYNC) {
                    // Push messages result in a sync. That sync response may contain instructions such as to
                    // unregister the device, disable downloads, or remote wipe.
                    if (result == Common.BackplaneResult.SUCCESS) {
                        updateRegisterButtons()
                    }
                }
                if (request == Common.BackplaneCallbackType.REGISTER || request == Common.BackplaneCallbackType.UNREGISTER) {
                    registering = false
                    if (result == Common.BackplaneResult.SUCCESS) {
                        updateRegisterButtons()
                    } else {
                        Toast.makeText(baseContext, "Backplane register / unregister failed", Toast.LENGTH_SHORT).show()
                    }
                } else if (request == Common.BackplaneCallbackType.DEVICE_UNREGISTERED) {
                    // Unregister a different device
                    Toast.makeText(baseContext,
                        if (result == Common.BackplaneResult.SUCCESS) R.string.unregister_success else R.string.unregister_fail,
                        Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initVirtuosoSDK()

        dlBtn = findViewById(R.id.download)
        dlBtn.setOnClickListener { downloadAsset() }
        plBtn= findViewById(R.id.play)
        plBtn.setOnClickListener { playAsset()}
        delBtn = findViewById(R.id.delete)
        delBtn.setOnClickListener { deleteAsset() }
        registerBtn = findViewById(R.id.register)
        registerBtn.setOnClickListener { if(!registering)register(false) }
        unregisterBtn = findViewById(R.id.unregister)
        unregisterBtn.setOnClickListener { unregister() }
        accountName = findViewById(R.id.account_name)

        text = findViewById(R.id.textView)
        progress = findViewById(R.id.progressBar)

        updateUI()
    }

    private fun initVirtuosoSDK() {

        virtuoso = Virtuoso(this)
        queueObserver = AssetQueueObserver(this)


        //load asset if it has already been downloaded
        val list : MutableList<IIdentifier>? = virtuoso.assetManager.getByAssetId(ASSET_ID)

        list?.let{
            if (it.isNotEmpty())
                asset = list[0] as IAsset
        }
    }

    private fun updateRegisterButtons() {

        if (virtuoso.backplane.authenticationStatus == Common.AuthenticationStatus.NOT_AUTHENTICATED) { // If not authenticated then execute sdk startup
            registerBtn.isEnabled = true
        } else {
            registerBtn.isEnabled = false
            val registeredUserId = virtuoso.backplane.settings.userId
            accountName.setText(registeredUserId)
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
        virtuoso.addObserver(backplaneObserver)
        updateUI()


    }

    override fun onPause() {
        super.onPause()

        //pause the VirtuosoSDK on activity pause
        virtuoso.onPause()
        virtuoso.removeObserver(queueObserver)
        virtuoso.removeObserver(backplaneObserver)
    }


    private fun register(retry : Boolean){

        if (virtuoso.backplane.authenticationStatus == Common.AuthenticationStatus.NOT_AUTHENTICATED) { // If not authenticated then execute sdk startup
            val name = accountName.text.toString()
            if (name.isNotEmpty()) {// Here we use the simplest login with hard coded values
                val backplaneUrl: URL?
                backplaneUrl = try {
                    URL(BACKPLANE_URL)
                } catch (mue: MalformedURLException) {
                    // In a real app we would handle this error
                    return
                }
                registering = true

                // This starts the SDK by registering it with the backplane. It is asynchronous. Success can be observed using the IEngineObserver interface.
                // Push registration callback,
                virtuoso.startup(
                    backplaneUrl,  // substitute the proper backplane url for your implementation
                    name,  // provide an appropriate unique user id. Virtuoso SDK device id is used here for demonstration purposes only
                    null,  // Optional additional device id to be associated with the user account. This is not the device id generated by the SDK
                    BACKPLANE_PUBLIC_KEY,  // Substitute the real public backplane key here
                    BACKPLANE_PRIVATE_KEY  // Substitute the real private backplane key here
                ) { pushService: Int, connectionResponse: Int ->                  // Push registration observer (IPushRegistrationObserver)
                    // This observer can be used for both FCM and ADM. Only FCM is demonstrated here
                    // This callback does not indicate that SDK startup is complete, only that push is registered
                    if (pushService == Common.PushService.FCM_PUSH) // Check for failure
                        if (connectionResponse != 0) {

                            // If FCM registration fails on older devices it is most likely due to lack
                            // of the appropriate support library. Check to see if it can be made available.
                            val gApi = GoogleApiAvailability.getInstance()
                            if (!retry && gApi.isUserResolvableError(connectionResponse)) {
                                runOnUiThread {
                                    gApi.makeGooglePlayServicesAvailable(this@MainActivity)
                                        .addOnCompleteListener { task ->
                                            Log.d("Push Registration", "makeGooglePlayServicesAvailable complete")

                                            if (task.isSuccessful) {
                                                Log.d("Push Registration", "makeGooglePlayServicesAvailable completed successfully")
                                                register(true)
                                            } else {
                                                task.exception?.let{
                                                    Log.e("Push Registration", "makeGooglePlayServicesAvailable completed with exception " + it.message, it)
                                                }
                                            }
                                        }
                                }
                            }
                        } // on success we do not take any action in this demonstration
                }
            } else {
                Toast.makeText(this, R.string.missing_account_name, Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun unregister(){

        unregisterBtn.isEnabled = false

        // Set up request to get account devices
        if (virtuoso.backplane.authenticationStatus == Common.AuthenticationStatus.AUTHENTICATED) {
            try {
                virtuoso.backplane
                    .getDevices { iBackplaneDevices: Array<IBackplaneDevice> ->
                        if (iBackplaneDevices.isNotEmpty()) {
                            // There should always be at least one device registered - ourselves!
                            // Iterate to identify all other devices registered to user
                            for (device in iBackplaneDevices) {
                                if (!device.isCurrentDevice) {
                                    // When we request the device to deregister, this should send a push
                                    // notification to that device via the backplane. It may not be a common
                                    // approach to deregister devices directly from another device,
                                    // but is useful for demonstration purposes.
                                    try {
                                        virtuoso.backplane.unregisterDevice(device)
                                    } catch (e: BackplaneException) {
                                        Log.e("Unregister", "Caught exception requesting unregister on device", e)
                                    }
                                }
                            }
                        }
                    }
            } catch (be: BackplaneException) {
                Log.e("Unregister", "Caught exception requesting devices", be)
            }
        }

        unregisterBtn.isEnabled = true

    }
    private fun playAsset() {
        asset?.let{
            VideoPlayerActivity.playVideoDownload(it, this)
        }

    }

    private fun deleteAsset() {
        virtuoso.assetManager.delete(asset)
        asset = null
        updateUI()
    }

    fun updateUI() {

        dlBtn.isEnabled = asset == null
        plBtn.isEnabled = asset != null
        delBtn.isEnabled = asset != null

        if(asset == null){
            text.text = ""
        }

        updateRegisterButtons()
    }

    private fun downloadAsset(){

        val params = HLSAssetBuilder().apply {
            assetId(ASSET_ID) //REQUIRED PARAMETER asset ID of the new asset
            manifestUrl(URL(ASSET_URL)) //REQUIRED PARAMETER  url of the new asset
            assetObserver(AssetParseObserver(this@MainActivity))//REQUIRED PARAMETER observer that will be notified of parsing status
            addToQueue(true) // add to the download queue after parsing complete
            desiredVideoBitrate(Integer.MAX_VALUE)//specify a bitrate for desired video quality Integer.MAX_VALUE for largest available
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
                        val value: String
                        when (asset.downloadStatus) {

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
    }

    companion object{
        // Important: Asset ID should be unique across your video catalog
        const val ASSET_ID : String = "TEST_ASSET_ID"
        const val ASSET_TITLE : String = "TEST ASSET"
        const val ASSET_URL: String = "http://hls-vbcp.s3.amazonaws.com/normal/small/im2_rel.m3u8"

        const val BACKPLANE_URL = "https://demo.penthera.com"
        const val BACKPLANE_PUBLIC_KEY =  
        const val BACKPLANE_PRIVATE_KEY = 

    }
}
