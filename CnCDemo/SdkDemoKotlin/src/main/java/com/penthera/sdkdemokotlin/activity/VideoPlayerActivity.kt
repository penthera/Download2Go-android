package com.penthera.sdkdemokotlin.activity


import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.*
import android.media.MediaDrm
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Pair
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.ParametersBuilder
import com.google.android.exoplayer2.ui.DebugTextViewHelper
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Util
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.catalog.CatalogItemType
import com.penthera.sdkdemokotlin.catalog.ExampleCatalogItem
import com.penthera.sdkdemokotlin.dialog.TrackSelectionDialog
import com.penthera.sdkdemokotlin.drm.DrmSessionManagerWrapper
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.*
import com.penthera.virtuososdk.client.drm.UnsupportedDrmException
import com.penthera.virtuososdk.client.drm.VirtuosoDrmSessionManager
import com.penthera.virtuososdk.utility.CommonUtil.Identifier.*
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by Penthera on 17/01/2019.
 */
class VideoPlayerActivity : AppCompatActivity(), View.OnClickListener, PlaybackPreparer, PlayerControlView.VisibilityListener {

    // Best practice is to ensure we have a Virtuoso instance available while playing segmented assets
    // as this will guarantee the proxy service remains available throughout.
    private var mVirtuoso: Virtuoso? = null

    private var playerView: PlayerView? = null
    private var debugRootView: LinearLayout? = null
    private var selectTracksButton: Button? = null
    private var debugTextView: TextView? = null
    private var isShowingTrackSelectionDialog = false

    private var mediaDataSourceFactory: DataSource.Factory? = null
    private var player: SimpleExoPlayer? = null
    private var mediaSource: MediaSource? = null
    private var drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var trackSelectorParameters: DefaultTrackSelector.Parameters? = null
    private var lastSeenTrackGroupArray: TrackGroupArray? = null

    private var debugViewHelper: DebugTextViewHelper? = null
    private var inErrorState = false

    private var shouldAutoPlay: Boolean = false
    private var startWindow: Int = 0
    private var startPosition: Long = 0

    // Activity lifecycle

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mVirtuoso = Virtuoso(applicationContext)

