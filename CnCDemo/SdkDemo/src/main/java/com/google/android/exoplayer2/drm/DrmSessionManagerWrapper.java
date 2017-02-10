package com.google.android.exoplayer2.drm;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.penthera.virtuososdk.client.IAsset;
import com.penthera.virtuososdk.client.drm.IDrmInitData;
import com.penthera.virtuososdk.client.drm.VirtuosoDrmSessionManager;

import java.util.HashMap;
import java.util.UUID;

/**
 * Wraps the VirtuosoDrmSessionManager
 */

public class DrmSessionManagerWrapper implements DrmSessionManager<FrameworkMediaCrypto>,
        DrmSession<FrameworkMediaCrypto> {

    private final VirtuosoDrmSessionManager mDrmSessionManager;

    public DrmSessionManagerWrapper(Context context,
                                     UUID uuid,
                                     IAsset asset,
                                     HashMap<String, String> optionalKeyRequestParameters,
                                     Looper playbackLooper,
                                     Handler eventHandler,
                                     VirtuosoDrmSessionManager.EventListener eventListener)  throws com.penthera.virtuososdk.client.drm.UnsupportedDrmException {
        mDrmSessionManager = new VirtuosoDrmSessionManager(context,uuid, asset,optionalKeyRequestParameters,
                playbackLooper,eventHandler,eventListener);
    }

    public DrmSessionManagerWrapper(Context context,
                                     UUID uuid,
                                     String remoteAssetId,
                                     HashMap<String, String> optionalKeyRequestParameters,
                                     Looper playbackLooper,
                                     Handler eventHandler,
                                     VirtuosoDrmSessionManager.EventListener eventListener)  throws com.penthera.virtuososdk.client.drm.UnsupportedDrmException {
        mDrmSessionManager = new VirtuosoDrmSessionManager(context,uuid,remoteAssetId,optionalKeyRequestParameters,
                playbackLooper,eventHandler,eventListener);
    }

    @Override
    public DrmSession<FrameworkMediaCrypto> acquireSession(Looper playbackLooper, final DrmInitData drmInitData) {
        mDrmSessionManager.setLooper(playbackLooper);
        mDrmSessionManager.open(new IDrmInitData() {
            @Override
            public SchemeInitData get(UUID schemeUuid) {
                DrmInitData.SchemeData sd = drmInitData.get(schemeUuid);
                return new SchemeInitData(sd.mimeType,sd.data);
            }
        });
        return this;
    }

    @Override
    public void releaseSession(DrmSession<FrameworkMediaCrypto> drmSession) {
        mDrmSessionManager.close();
    }

    @Override
    public int getState() {
        return mDrmSessionManager.getState();
    }

    @Override
    public FrameworkMediaCrypto getMediaCrypto() {
        return new FrameworkMediaCrypto(mDrmSessionManager.getMediaCrypto());
    }

    @Override
    public boolean requiresSecureDecoderComponent(String mimeType) {
        return mDrmSessionManager.requiresSecureDecoderComponent(mimeType);
    }

    @Override
    public Exception getError() {
        return mDrmSessionManager.getError();
    }
}
