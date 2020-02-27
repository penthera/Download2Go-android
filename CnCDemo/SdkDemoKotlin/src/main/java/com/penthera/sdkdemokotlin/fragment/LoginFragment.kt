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
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.IAssetManager
import com.penthera.virtuososdk.client.Observers
import kotlinx.android.synthetic.main.fragment_login.*
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        continueButton.setOnClickListener(this)

        val identifier = offlineVideoProvider?.getOfflineEngine()?.getVirtuoso()?.backplane?.settings?.deviceId

        edt_user.setText(identifier)
        edt_url.setText(Config.BACKPLANE_URL)

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onAttach(context: Context?) {
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

            offlineVideoProvider?.getOfflineEngine()?.getVirtuoso()?.let{
                it.startup(backplaneUrl, user, null, Config.BACKPLANE_PUBLIC_KEY, Config.BACKPLANE_PRIVATE_KEY) { _, _ ->
                    // TODO:  Set up push messaging
        //                    if (pushService == Common.PushService.FCM_PUSH && errorCode != ConnectionResult.SUCCESS) {
        //                        val gApi = GoogleApiAvailability.getInstance()
        //                        if (gApi.isUserResolvableError(errorCode)) {
        //
        //                            runOnUiThread(Runnable {
        //                                gApi.makeGooglePlayServicesAvailable(context)
        //                                        .addOnCompleteListener(object:OnCompleteListener<Void> {
        //                                            override fun onComplete(task:Task<Void>) {
        //                                                Log.d(TAG, "makeGooglePlayServicesAvailable complete")
        //                                                mGcmRegistered = true
        //                                                if (task.isSuccessful()) {
        //                                                    Log.d(TAG, "makeGooglePlayServicesAvailable completed successfully")
        //                                                } else {
        //                                                    val e = task.getException()
        //                                                    Log.e(TAG, "makeGooglePlayServicesAvailable completed with exception " + e!!.message, e)
        //                                                }
        //                                                if (mRegistered) {
        //                                                    Util.startActivity(this@SplashActivity, MainActivity::class.java, null)
        //                                                }
        //                                            }
        //                                        })
        //                            })
        //
        //                        }
        //                    } else {
        //                        mGcmRegistered = true
        //                        if (mRegistered) {
        //                            Util.startActivity(this@SplashActivity, MainActivity::class.java, null)
        //                        }
        //                    }
                }
// TODO:  add a progress dialog?
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
        var url = edt_url.text.toString()
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
        var user = edt_user.text.toString()
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

        override fun requestComplete(request: Int, result: Int, errorMessage: String) {
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