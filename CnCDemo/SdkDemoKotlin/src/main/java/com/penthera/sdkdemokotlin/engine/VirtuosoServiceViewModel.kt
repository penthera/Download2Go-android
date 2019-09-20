package com.penthera.sdkdemokotlin.engine

import android.arch.lifecycle.ViewModel
import com.penthera.virtuososdk.Common


/**
 *
 */
class VirtuosoServiceViewModel(private val offlineVideoEngine: OfflineVideoEngine) : ViewModel() {

    private val engineStateListener: VirtuosoEngineStateLiveData = offlineVideoEngine.getEngineState()

    fun getCurrentEngineStatusString(): String {
        var state : String
        when(engineStateListener.value?.downloadStatusInt) {
            Common.EngineStatus.AUTH_FAILURE -> state = "AUTH_FAILURE"
            Common.EngineStatus.ERROR -> state = "ERROR"
            Common.EngineStatus.BLOCKED -> state = "BLOCKED"
            Common.EngineStatus.DISABLED -> state = "DISABLED"
            Common.EngineStatus.PAUSED -> state = "PAUSED"
            else -> state = "OKAY"
        }
        return state
    }

    fun getEngineState() : VirtuosoEngineStateLiveData {
        return engineStateListener
    }
}