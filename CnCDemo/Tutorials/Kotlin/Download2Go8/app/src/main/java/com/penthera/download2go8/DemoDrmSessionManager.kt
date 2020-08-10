/*
    Created by Penthera.
    Copyright © 2020 penthera. All rights reserved.

    This source file contains a very basic example showing how to use the Penthera Download2Go SDK.
    Pay close attention to code comments marked IMPORTANT
*/
package com.penthera.download2go8

import android.content.Context
import android.media.MediaDrm
import android.os.Handler
import android.os.Looper
import com.google.android.exoplayer2.drm.DrmInitData
import com.google.android.exoplayer2.drm.DrmSession
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.penthera.virtuososdk.client.IAsset
import com.penthera.virtuososdk.client.drm.IDrmInitData
import com.penthera.virtuososdk.client.drm.IVirtuosoDrmSession
import com.penthera.virtuososdk.client.drm.VirtuosoDrmSessionManager
import java.util.*

/**
 * The DemoDrmSessionManager manages creation of the Drm sessions by encapsulating the SDK provided
 * session manager. This mirrors the ExoPlayer DRM class design.
 * This file has minor changes between versions of ExoPlayer.
 */
class DemoDrmSessionManager(
    context: Context?,
    uuid: UUID?,
    asset: IAsset?,
    optionalKeyRequestParameters: HashMap<String?, String?>?,
    eventListener: VirtuosoDrmSessionManager.EventListener?,
    onEventListener: MediaDrm.OnEventListener
) : DrmSessionManager<FrameworkMediaCrypto> {




    private val mDrmSessionManager: VirtuosoDrmSessionManager
    private val mDrmEventListener: MediaDrm.OnEventListener
    private var eventHandler: Handler? = null

    init {
        // If using an eventListener then also provide a handler for calling the events on
        if (eventListener != null) {
            eventHandler = Handler()
        }
        mDrmSessionManager = VirtuosoDrmSessionManager(
            context, uuid, asset, optionalKeyRequestParameters,
            null, eventHandler, eventListener
        )
        mDrmEventListener = onEventListener
    }
    override fun canAcquireSession(drmInitData: DrmInitData): Boolean {
        return mDrmSessionManager.canOpen { schemeUuid ->
            val sd = drmInitData[schemeUuid]
            IDrmInitData.SchemeInitData(sd!!.mimeType, sd.data)
        }
    }

    override fun acquireSession(
        playbackLooper: Looper,
        drmInitData: DrmInitData
    ): DrmSession<FrameworkMediaCrypto> {
        val session: IVirtuosoDrmSession<*> = mDrmSessionManager.open { schemeUuid ->
            val sd = drmInitData[schemeUuid]
            IDrmInitData.SchemeInitData(sd!!.mimeType, sd.data)
        }
        session.setLooper(playbackLooper)
        session.setDrmOnEventListener(mDrmEventListener)
        return DemoDrmSession(session, mDrmSessionManager.schemeUUID, mDrmSessionManager)
    }

    override fun getExoMediaCryptoType(drmInitData: DrmInitData): Class<FrameworkMediaCrypto>? {
        /* ExoMediaCrypto is An opaque {@link android.media.MediaCrypto} equivalent. */
        return if (canAcquireSession(drmInitData)) FrameworkMediaCrypto::class.java else null
    }

}