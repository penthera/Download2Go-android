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
package com.google.android.exoplayer2.drm;

import android.content.Context;
import android.media.MediaDrm;
import android.media.MediaCrypto;
import android.os.Handler;
import android.os.Looper;

import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.drm.IDrmInitData;
import com.penthera.virtuososdk.client.drm.IVirtuosoDrmSession;
import com.penthera.virtuososdk.client.drm.VirtuosoDrmSessionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Wraps the VirtuosoDrmSessionManager
 */

public class DrmSessionManagerWrapper implements DrmSessionManager<FrameworkMediaCrypto> {

    private final VirtuosoDrmSessionManager mDrmSessionManager;

    // This is optional if you wish to view the Drm events yourself
    private final MediaDrm.OnEventListener mDrmEventListener;

    public DrmSessionManagerWrapper(Context context,
                                     UUID uuid,
                                     IAsset asset,
                                     HashMap<String, String> optionalKeyRequestParameters,
                                     Looper playbackLooper,
                                     Handler eventHandler,
                                     VirtuosoDrmSessionManager.EventListener eventListener,
                                     MediaDrm.OnEventListener onEventListener)  throws com.penthera.virtuososdk.client.drm.UnsupportedDrmException {
        mDrmSessionManager = new VirtuosoDrmSessionManager(context,uuid, asset,optionalKeyRequestParameters,
                playbackLooper,eventHandler,eventListener);
        mDrmEventListener = onEventListener;
    }

    public DrmSessionManagerWrapper(Context context,
                                     UUID uuid,
                                     String remoteAssetId,
                                     HashMap<String, String> optionalKeyRequestParameters,
                                     Looper playbackLooper,
                                     Handler eventHandler,
                                     VirtuosoDrmSessionManager.EventListener eventListener,
                                     MediaDrm.OnEventListener onEventListener)  throws com.penthera.virtuososdk.client.drm.UnsupportedDrmException {
        mDrmSessionManager = new VirtuosoDrmSessionManager(context,uuid,remoteAssetId,optionalKeyRequestParameters,
                playbackLooper,eventHandler,eventListener);
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
        IVirtuosoDrmSession<MediaCrypto> session = mDrmSessionManager.open(new IDrmInitData() {
            @Override
            public SchemeInitData get(UUID schemeUuid) {
                DrmInitData.SchemeData sd = drmInitData.get(schemeUuid);
                return new SchemeInitData(sd.mimeType,sd.data);
            }
        });
        session.setLooper(playbackLooper);
        session.setDrmOnEventListener(mDrmEventListener);

        return new DrmSessionWrapper(session);
    }

    @Override
    public void releaseSession(DrmSession<FrameworkMediaCrypto> drmSession) {
        DrmSessionWrapper sessionWrapper = (DrmSessionWrapper) drmSession;
        mDrmSessionManager.close(sessionWrapper.getVirtuosoSession());
    }

    static class DrmSessionWrapper implements DrmSession<FrameworkMediaCrypto> {

        private final IVirtuosoDrmSession<MediaCrypto> drmSession;
        private FrameworkMediaCrypto mediaCrypto = null;

        public DrmSessionWrapper(IVirtuosoDrmSession<MediaCrypto> session) {
            drmSession = session;
            mediaCrypto = new FrameworkMediaCrypto(drmSession.getMediaCrypto());
        }

        public IVirtuosoDrmSession getVirtuosoSession() {
            return drmSession;
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
    }
}
