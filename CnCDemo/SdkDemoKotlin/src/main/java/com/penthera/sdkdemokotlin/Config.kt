package com.penthera.sdkdemokotlin

import android.content.Context
import android.content.pm.PackageManager

/**
 *
 */
class Config {

    companion object {
        /** The backplane base URL  */
        val BACKPLANE_URL = "https://qa.penthera.com/"

        //SdkDemo Keys
        /** The backplane public key.  NOTE: YOU MUST REPLACE THIS VALUE WITH YOUR OWN PENTHERA-ASSIGNED KEYS  */
        val BACKPLANE_PUBLIC_KEY = "c9adba5e6ceeed7d7a5bfc9ac24197971bbb4b2c34813dd5c674061a961a899e"

        /** The backplane private key.  NOTE: YOU MUST REPLACE THIS VALUE WITH YOUR OWN PENTHERA-ASSIGNED KEYS  */
        val BACKPLANE_PRIVATE_KEY = "41cc269275e04dcb4f2527b0af6e0ea11d227319fa743e4364255d07d7ed2830"

        /** Test download  */
        val SMALL_DOWNLOAD = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"

        /** Test download  */
        val SMALL_DOWNLOAD2 = "http://d22oovgqp3p1ij.cloudfront.net/WVGA5/Onion_News_Network/Behind_The_Pen-_The_Chinese_Threat_21-May-2012_14-30-00_GMT.mp4"

        /** Within this number of days a warning is shown on the catalog detail page  */
        val EXPIRY_WARNING_DAYS = 730L

        fun getAuthority(context: Context): String {
            var authority: String = ""
            try {
                val ai = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                val b = ai.metaData
                val auth = b.getString("com.penthera.virtuososdk.client.pckg")
                authority = auth?: ""
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }

            return authority
        }
    }
}