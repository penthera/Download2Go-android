/*
    Created by Penthera.
    Copyright © 2020 penthera. All rights reserved.

    This source file contains a very basic example showing how to use the Penthera Download2Go SDK.
    Pay close attention to code comments marked IMPORTANT
*/
package com.penthera.download2go8;

import android.content.Context;
import android.media.MediaDrm;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.drm.IDrmInitData;
import com.penthera.virtuososdk.client.drm.IVirtuosoDrmSession;
import com.penthera.virtuososdk.client.drm.VirtuosoDrmSessionManager;

import java.util.HashMap;
import java.util.UUID;

/**
 * The DemoDrmSessionManager manages creation of the Drm sessions by encapsulating the SDK provided
 * session manager. This mirrors the ExoPlayer DRM class design.
 * This file has minor changes between versions of ExoPlayer.
 */
public class DemoDrmSessionManager implements DrmSessionManager<FrameworkMediaCrypto> {

    private final VirtuosoDrmSessionManager mDrmSessionManager;
    private final MediaDrm.OnEventListener mDrmEventListener;
    private Handler eventHandler = null;

    public DemoDrmSessionManager(Context context,
                                    UUID uuid,
                                    IAsset asset,
                                    HashMap<String, String> optionalKeyRequestParameters,
                                    VirtuosoDrmSessionManager.EventListener eventListener,
                                    MediaDrm.OnEventListener onEventListener)  throws com.penthera.virtuososdk.client.drm.UnsupportedDrmException {
        // If using an eventListener then also provide a handler for calling the events on
        if (eventListener != null) {
            eventHandler = new Handler();
        }
        mDrmSessionManager = new VirtuosoDrmSessionManager(context,uuid, asset,optionalKeyRequestParameters,
                null,eventHandler,eventListener);
        mDrmEventListener = onEventListener;
    }

    @Override
    public boolean canAcquireSession(final DrmInitData drmInitData) {
        return mDrmSessionManager.canOpen(new IDrmInitData() {
            @Override
            public SchemeInitData get(UUID schemeUuid) {
                DrmInitData.SchemeData sd = drmInitData.get(schemeUuid);
                return new SchemeInitData(sd.mimeType, sd.data);
            }
        });
    }

    @Override
    public DrmSession<FrameworkMediaCrypto> acquireSession(Looper playbackLooper, final DrmInitData drmInitData) {
        IVirtuosoDrmSession session = mDrmSessionManager.open(new IDrmInitData() {
            @Override
            public SchemeInitData get(UUID schemeUuid) {
                DrmInitData.SchemeData sd = drmInitData.get(schemeUuid);
                return new SchemeInitData(sd.mimeType,sd.data);
            }
        });
        session.setLooper(playbackLooper);
        session.setDrmOnEventListener(mDrmEventListener);

        return new DemoDrmSession(session, mDrmSessionManager.getSchemeUUID(), mDrmSessionManager);
    }

    @Override
    @Nullable
    public Class<FrameworkMediaCrypto> getExoMediaCryptoType(DrmInitData drmInitData) {
        /* ExoMediaCrypto is An opaque {@link android.media.MediaCrypto} equivalent. */
        return canAcquireSession(drmInitData)
                ? FrameworkMediaCrypto.class
                : null;
    }

}
