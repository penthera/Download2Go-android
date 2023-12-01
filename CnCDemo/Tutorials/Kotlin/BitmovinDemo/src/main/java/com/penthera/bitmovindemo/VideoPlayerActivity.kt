package com.penthera.bitmovindemo

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.bitmovin.player.PlayerView
import com.bitmovin.player.api.Player
import com.penthera.virtuososdk.client.*
import com.penthera.virtuososdk.client.bitmovin.BitmovinSourceManager

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
class VideoPlayerActivity : Activity() {

    private var bitmovinPlayer: Player? = null

    private var virtuoso : Virtuoso? = null

    private var sourceManager: BitmovinSourceManager? = null

    private var asset: ISegmentedAsset? = null

    private lateinit var playerView: PlayerView
    // Activity lifecycle

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.player_activity)

        this.virtuoso = Virtuoso(this)

        playerView =  findViewById(R.id.playerView)
        this.bitmovinPlayer = playerView.player

        bitmovinPlayer?.config?.playbackConfig?.isAutoplayEnabled = true

        this.initializePlayer()
    }

    public override fun onNewIntent(intent: Intent) {
        setIntent(intent)
    }

    public override fun onStart() {
        super.onStart()
        playerView.onStart()
    }

    public override fun onResume() {
        super.onResume()

        sourceManager?.startMonitoring()
        virtuoso?.addObserver(object : EngineObserver() {
            override fun proxyPortUpdated() {
                /*
                 * The proxy update occurs if the proxy needs to change port after a restart,
                 * which can occur if the app is placed in the background and then brought back to the foreground.
                 * In this case the player needs to be set back up to get the new base url.
                 */
                runOnUiThread {
                    Log.w(VideoPlayerActivity::class.java.simpleName, "Received warning about change in port, restarting player")
                    playerView.player?.unload()
                    initializePlayer()
                }
            }

            override fun assetLicenseRetrieved(iIdentifier: IIdentifier, success: Boolean) {
                if (iIdentifier.id == asset?.id) {
                    // A new license request was made for our current asset.
                    if (success) {
                        // Retry playback
                        val checkAndRun = Thread { initializePlayer() }
                        checkAndRun.start()
                    } else {
                        // License retrieval failed. Tell the user.
                        runOnUiThread { Toast.makeText(this@VideoPlayerActivity, "Cannot play, License unavailable", Toast.LENGTH_LONG).show() }
                    }
                }
            }
        })

        playerView.onResume()
    }

    public override fun onPause() {
        playerView.onPause()
        super.onPause()
    }

    public override fun onStop() {
        playerView.onStop()
        super.onStop()
    }

    public override fun onDestroy() {
        playerView.onDestroy()
        sourceManager?.cleanup()
        super.onDestroy()
    }

    private fun initializePlayer() {
        //resolve the asset
        asset = intent.getParcelableExtra(VIRTUOSO_ASSET)

        asset.let {
            sourceManager = BitmovinSourceManager(this, it)
            val sourceConfig = sourceManager?.bitmovinSourceItem

            if (sourceConfig != null) {
                this.bitmovinPlayer?.load(sourceConfig)
            } else {
                if (sourceManager?.hasValidDRM() == true) {
                    // DRM valid but could not create source item
                    Log.e(VideoPlayerActivity.javaClass.simpleName, "Could not start video despite drm reporting valid")
                    showToast("Could not start video. Reason unknown.")
                } else {
                    handleDrmLicenseNotAvailable()
                }
            }

        }
    }

    fun handleDrmLicenseNotAvailable() {

        runOnUiThread {

            AlertDialog.Builder(this@VideoPlayerActivity).apply{
                title = "License unavailable"
                setMessage("License for offline playback expired and renew is unavailable.")
                setNeutralButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
            }.create().show()
        }
    }




    private fun showToast(messageId: Int) {
        showToast(getString(messageId))
    }

    private fun showToast(message: String?) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }


    companion object {
        private const val VIRTUOSO_ASSET = "asset"
        private const val ACTION_VIEW = "com.penthera.bitmovindemo.action.VIEW"

        fun playVideoDownload(asset: IAsset, context: Context){

            if(asset.playbackURL != null) {
                val path: Uri = Uri.parse(asset.playbackURL.toString())

                context.startActivity(Intent(context, VideoPlayerActivity::class.java).apply {
                    action = ACTION_VIEW
                    data = path
                    putExtra(VIRTUOSO_ASSET, asset)
                })
            }
        }
    }
}
