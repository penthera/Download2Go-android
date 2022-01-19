package com.penthera.sdkdemokotlin.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.penthera.VirtuosoSDK
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.databinding.FragmentAboutBinding

/**
 * Basic "About" screen with version numbers for SDK.
 */
class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var clientVersionString = ""
        try {
            clientVersionString = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0).versionName
        } catch (e: Exception) {
        }


        // SDK Version
        binding.virtuosoSdkVersion.text = String.format(getString(R.string.server_sdk_version), VirtuosoSDK.FULL_VERSION)

        // SDK Demo Version
        binding.sdkDemoVerison.text = String.format(getString(R.string.client_version), clientVersionString)

        // Android SDK Version
        binding.androidSdkVersion.text = String.format(getString(R.string.android_sdk_version), Build.VERSION.RELEASE)
    }
}