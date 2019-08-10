package com.penthera.sdkdemokotlin.engine

/**
 *
 */
data class VirtuosoEngineState (
        val downloadStatusInt: Int,
        val overallThroughput: Double,
        val currentThroughput: Double,
        val windowedThroughput: Double
)