        mediaDataSourceFactory = buildDataSourceFactory(true)
        if (CookieHandler.getDefault() !== DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER)
        }

        setContentView(R.layout.player_activity)
        val rootView = findViewById<View>(R.id.root)
        rootView.setOnClickListener(this)
        debugRootView = findViewById<View>(R.id.controls_root) as LinearLayout
        debugTextView = findViewById<View>(R.id.debug_text_view) as TextView
        selectTracksButton = findViewById<View>(R.id.select_tracks_button) as Button
        selectTracksButton!!.setOnClickListener(this)

        playerView = findViewById<PlayerView>(R.id.player_view).apply{
            setControllerVisibilityListener(this@VideoPlayerActivity)
            setErrorMessageProvider(PlayerErrorMessageProvider(this@VideoPlayerActivity))
            requestFocus()

        }


        if (savedInstanceState != null) {
            trackSelectorParameters = savedInstanceState.getParcelable(KEY_TRACK_SELECTOR_PARAMETERS)
            shouldAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY)
            startWindow = savedInstanceState.getInt(KEY_WINDOW)
            startPosition = savedInstanceState.getLong(KEY_POSITION)
        } else {
            trackSelectorParameters = ParametersBuilder(this).build()
            clearStartPosition()
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        releasePlayer()
        shouldAutoPlay = true
        clearStartPosition()
        setIntent(intent)
    }

    public override fun onStart() {
        super.onStart()
        initializePlayer()
        playerView?.onResume()
    }

    public override fun onResume() {
        super.onResume()
        mVirtuoso?.onResume()
        mVirtuoso?.addObserver(ProxyPortUpdated(this))
    }

    public override fun onPause() {
        super.onPause()
        mVirtuoso?.onPause()
    }

    public override fun onStop() {
        super.onStop()
        playerView?.onPause()
        releasePlayer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updateTrackSelectorParameters()
        updateStartPosition()
        outState.putParcelable(KEY_TRACK_SELECTOR_PARAMETERS, trackSelectorParameters)
        outState.putBoolean(KEY_AUTO_PLAY, shouldAutoPlay)
        outState.putInt(KEY_WINDOW, startWindow)
        outState.putLong(KEY_POSITION, startPosition)
    }

    // Activity input

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Show the controls on any key event.
        playerView?.showController()
        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || playerView?.dispatchMediaKeyEvent(event) ?: false
    }

    // OnClickListener methods

    override fun onClick(view: View) {
        trackSelector?.let {
            if (view === selectTracksButton
                    && !isShowingTrackSelectionDialog
                    && TrackSelectionDialog.willHaveContent(it)) {
                isShowingTrackSelectionDialog = true
                val trackSelectionDialog: TrackSelectionDialog = TrackSelectionDialog.createForTrackSelector(
                        it,  /* onDismissListener= */
                        DialogInterface.OnDismissListener { isShowingTrackSelectionDialog = false })
                trackSelectionDialog.show(supportFragmentManager,  /* tag= */null)
            }
        }
    }

    // PlaybackControlView.PlaybackPreparer implementation

    override fun preparePlayback() {
        player?.retry()
    }

    // PlaybackControlView.VisibilityListener implementation

    override fun onVisibilityChange(visibility: Int) {
        debugRootView?.visibility = visibility
    }

    // Internal methods

    private fun initializePlayer() {
        val intent = intent

        var segmentedAsset: ISegmentedAsset? = null
        val asset: IAsset? = intent.getParcelableExtra(VIRTUOSO_ASSET)
        if (asset != null && asset is ISegmentedAsset) {
            segmentedAsset = asset
        }

        val adaptiveTrackSelectionFactory: TrackSelection.Factory = AdaptiveTrackSelection.Factory()
        trackSelector = DefaultTrackSelector(this, adaptiveTrackSelectionFactory)
        trackSelector?.parameters = trackSelectorParameters!!
        lastSeenTrackGroupArray = null

        if (player == null) {

            if (segmentedAsset != null && segmentedAsset.isContentProtected()) {
                val drmUuid = segmentedAsset.contentProtectionUuid()
                if (drmUuid != null) {
                    var errorStringId = R.string.error_drm_unknown
                    try {
                        var drmSchemeUuid: UUID? = null
                        if (!TextUtils.isEmpty(drmUuid))
                            drmSchemeUuid = Util.getDrmUuid(drmUuid)

                        if (drmSchemeUuid != null) {
                            drmSessionManager = buildDrmSessionManager(drmSchemeUuid, segmentedAsset)
                        } else {
                            errorStringId = R.string.error_drm_unsupported_scheme
                        }
                    } catch (e: UnsupportedDrmException) {
                        errorStringId = if (e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME)
                            R.string.error_drm_unsupported_scheme
                        else
                            R.string.error_drm_unknown
                    }
                    if (drmSessionManager == null) {
                        showToast(errorStringId)
                        return
                    }
                }
            }

            @DefaultRenderersFactory.ExtensionRendererMode val extensionRendererMode =
                    if (BuildConfig.FLAVOR == "withExtensions") DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                    else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF

            val renderersFactory = DefaultRenderersFactory(this)
            renderersFactory.setExtensionRendererMode(extensionRendererMode)

            player = SimpleExoPlayer.Builder(this, renderersFactory).apply {
                setTrackSelector(trackSelector!!)
                setBandwidthMeter(BANDWIDTH_METER)
            }.build().apply {
                addListener(PlayerEventListener())
                addAnalyticsListener(EventLogger(trackSelector))
                playWhenReady = shouldAutoPlay
            }


            playerView?.player = player
            playerView?.setPlaybackPreparer(this)
            debugViewHelper = DebugTextViewHelper(player!!, debugTextView!!)
            debugViewHelper?.start()

            val action = intent.action

            val uri = intent.data
            val type = intent.getIntExtra(VIRTUOSO_CONTENT_TYPE, Common.AssetIdentifierType.FILE_IDENTIFIER)
            if (ACTION_VIEW != action) {
                showToast(getString(R.string.unexpected_intent_action, action))
                return
            }

            mediaSource = buildMediaSource(uri, type)
        }

        inErrorState = false
        val haveResumePosition = startWindow != C.INDEX_UNSET
        if (haveResumePosition) {
            player?.seekTo(startWindow, startPosition)
        }
        player?.prepare(mediaSource!!, !haveResumePosition, false)
        updateButtonVisibilities()
    }

    private fun buildMediaSource(uri: Uri?, type: Int): MediaSource {

        val ret : MediaSource
        when (type) {
            ISegmentedAsset.SEG_FILE_TYPE_MPD -> {
                val factory = DashMediaSource.Factory(
                        DefaultDashChunkSource.Factory(mediaDataSourceFactory!!),
                        buildDataSourceFactory(false))
                if (drmSessionManager != null) {
                    factory.setDrmSessionManager(drmSessionManager!!)
                }
                ret = factory.createMediaSource(uri!!)
            }
            ISegmentedAsset.SEG_FILE_TYPE_HLS -> {
                val factory = HlsMediaSource.Factory(mediaDataSourceFactory!!)
                if (drmSessionManager != null) {
                    factory.setDrmSessionManager(drmSessionManager!!)
                }
                ret = factory.createMediaSource(uri!!)
            }
            Common.AssetIdentifierType.FILE_IDENTIFIER -> ret = ProgressiveMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri)
            else -> {
                throw IllegalStateException("Unsupported type: $type")
            }
        }

        return ret
    }

    @Throws(UnsupportedDrmException::class)
    private fun buildDrmSessionManager(uuid: UUID,
                                       asset: ISegmentedAsset?): DrmSessionManager<FrameworkMediaCrypto>? {

        val keyRequestPropertiesMap: HashMap<String, String> = HashMap()
        val drmListener = DrmListener(this)
        val mediaDrmOnEventListener: MediaDrmOnEventListener = MediaDrmOnEventListener()

        return asset?.let{
            DrmSessionManagerWrapper(applicationContext, uuid,
                    asset, keyRequestPropertiesMap, drmListener, mediaDrmOnEventListener)
        }
    }

    private fun releasePlayer() {
        if (player != null) {
            debugViewHelper?.stop()
            debugViewHelper = null
            shouldAutoPlay = player?.playWhenReady ?: false
            updateStartPosition()
            player?.release()
            player = null
            trackSelector = null
            trackSelectorParameters = null
        }
    }

    private fun updateTrackSelectorParameters() {
        trackSelector?.let {
            trackSelectorParameters = it.parameters
        }
    }

    private fun updateStartPosition() {
        player?.let {
            shouldAutoPlay = it.playWhenReady
            startWindow = it.currentWindowIndex
            startPosition = 0L.coerceAtLeast(it.contentPosition)
        }
    }

    private fun clearStartPosition() {
        shouldAutoPlay = true
        startWindow = C.INDEX_UNSET
        startPosition = C.TIME_UNSET
    }

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set [.BANDWIDTH_METER] as a listener to the new
     * DataSource factory.
     * @return A new DataSource factory.
     */
    private fun buildDataSourceFactory(useBandwidthMeter: Boolean): DataSource.Factory {
        return DefaultDataSourceFactory(applicationContext, if (useBandwidthMeter) BANDWIDTH_METER else null,
                buildHttpDataSourceFactory(useBandwidthMeter))
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param useBandwidthMeter Whether to set [.BANDWIDTH_METER] as a listener to the new
     * DataSource factory.
     * @return A new HttpDataSource factory.
     */
    private fun buildHttpDataSourceFactory(useBandwidthMeter: Boolean): HttpDataSource.Factory {
        return DefaultHttpDataSourceFactory("virtuoso-sdk", if (useBandwidthMeter) BANDWIDTH_METER else null)
    }

    // User controls

    private fun updateButtonVisibilities() {
        trackSelector?.let {
            selectTracksButton!!.isEnabled = player != null && TrackSelectionDialog.willHaveContent(it)
        }
    }

    private inner class PlayerEventListener : Player.EventListener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                showControls()
            }
            updateButtonVisibilities()
        }

        // Error handling

        override fun onPositionDiscontinuity(@Player.DiscontinuityReason reason: Int) {
            if (inErrorState) {
                // This will only occur if the user has performed a seek whilst in the error state. Update
                // the resume position so that if the user then retries, playback will resume from the
                // position to which they seeked.
                updateStartPosition()
            }
        }

        override fun onPlayerError(e: ExoPlaybackException) {
            inErrorState = true
            if (isBehindLiveWindow(e)) {
                clearStartPosition()
                initializePlayer()
            } else {
                updateButtonVisibilities()
                showControls()
            }
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
            updateButtonVisibilities()
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

    fun handleDrmLicenseNotAvailable() {
        inErrorState = true
        clearStartPosition()
        debugRootView!!.visibility = View.GONE

        runOnUiThread {
            AlertDialog.Builder(this@VideoPlayerActivity).apply {
                title = "License unavailable"
                setMessage("License for offline playback expired and renew is unavailable.")
                setNeutralButton("OK", {dialog, _ ->
                    dialog.dismiss()
                    this@VideoPlayerActivity.finish()
                })
            }.create().show()
        }
    }

    private fun showControls() {
        debugRootView!!.visibility = View.VISIBLE
    }

    private fun showToast(messageId: Int) {
        showToast(getString(messageId))
    }

    private fun showToast(message: String?) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }


    companion object {

        private const val VIRTUOSO_CONTENT_TYPE = "asset_type"
        private const val VIRTUOSO_ASSET = "asset"

        private const val ACTION_VIEW = "com.penthera.harness.exoplayer.action.VIEW"

        // Saved instance state keys.
        private const val KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters"
        private const val KEY_WINDOW = "window"
        private const val KEY_POSITION = "position"
        private const val KEY_AUTO_PLAY = "auto_play"

        private val BANDWIDTH_METER = DefaultBandwidthMeter.Builder(null).build()
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

        fun playVideoStream(item : ExampleCatalogItem, context : Context){

            val type : Int = when(item.contentType){
                CatalogItemType.HLS_MANIFEST -> {
                    SEGMENTED_ASSET_IDENTIFIER_HLS
                }
                CatalogItemType.DASH_MANIFEST -> {
                    SEGMENTED_ASSET_IDENTIFIER_MPD
                }
                else -> {
                    FILE_IDENTIFIER
                }

            }


            val intent = Intent(context, VideoPlayerActivity::class.java)
                    .setAction(ACTION_VIEW)
                    .setData(Uri.parse(item.contentUri))
                    .putExtra(VIRTUOSO_CONTENT_TYPE, type)

            context.startActivity(intent)
        }

        fun playVideoDownload(asset: IAsset , context: Context){

            var type = Common.AssetIdentifierType.FILE_IDENTIFIER
            val path: Uri
            if (asset.type == Common.AssetIdentifierType.SEGMENTED_ASSET_IDENTIFIER) {
                val sa = asset as ISegmentedAsset
                type = sa.segmentedFileType()
                val url = sa.playbackURL ?: return
                path = Uri.parse(url.toString())
            } else {
                val f = asset as IFile
                path = Uri.parse(f.filePath)
            }

            val intent = Intent(context, VideoPlayerActivity::class.java)
                    .setAction(ACTION_VIEW)
                    .setData(path)
                    .putExtra(VIRTUOSO_CONTENT_TYPE, type)
                    .putExtra(VIRTUOSO_ASSET, asset)

            context.startActivity(intent)


        }


    }

    /**
     * Demonstrates how to use an observer class from the Download2Go session manager. This
     * enables the client to be informed of events for when keys are loaded or an error occurs
     * with fetching a license.
     */
    private class DrmListener(private val activity: VideoPlayerActivity) : VirtuosoDrmSessionManager.EventListener {

        override fun onDrmKeysLoaded() {
            activity.player?.analyticsCollector?.onDrmKeysLoaded()
        }

        override fun onDrmSessionManagerError(e: java.lang.Exception) { // Can't complete playback
            activity.handleDrmLicenseNotAvailable()
            activity.player?.analyticsCollector?.onDrmSessionManagerError(e)
        }

    }

    /**
     * Demonstrates how to view media drm events directly, which we use for logging
     */
    @TargetApi(18)
    private class MediaDrmOnEventListener : MediaDrm.OnEventListener {
        override fun onEvent(md: MediaDrm, sessionId: ByteArray?, event: Int, extra: Int, data: ByteArray?) {
            Log.d("MediaDrm", "MediaDrm event: $event")
        }
    }

    /**
     * The proxy update observes if the proxy needs to change port after a restart,
     * which can occur if the app is placed in the background and then brought back to the foreground.
     * In this case the player needs to be set back up to get the new base url.
     */
    private class ProxyPortUpdated(private val player: VideoPlayerActivity) : EngineObserver() {
        override fun proxyPortUpdated() {
            super.proxyPortUpdated()
            Log.w(VideoPlayerActivity::class.java.getSimpleName(), "Received warning about change in port, restarting player")
            player.releasePlayer()
            player.shouldAutoPlay = true
            player.initializePlayer()
        }
    }

    /**
     * From ExoPlayer demo: a message provide generates human readable error messages for internal error states.
     */
    private class PlayerErrorMessageProvider(private val context: Context) : ErrorMessageProvider<ExoPlaybackException> {
        override fun getErrorMessage(e: ExoPlaybackException): Pair<Int, String> {
            var errorString: String = context.getString(R.string.error_generic)
            if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                val cause = e.rendererException
                if (cause is DecoderInitializationException) { // Special case for decoder initialization failures.
                    val decoderInitializationException = cause
                    if (decoderInitializationException.codecInfo == null) {
                        if (decoderInitializationException.cause is DecoderQueryException) {
                            errorString = context.getString(R.string.error_querying_decoders)
                        } else if (decoderInitializationException.secureDecoderRequired) {
                            errorString = context.getString(
                                    R.string.error_no_secure_decoder, decoderInitializationException.mimeType)
                        } else {
                            errorString = context.getString(R.string.error_no_decoder, decoderInitializationException.mimeType)
                        }
                    } else {
                        errorString = context.getString(
                                R.string.error_instantiating_decoder,
                                decoderInitializationException.codecInfo!!.name)
                    }
                }
            }
            return Pair.create(0, errorString)
        }
    }
}
