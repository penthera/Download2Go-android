package com.penthera.sdkdemokotlin.engine

import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.database.Cursor
import com.penthera.virtuososdk.client.Virtuoso

/**
 *
 */
class VirtuosoQueuesViewModel(private val context: Context, val virtuoso: Virtuoso) : ViewModel() {

    var downloadedAssetsQueueLiveData: VirtuosoQueueLiveData
    var queuedAssetsQueueLiveData: VirtuosoQueueLiveData
    var expiredAssetsQueueLiveData: VirtuosoQueueLiveData

    val combinedQueuesLiveData: MediatorLiveData<List<Cursor?>> = MediatorLiveData()

    init {
        downloadedAssetsQueueLiveData = VirtuosoQueueLiveData(context, virtuoso.assetManager.downloaded.CONTENT_URI())
        queuedAssetsQueueLiveData = VirtuosoQueueLiveData(context, virtuoso.assetManager.queue.CONTENT_URI())
        expiredAssetsQueueLiveData = VirtuosoQueueLiveData(context, virtuoso.assetManager.expired.CONTENT_URI())

        combinedQueuesLiveData.addSource(downloadedAssetsQueueLiveData, Observer { combinedQueuesLiveData.value = createCombinedList() })
        combinedQueuesLiveData.addSource(queuedAssetsQueueLiveData, Observer { combinedQueuesLiveData.value = createCombinedList() })
        combinedQueuesLiveData.addSource(expiredAssetsQueueLiveData, Observer { combinedQueuesLiveData.value = createCombinedList() })
    }

    private fun createCombinedList(): List<Cursor?> {
        return listOf(downloadedAssetsQueueLiveData.value, queuedAssetsQueueLiveData.value, expiredAssetsQueueLiveData.value)
    }

    override fun onCleared() {
        super.onCleared()
        downloadedAssetsQueueLiveData.value?.close()
        queuedAssetsQueueLiveData.value?.close()
        expiredAssetsQueueLiveData.value?.close()
    }
}