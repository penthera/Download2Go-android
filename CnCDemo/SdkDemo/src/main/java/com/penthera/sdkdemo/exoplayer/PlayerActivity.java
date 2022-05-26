/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.penthera.sdkdemo.exoplayer;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.TracksInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.ExoTrackSelection;
import com.google.android.exoplayer2.ui.StyledPlayerControlView;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.util.DebugTextViewHelper;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Util;
import com.penthera.sdkdemo.R;
import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.Virtuoso;
import com.penthera.virtuososdk.support.exoplayer217.ExoplayerUtils;
import com.penthera.virtuososdk.support.exoplayer217.drm.ExoplayerDrmSessionManager;
import com.penthera.virtuososdk.utility.CommonUtil;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;



/**
 * An activity that plays media using {@link ExoPlayer}.
 */
public class PlayerActivity extends AppCompatActivity implements OnClickListener,
        StyledPlayerControlView.VisibilityListener {


    // Best practice is to ensure we have a Virtuoso instance available while playing segmented assets
    // as this will guarantee the proxy service remains available throughout.
    private Virtuoso mVirtuoso;

    private ProxyUpdateListener receiver;

    public static final String VIRTUOSO_CONTENT_TYPE = "asset_type";
    public static final String VIRTUOSO_ASSET = "asset";

    public static final String ACTION_VIEW = "com.penthera.harness.exoplayer.action.VIEW";

    // Saved instance state keys.
    private static final String KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters";
    private static final String KEY_WINDOW = "window";
    private static final String KEY_POSITION = "position";
    private static final String KEY_AUTO_PLAY = "auto_play";

    //private final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter.Builder(null).build();
    private static final CookieManager DEFAULT_COOKIE_MANAGER;

    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private StyledPlayerView playerView;
    private LinearLayout debugRootView;
    private Button selectTracksButton;
    private TextView debugTextView;
    private boolean isShowingTrackSelectionDialog;

    private Player player;
    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private TracksInfo lastSeenTracksInfo;

    private DebugTextViewHelper debugViewHelper;
    private boolean inErrorState;


    private boolean shouldAutoPlay;
    private int startWindow;
    private long startPosition;


    // Activity lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVirtuoso = new Virtuoso(getApplicationContext());

        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }

        setContentView(R.layout.player_activity);
        debugRootView =  findViewById(R.id.controls_root);
        debugTextView = findViewById(R.id.debug_text_view);
        selectTracksButton = findViewById(R.id.select_tracks_button);
        selectTracksButton.setOnClickListener(this);

        playerView = findViewById(R.id.player_view);
        playerView.setControllerVisibilityListener(this);
        playerView.setErrorMessageProvider(new PlayerErrorMessageProvider());
        playerView.requestFocus();

        if (savedInstanceState != null) {
            trackSelectorParameters = DefaultTrackSelector.Parameters.CREATOR.fromBundle(
                    savedInstanceState.getBundle(KEY_TRACK_SELECTOR_PARAMETERS));
            shouldAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY);
            startWindow = savedInstanceState.getInt(KEY_WINDOW);
            startPosition = savedInstanceState.getLong(KEY_POSITION);
        } else {
            trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder(this).build();
            clearStartPosition();
        }

        receiver = new ProxyUpdateListener();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        releasePlayer();
        shouldAutoPlay = true;
        clearStartPosition();
        setIntent(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
            if (playerView != null) {
                playerView.onResume();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mVirtuoso.onResume();  // best to ensure we have bound to ensure proxy remains available
        String auth = CommonUtil.getAuthority(this);
        IntentFilter filter = new IntentFilter(auth + Common.Notifications.INTENT_PROXY_UPDATE);
        registerReceiver(receiver, filter);

        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer();
            if (playerView != null) {
                playerView.onResume();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mVirtuoso.onPause();
        unregisterReceiver(receiver);
        if (Util.SDK_INT <= 23) {
            if (playerView != null) {
                playerView.onPause();
            }
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            if (playerView != null) {
                playerView.onPause();
            }
            releasePlayer();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        updateTrackSelectorParameters();
        updateStartPosition();
        outState.putBundle(KEY_TRACK_SELECTOR_PARAMETERS, trackSelectorParameters.toBundle());
        outState.putBoolean(KEY_AUTO_PLAY, shouldAutoPlay);
        outState.putInt(KEY_WINDOW, startWindow);
        outState.putLong(KEY_POSITION, startPosition);
    }

    // Activity input

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Show the controls on any key event.
        playerView.showController();
        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || playerView.dispatchMediaKeyEvent(event);
    }

    // OnClickListener methods

    @Override
    public void onClick(View view) {
        if (view == selectTracksButton
                && !isShowingTrackSelectionDialog
                && TrackSelectionDialog.willHaveContent(trackSelector)) {
            isShowingTrackSelectionDialog = true;
            TrackSelectionDialog trackSelectionDialog =
                    TrackSelectionDialog.createForTrackSelector(
                            trackSelector,
                            /* onDismissListener= */ dismissedDialog -> isShowingTrackSelectionDialog = false);
            trackSelectionDialog.show(getSupportFragmentManager(), /* tag= */ null);
        }
    }

    // PlaybackControlView.VisibilityListener implementation
    @Override
    public void onVisibilityChange(int visibility) {
        debugRootView.setVisibility(visibility);
    }

    // Internal methods

    private void initializePlayer() {
        Intent intent = getIntent();

        String action = intent.getAction();

        Uri uri = intent.getData();

        IAsset asset = intent.getParcelableExtra(VIRTUOSO_ASSET);

        // All our files are stored in the app private space so no need to check permissions after kitkat
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            if (Util.maybeRequestReadExternalStoragePermission(this, uri)) {
                // The player will be reinitialized if the permission is granted.
                return;
            }
        }

        ExoTrackSelection.Factory adaptiveTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory();
        trackSelector = new DefaultTrackSelector(this, adaptiveTrackSelectionFactory);
        trackSelector.setParameters(trackSelectorParameters);
        lastSeenTracksInfo = TracksInfo.EMPTY;

        if (player == null) {

            EventLogger eventLogger = new EventLogger(trackSelector);

            @DefaultRenderersFactory.ExtensionRendererMode int extensionRendererMode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this)
                    .setExtensionRendererMode(extensionRendererMode);

            ExoplayerUtils.PlayerConfigOptions.Builder builder = new ExoplayerUtils.PlayerConfigOptions.Builder(this)
                    .playWhenReady(true)
                    .withBandwidthMeter(DefaultBandwidthMeter.getSingletonInstance(this))
                    .withTrackSelector(trackSelector)
                    .withAnalyticsListener(eventLogger)
                    .userRenderersFactory(renderersFactory)
                    .withPlayerListener(new PlayerEventListener());

            builder.mediaSourceOptions()
                    .withTransferListener(DefaultBandwidthMeter.getSingletonInstance(this))
                    .withUserAgent("virtuoso-sdk");

            builder.drmOptions()
                    .withDrmSessionManagerEventListener(new DrmListener(this));

            if (startWindow != C.INDEX_UNSET) {
                builder.withSeekToPosition(startWindow, startPosition);
            }

            try {
                if (asset != null) {
                    player = ExoplayerUtils.setupPlayer(playerView, mVirtuoso.getAssetManager(), asset, false, builder.build());
                } else {
                    // Streaming, not download
                    ExoPlayer.Builder exoBuilder =
                            new ExoPlayer.Builder(this, renderersFactory);
                    exoBuilder.setTrackSelector(trackSelector);
                    ExoPlayer exoplayer = exoBuilder.build();
                    MediaItem.Builder mediaBuilder = new MediaItem.Builder().setUri(uri);
                    exoplayer.setMediaItem(mediaBuilder.build());
                    player = exoplayer;
                    playerView.setPlayer(player);
                    player.setPlayWhenReady(true);
                }
                if (player == null) {
                    runOnUiThread(() -> {
                        AlertDialog alertDialog = new AlertDialog.Builder(PlayerActivity.this).create();
                        alertDialog.setTitle("Asset unavailable");
                        alertDialog.setMessage("Could not initialize player for asset. Playback unavailable.");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", (dialog, which) -> {
                            dialog.dismiss();
                            PlayerActivity.this.finish();
                        });
                        alertDialog.show();
                    });
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return;
            }

            if (player != null && player instanceof ExoPlayer) {
                debugViewHelper = new DebugTextViewHelper((ExoPlayer) player, debugTextView);
                debugViewHelper.start();
            }
        }
        updateButtonVisibilities();
    }

    private void releasePlayer() {
        if (player != null) {
            debugViewHelper.stop();
            debugViewHelper = null;
            shouldAutoPlay = player.getPlayWhenReady();
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
            shouldAutoPlay = player.getPlayWhenReady();
            startWindow = player.getCurrentWindowIndex();
            startPosition = Math.max(0, player.getContentPosition());
        }
    }

    private void clearStartPosition() {
        shouldAutoPlay = true;
        startWindow = C.INDEX_UNSET;
        startPosition = C.TIME_UNSET;
    }


    // User controls
    private void updateButtonVisibilities() {
        selectTracksButton.setEnabled(player != null && TrackSelectionDialog.willHaveContent(trackSelector));
    }


    /**
     * Simple example of Exoplayer EventListener, taken from ExoPlayer Demo.
     */
    private class PlayerEventListener implements Player.Listener {

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == Player.STATE_ENDED) {
                showControls();
            }
            updateButtonVisibilities();
        }

        // Error handling

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
        public void onPlayerError(@NonNull PlaybackException e) {
            inErrorState = true;
            if (e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                clearStartPosition();
                initializePlayer();
            } else {
                updateButtonVisibilities();
                showControls();
            }
        }

        @Override
        @SuppressWarnings("ReferenceEquality")
        public void onTracksInfoChanged(TracksInfo tracksInfo) {
            updateButtonVisibilities();
            if (tracksInfo == lastSeenTracksInfo) {
                return;
            }
            if (!tracksInfo.isTypeSupportedOrEmpty(C.TRACK_TYPE_VIDEO)) {
                showToast(R.string.error_unsupported_video);
            }
            if (!tracksInfo.isTypeSupportedOrEmpty(C.TRACK_TYPE_AUDIO)) {
                showToast(R.string.error_unsupported_audio);
            }
            lastSeenTracksInfo = tracksInfo;
        }
    }

    public void handleDrmLicenseNotAvailable() {
        inErrorState = true;
        clearStartPosition();
        debugRootView.setVisibility(View.GONE);

        runOnUiThread(() -> {
            AlertDialog alertDialog = new AlertDialog.Builder(PlayerActivity.this).create();
            alertDialog.setTitle("License unavailable");
            alertDialog.setMessage("License for offline playback expired and renew is unavailable.");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", (dialog, which) -> {
                dialog.dismiss();
                PlayerActivity.this.finish();
            });
            alertDialog.show();
        });
    }

    private void showControls() {
        debugRootView.setVisibility(View.VISIBLE);
    }

    private void showToast(int messageId) {
        showToast(getString(messageId));
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    /**
     * Demonstrates how to use an observer class from the Download2Go session manager. This
     * enables the client to be informed of events for when keys are loaded or an error occurs
     * with fetching a license.
     */
    private static class DrmListener implements ExoplayerDrmSessionManager.EventListener {

        private final PlayerActivity mActivity;

        public DrmListener(PlayerActivity activity) {
            mActivity = activity;
        }

        @Override
        public void onDrmKeysLoaded() {
            Toast.makeText(mActivity, "DRM keys loaded", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDrmSessionManagerError(Exception e) {
            // Can't complete playback
            mActivity.handleDrmLicenseNotAvailable();

        }
    }

    /**
     * The proxy update listener receives broadcasts if the proxy needs to change port after a restart,
     * which can occur if the app is placed in the background and then brought back to the foreground.
     * In this case the player needs to be set back up to get the new base url.
     */
    private class ProxyUpdateListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(PlayerActivity.class.getSimpleName(), "Received warning about change in port, restarting player");
            releasePlayer();
            shouldAutoPlay = true;
            initializePlayer();
        }
    }

    /**
     * From ExoPlayer demo: a message provide generates human readable error messages for internal error states.
     */
    private class PlayerErrorMessageProvider implements ErrorMessageProvider<PlaybackException> {

        @NonNull
        @Override
        public Pair<Integer, String> getErrorMessage(@NonNull PlaybackException e) {
            String errorString = getString(R.string.error_generic);
            Throwable cause = e.getCause();
            if (cause instanceof DecoderInitializationException) {
                // Special case for decoder initialization failures.
                DecoderInitializationException decoderInitializationException =
                        (DecoderInitializationException) cause;
                if (decoderInitializationException.codecInfo == null) {
                    if (decoderInitializationException.getCause() instanceof DecoderQueryException) {
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

