package com.penthera.sdkdemokotlin.engine

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider

/**
 * A simple view model factory to add the VirtuosoQueueViewModel
 */
class VirtuosoQueueViewModelFactory (private val offlineEngine: OfflineVideoEngine) :
        ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return VirtuosoQueuesViewModel(offlineEngine.context, offlineEngine.getVirtuoso()) as T
    }
}