package com.penthera.sdkdemokotlin.activity

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.catalog.ExampleCatalogItem
import com.penthera.sdkdemokotlin.databinding.ActivityMainBinding
import com.penthera.sdkdemokotlin.engine.OfflineVideoEngine
import com.penthera.sdkdemokotlin.engine.VirtuosoEngineState
import com.penthera.sdkdemokotlin.engine.VirtuosoServiceModelFactory
import com.penthera.sdkdemokotlin.engine.VirtuosoServiceViewModel
import com.penthera.sdkdemokotlin.fragment.AddCatalogItemFragment
import com.penthera.sdkdemokotlin.fragment.AssetDetailFragment
import com.penthera.sdkdemokotlin.fragment.LoginFragment
import com.penthera.sdkdemokotlin.fragment.MainTabsFragment
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.IAsset

class MainActivity : AppCompatActivity(), NavigationListener, OfflineVideoProvider {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    private lateinit var offlineEngine: OfflineVideoEngine
    private lateinit var serviceViewModel: VirtuosoServiceViewModel

    private var downloadStatus: Int = Common.EngineStatus.IDLE

    private lateinit var binding: ActivityMainBinding

    private var mainTabs: MainTabsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)

        offlineEngine = OfflineVideoEngine(this , baseContext.applicationContext)

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            val status =  offlineEngine.getVirtuoso().backplane?.authenticationStatus;
            if (status == Common.AuthenticationStatus.NOT_AUTHENTICATED) {
                showLogin()
            } else {
                supportFragmentManager
                        .beginTransaction()
                        .add(binding.mainContainer.id, MainTabsFragment.newInstance(), "topTabs")
                        .commit()
            }
        }

        serviceViewModel =  ViewModelProvider(this, VirtuosoServiceModelFactory(offlineEngine)).get(VirtuosoServiceViewModel::class.java)
        serviceViewModel.getEngineState().observe(this, Observer<VirtuosoEngineState>{
            binding.downloadStatusTxt.text = serviceViewModel.getCurrentEngineStatusString()
            if (it != null) {
                updateThroughput(it.overallThroughput, it.currentThroughput)
                downloadStatus = it.downloadStatusInt
            }
            invalidateOptionsMenu()
        })
        binding.downloadStatusTxt.text = serviceViewModel.getCurrentEngineStatusString()
        downloadStatus = serviceViewModel.getEngineState().value?.downloadStatusInt ?: Common.EngineStatus.IDLE
    }

    // Initialize the main menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onPrepareOptionsMenu(menu: Menu) : Boolean
    {
        super.onPrepareOptionsMenu(menu)
        var mi: MenuItem? = menu.findItem(R.id.menu_pause_resume)
        if (mi != null) {

            mi.isEnabled = !offlineEngine.pauseRequested && !offlineEngine.resumeRequested
            if (offlineEngine.resumeRequested || offlineEngine.pauseRequested) {
                mi.title = "changing...."
            } else {
                mi.title = getString(if (downloadStatus == Common.EngineStatus.PAUSED) R.string.resume else R.string.pause)
            }

        }

        var loginMi: MenuItem? = menu.findItem(R.id.menu_login)
        var logoutMi: MenuItem? = menu.findItem(R.id.menu_logout)

        val authenticationStatus: Int = offlineEngine.getVirtuoso().backplane?.authenticationStatus
                ?: Common.AuthenticationStatus.NOT_AUTHENTICATED

        val isShutdown = authenticationStatus == Common.AuthenticationStatus.SHUTDOWN

        loginMi?.isVisible = isShutdown
        logoutMi?.isVisible = !isShutdown

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_login -> {
                offlineEngine.loginAccount()
                invalidateOptionsMenu()
                return true
            }
            R.id.menu_logout -> {
                offlineEngine.shutdownEngine()
                invalidateOptionsMenu()
                return true
            }
            R.id.menu_unregister -> {
                offlineEngine.unregisterAccount()
                invalidateOptionsMenu()
                // transition to login fragment after calling unregister
                showLogin()
                return true
            }
            R.id.menu_add_test_item -> {
                showAddCatalogItemView()
                return true
            }
            R.id.menu_pause_resume -> {
                var ok = offlineEngine.pauseResumeDownloads(getString(R.string.pause).equals(item.title.toString()))
                if (!ok) {
                    Log.w(TAG, "Could not change pause/resume state")
                }
                return true
            }
            android.R.id.home -> {
                super.onBackPressed()
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                supportActionBar?.title = getString(R.string.app_name)
                return true;
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun showInboxDetailsView(asset: IAsset) {
        supportFragmentManager
                .beginTransaction()
                .add(binding.mainContainer.id, AssetDetailFragment.newInstance(asset), "detailView")
                .addToBackStack("inbox_detail_view")
                .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun showCatalogDetailView(item: ExampleCatalogItem) {
        supportFragmentManager
                .beginTransaction()
                .add(binding.mainContainer.id, AssetDetailFragment.newInstance(item), "catalog_detail_view")
                .addToBackStack("catalog_detail_view")
                .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun addFragment(fragment: Fragment, backStackName: String) {
        supportFragmentManager
                .beginTransaction()
                .add(binding.mainContainer.id, fragment, backStackName)
                .addToBackStack(backStackName)
                .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun hideLogin() {

        if (mainTabs == null) {
            mainTabs = MainTabsFragment.newInstance()
        }
        mainTabs?.let {
            supportFragmentManager
                    .beginTransaction()
                    .replace(binding.mainContainer.id, it, "login")
                    .commit()
        }
        binding.statusView.visibility = View.VISIBLE
        supportActionBar?.show()
    }

    fun showLogin() {
        supportFragmentManager
                .beginTransaction()
                .add(binding.mainContainer.id, LoginFragment(), "login")
                .commit()
        binding.statusView.visibility = View.GONE
        supportActionBar?.hide()
    }

    override fun showAddCatalogItemView(){

        addFragment(AddCatalogItemFragment(), "add catalog_item")

        binding.statusView.visibility = View.GONE
        supportActionBar?.title = "Add item to catalog"

    }

    override fun getOfflineEngine(): OfflineVideoEngine = offlineEngine

    fun updateThroughput(overallThroughput: Double, currentThroughput: Double) {
        binding.overallThroughputTxt.text = String.format("%.2f", overallThroughput)
        binding.currentThroughputTxt.text = String.format("%.2f", currentThroughput)
    }
}
