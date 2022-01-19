@file:Suppress("DEPRECATION")

package com.penthera.sdkdemokotlin.fragment

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.database.Cursor
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.activity.OfflineVideoProvider
import com.penthera.sdkdemokotlin.activity.VideoPlayerActivity
import com.penthera.sdkdemokotlin.catalog.CatalogItemType
import com.penthera.sdkdemokotlin.catalog.ExampleCatalog
import com.penthera.sdkdemokotlin.catalog.ExampleCatalogItem
import com.penthera.sdkdemokotlin.catalog.ExampleMetaData
import com.penthera.sdkdemokotlin.databinding.FragmentAssetDetailBinding
import com.penthera.sdkdemokotlin.engine.OfflineVideoEngine
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.*
import com.penthera.virtuososdk.client.builders.HLSAssetBuilder
import com.penthera.virtuososdk.client.builders.MPDAssetBuilder
import com.penthera.virtuososdk.client.database.AssetColumns
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import org.ocpsoft.prettytime.PrettyTime
import java.lang.Long.MAX_VALUE
import java.net.URL
import java.util.*
import android.graphics.Bitmap as Bitmap1

/**
 *
 */
class AssetDetailFragment : Fragment()  {
    companion object {
        fun newInstance(asset: IAsset): AssetDetailFragment {
            val frag =  AssetDetailFragment()

            val args = Bundle()
            args.putString("assetID", asset.uuid)
             frag.arguments = args
            return frag
        }

        fun newInstance(catalogItem: ExampleCatalogItem): AssetDetailFragment {
            val frag = AssetDetailFragment()

            val args = Bundle()

            args.putString("catalogID", catalogItem.exampleAssetId)
            frag.arguments = args
            return frag
        }
    }

    var catalogItem: ExampleCatalogItem? = null

    private var asset: IAsset? = null

    private var offlineVideoEngine: OfflineVideoEngine? = null

    private lateinit var exampleCatalog : ExampleCatalog
    private lateinit var queueObserver : AssetQueueObserver

    private var _binding: FragmentAssetDetailBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        _binding = FragmentAssetDetailBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        asset?.let{outState.putString("assetID", it.uuid)}
        catalogItem?.let{outState.putString("catalogId", it.exampleAssetId)}


    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val offlineVideoProvider = activity as OfflineVideoProvider
        offlineVideoEngine = offlineVideoProvider.getOfflineEngine()
        exampleCatalog = ExampleCatalog(requireContext())
        queueObserver = AssetQueueObserver(activity)


        var assetid : String?  = null

        arguments?.let { assetid = if(it.containsKey("assetID") ) it.getString("assetID") else null }
        //value in saved instance state will override value in arguments
        savedInstanceState?.let{assetid = if(it.containsKey("assetID") ) it.getString("assetID") else if (assetid != null) assetid else null}

        asset = if(assetid != null) offlineVideoEngine?.getVirtuoso()?.assetManager?.get(assetid) as IAsset else null

        var catalogId : String? = null

        arguments?.let{catalogId = if(it.containsKey("catalogID") ) it.getString("catalogID") else null }
        savedInstanceState?.let{catalogId = if(it.containsKey("catalogID") ) it.getString("catalogID") else if (catalogId != null) catalogId else null}

        catalogItem = if(catalogId != null) exampleCatalog.findItemById(catalogId!!) else null

