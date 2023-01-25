/*
    Copyright © 2022 penthera. All rights reserved.

    This source file contains a very basic example showing how to use the Penthera PlayAssure SDK.
    Pay close attention to code comments marked IMPORTANT
*/
package com.penthera.playassurehelloworld;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.penthera.playassure.InitializationResult;
import com.penthera.playassure.PlayAssureConfig;
import com.penthera.playassure.PlayAssureEngineState;
import com.penthera.playassure.PlayAssureError;
import com.penthera.playassure.PlayAssureManager;
import com.penthera.playassure.PlayAssureStatus;
import com.penthera.playassure.PlayAssureStatusInfo;
import com.penthera.virtuososdk.support.exoplayer218.ExoplayerUtils;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;


/**
 * An activity that plays media using {@link Player}.
 */
public class VideoPlayerActivity extends AppCompatActivity implements PlayAssureStatus {

    // Saved instance state keys.
    private static final String KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters";
    private static final String KEY_WINDOW = "window";
    private static final String KEY_POSITION = "position";
    private static final String KEY_AUTO_PLAY = "auto_play";

    private static final String ACTION_VIEW = "com.penthera.playassure.exoplayer.action.VIEW";
    private static final String ACTION_VIEW_STREAM = "com.penthera.playassure.player.action.STREAM";

    private static final String BACKPLANE_URL = "backplane_url";
    private static final String PUBLIC_KEY = "public_key";
    private static final String PRIVATE_KEY = "private_key";
    private static final String USER_ID = "user_id";

    private static final CookieManager DEFAULT_COOKIE_MANAGER;

    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private StyledPlayerView playerView;

    private Player player;
    private DefaultTrackSelector trackSelector;
    private TrackSelectionParameters trackSelectorParameters;
    private Tracks lastSeenTracksInfo;

    private boolean inErrorState;
    private boolean startAutoPlay;
    private int startWindow;
    private long startPosition;

    private boolean errorReported = false;

    private PlayAssureManager playAssureManager = null;

    public static void playStream(String url, Context context) {
        context.startActivity(new Intent(context, VideoPlayerActivity.class)
                .setAction(ACTION_VIEW_STREAM)
                .setData(Uri.parse(url))
        );
    }

    public static void playAssure(String url,
                                  Context context,
                                  String backplaneUrl,
                                  String publicKey,
                                  String privateKey,
                                  String userId) {
        context.startActivity(new Intent(context, VideoPlayerActivity.class)
                .setAction(ACTION_VIEW)
                .setData(Uri.parse(url))
                .putExtra(BACKPLANE_URL, backplaneUrl)
                .putExtra(PUBLIC_KEY, publicKey)
                .putExtra(PRIVATE_KEY, privateKey)
                .putExtra(USER_ID, userId)
        );
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }

        setContentView(R.layout.player_activity);

        playerView = findViewById(R.id.player_view);
        playerView.setErrorMessageProvider(new PlayerErrorMessageProvider());
        playerView.requestFocus();

