package com.penthera.sdkdemokotlin.engine

import androidx.lifecycle.LiveData
import com.penthera.virtuososdk.client.IService
import com.penthera.virtuososdk.client.ServiceException


/**
 *
 */
class VirtuosoEngineStateLiveData(virtuosoService: IService) : LiveData<VirtuosoEngineState>() {

    val virtuosoService: IService = virtuosoService

    override fun onActive() {
        super.onActive()
        virtuosoService.bind()
    }

    override fun onInactive() {
        super.onInactive()
        virtuosoService.unbind()
    }

    fun doUpdate() {
        if (virtuosoService.isBound) {

            try {
                postValue(VirtuosoEngineState(virtuosoService.status,
                        virtuosoService.overallThroughput,
                        virtuosoService.currentThroughput,
                        virtuosoService.windowedThroughput))
            } catch (se: ServiceException) {

            }
        }
    }
}