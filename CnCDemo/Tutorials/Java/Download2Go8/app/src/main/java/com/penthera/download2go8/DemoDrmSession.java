/*
    Created by Penthera.
    Copyright © 2020 penthera. All rights reserved.

    This source file contains a very basic example showing how to use the Penthera Download2Go SDK.
    Pay close attention to code comments marked IMPORTANT
*/
package com.penthera.download2go8;

import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.penthera.virtuososdk.client.drm.IVirtuosoDrmSession;
import com.penthera.virtuososdk.client.drm.VirtuosoDrmSessionManager;

import java.util.Map;
import java.util.UUID;

/**
 * The DemoDrmSession encapsulates and manages the Download2Go DrmSession object.
 * It is necessary to override the ExoPlayer DrmSession, and not use the default offline
 * session provided by ExoPlayer, in order to allow the SDK to fetch the pre-stored offline
 * license keys and manage expiry and refresh of those keys.
 * This file has minor changes between versions of ExoPlayer. In this case it contains a
 * reference count for the internal resource to allow multiple accesses; this was introduced in
 * ExoPlayer version 2.11
 */
public class DemoDrmSession  implements DrmSession<FrameworkMediaCrypto> {

    private final IVirtuosoDrmSession drmSession;
    private VirtuosoDrmSessionManager drmSessionManager;
    private FrameworkMediaCrypto mediaCrypto = null;
    private int referenceCount = 0;

    public DemoDrmSession(IVirtuosoDrmSession session, UUID schemeUUID, VirtuosoDrmSessionManager drmSessionManager) {
        drmSession = session;
        mediaCrypto = new FrameworkMediaCrypto(schemeUUID, session.getSessionId(), false);
        this.drmSessionManager = drmSessionManager;
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
