package com.penthera.sdkdemokotlin.catalog

import org.json.JSONException
import org.json.JSONObject

/**
 * A simple class to demonstrate storing and retrieving metadata against the SDK asset.
 */
class ExampleMetaData(title: String = "", thumbnailUri: String = "") {

    companion object {
        /** The image thumbnail  */
        @JvmField val IMAGE_THUMBNAIL_URL = "image_thumbnail_url"

        /** The title  */
        @JvmField val TITLE = "title"
    }

    var title: String = title

    var thumbnailUri: String = thumbnailUri

    /**
     * Turn the current meta data into a JSON string
     * @return JSON string
     */
    fun toJson(): String {
        val obj = JSONObject()
        try {
            obj.put(TITLE, title)
            obj.put(IMAGE_THUMBNAIL_URL, thumbnailUri)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return obj.toString()
    }

    fun fromJson(jsonString: String) : ExampleMetaData {
        try {
            val obj = JSONObject(jsonString)
            title = obj.getString(TITLE)
            thumbnailUri = obj.getString(IMAGE_THUMBNAIL_URL)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return this
    }

}