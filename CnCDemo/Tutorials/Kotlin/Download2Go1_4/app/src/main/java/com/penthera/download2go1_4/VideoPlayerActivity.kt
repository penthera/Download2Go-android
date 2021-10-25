package com.penthera.download2go1_4

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Pair
import android.view.KeyEvent
import android.widget.Toast
import com.google.android.exoplayer2.*

import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.exoplayer2.util.EventLogger
import com.penthera.virtuososdk.client.IAsset
import com.penthera.virtuososdk.client.Virtuoso
import com.penthera.virtuososdk.support.exoplayer214.ExoplayerUtils

import java.net.CookieManager
import java.net.CookiePolicy
import java.net.MalformedURLException
import kotlin.math.max

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
class VideoPlayerActivity : Activity() {

    // IMPORTANT - Best practice is to ensure we have a Virtuoso instance available while playing segmented assets
    // as this will guarantee the proxy service remains available throughout. We can do this in the activity or store
    // a singleton for the whole application. But this should not be instantiated in an application onCreate().
    private lateinit var mVirtuoso: Virtuoso
    private var playerView: PlayerView? = null

    private var player: SimpleExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var trackSelectorParameters: DefaultTrackSelector.Parameters? = null
    private var inErrorState: Boolean = false
    private var lastSeenTrackGroupArray: TrackGroupArray? = null

    private var shouldAutoPlay: Boolean = false
    private var resumeWindow: Int = 0
    private var resumePosition: Long = 0

    // Activity lifecycle

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mVirtuoso = Virtuoso(this)
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

        val asset: IAsset? = intent.getParcelableExtra(VIRTUOSO_ASSET)

        if (player == null) {

            val adaptiveTrackSelectionFactory = AdaptiveTrackSelection.Factory()
            trackSelector = DefaultTrackSelector(this, adaptiveTrackSelectionFactory)
            trackSelector?.parameters = trackSelectorParameters!!
            lastSeenTrackGroupArray = null


            val renderersFactory = DefaultRenderersFactory(this)
            renderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)


            val builder = ExoplayerUtils.PlayerConfigOptions.Builder(this).apply {
                userRenderersFactory(renderersFactory)
                withTrackSelector(trackSelector)
                withAnalyticsListener(EventLogger(trackSelector))
                withPlayerEventListener(PlayerEventListener())
                if(resumeWindow != C.INDEX_UNSET)
                    withSeekToPosition(resumeWindow,resumePosition)
            }


            try {

                ExoplayerUtils.setupPlayer(
                        playerView!!,
                        mVirtuoso,
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

        override fun onPlayerError(e: ExoPlaybackException) {

            if (isBehindLiveWindow(e)) {
                clearResumePosition()
                initializePlayer()
            } else {
                updateResumePosition()
            }
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
            if (trackGroups !== lastSeenTrackGroupArray) {
                val mappedTrackInfo = trackSelector!!.currentMappedTrackInfo
                if (mappedTrackInfo != null) {
                    if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO) == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        showToast(R.string.error_unsupported_video)
                    }
                    if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_AUDIO) == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        showToast(R.string.error_unsupported_audio)
                    }
                }
                lastSeenTrackGroupArray = trackGroups
            }
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
        ErrorMessageProvider<ExoPlaybackException> {
        override fun getErrorMessage(e: ExoPlaybackException): Pair<Int, String> {
            var errorString: String = getString(R.string.error_generic)
            if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                val cause = e.rendererException
                if (cause is MediaCodecRenderer.DecoderInitializationException) {
                    // Special case for decoder initialization failures.
                    if (cause.codecInfo != null) {
                        errorString = getString(
                            R.string.error_instantiating_decoder,
                            cause.codecInfo!!.name
                        )
                    } else {
                        errorString = when {
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
                    }
                }
            }
            return Pair.create(0, errorString)
        }
    }

    companion object {
        private const val VIRTUOSO_ASSET = "asset"
        private const val ACTION_VIEW = "com.penthera.download2go1_4.exoplayer.action.VIEW"

        private val DEFAULT_COOKIE_MANAGER: CookieManager = CookieManager()

        init {
            DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
        }

        private fun isBehindLiveWindow(e: ExoPlaybackException): Boolean {
            if (e.type != ExoPlaybackException.TYPE_SOURCE) {
                return false
            }
            var cause: Throwable? = e.sourceException
            while (cause != null) {
                if (cause is BehindLiveWindowException) {
                    return true
                }
                cause = cause.cause
            }
            return false
        }

        fun playVideoDownload(asset: IAsset , context: Context){

            val path: Uri = Uri.parse(asset.playbackURL.toString())

            context.startActivity(Intent(context, VideoPlayerActivity::class.java).apply {
                action = ACTION_VIEW
                data = path
                putExtra(VIRTUOSO_ASSET, asset)
            })
        }
    }
}
