//Copyright (c) 2017 Penthera Partners, LLC. All rights reserved.
//
//PENTHERA CONFIDENTIAL
//
//(c) 2015 Penthera Partners Inc. All Rights Reserved.
//
//NOTICE: This file is the property of Penthera Partners Inc.
//The concepts contained herein are proprietary to Penthera Partners Inc.
//and may be covered by U.S. and/or foreign patents and/or patent
//applications, and are protected by trade secret or copyright law.
//Distributing and/or reproducing this information is forbidden
//unless prior written permission is obtained from Penthera Partners Inc.
//
package com.penthera.sdkdemo.drm;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaDrm;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.drm.UUIDS;
import com.penthera.virtuososdk.client.drm.VirtuosoDrmInitData;
import com.penthera.virtuososdk.client.drm.IVirtuosoDrmSession;
import com.penthera.virtuososdk.client.drm.VirtuosoDrmSessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Wraps the VirtuosoDrmSessionManager
 */
@TargetApi(18)
public class DrmSessionManagerWrapper implements DrmSessionManager<FrameworkMediaCrypto> {

    private final VirtuosoDrmSessionManager mDrmSessionManager;

    // This is optional if you wish to view the Drm events yourself
    private final MediaDrm.OnEventListener mDrmEventListener;

    // If you are using the event listener, then create a handler and pass into the session manager to deliver
    // the listener messages
    private Handler eventHandler = null;

    public DrmSessionManagerWrapper(Context context,
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
    public boolean canAcquireSession(final com.google.android.exoplayer2.drm.DrmInitData drmInitData) {

        ArrayList<VirtuosoDrmInitData.SchemeInitData> virtuosoSchemeDatas = new ArrayList<>();
        for (int i=0; i<drmInitData.schemeDataCount; i++) {
            com.google.android.exoplayer2.drm.DrmInitData.SchemeData sd = drmInitData.get(i);
            if (sd.matches(UUIDS.WIDEVINE_UUID)) {  // only supports widevine
                virtuosoSchemeDatas.add(new VirtuosoDrmInitData.SchemeInitData(UUIDS.WIDEVINE_UUID, sd.mimeType, sd.data));
            }
        }

        return mDrmSessionManager.canOpen(new VirtuosoDrmInitData(virtuosoSchemeDatas));
    }

    @Override
    public DrmSession<FrameworkMediaCrypto> acquireSession(Looper playbackLooper, final com.google.android.exoplayer2.drm.DrmInitData drmInitData) {
        ArrayList<VirtuosoDrmInitData.SchemeInitData> virtuosoSchemeDatas = new ArrayList<>();
        for (int i=0; i<drmInitData.schemeDataCount; i++) {
            com.google.android.exoplayer2.drm.DrmInitData.SchemeData sd = drmInitData.get(i);
            if (sd.matches(UUIDS.WIDEVINE_UUID)) {  // only supports widevine
                virtuosoSchemeDatas.add(new VirtuosoDrmInitData.SchemeInitData(UUIDS.WIDEVINE_UUID, sd.mimeType, sd.data));
            }
        }

        IVirtuosoDrmSession virtuosoDrmSession = mDrmSessionManager.open(new VirtuosoDrmInitData(virtuosoSchemeDatas));
        virtuosoDrmSession.setLooper(playbackLooper);
        virtuosoDrmSession.setDrmOnEventListener(mDrmEventListener);

        return new DrmSessionWrapper(virtuosoDrmSession, mDrmSessionManager.getSchemeUUID(), mDrmSessionManager);
    }

    @Override
    @Nullable
    public Class<FrameworkMediaCrypto> getExoMediaCryptoType(com.google.android.exoplayer2.drm.DrmInitData drmInitData) {
        /* ExoMediaCrypto is An opaque {@link android.media.MediaCrypto} equivalent. */
        return canAcquireSession(drmInitData)
                ? FrameworkMediaCrypto.class
                : null;
    }

    /**
     *
     */
    static class DrmSessionWrapper implements DrmSession<FrameworkMediaCrypto> {

       /** DrmSession that will provide the offline license */
        private final IVirtuosoDrmSession drmSession;

        /** Link to the session manager, used to close the session when finished */
        private VirtuosoDrmSessionManager drmSessionManager;

        /** The underlying media crypto object used by the DRM */
        private FrameworkMediaCrypto mediaCrypto = null;

        /** Exoplayer users a reference count in the acquire/release of the session */
        private int referenceCount = 0;

        public DrmSessionWrapper(IVirtuosoDrmSession session, UUID schemeUUID, VirtuosoDrmSessionManager drmSessionManager) {
            drmSession = session;
            mediaCrypto = new FrameworkMediaCrypto(schemeUUID, session.getSessionId(), false);
            this.drmSessionManager = drmSessionManager;
        }

        @Override
        public int getState() {
            return drmSession.getState();
        }

        @Override
        public FrameworkMediaCrypto getMediaCrypto() {
            return mediaCrypto;
        }

        @Override
        public DrmSessionException getError() {
            Exception e = drmSession.getError();
            if (e != null) {
                // Wrap the exception into the exoplayer exception type
                return new DrmSessionException(e);
            }
            return null;
        }

        @Override
        public Map<String, String> queryKeyStatus() {
            return drmSession.queryKeyStatus();
        }

        @Override
        public byte[] getOfflineLicenseKeySetId() {
            return drmSession.getLicenseKeySetId();
        }

        @Override
        public void acquire() {
            referenceCount++;
        }

        @Override
        public void release() {
            if (--referenceCount == 0){
                drmSessionManager.close(drmSession);
                drmSessionManager = null;
            }
        }
    }
}
