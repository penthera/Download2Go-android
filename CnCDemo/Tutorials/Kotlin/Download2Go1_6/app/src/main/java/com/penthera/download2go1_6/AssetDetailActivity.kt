package com.penthera.download2go1_6

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.penthera.virtuososdk.client.IIdentifier
import com.penthera.virtuososdk.client.ISegmentedAsset
import com.penthera.virtuososdk.client.Observers.IQueueObserver
import com.penthera.virtuososdk.client.Virtuoso
import java.net.MalformedURLException
import java.text.SimpleDateFormat
import java.util.*

class AssetDetailActivity : AppCompatActivity() {
    private lateinit var virtuoso: Virtuoso

    private var assetQueueObserver: AssetQueueObserver? = null
    var asset: ISegmentedAsset? = null
    var assetId: String? = null
    private var titleView: TextView? = null
    private var uuidView: TextView? = null
    private var idView: TextView? = null
    private var fileTypeView: TextView? = null
    private var expectedSizeView: TextView? = null
    private var currentSizeView: TextView? = null
    private var durationView: TextView? = null
    private var statusView: TextView? = null
    private var pathView: TextView? = null
    private var playlistView: TextView? = null
    private var segmentCountView: TextView? = null
    private var firstPlayView: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detail_activity)
        titleView = findViewById(R.id.assetTitleValue)
        uuidView = findViewById(R.id.uuidValue)
        idView = findViewById(R.id.idValue)
        fileTypeView = findViewById(R.id.fileTypeValue)
        expectedSizeView = findViewById(R.id.expectedSizeValue)
        currentSizeView = findViewById(R.id.currentSizeValue)
        durationView = findViewById(R.id.durationValue)
        statusView = findViewById(R.id.statusValue)
        pathView = findViewById(R.id.pathValue)
        playlistView = findViewById(R.id.playlistValue)
        segmentCountView = findViewById(R.id.segmentCountValue)
        firstPlayView = findViewById(R.id.firstPlayValue)
        findViewById<View>(R.id.btn_play).setOnClickListener { playAsset() }
        findViewById<View>(R.id.btn_delete).setOnClickListener {
            if (asset != null) {
                virtuoso.assetManager.delete(asset)
            }
            finish()
        }

        // Initialise the SDK
        initVirtuosoSDK()
        if (savedInstanceState != null) {
            assetId = savedInstanceState.getString(ASSET_ID_KEY)
        }
    }

    public override fun onResume() {
        super.onResume()

        // Resume the Download2Go SDK on activity resume
        virtuoso.onResume()
        virtuoso.addObserver(assetQueueObserver)

        virtuoso.assetManager?.let{
            assetId?.let{ asset_id ->
                val assets = it.getByAssetId(asset_id)
                assets?.let{list ->
                    if(list.isNotEmpty()){
                        asset = list[0] as ISegmentedAsset
                    }
                }
            }
        }

        updateUI()
    }

    public override fun onPause() {
        super.onPause()

        // Pause the Download2Go SDK on activity pause
        virtuoso.onPause()
        virtuoso.removeObserver(assetQueueObserver)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ASSET_ID_KEY, assetId)
    }

    private fun initVirtuosoSDK() {
        // An alternative here is to create a singleton instance somewhere like the Application class
        // and share it between Activities.
        virtuoso = Virtuoso(this)
        if (assetId == null) {
            assetId = intent.getStringExtra(ASSET_ID_KEY)
        }
        val assets = virtuoso.assetManager?.getByAssetId(assetId)
        assets?.let{
            if (it.isNotEmpty()){
                asset = it[0] as ISegmentedAsset
            }
        }

        // set up observer
        assetQueueObserver = AssetQueueObserver(this)
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        
        asset?.let{
            titleView!!.text = it.metadata
            uuidView!!.text = it.uuid
            idView!!.text = it.assetId
            fileTypeView!!.text = fileTypeFromId(it.segmentedFileType())
            expectedSizeView!!.text = String.format("%.2f MB", it.expectedSize / 1048576.00)
            currentSizeView!!.text = String.format("%.2f MB", it.currentSize / 1048576.00)
            durationView!!.text = String.format("%d seconds", it.duration)
            statusView?.text = MainActivity.getStatusText(this, it.downloadStatus)
            pathView!!.text = it.localBaseDir
            try {
                val url = it.playlist
                if (url != null) {
                    playlistView!!.text = url.toString()
                } else {
                    playlistView!!.setText(R.string.unavailable)
                }
            } catch (mue: MalformedURLException) {
                playlistView!!.setText(R.string.unavailable)
            }
            segmentCountView!!.text = it.totalSegments.toString()
            val firstPlayTime = it.firstPlayTime
            if (firstPlayTime > 0) {
                firstPlayView!!.text = dateFormatter.format(Date(firstPlayTime * 1000))
            } else {
                firstPlayView!!.setText(R.string.not_yet_played)
            }
        }
    }

    private fun fileTypeFromId(fileTypeCode: Int): String {
        var fileTypeText = getString(R.string.unknown_type)
        when (fileTypeCode) {
            ISegmentedAsset.SEG_FILE_TYPE_HLS -> fileTypeText = getString(R.string.hls_type)
            ISegmentedAsset.SEG_FILE_TYPE_MPD -> fileTypeText = getString(R.string.mpd_type)
        }
        return fileTypeText
    }

    private fun playAsset() {
        asset?.let{
            VideoPlayerActivity.playVideoDownload(it,this )
        }
    }

    /**
     * This class observes the SDK download queue and provides updates during the download process.
     */
    internal class AssetQueueObserver(private val activity: AssetDetailActivity) :
        IQueueObserver {
        override fun engineStartedDownloadingAsset(asset: IIdentifier) {
            updateItem(asset)
        }

        override fun enginePerformedProgressUpdateDuringDownload(asset: IIdentifier) {
            updateItem(asset)
        }

        override fun engineCompletedDownloadingAsset(asset: IIdentifier) {
            updateItem(asset)
        }

        override fun engineEncounteredErrorDownloadingAsset(asset: IIdentifier) {}

        override fun engineUpdatedQueue() {}

        override fun engineEncounteredErrorParsingAsset(s: String) {}

        private fun updateItem(identifier: IIdentifier?) {
            identifier?.let{
                if(it is ISegmentedAsset){
                    if(it.assetId.isNotEmpty() && it.assetId == activity.assetId){
                        activity.runOnUiThread {
                            activity.asset = it
                            activity.updateUI()
                        }
                    }
                }
            }
        }

    }

    companion object {
        const val ASSET_ID_KEY = "asset_id"
        @SuppressLint("SimpleDateFormat")
        private val dateFormatter = SimpleDateFormat("MM/dd/yyyy HH:mm:ss a")
    }
}