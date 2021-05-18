/*
    Created by Penthera.
    Copyright © 2020 penthera. All rights reserved.

    This source file contains a very basic example showing how to use the Penthera Download2Go SDK.
    Pay close attention to code comments marked IMPORTANT
*/
package com.penthera.download2go8;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaDrm;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackPreparer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Util;
import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.ISegmentedAsset;
import com.penthera.virtuososdk.client.Virtuoso;
import com.penthera.virtuososdk.client.drm.UnsupportedDrmException;
import com.penthera.virtuososdk.support.exoplayer211.drm.SupportDrmSessionManager;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import static com.penthera.virtuososdk.Common.AssetIdentifierType.FILE_IDENTIFIER;

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public class VideoPlayerActivity extends AppCompatActivity implements PlaybackPreparer {

    // Saved instance state keys.
    private static final String KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters";
    private static final String KEY_WINDOW = "window";
    private static final String KEY_POSITION = "position";
    private static final String KEY_AUTO_PLAY = "auto_play";

    private static final String VIRTUOSO_ASSET = "asset";
    private static final String ACTION_VIEW = "com.penthera.harness.exoplayer.action.VIEW";


    private static final CookieManager DEFAULT_COOKIE_MANAGER;

    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    // IMPORTANT - Best practice is to ensure we have a Virtuoso instance available while playing segmented assets
    // as this will guarantee the proxy service remains available throughout. We can do this in the activity or store
    // a singleton for the whole application. But this should not be instantiated in an application onCreate().
    private Virtuoso mVirtuoso;

    private PlayerView playerView;

    private DataSource.Factory dataSourceFactory;
    private SimpleExoPlayer player;
    private MediaSource mediaSource;
    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private TrackGroupArray lastSeenTrackGroupArray;

    private DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;

    private boolean startAutoPlay;
    private int startWindow;
    private long startPosition;

    public static void playVideoDownload(Context context, IAsset asset) {
        try {
            // We get the path in advance to ensure the asset is playable before sending to the player activity
            URL playbackURL = asset.getPlaybackURL(); // This will return null if the asset is unavailable due to business rules
            if (playbackURL != null) {
                Uri path = Uri.parse(playbackURL.toString());
                Intent intent = new Intent(context, VideoPlayerActivity.class)
                        .setAction(ACTION_VIEW)
                        .setData(path)
                        .putExtra(VIRTUOSO_ASSET, asset);
                context.startActivity(intent);
            } else {
                Toast.makeText(context, R.string.error_cannot_play, Toast.LENGTH_LONG).show();
            }
        } catch (MalformedURLException mue) {
            Toast.makeText(context, R.string.error_invalid_url, Toast.LENGTH_LONG).show();
        }
    }

    // Check out https://github.com/google/ExoPlayer/tree/release-v2/demos

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVirtuoso = new Virtuoso(getApplicationContext());

        dataSourceFactory = buildDataSourceFactory();

        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }

        setContentView(R.layout.player_activity);

        playerView = findViewById(R.id.player_view);
        playerView.setErrorMessageProvider(new PlayerErrorMessageProvider());
        playerView.requestFocus();

        if (savedInstanceState != null) {
            trackSelectorParameters = savedInstanceState.getParcelable(KEY_TRACK_SELECTOR_PARAMETERS);
            startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY);
            startWindow = savedInstanceState.getInt(KEY_WINDOW);
            startPosition = savedInstanceState.getLong(KEY_POSITION);
        } else {
            DefaultTrackSelector.ParametersBuilder builder =
                    new DefaultTrackSelector.ParametersBuilder(/* context= */ this);
            trackSelectorParameters = builder.build();
            clearStartPosition();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        releasePlayer();
        clearStartPosition();
        setIntent(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        initializePlayer();
        if (playerView != null) {
            playerView.onResume();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (player == null) {
            initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (playerView != null) {
            playerView.onPause();
        }
        releasePlayer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        updateTrackSelectorParameters();
        updateStartPosition();
        outState.putParcelable(KEY_TRACK_SELECTOR_PARAMETERS, trackSelectorParameters);
        outState.putBoolean(KEY_AUTO_PLAY, startAutoPlay);
        outState.putInt(KEY_WINDOW, startWindow);
        outState.putLong(KEY_POSITION, startPosition);
    }

    // Activity input

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // See whether the player view wants to handle media or DPAD keys events.
        return playerView.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    @Override
    public void preparePlayback() {
        player.retry();
    }

    private void initializePlayer() {
        if (player == null) {
            Intent intent = getIntent();

            String action = intent.getAction();
            if (!ACTION_VIEW.equals(action)) {
                showToast(getString(R.string.unexpected_intent_action, action));
                return;
            }

            Uri uri = intent.getData();
            IAsset asset = intent.getParcelableExtra(VIRTUOSO_ASSET);
            ISegmentedAsset segmentedAsset = asset instanceof ISegmentedAsset ? (ISegmentedAsset) asset : null;
            int type = segmentedAsset != null ? segmentedAsset.segmentedFileType() : FILE_IDENTIFIER;

            if (segmentedAsset != null && segmentedAsset.isContentProtected()) {

                String drmUuid = segmentedAsset.contentProtectionUuid();
                if (drmUuid != null) {
                    int errorStringId = R.string.error_drm_unknown;
                    try {
                        UUID drmSchemeUuid = null;
                        if (!TextUtils.isEmpty(drmUuid))
                            drmSchemeUuid = Util.getDrmUuid(drmUuid);

                        if (drmSchemeUuid != null) {
                            DrmListener drmListener = new DrmListener(this);
                            MediaDrmOnEventListener mediaDrmOnEventListener = new MediaDrmOnEventListener();

                            drmSessionManager = new DemoDrmSessionManager(getApplicationContext(), drmSchemeUuid, segmentedAsset, null, drmListener, mediaDrmOnEventListener);
                        } else {
                            errorStringId = R.string.error_drm_unsupported_scheme;
                        }
                    } catch (UnsupportedDrmException e) {
                        errorStringId = e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                                ? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown;
                    }
                    if (drmSessionManager == null) {
                        showToast(errorStringId);
                        return;
                    }
                }
            }

            mediaSource = buildMediaSource(uri, type);
            if (mediaSource == null) {
                return;
            }

            TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory();
            trackSelector = new DefaultTrackSelector(this, trackSelectionFactory);
            trackSelector.setParameters(trackSelectorParameters);
            lastSeenTrackGroupArray = null;

            RenderersFactory renderersFactory = new DefaultRenderersFactory(this)
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);

            player = new SimpleExoPlayer.Builder(this, renderersFactory)
                    .setTrackSelector(trackSelector)
                    .build();
            player.addListener(new PlayerEventListener());
            player.setPlayWhenReady(startAutoPlay);
            player.addAnalyticsListener(new EventLogger(trackSelector));
            playerView.setPlayer(player);
            playerView.setPlaybackPreparer(this);
        }
        boolean haveStartPosition = startWindow != C.INDEX_UNSET;
        if (haveStartPosition) {
            player.seekTo(startWindow, startPosition);
        }
        player.prepare(mediaSource, !haveStartPosition, false);
        updateButtonVisibility();
    }


    private MediaSource buildMediaSource(Uri uri, int type) {
        switch (type) {
            case ISegmentedAsset.SEG_FILE_TYPE_MPD: {
                DashMediaSource.Factory factory = new DashMediaSource.Factory(
                        new DefaultDashChunkSource.Factory(dataSourceFactory),
                        buildDataSourceFactory());
                // If we have a drm session manager then provide it here to the media source factory
                if (drmSessionManager != null) {
                    factory.setDrmSessionManager(drmSessionManager);
                }
                return factory.createMediaSource(uri);
            }
            case ISegmentedAsset.SEG_FILE_TYPE_HLS: {
                HlsMediaSource.Factory factory = new HlsMediaSource.Factory(dataSourceFactory);
                // If we have a drm session manager then provide it here to the media source factory
                if (drmSessionManager != null) {
                    factory.setDrmSessionManager(drmSessionManager);
                }
                return factory.createMediaSource(uri);
            }
            case Common.AssetIdentifierType.FILE_IDENTIFIER:
                return new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(uri);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    private void releasePlayer() {
        if (player != null) {
            updateTrackSelectorParameters();
            updateStartPosition();
            player.release();
            player = null;
            mediaSource = null;
            trackSelector = null;
        }
    }

    private void updateTrackSelectorParameters() {
        if (trackSelector != null) {
            trackSelectorParameters = trackSelector.getParameters();
        }
    }

    private void updateStartPosition() {
        if (player != null) {
            startAutoPlay = player.getPlayWhenReady();
            startWindow = player.getCurrentWindowIndex();
            startPosition = Math.max(0, player.getContentPosition());
        }
    }


    private void clearStartPosition() {
        startAutoPlay = true;
        startWindow = C.INDEX_UNSET;
        startPosition = C.TIME_UNSET;
    }

    /**
     * Returns an exoplayer {@link DataSource.Factory}.
     */
    private DataSource.Factory buildDataSourceFactory() {
        return new DefaultDataSourceFactory(getApplicationContext(), new DefaultHttpDataSourceFactory("download2gohelloworld"));
    }

    // User controls
    private void updateButtonVisibility() {

    }

    private void showControls() {
    }

    // Utility methods for showing toast messages
    private void showToast(int messageId) {
        showToast(getString(messageId));
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private static boolean isBehindLiveWindow(ExoPlaybackException e) {
        if (e.type != ExoPlaybackException.TYPE_SOURCE) {
            return false;
        }
        Throwable cause = e.getSourceException();
        while (cause != null) {
            if (cause instanceof BehindLiveWindowException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    public void handleDrmLicenseNotAvailable() {
        clearStartPosition();

        runOnUiThread(() -> {
            AlertDialog alertDialog = new AlertDialog.Builder(VideoPlayerActivity.this).create();
            alertDialog.setTitle("License unavailable");
            alertDialog.setMessage("License for offline playback expired and renew is unavailable.");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", (dialog, which) -> {
                dialog.dismiss();
                VideoPlayerActivity.this.finish();
            });
            alertDialog.show();
        });
    }

    // Observer class from the Download2Go session manager which enables the client to be informed of
    // events for when keys are loaded or an error occurs with fetching a license.
    private static class DrmListener implements SupportDrmSessionManager.EventListener {

        private VideoPlayerActivity mActivity;

        public DrmListener(VideoPlayerActivity activity) {
            mActivity = activity;
        }

        @Override
        public void onDrmKeysLoaded() {
            if (mActivity.player != null) {
                mActivity.player.getAnalyticsCollector().onDrmKeysLoaded();
            }
        }

        @Override
        public void onDrmSessionManagerError(Exception e) {
            // Can't complete playback
            mActivity.handleDrmLicenseNotAvailable();

            if (mActivity.player != null) {
                mActivity.player.getAnalyticsCollector().onDrmSessionManagerError(e);
            }
        }
    }

    /**
     * Demonstrates how to view media drm events directly, which can be used for logging
     */
    @TargetApi(18)
    private static class MediaDrmOnEventListener implements MediaDrm.OnEventListener {
        @Override
        public void onEvent(@NonNull MediaDrm md, @Nullable byte[] sessionId, int event, int extra, @Nullable byte[] data) {
            Log.d("MediaDrm", "MediaDrm event: " + event);
        }
    }

    // This inner class is taken directly from the Exoplayer demo. It provides the player listener interface for updating overlay buttons.
    private class PlayerEventListener implements Player.EventListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, @Player.State int playbackState) {
            if (playbackState == Player.STATE_ENDED) {
                showControls();
            }
            updateButtonVisibility();
        }

        @Override
        public void onPlayerError(ExoPlaybackException e) {
            if (isBehindLiveWindow(e)) {
                clearStartPosition();
                initializePlayer();
            } else {
                updateButtonVisibility();
                showControls();
            }
        }

        @Override
        @SuppressWarnings("ReferenceEquality")
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            updateButtonVisibility();
            if (trackGroups != lastSeenTrackGroupArray) {
                MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                if (mappedTrackInfo != null) {
                    if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO)
                            == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        showToast(R.string.error_unsupported_video);
                    }
                    if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_AUDIO)
                            == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        showToast(R.string.error_unsupported_audio);
                    }
                }
                lastSeenTrackGroupArray = trackGroups;
            }
        }
    }


    // This inner class is taken directly from the Exoplayer demo. It provides human readable error messages for exoplayer errors.
    private class PlayerErrorMessageProvider implements ErrorMessageProvider<ExoPlaybackException> {

        @NonNull
        @Override
        public Pair<Integer, String> getErrorMessage(ExoPlaybackException e) {
            String errorString = getString(R.string.error_generic);
            if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                Exception cause = e.getRendererException();
                if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
                    // Special case for decoder initialization failures.
                    MediaCodecRenderer.DecoderInitializationException decoderInitializationException =
                            (MediaCodecRenderer.DecoderInitializationException) cause;
                    if (decoderInitializationException.codecInfo == null) {
                        if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
                            errorString = getString(R.string.error_querying_decoders);
                        } else if (decoderInitializationException.secureDecoderRequired) {
                            errorString =
                                    getString(
                                            R.string.error_no_secure_decoder, decoderInitializationException.mimeType);
                        } else {
                            errorString =
                                    getString(R.string.error_no_decoder, decoderInitializationException.mimeType);
                        }
                    } else {
                        errorString =
                                getString(
                                        R.string.error_instantiating_decoder,
                                        decoderInitializationException.codecInfo.name);
                    }
                }
            }
            return Pair.create(0, errorString);
        }
    }
}
