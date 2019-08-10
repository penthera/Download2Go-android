package com.penthera.sdkdemokotlin.activity

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.Toast
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.catalog.ExampleCatalogItem
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
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), NavigationListener, OfflineVideoProvider {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    private lateinit var offlineEngine: OfflineVideoEngine
    private lateinit var serviceViewModel: VirtuosoServiceViewModel

    private var downloadStatus: Int = Common.EngineStatus.IDLE

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)

        offlineEngine = OfflineVideoEngine(this , baseContext.applicationContext)

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            val status =  offlineEngine.getVirtuoso().backplane?.authenticationStatus;
            if (status == Common.AuthenticationStatus.NOT_AUTHENTICATED) {
                showLogin()
            } else {
                supportFragmentManager
                        .beginTransaction()
                        .add(R.id.main_container, MainTabsFragment.newInstance(), "topTabs")
                        .commit()
            }
        }

        serviceViewModel = ViewModelProviders.of(this, VirtuosoServiceModelFactory(offlineEngine))
                .get(VirtuosoServiceViewModel::class.java)
        serviceViewModel.getEngineState().observe(this, Observer<VirtuosoEngineState>{
            downloadStatusTxt.text = serviceViewModel.getCurrentEngineStatusString()
            if (it != null) {
                updateThroughput(it.overallThroughput, it.currentThroughput)
                downloadStatus = it.downloadStatusInt
            }
            invalidateOptionsMenu()
        })
        downloadStatusTxt.text = serviceViewModel.getCurrentEngineStatusString()
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

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_login -> {

                return true
            }
            R.id.menu_logout -> {

                return true
            }
            R.id.menu_unregister -> {

                return true
            }
            R.id.menu_add_test_item -> {

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
                .add(R.id.main_container, AssetDetailFragment.newInstance(asset), "detailView")
                .addToBackStack("inbox_detail_view")
                .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun showCatalogDetailView(item: ExampleCatalogItem) {
        supportFragmentManager
                .beginTransaction()
                .add(R.id.main_container, AssetDetailFragment.newInstance(item), "catalog_detail_view")
                .addToBackStack("catalog_detail_view")
                .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun addFragment(fragment: Fragment, backStackName: String) {
        supportFragmentManager
                .beginTransaction()
                .add(R.id.main_container, fragment, backStackName)
                .addToBackStack(backStackName)
                .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun hideLogin() {

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.main_container, MainTabsFragment.newInstance(), "login")
                .commit()
        statusView.visibility = View.VISIBLE
        supportActionBar?.show()
    }

    fun showLogin() {
        supportFragmentManager
                .beginTransaction()
                .add(R.id.main_container, LoginFragment(), "login")
                .commit()
        statusView.visibility = View.GONE
        supportActionBar?.hide()
    }

    override fun showAddCatalogItemView(){

        addFragment(AddCatalogItemFragment(), "add catalog_item")

        statusView.visibility = View.GONE
        supportActionBar?.title = "Add item to catalog"

    }

    override fun getOfflineEngine(): OfflineVideoEngine = offlineEngine

    fun updateThroughput(overallThroughput: Double, currentThroughput: Double) {
        overallThroughputTxt.text = String.format("%.2f", overallThroughput)
        currentThroughputTxt.text = String.format("%.2f", currentThroughput)
    }
}
