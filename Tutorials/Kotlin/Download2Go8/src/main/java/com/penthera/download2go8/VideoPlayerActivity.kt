package com.penthera.download2go8


import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Pair
import android.view.KeyEvent
import android.widget.Toast
import androidx.media3.common.*
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.ui.PlayerView
import com.penthera.virtuososdk.client.IAsset
import com.penthera.virtuososdk.client.Virtuoso
import com.penthera.virtuososdk.support.androidx.media311.ExoplayerUtils
import com.penthera.virtuososdk.support.androidx.media311.drm.ExoplayerDrmSessionManager
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.MalformedURLException
import kotlin.math.max

/**
 * An activity that plays media using {@link Player}.
 */
class VideoPlayerActivity : Activity() {

    // IMPORTANT - Best practice is to ensure we have a Virtuoso instance available while playing segmented assets
    // as this will guarantee the proxy service remains available throughout. We can do this in the activity or store
    // a singleton for the whole application. But this should not be instantiated in an application onCreate().
    private lateinit var mVirtuoso: Virtuoso
    private var playerView: PlayerView? = null

    private var player: Player? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var trackSelectorParameters: DefaultTrackSelector.Parameters? = null
    private var inErrorState: Boolean = false
    private var lastSeenTracksInfo: Tracks? = null


    private var shouldAutoPlay: Boolean = false
    private var resumeWindow: Int = 0
    private var resumePosition: Long = 0

    // Activity lifecycle

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mVirtuoso = Virtuoso(applicationContext)

        shouldAutoPlay = true
        clearResumePosition()

        setContentView(R.layout.player_activity)

        playerView = findViewById<PlayerView>(R.id.player_view).apply {
            setErrorMessageProvider(PlayerErrorMessageProvider())
            requestFocus()
        }

