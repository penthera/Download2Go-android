package com.penthera.sdkdemokotlin.catalog

import com.squareup.moshi.Json
import java.io.Serializable

enum class CatalogItemType {
    @Json(name = "mp4") FILE,
    @Json(name = "hls") HLS_MANIFEST,
    @Json(name = "mpdash") DASH_MANIFEST
}

/**
 *
 */
data class ExampleCatalogItem (
        @Json(name = "id") val exampleAssetId: String,
        val title: String,
        val contentUri: String,
        val contentType: CatalogItemType,
        val mimeType: String,
        val description: String,
        val expiryDate: Long = -1,
        val downloadFrom: Long = 0,
        val expiryAfterPlay: Long = -1,
        val contentRating: String = "PG",
        val durationSeconds: Int,
        val imageUri: String
)