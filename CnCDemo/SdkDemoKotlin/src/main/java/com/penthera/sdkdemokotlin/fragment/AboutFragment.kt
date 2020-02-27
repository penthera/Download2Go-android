package com.penthera.sdkdemokotlin.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.penthera.VirtuosoSDK
import com.penthera.sdkdemokotlin.R
import kotlinx.android.synthetic.main.fragment_about.*

/**
 * Basic "About" screen with version numbers for SDK.
 */
class AboutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var clientVersionString = ""
        try {
            clientVersionString = activity!!.packageManager.getPackageInfo(activity!!.packageName, 0).versionName
        } catch (e: Exception) {
        }


        // SDK Version
        virtuosoSdkVersion.text = String.format(getString(R.string.server_sdk_version), VirtuosoSDK.FULL_VERSION)

        // SDK Demo Version
        sdkDemoVerison.text = String.format(getString(R.string.client_version), clientVersionString)

        // Android SDK Version
        androidSdkVersion.text = String.format(getString(R.string.android_sdk_version), Build.VERSION.RELEASE)
    }
}