        trackSelectorParameters = DefaultTrackSelector.ParametersBuilder(this).build()
    }

    public override fun onNewIntent(intent: Intent) {
        releasePlayer()
        shouldAutoPlay = true
        clearResumePosition()
        setIntent(intent)
    }

    public override fun onStart() {
        super.onStart()

        initializePlayer()
    }

    public override fun onResume() {
        super.onResume()
		mVirtuoso.onResume()
        if (player == null) {
            initializePlayer()
        }
    }

    public override fun onPause() {
        super.onPause()

    }

    public override fun onStop() {
        super.onStop()
        releasePlayer()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) =
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializePlayer()
        } else {
            showToast(R.string.storage_permission_denied)
            finish()
        }

    // Activity input

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || playerView!!.dispatchMediaKeyEvent(event)
    }

    // Internal methods
    private fun initializePlayer() {
        val intent = intent

        val action = intent.action

        if (ACTION_VIEW != action) {
            showToast(getString(R.string.unexpected_intent_action, action))
            return
        }

        val uri = intent.data
        val asset: IAsset? = intent.getParcelableExtra(VIRTUOSO_ASSET)


        // All our files are stored in the app private space so no need to check permissions after kitkat
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            if (Util.maybeRequestReadExternalStoragePermission(this, uri!!)) {
                // The player will be reinitialized if the permission is granted.
                return
            }
        }


        if (player == null) {

            val adaptiveTrackSelectionFactory = AdaptiveTrackSelection.Factory()
            trackSelector = DefaultTrackSelector(this, adaptiveTrackSelectionFactory)
            trackSelector?.parameters = trackSelectorParameters!!
            lastSeenTracksInfo = Tracks.EMPTY


            val renderersFactory = DefaultRenderersFactory(this)
            renderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            
            
            val builder = ExoplayerUtils.PlayerConfigOptions.Builder(this).apply{
                withTrackSelector(trackSelector)
                withPlayerListener(PlayerEventListener())
                withAnalyticsListener(EventLogger())
                userRenderersFactory(renderersFactory)
                if(resumeWindow != C.INDEX_UNSET) withSeekToPosition(resumeWindow, resumePosition)
                playWhenReady(shouldAutoPlay)
                
                mediaSourceOptions().apply {
                    useTransferListener(true)
                    withUserAgent("virtuoso-sdk")
                }

                drmOptions().apply {
                    withDrmSessionManagerEventListener(DrmListener(this@VideoPlayerActivity))
                }
                    
            }



            try {

                player = ExoplayerUtils.setupPlayer(
                    playerView!!,
                    mVirtuoso.assetManager,
                    asset!!,
                    false,
                    builder.build()
                )
                inErrorState = false
            }catch (e : MalformedURLException){
                inErrorState = true
            }
        }
    }

    private fun clearStartPosition() {
        shouldAutoPlay = true
        resumeWindow = C.INDEX_UNSET
        resumePosition = C.TIME_UNSET
    }

    fun handleDrmLicenseNotAvailable() {
        clearStartPosition()
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

    private fun releasePlayer() {

        player?.let{
            shouldAutoPlay = it.playWhenReady
            updateResumePosition()
            it.release()
            player = null
            trackSelector = null
            trackSelectorParameters = null
        }
    }

    private fun updateResumePosition() {
        resumeWindow = player!!.currentWindowIndex
        resumePosition = max(0, player!!.contentPosition)
    }

    private fun clearResumePosition() {
        resumeWindow = C.INDEX_UNSET
        resumePosition = C.TIME_UNSET
    }


    private inner class PlayerEventListener : Player.Listener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {}

        override fun onPlayerError(e: PlaybackException) {

            if (e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                clearResumePosition()
                initializePlayer()
            } else {
                updateResumePosition()
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            if (tracks === lastSeenTracksInfo) {
                return
            }

            if (tracks.containsType(C.TRACK_TYPE_VIDEO)
                && !tracks.isTypeSupported(C.TRACK_TYPE_VIDEO, true)
            ) {
                showToast(R.string.error_unsupported_video)
            }
            if (tracks.containsType(C.TRACK_TYPE_AUDIO)
                && !tracks.isTypeSupported(C.TRACK_TYPE_AUDIO, true)
            ) {
                showToast(R.string.error_unsupported_audio)
            }

            lastSeenTracksInfo = tracks
        }
    }


    private fun showToast(messageId: Int) {
        showToast(getString(messageId))
    }

    private fun showToast(message: String?) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }


    // This inner class is taken directly from the Exoplayer demo. It provides human readable error messages for exoplayer errors.
    private inner class PlayerErrorMessageProvider :
        ErrorMessageProvider<PlaybackException> {
        override fun getErrorMessage(e: PlaybackException): Pair<Int, String> {
            var errorString = getString(R.string.error_generic)
            val cause = e.cause
            if (cause is MediaCodecRenderer.DecoderInitializationException) {
                // Special case for decoder initialization failures.
                errorString = if (cause.codecInfo == null) {
                    when {
                        cause.cause is MediaCodecUtil.DecoderQueryException -> {
                            getString(R.string.error_querying_decoders)
                        }
                        cause.secureDecoderRequired -> {
                            getString(
                                R.string.error_no_secure_decoder,
                                cause.mimeType
                            )
                        }
                        else -> {
                            getString(
                                R.string.error_no_decoder,
                                cause.mimeType
                            )
                        }
                    }
                } else {
                    getString(
                        R.string.error_instantiating_decoder,
                        cause.codecInfo!!.name
                    )
                }
            }
            return Pair.create(0, errorString)
        }
    }

    // Observer class from the Download2Go session manager which enables the client to be informed of
    // events for when keys are loaded or an error occurs with fetching a license.
    private class DrmListener(private val mActivity: VideoPlayerActivity) :
        ExoplayerDrmSessionManager.EventListener {
        override fun onDrmKeysLoaded() {
        }

        override fun onDrmSessionManagerError(e: Exception) {
            mActivity.handleDrmLicenseNotAvailable()
        }

    }

    companion object {
        private const val VIRTUOSO_ASSET = "asset"
        private const val ACTION_VIEW = "com.penthera.download2go8.exoplayer.action.VIEW"

        private val DEFAULT_COOKIE_MANAGER: CookieManager = CookieManager()

        init {
            DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
        }

        fun playVideoDownload(asset: IAsset, context: Context) {

            if (asset.playbackURL != null) {
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