        if (savedInstanceState != null) {
            trackSelectorParameters = TrackSelectionParameters.fromBundle(
                    savedInstanceState.getBundle(KEY_TRACK_SELECTOR_PARAMETERS));
            startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY);
            startWindow = savedInstanceState.getInt(KEY_WINDOW);
            startPosition = savedInstanceState.getLong(KEY_POSITION);
        } else {
            trackSelectorParameters = new TrackSelectionParameters.Builder(this).build();
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

        if (getIntent().getAction().equals(ACTION_VIEW)) {
            if (playAssureManager == null) {
                PlayAssureConfig config = new PlayAssureConfig.Builder(this)
                        .playAssureObserver(this).build();

                playAssureManager = new PlayAssureManager(this, getIntent().getDataString(), config);
            } else {
                initializePlayer();
            }
        } else {
            // For streaming, simply initialize the player immediately
            initializePlayer();
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
        releasePlayer();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        updateTrackSelectorParameters();
        updateStartPosition();
        outState.putBundle(KEY_TRACK_SELECTOR_PARAMETERS, trackSelectorParameters.toBundle());
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

    private void initializePlayer() {
        if (player == null) {
            Intent intent = getIntent();

            String action = intent.getAction();

            AdaptiveTrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory();
            trackSelector = new DefaultTrackSelector(this, trackSelectionFactory);
            trackSelector.setParameters(trackSelectorParameters);
            lastSeenTracksInfo = Tracks.EMPTY;

            RenderersFactory renderersFactory = new DefaultRenderersFactory(this)
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);

            player = new SimpleExoPlayer.Builder(this, renderersFactory)
                    .setTrackSelector(trackSelector)
                    .build();

            playerView.setPlayer(player);
            player.addListener(new PlayerEventListener());

            String playerUrl = intent.getDataString();
            String assetUrl = null;

            boolean playAssurePlayback = action.equals(ACTION_VIEW);

            if (playAssurePlayback) {
                // Play Assure
                playerUrl = playAssureManager.getStreamingUrl();
                assetUrl = playAssureManager.getSourceURL();
                if (playerUrl == null) {
                    playerUrl = assetUrl;  // backup for if playassure is not available
                }
                player.setMediaItem(MediaItem.fromUri(playerUrl));

            } else {
                // Streaming
                player.setMediaItem(MediaItem.fromUri(playerUrl));
            }
            player.setPlayWhenReady(true);

            ExoplayerUtils.setupPlayerAnalytics(this, player, playerUrl, assetUrl, playAssurePlayback);

            player.prepare();
        }
    }


    private void releasePlayer() {
        if (player != null) {
            updateTrackSelectorParameters();
            updateStartPosition();
            player.release();
            player = null;
            trackSelector = null;
            trackSelectorParameters = null;
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

    // Utility methods for showing toast messages
    private void showToast(int messageId) {
        showToast(getString(messageId));
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    // This inner class is taken directly from the Exoplayer demo. It provides the player listener interface for updating overlay buttons.
    private class PlayerEventListener implements Player.Listener {

        @Override
        public void onPlaybackStateChanged(int playbackState) {
        }

        @Override
        public void onPlayerError(@NonNull PlaybackException e) {
            inErrorState = true;
            if (e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                clearStartPosition();
                initializePlayer();
            } else {
                updateStartPosition();
            }
        }

        @Override
        public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
            if (inErrorState) {
                // This will only occur if the user has performed a seek whilst in the error state. Update
                // the resume position so that if the user then retries, playback will resume from the
                // position to which they seeked.
                updateStartPosition();
            }
        }

        @Override
        @SuppressWarnings("ReferenceEquality")
        public void onTracksChanged(Tracks tracks) {
            if (tracks == lastSeenTracksInfo) {
                return;
            }
            if (tracks.containsType(C.TRACK_TYPE_VIDEO)
                    && !tracks.isTypeSupported(C.TRACK_TYPE_VIDEO, /* allowExceedsCapabilities= */ true)) {
                showToast(R.string.error_unsupported_video);
            }
            if (tracks.containsType(C.TRACK_TYPE_AUDIO)
                    && !tracks.isTypeSupported(C.TRACK_TYPE_AUDIO, /* allowExceedsCapabilities= */ true)) {
                showToast(R.string.error_unsupported_audio);
            }
            lastSeenTracksInfo = tracks;
        }
    }

    /**
     * From ExoPlayer demo: a message provide generates human readable error messages for internal error states.
     */
    private class PlayerErrorMessageProvider implements ErrorMessageProvider<PlaybackException> {

        @Override
        @NonNull
        public Pair<Integer, String> getErrorMessage(@NonNull PlaybackException e) {
            String errorString = getString(R.string.error_generic);
            Throwable cause = e.getCause();
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
            return Pair.create(0, errorString);
        }
    }

    // Play Assure Actions

    private void shutdown() {
        if (!getIntent().getAction().equals(ACTION_VIEW_STREAM)) {
            if (playAssureManager != null) {
                // Run the shutdown on a background thread
                Runnable shutdown = () -> playAssureManager.shutdownSync();
                new Thread(shutdown).start();
            }
        }
    }

    // Play Assure Observer
    @Override
    public void initializationComplete(@NonNull InitializationResult initializationResult) {
        if (initializationResult != InitializationResult.SUCCESS) {
            displayFailureDialog("Unable to initialize SDK: " + initializationResult.name());
        }
    }

    @Override
    public void reportEngineStatusChanged(@NonNull PlayAssureEngineState playAssureEngineState) {

        if (playAssureEngineState == PlayAssureEngineState.Started) {
            initializePlayer();
        } else if (playAssureEngineState == PlayAssureEngineState.Unregistered) {
            // Try to register the SDK when the observer reports unregistered
            if (playAssureManager != null) {
                Bundle extras = getIntent().getExtras();
                // Run on a background thread
                Runnable startup = () -> playAssureManager.startupSDK(
                        extras.getString(BACKPLANE_URL),
                        extras.getString(PUBLIC_KEY),
                        extras.getString(PRIVATE_KEY),
                        extras.getString(USER_ID),
                        null
                );
                new Thread(startup).start();
            }
        } else if (playAssureEngineState == PlayAssureEngineState.Errored) {
            runOnUiThread(() -> displayFailureDialog("Error encountered during initialization."));
        }
    }

    @Override
    public void bitrateChangeReported(@NonNull PlayAssureStatusInfo playAssureStatusInfo) {

    }

    @Override
    public void networkReachabilityChanged(boolean b) {

    }

    @Override
    public void progressReported(@NonNull PlayAssureStatusInfo playAssureStatusInfo) {

    }

    @Override
    public void reportNetworkFail() {
        runOnUiThread(() -> displayFailureDialog("Network bandwidth does not support playback."));
    }

    @Override
    public void reportParsingFail(@NonNull PlayAssureError playAssureError, @NonNull String error) {
        runOnUiThread(() -> displayFailureDialog("Error encountered parsing manifest: " + error + ", code: " + playAssureError.name()));
    }

    @Override
    public void reportParsingSuccess() {
        runOnUiThread(() -> showToast("Manifests successfully parsed."));
    }

    @Override
    public void reportPlaybackFail(@NonNull PlayAssureError playAssureError, @NonNull String error) {
        runOnUiThread(() -> displayFailureDialog("Error encountered during playback: " + error + ", code: " + playAssureError.name()));
    }

    @Override
    public void reportPlaybackThrottled(boolean throttled) {
        // Demonstrates how to observe if the SDK needs to throttle the player
        // bandwidth in order to ensure smooth playback
        runOnUiThread(() -> showToast(throttled ? "Playback throttled" : "Playback throttling finished"));
    }

    private void displayFailureDialog(String text) {
        if (!errorReported) {
            errorReported = true;
            new AlertDialog.Builder(this)
                    .setTitle("Playback Failure")
                    .setMessage(text)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        shutdown();
                        finish();
                    }).show();
        }
    }
}
