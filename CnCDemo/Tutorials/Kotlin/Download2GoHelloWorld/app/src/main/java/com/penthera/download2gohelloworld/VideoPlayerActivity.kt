package com.penthera.download2gohelloworld

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Pair
import android.view.KeyEvent
import android.widget.Toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.*
import com.google.android.exoplayer2.ui.*
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Util
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.IAsset
import com.penthera.virtuososdk.client.ISegmentedAsset
import com.penthera.virtuososdk.utility.CommonUtil.Identifier.FILE_IDENTIFIER
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import kotlin.math.max

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
class VideoPlayerActivity : Activity(),  PlaybackPreparer {

    private var playerView: PlayerView? = null

    private var mediaDataSourceFactory: DataSource.Factory? = null
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

        shouldAutoPlay = true
        clearResumePosition()

        mediaDataSourceFactory = buildDataSourceFactory()
        if (CookieHandler.getDefault() !== DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER)
        }

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

    // PlaybackControlView.PlaybackPreparer implementation
    override fun preparePlayback() {
        initializePlayer()
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
        val type = if (asset is ISegmentedAsset) {
            asset.segmentedFileType()
        } else {
            FILE_IDENTIFIER
        }


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
            lastSeenTrackGroupArray = null


            val renderersFactory = DefaultRenderersFactory(this)
            renderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)

            player = SimpleExoPlayer.Builder(this,renderersFactory)
                .setTrackSelector(trackSelector!!)
                .setLoadControl(DefaultLoadControl())
                .build()
                .apply {
                    addListener(PlayerEventListener())
                    playWhenReady = shouldAutoPlay
                    addAnalyticsListener(EventLogger(trackSelector))
                }

            playerView?.let {
                it.player = player
                it.setPlaybackPreparer(this)
            }


            val mediaSource = buildMediaSource(uri!!, type)
            val haveResumePosition = resumeWindow != C.INDEX_UNSET
            if (haveResumePosition) {
                player!!.seekTo(resumeWindow, resumePosition)
            }
            player!!.prepare(mediaSource, !haveResumePosition, false)
            inErrorState = false
        }
    }

    private fun buildMediaSource(uri: Uri, type: Int): MediaSource {

        val ret : MediaSource
        when (type) {
            ISegmentedAsset.SEG_FILE_TYPE_HSS -> ret =  SsMediaSource.Factory(
                DefaultSsChunkSource.Factory(mediaDataSourceFactory!!),
                buildDataSourceFactory())
                .createMediaSource(uri)
            ISegmentedAsset.SEG_FILE_TYPE_MPD -> ret =  DashMediaSource.Factory(
                DefaultDashChunkSource.Factory(mediaDataSourceFactory!!),
                buildDataSourceFactory())
                .createMediaSource(uri)
            ISegmentedAsset.SEG_FILE_TYPE_HLS -> ret = HlsMediaSource.Factory(mediaDataSourceFactory!!)
                .createMediaSource(uri)
            Common.AssetIdentifierType.FILE_IDENTIFIER -> ret = ProgressiveMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri)
            else -> {
                throw IllegalStateException("Unsupported type: $type")
            }
        }
        return ret
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

    /**
     * Returns a new DataSource factory.
     *
     * @return A new DataSource factory.
     */
    private fun buildDataSourceFactory(): DataSource.Factory =
        DefaultDataSourceFactory(applicationContext, DefaultHttpDataSourceFactory("download2gohelloworld"))



    private inner class PlayerEventListener : Player.EventListener {

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
                        errorString = getString(R.string.error_instantiating_decoder,
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
        private const val ACTION_VIEW = "com.penthera.download2gohelloworld.exoplayer.action.VIEW"

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

            val path: Uri = Uri.parse(asset.playlist.toString())

            context.startActivity(Intent(context, VideoPlayerActivity::class.java).apply {
                action = ACTION_VIEW
                data = path
                putExtra(VIRTUOSO_ASSET, asset)
            })
        }
    }
}
