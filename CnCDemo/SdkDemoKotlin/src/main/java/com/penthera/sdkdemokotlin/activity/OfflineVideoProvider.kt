package com.penthera.sdkdemokotlin.activity

import com.penthera.sdkdemokotlin.engine.OfflineVideoEngine

/**
 *
 */
interface OfflineVideoProvider {
    fun getOfflineEngine(): OfflineVideoEngine

}