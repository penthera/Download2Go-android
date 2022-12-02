package com.penthera.playassurehelloworld

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Pair
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.penthera.playassure.*
import kotlinx.coroutines.*
import com.penthera.virtuososdk.support.exoplayer217.ExoplayerUtils

import java.net.CookieManager
import java.net.CookiePolicy
import kotlin.math.max

/**
 * An activity that plays media using {@link Player}, either with or without playassure.
 */
class VideoPlayerActivity : AppCompatActivity() , PlayAssureStatus {

    private var playerView: StyledPlayerView? = null

    private var player: Player? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var trackSelectorParameters: DefaultTrackSelector.Parameters? = null
    private var inErrorState: Boolean = false
    private var lastSeenTracksInfo: TracksInfo? = null

    private var shouldAutoPlay: Boolean = false
    private var resumeWindow: Int = 0
    private var resumePosition: Long = 0

    private var errorReported : Boolean = false

    private var playAssureManager: PlayAssureManager? = null

    // Activity lifecycle

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        shouldAutoPlay = true
        clearResumePosition()

        setContentView(R.layout.player_activity)

        playerView = findViewById<StyledPlayerView>(R.id.player_view).apply {
            setErrorMessageProvider(PlayerErrorMessageProvider())
            requestFocus()
        }

