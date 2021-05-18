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
/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaDrm;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackPreparer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.DebugTextViewHelper;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Util;
import com.penthera.sdkdemo.R;
import com.penthera.virtuososdk.client.EngineObserver;
import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.Virtuoso;
import com.penthera.virtuososdk.support.exoplayer211.drm.SupportDrmSessionManager;
import com.penthera.virtuososdk.support.exoplayer211.ExoplayerUtils;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;


/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public class PlayerActivity extends AppCompatActivity implements OnClickListener, PlaybackPreparer,
        PlayerControlView.VisibilityListener {


    // Best practice is to ensure we have a Virtuoso instance available while playing segmented assets
    // as this will guarantee the proxy service remains available throughout.
    private Virtuoso mVirtuoso;

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

    private PlayerView playerView;
    private LinearLayout debugRootView;
    private Button selectTracksButton;
    private TextView debugTextView;
    private boolean isShowingTrackSelectionDialog;

    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private TrackGroupArray lastSeenTrackGroupArray;

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
            trackSelectorParameters = savedInstanceState.getParcelable(KEY_TRACK_SELECTOR_PARAMETERS);
            shouldAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY);
            startWindow = savedInstanceState.getInt(KEY_WINDOW);
            startPosition = savedInstanceState.getLong(KEY_POSITION);
        } else {
            trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder(this).build();
            clearStartPosition();
        }

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
        mVirtuoso.addObserver(new EngineObserver(){
            @Override
            public void proxyPortUpdated(){
                /*
                 * The proxy update occurs if the proxy needs to change port after a restart,
                 * which can occur if the app is placed in the background and then brought back to the foreground.
                 * In this case the player needs to be set back up to get the new base url.
                 */
                runOnUiThread(() -> {
                    Log.w(PlayerActivity.class.getSimpleName(), "Received warning about change in port, restarting player");
                    releasePlayer();
                    shouldAutoPlay = true;
                    initializePlayer();
                });
            }
        });
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
        outState.putParcelable(KEY_TRACK_SELECTOR_PARAMETERS, trackSelectorParameters);
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

    // PlaybackControlView.PlaybackPreparer implementation

    @Override
    public void preparePlayback() {
        player.retry();
    }

    // PlaybackControlView.VisibilityListener implementation

    @Override
    public void onVisibilityChange(int visibility) {
        debugRootView.setVisibility(visibility);
    }

    // Internal methods

    private void initializePlayer() {
        Intent intent = getIntent();

        IAsset asset = intent.getParcelableExtra(VIRTUOSO_ASSET);

        TrackSelection.Factory adaptiveTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory();
        trackSelector = new DefaultTrackSelector(this, adaptiveTrackSelectionFactory);
        trackSelector.setParameters(trackSelectorParameters);
        lastSeenTrackGroupArray = null;


        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this);


        if (player == null) {

            ExoplayerUtils.PlayerConfigOptions.Builder builder = new ExoplayerUtils.PlayerConfigOptions.Builder(this)
                    .userRenderersFactory(renderersFactory)
                    .withTrackSelector(trackSelector)
                    .withPlayerEventListener(new PlayerEventListener())
                    .withAnalyticsListener(new EventLogger(trackSelector))
                    .withPlaybackPreparer(this)
                    .playerWhenReady(true);

            //set drm options
            builder.drmOptions().withDrmSessionManagerEventListener(new DrmListener(this))
                .withMediaDrmEventListener(new MediaDrmOnEventListener());

            builder.mediaSourceOptions().useTransferListener(true)
                    .withUserAgent("virtuoso-sdk");


            boolean haveResumePosition = startWindow != C.INDEX_UNSET;
            if (haveResumePosition) {
                builder.withSeekToPosition(startWindow, startPosition);
            }

            try {

                player = ExoplayerUtils.setupPlayer(playerView, mVirtuoso, asset, false, builder.build());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return;
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

    /**
     * Simple example of Exoplayer EventListener, taken from ExoPlayer Demo.
     */
    private class PlayerEventListener implements Player.EventListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playbackState == Player.STATE_ENDED) {
                showControls();
            }
            updateButtonVisibilities();
        }

        // Error handling

        @Override
        public void onPositionDiscontinuity(@Player.DiscontinuityReason int reason) {
            if (inErrorState) {
                // This will only occur if the user has performed a seek whilst in the error state. Update
                // the resume position so that if the user then retries, playback will resume from the
                // position to which they seeked.
                updateStartPosition();
            }
        }

        @Override
        public void onPlayerError(@NonNull ExoPlaybackException e) {
            inErrorState = true;
            if (isBehindLiveWindow(e)) {
                clearStartPosition();
                initializePlayer();
            } else {
                updateButtonVisibilities();
                showControls();
            }
        }

        @Override
        @SuppressWarnings("ReferenceEquality")
        public void onTracksChanged(@NonNull TrackGroupArray trackGroups, @NonNull TrackSelectionArray trackSelections) {
            updateButtonVisibilities();
            if (trackGroups != lastSeenTrackGroupArray) {
                MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                if (mappedTrackInfo != null) {
                    if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO)
                            == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        showToast(R.string.error_unsupported_video);
                    }
                    if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_AUDIO)
                            == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        showToast(R.string.error_unsupported_audio);
                    }
                }
                lastSeenTrackGroupArray = trackGroups;
            }
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
    private static class DrmListener implements SupportDrmSessionManager.EventListener {

        private final PlayerActivity mActivity;

        public DrmListener(PlayerActivity activity) {
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
     * Demonstrates how to view media drm events directly, which we use for logging
     */
    @TargetApi(18)
    private static class MediaDrmOnEventListener implements MediaDrm.OnEventListener {
        @Override
        public void onEvent(@NonNull MediaDrm md, @Nullable byte[] sessionId, int event, int extra, @Nullable byte[] data) {
            Log.d("MediaDrm", "MediaDrm event: " + event);
        }
    }

    /**
     * From ExoPlayer demo: a message provide generates human readable error messages for internal error states.
     */
    private class PlayerErrorMessageProvider implements ErrorMessageProvider<ExoPlaybackException> {

        @NonNull
        @Override
        public Pair<Integer, String> getErrorMessage(ExoPlaybackException e) {
            String errorString = getString(R.string.error_generic);
            if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                Exception cause = e.getRendererException();
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
            }
            return Pair.create(0, errorString);
        }
    }
}

