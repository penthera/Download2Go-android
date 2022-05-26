package com.penthera.download2go1_6

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.penthera.download2go1_6.databinding.DetailActivityBinding
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

    private lateinit var binding: DetailActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DetailActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnPlay.setOnClickListener { playAsset() }
        binding.btnDelete.setOnClickListener {
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
            binding.assetTitleValue.text = it.metadata
            binding.uuidValue.text = it.uuid
            binding.idValue.text = it.assetId
            binding.fileTypeValue.text = fileTypeFromId(it.segmentedFileType())
            binding.expectedSizeValue.text = String.format("%.2f MB", it.expectedSize / 1048576.00)
            binding.currentSizeValue.text = String.format("%.2f MB", it.currentSize / 1048576.00)
            binding.durationValue.text = String.format("%d seconds", it.duration)
            binding.statusValue.text = MainActivity.getStatusText(this, it.downloadStatus)
            binding.pathValue.text = it.localBaseDir
            try {
                val url = it.playbackURL
                if (url != null) {
                    binding.playbackValue.text = url.toString()
                } else {
                    binding.playbackValue!!.setText(R.string.unavailable)
                }
            } catch (mue: MalformedURLException) {
                binding.playbackValue!!.setText(R.string.unavailable)
            }
            binding.segmentCountValue.text = it.totalSegments.toString()
            val firstPlayTime = it.firstPlayTime
            if (firstPlayTime > 0) {
                binding.firstPlayValue.text = dateFormatter.format(Date(firstPlayTime * 1000))
            } else {
                binding.firstPlayValue.setText(R.string.not_yet_played)
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