        setupUI()
    }

    override fun onPause() {
        super.onPause()
        offlineVideoEngine?.getVirtuoso()?.removeObserver(queueObserver)

    }

    override fun onResume() {
        super.onResume()
        queueObserver.update(this)
        offlineVideoEngine?.getVirtuoso()?.addObserver(queueObserver)
        asset?.let {
            queueObserver.enginePerformedProgressUpdateDuringDownload(it)
        }
    }


    private fun setupUI(){
        if(catalogItem == null ){
            asset?.let{
                catalogItem = exampleCatalog.findItemById(asset!!.assetId)

            }
        }

        if(asset == null){

            val list : MutableList<IIdentifier>? = offlineVideoEngine?.getVirtuoso()?.assetManager?.getByAssetId(catalogItem?.exampleAssetId)

            list?.let{
                if (it.size > 0)
                    asset = list[0] as IAsset
            }
        }

        if(catalogItem == null && asset == null){
            activity?.onBackPressed()
        }

        binding.txtTitle.text = catalogItem?.title

        if(!catalogItem?.contentRating.isNullOrEmpty()){
            binding.txtParentalRating.text = catalogItem?.contentRating
        }

        Picasso.get()
                .load(catalogItem?.imageUri)
                .into(object : Target {
                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}

                    override fun onBitmapLoaded(bitmap: Bitmap1?, from: Picasso.LoadedFrom?) {
                        val mutableBitmap = if (bitmap!!.isMutable)
                            bitmap
                        else
                            bitmap.copy(Bitmap1.Config.ARGB_8888, true)
                        val canvas = Canvas(mutableBitmap)
                        val colour = 88 and 0xFF shl 24
                        canvas.drawColor(colour, PorterDuff.Mode.DST_IN)

                        binding.detailBgImg.background = BitmapDrawable(context?.resources,mutableBitmap)
                    }

                })


        binding.txtDescription.text = catalogItem?.description
        binding.txtDuration.text = getDurationString(catalogItem!!.durationSeconds)

        binding.txtExpiry.text = makePretty(getExpiry(), "Never")
        binding.txtAvailable.visibility = View.GONE
        binding.txtDescription.text = catalogItem?.description

        val assetId : String  = catalogItem!!.exampleAssetId

        binding.btnDownload.text = if (isDownloaded(assetId) || isQ(assetId) || isExpired(assetId) ) "Delete " else  "Download"

        binding.btnDownload.setOnClickListener {
            if(isDownloaded(assetId) || isQ(assetId) || isExpired(assetId) ){
                showDeleteDialog()
            }
            else{
                downloadItem()
            }
        }

        binding.btnWatch.setOnClickListener {
            watchItem()
        }
    }

    /**
     * @return expiration time in milliseconds
     */
    private fun getExpiry(): Long {
        // Use Catalog Value
        var expiry = catalogItem!!.expiryDate
        // Override with SDK value if exists
        if (asset != null) {
            expiry = getExpiration(asset!!)
        }
        if (expiry > 0)
            expiry *= 1000

        return expiry
    }


    /**
     * Used by inbox to print expiration time
     *
     * @param asset the asset
     * @return expiration in seconds, -1 never
     */
    private fun getExpiration(asset: IAsset): Long {
        val completionTime = asset.completionTime
        val endWindow = asset.endWindow
        // Not downloaded

        val ret : Long
        when(completionTime) {
            0L -> ret = when (endWindow) {
                MAX_VALUE -> -1
                else -> endWindow
            }
            else -> ret =  getExpiration(asset.completionTime, asset.endWindow, asset.firstPlayTime, asset.eap, asset.ead)

        }

        return ret
    }

    private fun getExpiration(completionTime: Long, endWindow: Long, firstPlayTime: Long, expiryAfterPlay: Long, expiryAfterDownload: Long): Long {

        val ret : Long

        when(completionTime){

            0L -> ret = when (endWindow) {
                MAX_VALUE -> -1
                else -> endWindow
            }
            else -> {var playExpiry = MAX_VALUE
                var expiry = endWindow

                if (firstPlayTime > 0 && expiryAfterPlay > -1)
                    playExpiry = firstPlayTime + expiryAfterPlay

                expiry = Math.min(expiry, playExpiry)

                if (expiryAfterDownload > -1)
                    expiry = Math.min(expiry, completionTime + expiryAfterDownload)

                ret = if (expiry == MAX_VALUE) -1 else expiry}
        }

        return ret
    }

    private fun getDurationString(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = seconds % 3600 / 60
        val secondsVal = seconds % 60
        return twoDigitString(hours) + " : " + twoDigitString(minutes) + " : " + twoDigitString(secondsVal)
    }

    /**
     * Create a two digit time [prepend 0s to the time]
     *
     * @param number the number to pad
     *
     * @return two digit time as string
     */
    private fun twoDigitString(number: Int): String {
        if (number == 0) {
            return "00"
        }
        return if (number / 10 == 0) {
            "0$number"
        } else number.toString()
    }


    /**
     * true, item is in Q
     * @param assetId
     * @return true, item is in Q
     */
    private fun isQ(assetId: String): Boolean {
        val ret: Boolean

        var c: Cursor? = null
        try {
            c =  offlineVideoEngine?.getVirtuoso()?.assetManager?.queue?.getCursor(arrayOf(AssetColumns._ID), AssetColumns.ASSET_ID + "=?", arrayOf(assetId))
            ret =  c != null && c.count > 0
        } finally {
            if (c != null && !c.isClosed)
                c.close()
        }

        return ret
    }

    /**
     * true, the item has been downloaded
     * @param assetId
     * @return true, the item has been downloaded
     */
    private fun isDownloaded(assetId: String): Boolean {

        val ret: Boolean

        var c: Cursor? = null
        try {
            c =  offlineVideoEngine?.getVirtuoso()?.assetManager?.downloaded?.getCursor(arrayOf(AssetColumns._ID), AssetColumns.ASSET_ID + "=?", arrayOf(assetId))
            ret =  c != null && c.count > 0
        } finally {
            if (c != null && !c.isClosed)
                c.close()
        }

        return ret
    }

    /**
     * true, the item has been downloaded
     * @param assetId
     * @return true, the item has been downloaded
     */
    private fun isExpired(assetId: String): Boolean {

        val ret :Boolean

        var c: Cursor? = null
        try {
            c =  offlineVideoEngine?.getVirtuoso()?.assetManager?.expired?.getCursor(arrayOf(AssetColumns._ID), AssetColumns.ASSET_ID + "=?", arrayOf(assetId))
            ret =  c != null && c.count > 0
        } finally {
            if (c != null && !c.isClosed)
                c.close()
        }

        return ret
    }

    /**
     * Displays times like "2 Days from now"
     *
     * @param value timestamp in milliseconds
     *
     * @return Pretty time, never if -1 passeed
     */
    private fun makePretty(value: Long, fallback: String): String {
        if (value < 0) {
            return fallback
        }

        val p = PrettyTime()
        return p.format(Date(value))
    }

    /**
     * Show delete confirmation dialog
     */
    private fun showDeleteDialog() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(getString(R.string.are_you_sure))
        builder.setMessage(getString(R.string.delete_item))
        builder.setPositiveButton(android.R.string.yes) { dialog, _ ->
            deleteItem()
            dialog!!.dismiss()

        }
        builder.setNegativeButton(android.R.string.no) { dialog, _ ->
            dialog!!.dismiss()

        }
        builder.create().show()
    }

    private fun deleteItem() {

        val assetId : String = catalogItem!!.exampleAssetId
        if(asset == null){
            asset = offlineVideoEngine?.getVirtuosoAsset(assetId)
        }

        offlineVideoEngine?.getVirtuoso()?.assetManager?.delete(asset)
        activity?.runOnUiThread{
            binding.btnDownload.text = if (isDownloaded(assetId) || isQ(assetId) || isExpired(assetId) ) "Delete " else  "Download"
        }
    }

    private fun downloadItem(){

        val title = catalogItem?.title ?: "Untitled"
        val image = catalogItem?.imageUri ?: ""
        val metadata = ExampleMetaData(title, image).toJson()

        when(catalogItem?.contentType){

            CatalogItemType.DASH_MANIFEST -> {
                val pdlg = ProgressDialog.show(context, "Processing manifest", "Adding fragments...")
                val contentURL = URL(catalogItem?.contentUri)

                offlineVideoEngine?.getVirtuoso()?.assetManager?.createMPDSegmentedAssetAsync(MPDAssetBuilder().assetId(catalogItem?.exampleAssetId)
                        .manifestUrl(contentURL)
                        .assetObserver(AssetObserver(pdlg, requireActivity()))
                        .addToQueue(true)
                        .desiredAudioBitrate(Integer.MAX_VALUE)
                        .desiredVideoBitrate(Integer.MAX_VALUE)
                        .withMetadata(metadata)
                        .withPermissionObserver(AssetPermissionObserver(requireActivity()))
                        .build())
            }

            CatalogItemType.HLS_MANIFEST ->{
                val pdlg = ProgressDialog.show(context, "Processing manifest", "Adding fragments...")
                val contentURL = URL(catalogItem?.contentUri)
                offlineVideoEngine?.getVirtuoso()?.assetManager?.createHLSSegmentedAssetAsync(HLSAssetBuilder().assetId(catalogItem?.exampleAssetId)
                        .manifestUrl(contentURL)
                        .assetObserver(AssetObserver(pdlg, requireActivity()))
                        .addToQueue(true)
                        .desiredVideoBitrate(0)
                        .withMetadata(metadata)
                        .withPermissionObserver(AssetPermissionObserver(requireActivity()))
                        .build())

            }

            CatalogItemType.FILE -> {

                val manager :IAssetManager = offlineVideoEngine?.getVirtuoso()!!.assetManager

                val file: IFile = manager.createFileAsset(catalogItem?.contentUri, catalogItem?.exampleAssetId, catalogItem?.mimeType, metadata)
                manager.queue.add(file)

            }

        }

    }

    private fun watchItem(){

        if(asset == null){
            VideoPlayerActivity.playVideoStream(catalogItem!!, requireContext())
        }
        else{
            VideoPlayerActivity.playVideoDownload(asset!!,requireContext())
        }

    }

    class AssetQueueObserver(activity: Activity?) : Observers.IQueueObserver {


        private var lastProgress : Int = -1
        private var  mActivity : Activity? = activity

        lateinit var parent: AssetDetailFragment

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
            updateItem(aAsset, true)
        }

        override fun engineUpdatedQueue() {
            // The base implementation does nothing.  See class documentation.
        }

        override fun engineEncounteredErrorParsingAsset(mAssetId: String) {}

        fun  update(fragment: AssetDetailFragment ){
           parent = fragment
        }
        private fun updateItem(aFile: IIdentifier, forceUpdate: Boolean) {

            val asset = aFile as IAsset
            val assetId = asset.assetId

            // Progress is for catalog item
            if (!TextUtils.isEmpty(assetId) && assetId == parent.catalogItem?.exampleAssetId) {
                //update our asset status
                mActivity?.runOnUiThread{ updateItemStatus(asset, forceUpdate) }

            }
        }

        private fun updateItemStatus(asset: IAsset?, forceUpdate: Boolean) {
            if (asset != null && asset.assetId == parent.catalogItem?.exampleAssetId) {

                //update our asset reference
                parent.asset = asset

                var progress = (parent.asset!!.fractionComplete * 100.0).toInt()
                // Not a repeated progress -- Keep context switches minimal due to frequency of messages, unless forced
                if (forceUpdate || progress != lastProgress) {
                    var assetStatus : String
                    val fds = parent.asset?.downloadStatus
                    val value: String
                    var checkRetryState = false
                    when (fds) {

                        Common.AssetStatus.DOWNLOADING -> {
                            assetStatus = parent.getString(R.string.status_downloading)
                            value = "downloading"
                        }

                        Common.AssetStatus.EARLY_DOWNLOADING -> {
                            assetStatus = parent.getString(R.string.status_downloading)
                            value = "downloading"
                        }

                        Common.AssetStatus.DOWNLOAD_COMPLETE -> {
                            assetStatus = parent.getString(R.string.status_downloaded)
                            value = "complete"
                        }

                        Common.AssetStatus.EXPIRED -> {
                            assetStatus = parent.getString(R.string.status_expired)
                            value = "expired"
                        }

                        Common.AssetStatus.DOWNLOAD_DENIED_ASSET -> {
                            assetStatus = "Queued"
                            value = "DENIED : MAD"
                            checkRetryState = true
                        }

                        Common.AssetStatus.DOWNLOAD_DENIED_ACCOUNT -> {
                            assetStatus = "Queued"
                            value = "DENIED : MDA"
                            checkRetryState = true
                        }

                        Common.AssetStatus.DOWNLOAD_DENIED_EXTERNAL_POLICY -> {
                            assetStatus = "Queued"
                            value = "DENIED : EXT"
                            checkRetryState = true
                        }

                        Common.AssetStatus.DOWNLOAD_DENIED_MAX_DEVICE_DOWNLOADS -> {
                            assetStatus = "Queued"
                            value = "DENIED :MPD"
                            checkRetryState = true
                        }

                        Common.AssetStatus.DOWNLOAD_BLOCKED_AWAITING_PERMISSION -> {
                            assetStatus = "Queued"
                            value = "AWAITING PERMISSION"
                            checkRetryState = true
                        }

                        Common.AssetStatus.DOWNLOAD_DENIED_COPIES -> {
                            assetStatus = "Queued"
                            value = "DENIED : COPIES"
                            checkRetryState = true
                        }

                        else -> {
                            assetStatus = parent.getString(R.string.status_pending)
                            value = "pending"
                            checkRetryState = true
                        }
                    }
                    val tv = parent.binding.txtAssetstatus
                    tv.visibility = View.VISIBLE
                    tv.text = String.format(parent.getString(R.string.asset_status), assetStatus, asset.errorCount, value)

                    val retryTv = parent.binding.txtRetrystatus
                    var showRetryState = false
                    if (checkRetryState) {
                        if (parent.asset?.maximumRetriesExceeded() == true) {
                            retryTv.setText(R.string.retries_exceeded)
                            showRetryState = true
                        }
                    }
                    retryTv.visibility = if (showRetryState) View.VISIBLE else View.GONE

                    lastProgress = progress
                    // Tiny Progress
                    if (progress == 0) progress = 1

                    // Progress Bar
                    val pb = parent.binding.prg
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

    class AssetPermissionObserver (activity : FragmentActivity) : IQueue.IQueuedAssetPermissionObserver {

        private var mActivity : FragmentActivity = activity

        override fun onQueuedWithAssetPermission(aQueued: Boolean, aDownloadPermitted: Boolean, aAsset: IAsset?, aAssetPermissionError: Int) {
            var errorString: String
            val permResponse = aAsset?.lastPermissionResponse
            val assetPerm = if (permResponse?.permission == IAssetPermission.PermissionCode.PERMISSION_DENIED_EXTERNAL_POLICY)
                permResponse.friendlyName()
            else
                IAssetPermission.PermissionCode.friendlyName(aAssetPermissionError)
            val title: String
            if (!aQueued) {

                title = "Queue Permission Denied"
                errorString = "Not permitted to queue asset [$assetPerm]  response: $permResponse"
                if (aAssetPermissionError == IAssetPermission.PermissionCode.PERMISSON_REQUEST_FAILED) {
                    errorString = "Not permitted to queue asset [$assetPerm]  This could happen if the device is currently offline."


                }

            } else {
                title = "Queue Permission Granted"
                errorString = "Asset " + (if (aDownloadPermitted) "Granted" else "Denied") + " Download Permission [" + assetPerm + "]  response: " + permResponse

            }

            mActivity.runOnUiThread {
                val permDlgBuilder = AlertDialog.Builder(mActivity)
                permDlgBuilder.setTitle(title)
                permDlgBuilder.setMessage(errorString)
                permDlgBuilder.setCancelable(false)
                permDlgBuilder.setPositiveButton("OK"
                ) { dialog, _ -> dialog.cancel() }

                val alert11 = permDlgBuilder.create()
                alert11.show()
            }



        }

    }

    class AssetObserver(progress : ProgressDialog, activity: FragmentActivity) : ISegmentedAssetFromParserObserver  {

        private var mProgress : ProgressDialog
        private var mActivity : FragmentActivity
        init{
           mProgress = progress
            mActivity = activity
        }

        override fun complete(aSegmentedAsset: ISegmentedAsset?, aError: Int, addedToQueue: Boolean) {
            try {
                mProgress.dismiss()
            }
            catch (e : Exception){}


            if (aSegmentedAsset == null) {
                val builder1 = AlertDialog.Builder(mActivity)
                builder1.setTitle("Could Not Create Asset")
                builder1.setMessage("Encountered error(" + Integer.toString(aError) + ") while creating asset.  This could happen if the device is currently offline, or if the asset manifest was not accessible.  Please try again later.")
                builder1.setCancelable(false)
                builder1.setPositiveButton("OK"
                ) { dialog, _ -> dialog.cancel() }

                val alert11 = builder1.create()
                alert11.show()
            }
            Log.i("ASSET_DETAIL", "Finished procesing hls file addedToQueue:$addedToQueue error:$aError")



        }
    }
}