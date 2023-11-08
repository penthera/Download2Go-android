/*
    Created by Penthera.
    Copyright © 2020 penthera. All rights reserved.

    This source file contains a very basic example showing how to use the Penthera Download2Go SDK.
    Pay close attention to code comments marked IMPORTANT
*/
package com.penthera.download2go8;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.media3.common.C;
import androidx.media3.common.ErrorMessageProvider;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.RenderersFactory;
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer;
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil;
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.util.EventLogger;
import androidx.media3.ui.PlayerView;
import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.Virtuoso;
import com.penthera.virtuososdk.support.androidx.media311.ExoplayerUtils;
import com.penthera.virtuososdk.support.androidx.media311.drm.ExoplayerDrmSessionManager;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * An activity that plays media using {@link Player}.
 */
public class VideoPlayerActivity extends AppCompatActivity {

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

    private Player player;
    private DefaultTrackSelector trackSelector;
    private TrackSelectionParameters trackSelectorParameters;
    private Tracks lastSeenTracksInfo;

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
        initializePlayer();
    }

    @Override
    public void onResume() {
        super.onResume();
        mVirtuoso.onResume();
        if ( player == null) {
            initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mVirtuoso.onPause();
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
            if (!ACTION_VIEW.equals(action)) {
                showToast(getString(R.string.unexpected_intent_action, action));
                return;
            }

            IAsset asset = intent.getParcelableExtra(VIRTUOSO_ASSET);

            AdaptiveTrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory();
            trackSelector = new DefaultTrackSelector(this, trackSelectionFactory);
            trackSelector.setParameters(trackSelectorParameters);
            lastSeenTracksInfo = Tracks.EMPTY;

            RenderersFactory renderersFactory = new DefaultRenderersFactory(this)
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);

            ExoplayerUtils.PlayerConfigOptions.Builder builder = new ExoplayerUtils.PlayerConfigOptions.Builder(this)
                    .userRenderersFactory(renderersFactory)
                    .withTrackSelector(trackSelector)
                    .withPlayerListener(new PlayerEventListener())
                    .withAnalyticsListener(new EventLogger())
                    .playWhenReady(true);

            builder.mediaSourceOptions().useTransferListener(true)
                    .withUserAgent("virtuoso-sdk");

            builder.drmOptions()
                    .withDrmSessionManagerEventListener(new DrmListener(this));

            boolean haveResumePosition = startWindow != C.INDEX_UNSET;
            if (haveResumePosition) {
                builder.withSeekToPosition(startWindow, startPosition);
            }

            try {
                player = ExoplayerUtils.setupPlayer(playerView, mVirtuoso.getAssetManager(), asset, false, builder.build());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return;
            }

        }
        updateButtonVisibility();
    }

    private void releasePlayer() {
        if (player != null) {
            updateTrackSelectorParameters();
            updateStartPosition();
            player.release();
            player = null;
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
    private static class DrmListener implements ExoplayerDrmSessionManager.EventListener {

        private final VideoPlayerActivity mActivity;

        public DrmListener(VideoPlayerActivity activity) {
            mActivity = activity;
        }

        @Override
        public void onDrmKeysLoaded() {

        }

        @Override
        public void onDrmSessionManagerError(Exception e) {
            // Can't complete playback
            mActivity.handleDrmLicenseNotAvailable();


        }
    }

    // This inner class is taken directly from the Exoplayer demo. It provides the player listener interface for updating overlay buttons.
    private class PlayerEventListener implements Player.Listener {

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == Player.STATE_ENDED) {
                showControls();
            }
            updateButtonVisibility();
        }

        @Override
        public void onPlayerError(@NonNull PlaybackException e) {
            if (e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                clearStartPosition();
                initializePlayer();
            } else {
                updateButtonVisibility();
                showControls();
            }
        }

        @Override
        @SuppressWarnings("ReferenceEquality")
        public void onTracksChanged(Tracks tracks) {
            updateButtonVisibility();
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
}
