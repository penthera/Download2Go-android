package com.google.android.exoplayer2.drm

import android.content.Context
import android.media.MediaCrypto
import android.media.MediaDrm
import android.os.Handler
import android.os.Looper
import com.penthera.virtuososdk.client.IAsset
import com.penthera.virtuososdk.client.drm.IDrmInitData
import com.penthera.virtuososdk.client.drm.IVirtuosoDrmSession
import com.penthera.virtuososdk.client.drm.VirtuosoDrmSessionManager
import java.util.*

class DrmSessionManagerWrapper : DrmSessionManager<FrameworkMediaCrypto> {

    private val mDrmSessionManager: VirtuosoDrmSessionManager

    // This is optional if you wish to view the Drm events yourself
    private val mDrmEventListener: MediaDrm.OnEventListener

    @Throws(com.penthera.virtuososdk.client.drm.UnsupportedDrmException::class)
    constructor(context: Context,
                uuid: UUID,
                asset: IAsset,
                optionalKeyRequestParameters: HashMap<String, String>,
                playbackLooper: Looper,
                eventHandler: Handler,
                eventListener: VirtuosoDrmSessionManager.EventListener,
                onEventListener: MediaDrm.OnEventListener) {
        mDrmSessionManager = VirtuosoDrmSessionManager(context, uuid, asset, optionalKeyRequestParameters,
                playbackLooper, eventHandler, eventListener)
        mDrmEventListener = onEventListener
    }

    @Throws(com.penthera.virtuososdk.client.drm.UnsupportedDrmException::class)
    constructor(context: Context,
                uuid: UUID,
                remoteAssetId: String,
                optionalKeyRequestParameters: HashMap<String, String>,
                playbackLooper: Looper,
                eventHandler: Handler,
                eventListener: VirtuosoDrmSessionManager.EventListener,
                onEventListener: MediaDrm.OnEventListener) {
        mDrmSessionManager = VirtuosoDrmSessionManager(context, uuid, remoteAssetId, optionalKeyRequestParameters,
                playbackLooper, eventHandler, eventListener)
        mDrmEventListener = onEventListener
    }

    override fun canAcquireSession(drmInitData: DrmInitData): Boolean {
        return mDrmSessionManager.canOpen { schemeUuid ->

            var sd : DrmInitData.SchemeData? = null
            for( i in 0 until drmInitData.schemeDataCount){
                sd = drmInitData.get(i)

                if(sd.matches(schemeUuid))
                    break;

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

        return DrmSessionWrapper(session)
    }

    private fun getMatchingSchemeData(drmInitData: DrmInitData, schemeUuid: UUID) : DrmInitData.SchemeData?{

        var ret :DrmInitData.SchemeData? = null

        for(i in 0 until drmInitData.schemeDataCount ){
            if(drmInitData.get(i).matches(schemeUuid)){
                ret = drmInitData.get(i)
                break
            }
        }

        return ret;
    }

    override fun releaseSession(drmSession: DrmSession<FrameworkMediaCrypto>) {
        val sessionWrapper = drmSession as DrmSessionWrapper
        mDrmSessionManager.close(sessionWrapper.virtuosoSession)
    }

    internal class DrmSessionWrapper(private val drmSession: IVirtuosoDrmSession<MediaCrypto>) : DrmSession<FrameworkMediaCrypto> {
        private var mediaCrypto: FrameworkMediaCrypto? = null

        val virtuosoSession: IVirtuosoDrmSession<*>
            get() = drmSession

        init {
            mediaCrypto = FrameworkMediaCrypto(drmSession.mediaCrypto)
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
    }
}
