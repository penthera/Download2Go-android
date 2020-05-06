package com.penthera.sdkdemokotlin.drm

import android.content.Context
import android.media.MediaCrypto
import android.media.MediaDrm
import android.os.Handler
import android.os.Looper
import com.google.android.exoplayer2.drm.*
import com.penthera.virtuososdk.client.IAsset
import com.penthera.virtuososdk.client.drm.IDrmInitData
import com.penthera.virtuososdk.client.drm.IVirtuosoDrmSession
import com.penthera.virtuososdk.client.drm.VirtuosoDrmSessionManager
import java.util.*

class DrmSessionManagerWrapper @Throws(com.penthera.virtuososdk.client.drm.UnsupportedDrmException::class)
        constructor(context: Context, uuid: UUID, asset: IAsset, optionalKeyRequestParameters: HashMap<String, String>,
                    eventListener: VirtuosoDrmSessionManager.EventListener?,
                    onEventListener: MediaDrm.OnEventListener) : DrmSessionManager<FrameworkMediaCrypto> {

    private val mDrmSessionManager: VirtuosoDrmSessionManager

    // This is optional if you wish to view the Drm events yourself
    private val mDrmEventListener: MediaDrm.OnEventListener? = onEventListener

    // If you are using the event listener, then create a handler and pass into the session manager to deliver
    // the listener messages
    private var eventHandler: Handler? = null

    init {
        // If using an eventListener then also provide a handler for calling the events on
        if (eventListener != null) {
            eventHandler = Handler()
        }
        mDrmSessionManager = VirtuosoDrmSessionManager(context,uuid, asset,optionalKeyRequestParameters,
                null,eventHandler,eventListener)
    }

    override fun canAcquireSession(drmInitData: DrmInitData): Boolean {
        return mDrmSessionManager.canOpen { schemeUuid ->

            var sd : DrmInitData.SchemeData? = null
            for( i in 0 until drmInitData.schemeDataCount){
                sd = drmInitData.get(i)

                if(sd.matches(schemeUuid))
                    break

                sd = null

            }

            sd?.let{IDrmInitData.SchemeInitData(it.mimeType, it.data)}

        }
    }

    override fun acquireSession(playbackLooper: Looper, drmInitData: DrmInitData): DrmSession<FrameworkMediaCrypto> {
        val session = mDrmSessionManager.open { schemeUuid ->

            val sd = getMatchingSchemeData(drmInitData,schemeUuid)
            IDrmInitData.SchemeInitData(sd?.mimeType, sd?.data)
        }
        session.setLooper(playbackLooper)
        session.setDrmOnEventListener(mDrmEventListener)

        return DrmSessionWrapper(session, mDrmSessionManager.schemeUUID, mDrmSessionManager)
    }

    private fun getMatchingSchemeData(drmInitData: DrmInitData, schemeUuid: UUID) : DrmInitData.SchemeData?{

        var ret : DrmInitData.SchemeData? = null

        for(i in 0 until drmInitData.schemeDataCount ){
            if(drmInitData.get(i).matches(schemeUuid)){
                ret = drmInitData.get(i)
                break
            }
        }

        return ret
    }

    override fun getExoMediaCryptoType(drmInitData: DrmInitData): Class<out ExoMediaCrypto>? {
        /* ExoMediaCrypto is An opaque {@link android.media.MediaCrypto} equivalent. */
        return if (canAcquireSession(drmInitData)) FrameworkMediaCrypto::class.java else null
    }

    internal class DrmSessionWrapper(private val drmSession: IVirtuosoDrmSession<MediaCrypto>,
                                     schemeUUID: UUID, private val drmSessionManager: VirtuosoDrmSessionManager) : DrmSession<FrameworkMediaCrypto> {

        private var mediaCrypto: FrameworkMediaCrypto? = null
        private var referenceCount = 0

        val virtuosoSession: IVirtuosoDrmSession<*>
            get() = drmSession

        init {
            mediaCrypto = FrameworkMediaCrypto(schemeUUID, drmSession.sessionId, false)
        }

        override fun getState(): Int {
            return drmSession.state
        }

        override fun getMediaCrypto(): FrameworkMediaCrypto? {
            return mediaCrypto
        }

        override fun getError(): DrmSession.DrmSessionException? {
            val e = drmSession.error
            return if (e != null) {
                DrmSession.DrmSessionException(e)
            } else null
        }

        override fun queryKeyStatus(): Map<String, String> {
            return drmSession.queryKeyStatus()
        }

        override fun getOfflineLicenseKeySetId(): ByteArray {
            return drmSession.licenseKeySetId
        }

        override fun acquire() {
            referenceCount++
        }

        override fun release() {
            if (--referenceCount == 0) {
                drmSessionManager.close(drmSession)
            }
        }
    }
}
