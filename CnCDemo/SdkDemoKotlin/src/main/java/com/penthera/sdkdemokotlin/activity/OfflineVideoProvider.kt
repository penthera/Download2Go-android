package com.penthera.sdkdemokotlin.activity

import com.penthera.sdkdemokotlin.engine.OfflineVideoEngine

/**
 *
 */
interface OfflineVideoProvider {

    open fun getOfflineEngine(): OfflineVideoEngine

}