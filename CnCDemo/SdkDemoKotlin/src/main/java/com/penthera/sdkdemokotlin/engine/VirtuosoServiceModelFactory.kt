package com.penthera.sdkdemokotlin.engine

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider

/**
 * A simple view model factory to add the VirtuosoServiceViewModel
 */
class VirtuosoServiceModelFactory (private val offlineEngine: OfflineVideoEngine) :
        ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return VirtuosoServiceViewModel(offlineEngine) as T
    }
}