        trackSelectorParameters = DefaultTrackSelector.ParametersBuilder(this).build()
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        releasePlayer()
        shouldAutoPlay = true
        clearResumePosition()
        setIntent(intent)
    }

    public override fun onStart() {
        super.onStart()

        if (intent.action == ACTION_VIEW) {
            // Play using play assure. Setup the SDK to get the local URL prior to initializing the player

            // Use the lifecycle scope to run launch the SDK
            lifecycleScope.launchWhenStarted {
                if (playAssureManager == null) {  // only create once
                    withContext(Dispatchers.Default) {
                        // This is the config object for playassure, an observer must be provided. All other
                        // settings are being left as defaults in this example
                        val config: PlayAssureConfig =
                            PlayAssureConfig.Builder(this@VideoPlayerActivity)
                                                        .playAssureObserver(this@VideoPlayerActivity)
                                                        .build()

                        intent.data?.let {

                            // Result of construction is reported in observer
                            playAssureManager = PlayAssureManager(this@VideoPlayerActivity, it.toString(), config)

                        }
                    }
                } else {
                    initializePlayer()
                }
            }


        } else {
            // For streaming, simply initialize the player immediately
            initializePlayer()
        }
    }

    public override fun onResume() {
        super.onResume()
        if (playerView == null) {
            initializePlayer()
        }
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onStop() {
        super.onStop()
        playerView?.onPause()
        releasePlayer()
    }

    public override fun onDestroy() {
        super.onDestroy()
        shutdown()  // shuts down the SDK
    }

    // Activity input

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || (playerView?.dispatchMediaKeyEvent(event) == true)
    }


    // Internal methods
    private fun initializePlayer() {

        val action = intent.action

        if (player == null) {

            val adaptiveTrackSelectionFactory = AdaptiveTrackSelection.Factory()
            trackSelector = DefaultTrackSelector(this, adaptiveTrackSelectionFactory)
            trackSelector?.parameters = trackSelectorParameters!!
            lastSeenTracksInfo = TracksInfo.EMPTY

            val renderersFactory = DefaultRenderersFactory(this)
            renderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)

            player = SimpleExoPlayer.Builder(this,renderersFactory).apply {
                trackSelector?.let{
                    setTrackSelector(it)
                }
            }.build()

            player?.apply {

                playerView?.player = player
                addListener(PlayerEventListener())

                var playerUrl: String = intent.data.toString()
                var assetUrl: String? = null

                if (action == ACTION_VIEW) {

                    playAssureManager?.let {
                        // Initialize the player with the SDK streaming url, or if missing with
                        // the original source url.
                        setMediaItem(MediaItem.fromUri(it.streamingUrl ?: it.sourceURL))

                        playerUrl = it.streamingUrl ?: it.sourceURL
                        assetUrl = it.sourceURL
                    }
                } else {
                    setMediaItem(MediaItem.fromUri(playerUrl))
                }

                playWhenReady = true

                ExoplayerUtils.setupPlayerAnalytics(
                    this@VideoPlayerActivity, this,
                    playerUrl, assetUrl, action == ACTION_VIEW
                )

                prepare()
            }

            inErrorState = false
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

        override fun onTracksInfoChanged(tracksInfo: TracksInfo) {
            if (tracksInfo === lastSeenTracksInfo) {
                return
            }
            if (!tracksInfo.isTypeSupportedOrEmpty(C.TRACK_TYPE_VIDEO)) {
                showToast(R.string.error_unsupported_video)
            }
            if (!tracksInfo.isTypeSupportedOrEmpty(C.TRACK_TYPE_AUDIO)) {
                showToast(R.string.error_unsupported_audio)
            }
            lastSeenTracksInfo = tracksInfo
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
            if (cause is DecoderInitializationException) {
                // Special case for decoder initialization failures.
                errorString = if (cause.codecInfo == null) {
                    when {
                        cause.cause is DecoderQueryException -> {
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

    // Play Assure actions


    private fun shutdown() {
        if (intent.action != ACTION_VIEW_STREAM) {
            // We use global scope to ensure the coroutine containing the shutdown will always get to run to completion
            GlobalScope.launch(NonCancellable) {
                playAssureManager?.shutdown()
            }
        }
    }

    // Play Assure Observer

    override fun initializationComplete(result: InitializationResult) {
        when (result) {
            InitializationResult.SUCCESS -> { /* no action ?  */ }
            else -> {
                displayFailureDialog("Unable to initialize sdk: ${result.name}")
            }
        }
    }

    // This reports a change to the overall status of the SDK.
    override fun reportEngineStatusChanged(newStatus: PlayAssureEngineState) {
        when (newStatus) {
            PlayAssureEngineState.Started -> initializePlayer() // initialize player once SDK started
            PlayAssureEngineState.Unregistered -> {
                // Try to register the SDK when the observer reports unregistered
                    playAssureManager?.startupSDK(
                        intent.getStringExtra(BACKPLANE_URL) ?: "",
                        intent.getStringExtra(PUBLIC_KEY) ?: "",
                        intent.getStringExtra(PRIVATE_KEY)?: "",
                        intent.getStringExtra(USER_ID) ?: "",
                        null
                    )
            }
            PlayAssureEngineState.Errored -> {
                runOnUiThread {
                    displayFailureDialog("Error encountered during initialization.\n\nExiting player.")
                }
            }
            else -> {
                // No action required on shutdown
            }
        }
    }

    override fun bitrateChangeReported(statusInfo: PlayAssureStatusInfo) {

    }

    override fun networkReachabilityChanged(reachable: Boolean) {

    }

    override fun progressReported(statusInfo: PlayAssureStatusInfo) {

    }

    override fun reportNetworkFail() {
        runOnUiThread {
            displayFailureDialog("Network bandwidth does not support playback.\n\nExiting player.")
        }
    }

    override fun reportParsingFail(errorCode: PlayAssureError, error: String) {
        // This indicates that playassure playback could not commence due to an issue parsing the
        // asset manifests. In a production application you would fall back to streaming playback at this point.
        runOnUiThread {
            displayFailureDialog("Error encountered parsing manifest: $error, code ($errorCode).\n\nExiting player.")
        }
    }

    override fun reportParsingSuccess() {
        // This indicates that the SDK has parsed the asset manifests and is beginning to
        // prepare segments for playback.
        runOnUiThread {
            showToast("Manifests successfully parsed.")
        }
    }

    override fun reportPlaybackFail(errorCode: PlayAssureError, error: String) {
        runOnUiThread {
            displayFailureDialog("Error encountered during playback: $error, code ($errorCode).\n\nExiting player.")
        }
    }

    override fun reportPlaybackThrottled(throttled: Boolean) {
        // Demonstrates how to observe if the SDK needs to throttle the player
        // bandwidth in order to ensure smooth playback
        runOnUiThread {
            if (throttled) {
                showToast("Playback throttled")
            } else {
                showToast("Playback throttling finished")
            }
        }
    }

    private fun displayFailureDialog(text : String) {
        if (!errorReported) {
            errorReported = true
            with(AlertDialog.Builder(this))
            {
                setTitle("Playback Failure")
                setMessage(text)
                setPositiveButton("OK") { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    shutdown()
                    finish()
                }

                show()
            }
        }
    }

    companion object {
        private const val ACTION_VIEW = "com.penthera.playassure.exoplayer.action.VIEW"
        private const val ACTION_VIEW_STREAM = "com.penthera.playassure.player.action.STREAM"

        private const val BACKPLANE_URL = "backplane_url"
        private const val PUBLIC_KEY = "public_key"
        private const val PRIVATE_KEY = "private_key"
        private const val USER_ID = "user_id"

        private val DEFAULT_COOKIE_MANAGER: CookieManager = CookieManager()

        init {
            DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
        }

        fun playAssure( url : String,
                        context: Context,
                        backplaneUrl: String,
                        publicKey: String,
                        privateKey: String,
                        userId: String){

            val path: Uri = Uri.parse(url)

            context.startActivity(Intent(context, VideoPlayerActivity::class.java).apply {
                action = ACTION_VIEW
                data = path
                putExtra(BACKPLANE_URL, backplaneUrl)
                putExtra(PUBLIC_KEY, publicKey)
                putExtra(PRIVATE_KEY, privateKey)
                putExtra(USER_ID, userId)
            })
        }

        fun playStream(url: String, context: Context) {
            val path: Uri = Uri.parse(url)

            context.startActivity(Intent(context, VideoPlayerActivity::class.java).apply {
                action = ACTION_VIEW_STREAM
                data = path
            })
        }
    }
}