/*
    Created by Penthera.
    Copyright © 2020 penthera. All rights reserved.

    This source file contains a very basic example showing how to use the Penthera Download2Go SDK.
    Pay close attention to code comments marked IMPORTANT
*/
package com.penthera.download2go8

import com.google.android.exoplayer2.drm.DrmSession
import com.google.android.exoplayer2.drm.DrmSession.DrmSessionException
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.penthera.virtuososdk.client.drm.IVirtuosoDrmSession
import com.penthera.virtuososdk.client.drm.VirtuosoDrmSessionManager
import java.util.*

/**
 * The DemoDrmSession encapsulates and manages the Download2Go DrmSession object.
 * It is necessary to override the ExoPlayer DrmSession, and not use the default offline
 * session provided by ExoPlayer, in order to allow the SDK to fetch the pre-stored offline
 * license keys and manage expiry and refresh of those keys.
 * This file has minor changes between versions of ExoPlayer. In this case it contains a
 * reference count for the internal resource to allow multiple accesses; this was introduced in
 * ExoPlayer version 2.11
 */
class DemoDrmSession(
    private val virtuosoSession: IVirtuosoDrmSession<*>,
    schemeUUID: UUID?,
    drmSessionManager: VirtuosoDrmSessionManager?) : DrmSession<FrameworkMediaCrypto> {

    private var drmSessionManager: VirtuosoDrmSessionManager?
    private var mediaCrypto: FrameworkMediaCrypto? = null
    private var referenceCount = 0

    init {
        mediaCrypto = FrameworkMediaCrypto(schemeUUID!!, virtuosoSession.sessionId, false)
        this.drmSessionManager = drmSessionManager
    }

    override fun getState(): Int {
        return virtuosoSession.state
    }

    override fun getMediaCrypto(): FrameworkMediaCrypto? {
        return mediaCrypto
    }

    override fun getError(): DrmSessionException? {
        val e = virtuosoSession.error
        return e?.let { DrmSessionException(it) }
    }

    override fun queryKeyStatus(): Map<String, String>? {
        return virtuosoSession.queryKeyStatus()
    }

    override fun getOfflineLicenseKeySetId(): ByteArray? {
        return virtuosoSession.licenseKeySetId
    }

    override fun acquire() {
        referenceCount++
    }

    override fun release() {
        if (--referenceCount == 0) {
            drmSessionManager?.let{
                it.close(virtuosoSession)
            }

            drmSessionManager = null
        }
    }


}