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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaDrm;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
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

import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.android.exoplayer2.BuildConfig;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ControlDispatcher;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackPreparer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.ads.AdsMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.DebugTextViewHelper;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Util;
import com.penthera.sdkdemo.R;
import com.penthera.sdkdemo.drm.DrmSessionManagerWrapper;
import com.penthera.virtuososdk.Common;
import com.penthera.virtuososdk.ads.VirtuosoAdScheduling;
import com.penthera.virtuososdk.ads.vast.VirtuosoVideoAd;
import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.ISegmentedAsset;
import com.penthera.virtuososdk.client.Virtuoso;
import com.penthera.virtuososdk.client.ads.IServerDAICuePoint;
import com.penthera.virtuososdk.client.ads.IServerDAIPackage;
import com.penthera.virtuososdk.client.ads.IVideoAdPackage;
import com.penthera.virtuososdk.client.ads.IVirtuosoAdManager;
import com.penthera.virtuososdk.client.drm.UnsupportedDrmException;
import com.penthera.virtuososdk.client.drm.VirtuosoDrmSessionManager;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.List;
import java.util.UUID;

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

    private final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter.Builder(null).build();
    private static final CookieManager DEFAULT_COOKIE_MANAGER;

    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private ProxyUpdateListener receiver;

    private PlayerView playerView;
    private LinearLayout debugRootView;
    private Button selectTracksButton;
    private TextView debugTextView;
    private boolean isShowingTrackSelectionDialog;

    private DataSource.Factory mediaDataSourceFactory;
    private SimpleExoPlayer player;
    private MediaSource mediaSource;
    private DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;
    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private TrackGroupArray lastSeenTrackGroupArray;

    private DebugTextViewHelper debugViewHelper;
    private boolean inErrorState;

    private PlayerControlView customController;

    private boolean shouldAutoPlay;
    private int startWindow;
    private long startPosition;

    // Fields for Ad playback
    ImaAdsLoader adsLoader;
    private IVirtuosoAdManager adManager;
    private IServerDAIPackage serverDAIPackage;

    // Activity lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVirtuoso = new Virtuoso(getApplicationContext());

        mediaDataSourceFactory = buildDataSourceFactory(true);
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }

        setContentView(R.layout.player_activity);
        debugRootView = (LinearLayout) findViewById(R.id.controls_root);
        debugTextView = (TextView) findViewById(R.id.debug_text_view);
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
            trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder().build();
            clearStartPosition();
        }

        // In a real application you may want to create a custom controller or override the timebar.
        // You cannot find the controller by view id as it is constructed internally in code, so
        // we find the timebar and then grab the parent instead for this demonstration.
        View progressView = playerView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_progress);
        customController = (PlayerControlView)(progressView.getParent().getParent().getParent());

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
        String auth = "com.penthera.virtuososdk.provider.sdkdemo";
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
    public void onSaveInstanceState(Bundle outState) {
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
        ISegmentedAsset segmentedAsset = null;
        IAsset asset = intent.getParcelableExtra(VIRTUOSO_ASSET);
        if (asset != null && asset instanceof ISegmentedAsset) {
            segmentedAsset = (ISegmentedAsset) asset;
        }

        TrackSelection.Factory adaptiveTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory();
        trackSelector = new DefaultTrackSelector(this, adaptiveTrackSelectionFactory);
        trackSelector.setParameters(trackSelectorParameters);
        lastSeenTrackGroupArray = null;

        if (player == null) {

            if (segmentedAsset != null && segmentedAsset.isContentProtected()) {

                String drmUuid = segmentedAsset.contentProtectionUuid();
                if (drmUuid != null) {

                    int errorStringId = R.string.error_drm_unknown;
                    if (Util.SDK_INT < 18) {
                        errorStringId = R.string.error_drm_not_supported;
                    } else {
                        try {

                        UUID drmSchemeUuid = null;
                        if (!TextUtils.isEmpty(drmUuid))
                            drmSchemeUuid = Util.getDrmUuid(drmUuid);

                        if (drmSchemeUuid != null) {
                            drmSessionManager = buildDrmSessionManager(drmSchemeUuid, segmentedAsset);
                        } else {
                            errorStringId = R.string.error_drm_unsupported_scheme;
                        }
                        } catch (UnsupportedDrmException e) {
                            errorStringId = e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                                    ? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown;
                        }
                    }
                    if (drmSessionManager == null) {
                        showToast(errorStringId);
                        return;
                    }
                }
            }

            @DefaultRenderersFactory.ExtensionRendererMode int extensionRendererMode =
                    BuildConfig.FLAVOR.equals("withExtensions")
                            ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                            : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;


            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this)
                    .setExtensionRendererMode(extensionRendererMode);

            player =  new SimpleExoPlayer.Builder(this, renderersFactory)
                    .setTrackSelector(trackSelector)
                    .setBandwidthMeter(BANDWIDTH_METER)
                    .build();
            player.addListener(new PlayerEventListener());
            player.addAnalyticsListener(new EventLogger(trackSelector));
            player.setPlayWhenReady(shouldAutoPlay);

            playerView.setPlayer(player);
            playerView.setPlaybackPreparer(this);
            debugViewHelper = new DebugTextViewHelper(player, debugTextView);
            debugViewHelper.start();

            String action = intent.getAction();

            Uri uri = intent.getData();
            int type = intent.getIntExtra(VIRTUOSO_CONTENT_TYPE, Common.AssetIdentifierType.FILE_IDENTIFIER);
            if (!ACTION_VIEW.equals(action)) {
                showToast(getString(R.string.unexpected_intent_action, action));
                return;
            }

            mediaSource = buildMediaSource(uri, type);

            if (segmentedAsset != null && segmentedAsset.adSupport() == Common.AdSupportType.CLIENT_ADS) {

                String adsResponse = null;
                if (asset != null) {
                    adManager = mVirtuoso.getAssetManager().getAdManager();
                    IVideoAdPackage adsPackage = adManager.fetchAdsForAsset(segmentedAsset);
                    if (adsPackage != null) {


                        if (adsPackage.getAds().size() > 1 //multiple vast ads
                                && TextUtils.isEmpty(((VirtuosoVideoAd) adsPackage.getAds().get(0)).getAdScheduling().getBreakType())) {//ad scheduling is not provided
                            //exoplayer does not like the ads xml that is generated by multiple vast ads without any
                            //scheduling info.   We take the first 2 ads in the collection and run them as pre and post roll

                            //TODO:  allow apps to customize this ad scheduling

                            VirtuosoVideoAd preroll = (VirtuosoVideoAd) adsPackage.getAds().get(0);
                            VirtuosoAdScheduling presched = new VirtuosoAdScheduling();
                            presched.setAdSourceId(preroll.getAdId());
                            presched.setAllowMultiple(false);
                            presched.setBreakId("preroll");
                            presched.setBreakType("linear");
                            presched.setFollowRedirects(true);
                            presched.setTimeOffset("start");
                            preroll.setAdScheduling(presched);

                            VirtuosoVideoAd postroll = (VirtuosoVideoAd) adsPackage.getAds().get(1);
                            VirtuosoAdScheduling postsched = new VirtuosoAdScheduling();
                            postsched.setAdSourceId(postroll.getAdId());
                            postsched.setAllowMultiple(false);
                            postsched.setBreakId("postroll");
                            postsched.setBreakType("linear");
                            postsched.setFollowRedirects(true);
                            postsched.setTimeOffset("end");
                            postroll.setAdScheduling(postsched);


                            while (adsPackage.getAds().size() > 2) {//remove other ads
                                adsPackage.getAds().remove(2);
                            }
                        }


                        adsResponse = adsPackage.getInlineAdResponse();

                    }
                }

                if (!TextUtils.isEmpty(adsResponse)) {
                    ImaAdsLoader.Builder builder = new ImaAdsLoader.Builder(this);
                    ImaSdkSettings settings = ImaSdkFactory.getInstance().createImaSdkSettings();
                    settings.setDebugMode(true);
                    builder.setImaSdkSettings(settings);
                    adsLoader = builder.buildForAdsResponse(adsResponse);
                    adsLoader.setPlayer(player);

                    AdsMediaSource mediaSourceWithAds = new AdsMediaSource(mediaSource, mediaDataSourceFactory, adsLoader, playerView);
                    if (mediaSourceWithAds != null) {
                        mediaSource = mediaSourceWithAds;
                    }
                }
            }
        }

        if (segmentedAsset != null && segmentedAsset.adSupport() == Common.AdSupportType.SERVER_ADS) {
			//Common.AdSupportType.SERVER_ADS indicates that the asset is using google server side ads
            //This block of code is optional for applications that do not ue google server ads fpr their video content

            // Set up a handler for string metadata from the stream, which contains media verification ids.
            // These are posted back to the server for impression tracking.
            adManager = mVirtuoso.getAssetManager().getAdManager();
            serverDAIPackage = adManager.fetchServerAdsForAsset(segmentedAsset);
            if (serverDAIPackage != null) {

                player.addMetadataOutput(new MetadataOutput() {
                    @Override
                    public void onMetadata(Metadata metadata) {
                        for (int i = 0; i < metadata.length(); i++) {
                            Metadata.Entry entry = metadata.get(i);
                            if (entry instanceof TextInformationFrame) {
                                TextInformationFrame textFrame = (TextInformationFrame) entry;
                                if ("TXXX".equals(textFrame.id)) {
                                    String value = textFrame.value;
                                    Log.d("Player", "Received user text meta: " + value);

                                    // Google DAI - tracking ids all start with "google_"
                                    if (value.startsWith("google_")) {
                                        serverDAIPackage.sendMediaVerificationId(value, player.getCurrentPosition());
                                    }

                                }
                            }
                        }
                    }
                });

                updateAdTimebarMarkers();
                serverDAIPackage.useAutomaticCueMarks(true);

                playerView.setControlDispatcher(new DemoControlDispatcher());
            }
        }

        inErrorState = false;

        boolean haveResumePosition = startWindow != C.INDEX_UNSET;
        if (haveResumePosition) {
            player.seekTo(startWindow, startPosition);
        }
        player.prepare(mediaSource, !haveResumePosition, false);
        updateButtonVisibilities();
    }


    private MediaSource buildMediaSource(Uri uri, int type) {
        switch (type) {
            case ISegmentedAsset.SEG_FILE_TYPE_HSS: {
                SsMediaSource.Factory factory = new SsMediaSource.Factory(
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory),
                        buildDataSourceFactory(false));
                if (drmSessionManager != null) {
                    factory.setDrmSessionManager(drmSessionManager);
                }
                return factory.createMediaSource(uri);
            }
            case ISegmentedAsset.SEG_FILE_TYPE_MPD: {
                DashMediaSource.Factory factory = new DashMediaSource.Factory(
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                        buildDataSourceFactory(false));
                if (drmSessionManager != null) {
                    factory.setDrmSessionManager(drmSessionManager);
                }
                return factory.createMediaSource(uri);
            }
            case ISegmentedAsset.SEG_FILE_TYPE_HLS: {
                HlsMediaSource.Factory factory = new HlsMediaSource.Factory(mediaDataSourceFactory);
                if (drmSessionManager != null) {
                    factory.setDrmSessionManager(drmSessionManager);
                }
                return factory.createMediaSource(uri);
            }
            case Common.AssetIdentifierType.FILE_IDENTIFIER:
                return new ExtractorMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    private void updateAdTimebarMarkers(){
        List<IServerDAICuePoint> cuePoints = serverDAIPackage.getCuePoints();

        int sz = cuePoints.size();
        long[] startTimes = new long[sz];
        boolean[] played = new boolean[sz];
        for (int i=0; i<sz; i++) {
            IServerDAICuePoint pt = cuePoints.get(i);
            startTimes[i] = pt.getStartTime();
            played[i] = pt.isPlayed();
        }
        customController.setExtraAdGroupMarkers(startTimes, played);
    }

    private DrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManager(UUID uuid,
                                                                           ISegmentedAsset segmentedAsset) throws UnsupportedDrmException {
        if (Util.SDK_INT < 18) {
            return null;
        }

        DrmListener drmListener = new DrmListener(this);
        MediaDrmOnEventListener mediaDrmOnEventListener = new MediaDrmOnEventListener();

        return new DrmSessionManagerWrapper(getApplicationContext(), uuid, segmentedAsset, null, drmListener, mediaDrmOnEventListener);
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
            if (adsLoader != null) {
                adsLoader.release();
                adsLoader = null;
            }
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

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *                          DataSource factory.
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return new DefaultDataSourceFactory(getApplicationContext(), useBandwidthMeter ? BANDWIDTH_METER : null,
                buildHttpDataSourceFactory(useBandwidthMeter));
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *                          DataSource factory.
     * @return A new HttpDataSource factory.
     */
    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return new DefaultHttpDataSourceFactory("virtuoso-sdk", useBandwidthMeter ? BANDWIDTH_METER : null);
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
        public void onPlayerError(ExoPlaybackException e) {
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
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
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

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog alertDialog = new AlertDialog.Builder(PlayerActivity.this).create();
                alertDialog.setTitle("License unavailable");
                alertDialog.setMessage("License for offline playback expired and renew is unavailable.");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        PlayerActivity.this.finish();
                    }
                });
                alertDialog.show();
            }
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
     * We override the control dispatcher in order to demonstrate limiting the ability to seek past
     * adverts.
     */
    class DemoControlDispatcher implements ControlDispatcher {
        @Override
        public boolean dispatchSetPlayWhenReady(Player player, boolean playWhenReady) {
            player.setPlayWhenReady(playWhenReady);
            return true;
        }

        /**
         * Simplified control over seeking - will only block a seek past the beginning of an ad block.
         * @param player The player
         * @param windowIndex The current window index
         * @param positionMs The requested position to seek.
         * @return true to indicate the seek was dispatched
         */
        @Override
        public boolean dispatchSeekTo(Player player, int windowIndex, long positionMs) {
            long newSeekPositionMs = positionMs;
            if (serverDAIPackage != null) {
                IServerDAICuePoint prevCuePoint  =
                        serverDAIPackage.getPreviousCuePointForStreamTime(positionMs);
                if (prevCuePoint != null && !prevCuePoint.isPlayed()) {
                    newSeekPositionMs = (long) (prevCuePoint.getStartTime());
                }
            }
            player.seekTo(windowIndex, newSeekPositionMs);
            return true;
        }

        @Override
        public boolean dispatchSetRepeatMode(Player player, int repeatMode) {
            return false;
        }

        @Override
        public boolean dispatchSetShuffleModeEnabled(Player player,
                                                     boolean shuffleModeEnabled) {
            return false;
        }

        @Override
        public boolean dispatchStop(Player player, boolean reset) {
            player.stop(reset);
            return true;
        }
    }

    /**
     * Demonstrates how to use an observer class from the Download2Go session manager. This
     * enables the client to be informed of events for when keys are loaded or an error occurs
     * with fetching a license.
     */
    private static class DrmListener implements VirtuosoDrmSessionManager.EventListener {

        private PlayerActivity mActivity;

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
    private class PlayerErrorMessageProvider implements ErrorMessageProvider<ExoPlaybackException> {

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

