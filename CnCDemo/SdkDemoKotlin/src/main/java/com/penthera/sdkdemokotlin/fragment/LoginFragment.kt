package com.penthera.sdkdemokotlin.fragment

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.penthera.sdkdemokotlin.Config
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.activity.NavigationListener
import com.penthera.sdkdemokotlin.activity.OfflineVideoProvider
import com.penthera.sdkdemokotlin.databinding.FragmentLoginBinding
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.IAssetManager
import com.penthera.virtuososdk.client.Observers
import java.lang.IllegalArgumentException
import java.net.MalformedURLException
import java.net.URL

/**
 *
 */
class LoginFragment : Fragment(), View.OnClickListener {

    companion object {
        private val TAG = LoginFragment::class.java.simpleName
    }

    private var navigationListener: NavigationListener? = null
    private var offlineVideoProvider: OfflineVideoProvider? = null
    private var assetManager: IAssetManager? = null

    private var _binding: FragmentLoginBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.continueButton.setOnClickListener(this)

        val identifier = offlineVideoProvider?.getOfflineEngine()?.getVirtuoso()?.backplane?.settings?.deviceId

        binding.edtUser.setText(identifier)
        binding.edtUrl.setText(Config.BACKPLANE_URL)

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navigationListener = activity as NavigationListener
        offlineVideoProvider = activity as OfflineVideoProvider
        assetManager = offlineVideoProvider?.getOfflineEngine()?.getVirtuoso()?.assetManager

    }

    override fun onResume() {
        super.onResume()
        offlineVideoProvider?.getOfflineEngine()?.getVirtuoso()?.addObserver(backplaneObserver)
    }

    override fun onPause() {
        super.onPause()
        offlineVideoProvider?.getOfflineEngine()?.getVirtuoso()?.removeObserver(backplaneObserver)
    }

    override fun onClick(v: View?) {
        try {
            val backplaneUrl = getUrl()
            val user = getUser()
            if (TextUtils.isEmpty(user)) {
                throw IllegalArgumentException("Missing username")
            }

            offlineVideoProvider?.getOfflineEngine()?.getVirtuoso()?.let {
                it.startup(backplaneUrl, user, null, Config.BACKPLANE_PUBLIC_KEY, Config.BACKPLANE_PRIVATE_KEY) { _, _ ->
                    // THIS IS WHERE WE WOULD SET UP PUSH MESSAGING
                }
            }

        } catch (e: Exception) {
            Toast.makeText(context, getString(R.string.error_login_details), Toast.LENGTH_LONG).show()
            Log.d(TAG, "Failed login ", e)
        }
    }

    /**
     * Get URL from editor
     *
     * @return
     * @throws MalformedURLException
     */
    @Throws(MalformedURLException::class)
    private fun getUrl(): URL {
        var url = binding.edtUrl.text.toString()
        if (TextUtils.isEmpty(url)) {
            url = Config.BACKPLANE_URL
        }
        return URL(url)
    }

    /**
     * get user from editor
     *
     * @return
     */
    private fun getUser(): String {
        var user = binding.edtUser.text.toString()
        if (TextUtils.isEmpty(user)) {
            user = offlineVideoProvider?.getOfflineEngine()?.getVirtuoso()?.backplane?.settings?.deviceId
                    ?: ""
        }
        return user
    }

    /**
     * Observe the registration request
     */
    private val backplaneObserver = object : Observers.IBackplaneObserver {

        /**
         * Registration request succeeded
         *
         * @param request
         */
        private fun handleSuccess(request: Int) {
            when (request) {
                Common.BackplaneCallbackType.SYNC -> {
                    Log.i(TAG, "sync")
                    navigationListener?.hideLogin()
                }

                Common.BackplaneCallbackType.REGISTER -> {
                    Log.i(TAG, "registered")

                }

                Common.BackplaneCallbackType.VALIDATE -> {
                    Log.i(TAG, "validated")

                }
            }
        }

        /**
         * Registration request failed
         *
         * @param result
         */
        private fun handleFailure(result: Int) {
            if (result == Common.BackplaneResult.INVALID_CREDENTIALS) {
                showAlertDialog(R.string.invalid_credentials, R.string.invalid_credentials)
            } else {
                showAlertDialog(R.string.generic_problem, R.string.generic_problem)
            }
        }

        private fun showAlertDialog(title: Int, body: Int) {
            val ad = AlertDialog.Builder(context).create()
            ad.setTitle(title)
            ad.setMessage(getString(body))
            ad.show()
        }

        override fun requestComplete(request: Int, result: Int, errorMessage: String?) {
            activity?.runOnUiThread {
                if (result == Common.BackplaneResult.SUCCESS || request == Common.BackplaneCallbackType.SYNC) {
                    handleSuccess(request)
                } else {
                    handleFailure(result)
                }
            }
        }
    }

}