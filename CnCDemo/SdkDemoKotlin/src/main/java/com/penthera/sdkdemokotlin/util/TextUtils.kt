package com.penthera.sdkdemokotlin.util

import com.penthera.virtuososdk.Common
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

/**
 *
 */
class TextUtils {

    /**
     * Convert seconds to human readable hours, minutes and seconds
     *
     * @param seconds
     *
     * @return human readable hours, minutes, seconds string
     */
    fun getDurationString(seconds: Int): String {
        var secondsVal = seconds
        val hours = secondsVal / 3600
        val minutes = secondsVal % 3600 / 60
        secondsVal = seconds % 60
        return twoDigitString(hours) + " : " + twoDigitString(minutes) + " : " + twoDigitString(secondsVal)
    }

    /**
     * Create a two digit time [prepend 0s to the time]
     *
     * @param number the number to pad
     *
     * @return two digit time as string
     */
    fun twoDigitString(number: Int): String {
        if (number == 0) {
            return "00"
        }
        return if (number / 10 == 0) {
            "0$number"
        } else number.toString()
    }

    /**
     * Return a string description for the download status of an asset
     */
    fun getAssetStatusDescription(status: Int) : String {

        var value : String
        when (status) {
            Common.AssetStatus.MANIFEST_PARSE_PENDING -> value = "parse pending"

            Common.AssetStatus.MANIFEST_PARSING -> value = "parsing"

            Common.AssetStatus.DOWNLOADING -> value = "downloading"

            Common.AssetStatus.EARLY_DOWNLOADING -> value = "downloading"

            Common.AssetStatus.DOWNLOAD_COMPLETE -> value = "complete"

            Common.AssetStatus.DOWNLOAD_PAUSED -> value = "paused"

            Common.AssetStatus.EXPIRED -> value = "expired"

            Common.AssetStatus.DOWNLOAD_FILE_COPY_ERROR -> value = "io error"

            Common.AssetStatus.DOWNLOAD_FILE_MIME_MISMATCH -> value = "error on mime"

            Common.AssetStatus.DOWNLOAD_FILE_SIZE_MISMATCH -> value = "error on size"

            Common.AssetStatus.DOWNLOAD_NETWORK_ERROR -> value = "network error"

            Common.AssetStatus.DOWNLOAD_REACHABILITY_ERROR -> value = "unreachable"

            Common.AssetStatus.MANIFEST_REACHABILITY_ERROR -> value = "manifest unreachable"

            Common.AssetStatus.MANIFEST_PARSING_ERROR -> value = "parsing error"

            Common.AssetStatus.DOWNLOAD_DENIED_ASSET -> value = "DENIED : MAD"

            Common.AssetStatus.DOWNLOAD_DENIED_ACCOUNT -> value = "DENIED : MDA"

            Common.AssetStatus.DOWNLOAD_DENIED_EXTERNAL_POLICY -> value = "DENIED : EXT"

            Common.AssetStatus.DOWNLOAD_DENIED_MAX_DEVICE_DOWNLOADS -> value = "DENIED :MPD"

            Common.AssetStatus.DOWNLOAD_DENIED_COPIES -> value = "DENIED : COPIES"

            Common.AssetStatus.DOWNLOAD_BLOCKED_AWAITING_PERMISSION -> value = "AWAITING PERMISSION"

            else -> value = "pending"
        }

        return value
    }

    /**
     * Returns a string value using pretty time for the expiration time, or null otherwise.
     */
    fun getExpirationString(completionTime: Long, endWindow: Long, firstPlayTime: Long, expiryAfterPlay: Long, expiryAfterDownload: Long) : String? {

        var result : String? = null
        val expirationTime = getExpiration(completionTime, endWindow, firstPlayTime, expiryAfterPlay, expiryAfterDownload)
        if (expirationTime > 0) {
            result = makePrettyTime(expirationTime)
        }

        return result
    }

    /**
     * Calculate expiration time from all the parameters
     */
    fun getExpiration(completionTime: Long, endWindow: Long, firstPlayTime: Long, expiryAfterPlay: Long, expiryAfterDownload: Long): Long {

        if (completionTime == 0L) {
            // Not downloaded
            return if (endWindow == java.lang.Long.MAX_VALUE) -1 else endWindow
        } else {
            // Downloaded
            //here the minimum value is used in the calculation.
            var playExpiry = java.lang.Long.MAX_VALUE
            var expiry = endWindow

            if (firstPlayTime > 0 && expiryAfterPlay > -1)
                playExpiry = firstPlayTime + expiryAfterPlay

            expiry = Math.min(expiry, playExpiry)

            if (expiryAfterDownload > -1)
                expiry = Math.min(expiry, completionTime + expiryAfterDownload)

            return if (expiry == java.lang.Long.MAX_VALUE) -1 else expiry
        }
    }

    /**
     * Displays times like "2 Days from now"
     *
     * @param value timestamp in milliseconds
     *
     * @return Pretty time, never if -1 passeed
     */
    private fun makePrettyTime(value: Long): String {
        var timeVal = value
        timeVal *= 1000
        val p = PrettyTime()
        return p.format(Date(value))
    }

}