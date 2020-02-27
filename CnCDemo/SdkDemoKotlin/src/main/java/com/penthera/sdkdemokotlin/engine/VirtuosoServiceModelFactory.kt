package com.penthera.sdkdemokotlin.engine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * A simple view model factory to add the VirtuosoServiceViewModel
 */
@Suppress("UNCHECKED_CAST")
class VirtuosoServiceModelFactory (private val offlineEngine: OfflineVideoEngine) :
        ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return VirtuosoServiceViewModel(offlineEngine) as T
    }
}