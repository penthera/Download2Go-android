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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.android.exoplayer2.BuildConfig;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ControlDispatcher;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackPreparer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManagerWrapper;
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
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.ui.TrackSelectionView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.penthera.sdkdemo.R;
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
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public class PlayerActivity extends Activity implements OnClickListener, PlaybackPreparer,
        PlayerControlView.VisibilityListener {

    // For backwards compatability
    public static final String DRM_SCHEME_UUID_EXTRA = "drm_scheme_uuid";

    // Best practice is to ensure we have a Virtuoso instance available while playing segmented assets
    // as this will guarantee the proxy service remains available throughout.
    private Virtuoso mVirtuoso;

    // DRM
    public static final String DRM_SCHEME_EXTRA = "drm_scheme";
    public static final String DRM_LICENSE_URL = "drm_license_url"; //  ??
    public static final String DRM_KEY_REQUEST_PROPERTIES = "drm_key_request_properties";

    public static final String VIRTUOSO_CONTENT_TYPE = "asset_type";
    public static final String VIRTUOSO_ASSET = "asset";
    public static final String VIRTUOSO_ASSET_ID = "asset_id";
    public static final String PREFER_EXTENSION_DECODERS = "prefer_extension_decoders";

    public static final String ACTION_VIEW = "com.penthera.harness.exoplayer.action.VIEW";

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private static final CookieManager DEFAULT_COOKIE_MANAGER;

    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private Handler mainHandler;
    private EventLogger eventLogger;
    private PlayerView playerView;
    private LinearLayout debugRootView;
    private TextView debugTextView;
    private Button retryButton;

    private DataSource.Factory mediaDataSourceFactory;
    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private DebugTextViewHelper debugViewHelper;
    private boolean playerNeedsSource;
    private boolean inErrorState;
    private TrackGroupArray lastSeenTrackGroupArray;
    private PlayerControlView customController;

    private boolean shouldAutoPlay;
    private int resumeWindow;
    private long resumePosition;

    private IVirtuosoAdManager adManager;
    private IServerDAIPackage serverDAIPackage;
    ImaAdsLoader adsLoader;

    // Activity lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVirtuoso = new Virtuoso(getApplicationContext());

        shouldAutoPlay = true;
        clearResumePosition();

        mediaDataSourceFactory = buildDataSourceFactory(true);
        mainHandler = new Handler();
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }

        setContentView(R.layout.player_activity);
        View rootView = findViewById(R.id.root);
        rootView.setOnClickListener(this);
        debugRootView = (LinearLayout) findViewById(R.id.controls_root);
        debugTextView = (TextView) findViewById(R.id.debug_text_view);
        retryButton = (Button) findViewById(R.id.retry_button);
        retryButton.setOnClickListener(this);

        playerView = findViewById(R.id.player_view);
        playerView.setControllerVisibilityListener(this);
        playerView.requestFocus();

        trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder().build();

        // In a real application you may want to create a custom controller or override the timebar.
        // You cannot find the controller by viewid as it is constructed internally in code, so
        // we find the timebar and then grab the parent instead for this demonstration.
        View progressView = playerView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_progress);
        customController = (PlayerControlView)(progressView.getParent().getParent().getParent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        releasePlayer();
        shouldAutoPlay = true;
        clearResumePosition();
        setIntent(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer();
        }
        mVirtuoso.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
        mVirtuoso.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializePlayer();
        } else {
            showToast(R.string.storage_permission_denied);
            finish();
        }
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
        if (view == retryButton) {
            initializePlayer();
        } else if (view.getParent() == debugRootView) {
            MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
            if (mappedTrackInfo == null) {
                return;
            }

            int rendererIndex = (int) view.getTag();
            String title = ((Button) view).getText().toString();
            int rendererType = mappedTrackInfo.getRendererType(rendererIndex);
            boolean allowAdaptiveSelections =
                    rendererType == C.TRACK_TYPE_VIDEO
                            || (rendererType == C.TRACK_TYPE_AUDIO
                            && mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO)
                            == MappedTrackInfo.RENDERER_SUPPORT_NO_TRACKS);
            Pair<AlertDialog, TrackSelectionView> dialogPair =
                    TrackSelectionView.getDialog(this, title, trackSelector, rendererIndex);
            dialogPair.second.setShowDisableOption(true);
            dialogPair.second.setAllowAdaptiveSelections(allowAdaptiveSelections);
            dialogPair.first.show();
        }
    }

    // PlaybackControlView.PlaybackPreparer implementation

    @Override
    public void preparePlayback() {
        initializePlayer();
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
        if (player == null) {

            TrackSelection.Factory adaptiveTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
            trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);
            trackSelector.setParameters(trackSelectorParameters);
            lastSeenTrackGroupArray = null;
            eventLogger = new EventLogger(trackSelector);

            DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;
            if (intent.hasExtra(DRM_SCHEME_UUID_EXTRA) || intent.hasExtra(DRM_SCHEME_EXTRA)) {

                String drmSchemeExtra = intent.hasExtra(DRM_SCHEME_EXTRA) ? DRM_SCHEME_EXTRA
                        : DRM_SCHEME_UUID_EXTRA;
                String drmUuid = intent.getStringExtra(drmSchemeExtra);
                String[] keyRequestPropertiesArray = intent.getStringArrayExtra(DRM_KEY_REQUEST_PROPERTIES);

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

                                drmSessionManager = buildDrmSessionManager(drmSchemeUuid,
                                        keyRequestPropertiesArray,
                                        asset, intent.getStringExtra(VIRTUOSO_ASSET_ID));
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



            boolean preferExtensionDecoders = intent.getBooleanExtra(PREFER_EXTENSION_DECODERS, false);
            @DefaultRenderersFactory.ExtensionRendererMode int extensionRendererMode =
                    BuildConfig.FLAVOR.equals("withExtensions")
                            ? (preferExtensionDecoders ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                            : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                            : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;


            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this,
                    drmSessionManager, extensionRendererMode);

            player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector);
            player.addListener(new PlayerEventListener());
            player.addListener(eventLogger);
            player.addMetadataOutput(eventLogger);
            player.addAudioDebugListener(eventLogger);
            player.addVideoDebugListener(eventLogger);
            player.setPlayWhenReady(shouldAutoPlay);

            playerView.setPlayer(player);
            playerView.setPlaybackPreparer(this);
            debugViewHelper = new DebugTextViewHelper(player, debugTextView);
            debugViewHelper.start();
            playerNeedsSource = true;
        }
        if (playerNeedsSource) {
            String action = intent.getAction();

            Uri uri = intent.getData();
            int type = intent.getIntExtra(VIRTUOSO_CONTENT_TYPE, Common.AssetIdentifierType.FILE_IDENTIFIER);
            if (!ACTION_VIEW.equals(action)) {
                showToast(getString(R.string.unexpected_intent_action, action));
                return;
            }
            // All our files are stored in the app private space so no need to check permissions after kitkat
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                if (Util.maybeRequestReadExternalStoragePermission(this, uri)) {
                    // The player will be reinitialized if the permission is granted.
                    return;
                }
            }
            MediaSource mediaSource = buildMediaSource(uri, type);
            playerNeedsSource = false;
            boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
            if (haveResumePosition) {
                player.seekTo(resumeWindow, resumePosition);
            }

            //simple video playback only requires the url of the relevant content

            if(asset != null) {//additional features require the asset to be passed in the intent extras

                if (asset.adSupport() == Common.AdSupportType.CLIENT_ADS) {

                    String adsResponse = null;


                    adManager = mVirtuoso.getAssetManager().getAdManager();
                    IVideoAdPackage adsPackage = adManager.fetchAdsForAsset(asset);

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

                    if (!TextUtils.isEmpty(adsResponse)) {
                        ImaAdsLoader.Builder builder = new ImaAdsLoader.Builder(this);
                        ImaSdkSettings settings = ImaSdkFactory.getInstance().createImaSdkSettings();
                        settings.setDebugMode(true);
                        builder.setImaSdkSettings(settings);
                        adsLoader = builder.buildForAdsResponse(adsResponse);
                        adsLoader.setPlayer(player);

                        AdsMediaSource mediaSourceWithAds = new AdsMediaSource(mediaSource, mediaDataSourceFactory, adsLoader, playerView);
                        player.prepare(mediaSourceWithAds, !haveResumePosition, false);
                    } else {
                        player.prepare(mediaSource, !haveResumePosition, false);
                    }
                } else {
                    player.prepare(mediaSource, !haveResumePosition, false);
                }

                if ( asset.adSupport() == Common.AdSupportType.SERVER_ADS) {
                    //Common.AdSupportType.SERVER_ADS indicates that the asset is using google server side ads
                    //This block of code is optional for applications that do not ue google server ads fpr their video content


                    // Set up a handler for string metadata from the stream, which contains media verification ids.
                    // These are posted back to the server for impression tracking.
                    adManager = mVirtuoso.getAssetManager().getAdManager();
                    serverDAIPackage = adManager.fetchServerAdsForAsset(asset);
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

                        playerView.setControlDispatcher(new ControlDispatcher() {
                            @Override
                            public boolean dispatchSetPlayWhenReady(Player player, boolean playWhenReady) {
                                player.setPlayWhenReady(playWhenReady);
                                return true;
                            }

                            @Override
                            public boolean dispatchSeekTo(Player player, int windowIndex, long positionMs) {
                                long newSeekPositionMs = positionMs;
                                if (serverDAIPackage != null) {
                                    IServerDAICuePoint prevCuePoint =
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

                        });
                    }

                }
            }


            inErrorState = false;
            updateButtonVisibilities();
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


    private MediaSource buildMediaSource(Uri uri, int type) {
        switch (type) {
            case ISegmentedAsset.SEG_FILE_TYPE_HSS:
                return new SsMediaSource.Factory(
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory),
                        buildDataSourceFactory(false))
                        .createMediaSource(uri, mainHandler, eventLogger);
            case ISegmentedAsset.SEG_FILE_TYPE_MPD:
                return new DashMediaSource.Factory(
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                        buildDataSourceFactory(false))
                        .createMediaSource(uri, mainHandler, eventLogger);
            case ISegmentedAsset.SEG_FILE_TYPE_HLS:
                return new HlsMediaSource.Factory(mediaDataSourceFactory)
                        .createMediaSource(uri, mainHandler, eventLogger);
            case Common.AssetIdentifierType.FILE_IDENTIFIER:
                return new ExtractorMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri,
                        mainHandler, eventLogger);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    private DrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManager(UUID uuid,
                                                                           String[] keyRequestPropertiesArray,
                                                                           IAsset asset,
                                                                           String assetId) throws UnsupportedDrmException {
        if (Util.SDK_INT < 18) {
            return null;
        }
        HashMap<String,String> keyRequestPropertiesMap = null;
        if (keyRequestPropertiesArray != null && keyRequestPropertiesArray.length > 0) {
            keyRequestPropertiesMap = new HashMap<String,String>();
            String key = null;
            for (int i=0; i<keyRequestPropertiesArray.length; i++) {
                if (i%2 == 0){
                    key = keyRequestPropertiesArray[i];
                } else {
                    String val = keyRequestPropertiesArray[i];
                    if (key != null && key.length() > 0 && val != null && val.length() > 0)
                        keyRequestPropertiesMap.put(key,val);
                }
            }
        }

        DrmListener drmListener = new DrmListener(this, eventLogger);

        if (asset == null) {
            return new DrmSessionManagerWrapper(getApplicationContext(), uuid,
                    assetId, keyRequestPropertiesMap, null, mainHandler, drmListener, eventLogger);
        } else {
            return new DrmSessionManagerWrapper(getApplicationContext(), uuid,
                    asset, keyRequestPropertiesMap, null, mainHandler, drmListener, eventLogger);
        }
    }

    private void releasePlayer() {
        if (player != null) {
            debugViewHelper.stop();
            debugViewHelper = null;
            shouldAutoPlay = player.getPlayWhenReady();
            updateResumePosition();
            player.release();
            player = null;
            trackSelector = null;
            eventLogger = null;
        }
    }

    private void updateResumePosition() {
        resumeWindow = player.getCurrentWindowIndex();
        resumePosition = Math.max(0, player.getContentPosition());
    }

    private void clearResumePosition() {
        resumeWindow = C.INDEX_UNSET;
        resumePosition = C.TIME_UNSET;
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
        debugRootView.removeAllViews();

        retryButton.setVisibility(playerNeedsSource ? View.VISIBLE : View.GONE);
        debugRootView.addView(retryButton);

        if (player == null) {
            return;
        }

        MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            return;
        }

        for (int i = 0; i < mappedTrackInfo.length; i++) {
            TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
            if (trackGroups.length != 0) {
                Button button = new Button(this);
                int label;
                switch (player.getRendererType(i)) {
                    case C.TRACK_TYPE_AUDIO:
                        label = R.string.audio;
                        break;
                    case C.TRACK_TYPE_VIDEO:
                        label = R.string.video;
                        break;
                    case C.TRACK_TYPE_TEXT:
                        label = R.string.text;
                        break;
                    default:
                        continue;
                }
                button.setText(label);
                button.setTag(i);
                button.setOnClickListener(this);
                debugRootView.addView(button, debugRootView.getChildCount() - 1);
            }
        }
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

    private class PlayerEventListener extends Player.DefaultEventListener {

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
                updateResumePosition();
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException e) {
            String errorString = null;
            if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                Exception cause = e.getRendererException();
                if (cause instanceof DecoderInitializationException) {
                    // Special case for decoder initialization failures.
                    DecoderInitializationException decoderInitializationException =
                            (DecoderInitializationException) cause;
                    if (decoderInitializationException.decoderName == null) {
                        if (decoderInitializationException.getCause() instanceof DecoderQueryException) {
                            errorString = getString(R.string.error_querying_decoders);
                        } else if (decoderInitializationException.secureDecoderRequired) {
                            errorString = getString(R.string.error_no_secure_decoder,
                                    decoderInitializationException.mimeType);
                        } else {
                            errorString = getString(R.string.error_no_decoder,
                                    decoderInitializationException.mimeType);
                        }
                    } else {
                        errorString = getString(R.string.error_instantiating_decoder,
                                decoderInitializationException.decoderName);
                    }
                }
            }
            if (errorString != null) {
                showToast(errorString);
            }
            inErrorState = true;
            if (isBehindLiveWindow(e)) {
                clearResumePosition();
                initializePlayer();
            } else {
                updateResumePosition();
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
                    if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
                            == MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        showToast(R.string.error_unsupported_video);
                    }
                    if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
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
        clearResumePosition();
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

    private static class DrmListener implements VirtuosoDrmSessionManager.EventListener {

        private PlayerActivity mActivity;
        private EventLogger mLogger;

        public DrmListener(PlayerActivity activity, EventLogger logger) {
            mActivity = activity;
            mLogger = logger;
        }

        @Override
        public void onDrmKeysLoaded() {
            mLogger.onDrmKeysLoaded();
        }

        @Override
        public void onDrmSessionManagerError(Exception e) {
            // Can't complete playback
            mActivity.handleDrmLicenseNotAvailable();
            mLogger.onDrmSessionManagerError(e);
        }
    }
}

