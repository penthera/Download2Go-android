package com.penthera.download2go1_6

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.penthera.common.Common.AssetStatus
import com.penthera.common.Common.AuthenticationStatus
import com.penthera.virtuososdk.client.IPushRegistrationObserver
import com.penthera.virtuososdk.client.ISegmentedAsset
import com.penthera.virtuososdk.client.ISegmentedAssetFromParserObserver
import com.penthera.virtuososdk.client.Virtuoso
import com.penthera.virtuososdk.client.builders.HLSAssetBuilder
import com.penthera.virtuososdk.client.builders.MPDAssetBuilder
import com.penthera.virtuososdk.client.database.AssetColumns
import java.net.URL
import java.util.*

class MainActivity : AppCompatActivity() ,
    LoaderManager.LoaderCallbacks<Cursor>{

    private lateinit var  virtuoso : Virtuoso

    private lateinit var dlBtn1 : Button
    private lateinit var dlBtn2 : Button
    private lateinit var dlBtn3 : Button

    private lateinit var recyclerView : RecyclerView
    private lateinit var assetAdapter : AssetRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        virtuoso = Virtuoso(this)

        ASSET_TITLE_1 = getString(R.string.download1_name)
        ASSET_TITLE_2 = getString(R.string.download2_name)
        ASSET_TITLE_3 = getString(R.string.download3_name)


        dlBtn1 = findViewById(R.id.download_1)
        dlBtn2= findViewById(R.id.download_2)
        dlBtn3 = findViewById(R.id.download_3)

        dlBtn1.setOnClickListener{downloadAsset(0)}
        dlBtn2.setOnClickListener{downloadAsset(1)}
        dlBtn3.setOnClickListener{downloadAsset(2)}

        recyclerView = findViewById(R.id.downloads_list)
        assetAdapter = AssetRecyclerAdapter(this)
        recyclerView.apply {
            adapter = assetAdapter
            layoutManager = LinearLayoutManager(this@MainActivity, VERTICAL, false)
            addItemDecoration(DividerItemDecoration(this@MainActivity, VERTICAL))
        }

        LoaderManager.getInstance(this).initLoader(LOADER_ID,null, this)
    }

    private fun initVirtuosoSDK() {
        //this is the current best practice for initializing the SDK
            val status = virtuoso.backplane?.authenticationStatus
            if(status != AuthenticationStatus.AUTHENTICATED){//if not authenticated execute sdk startup
                //here we use the simplest login with hard coded values

                virtuoso.startup(
                    UUID.randomUUID()
                        .toString(),    // provide an appropriate unique user id. A random uuid is used here for demonstration purposes only
                    null, //Optional additional device id to be associated with the user account.  This is not the device id generated by the virtuoso SDK
                    BACKPLANE_PUBLIC_KEY,//Penthera demo public key.  Substitute the correct one.
                    BACKPLANE_PRIVATE_KEY,
                    object : IPushRegistrationObserver {
                        override fun onServiceAvailabilityResponse(
                            pushService: Int,
                            errorCode: Int
                        ) {
                            //callback for push registration.  this will be detailed in subsequent tutorials
                        }
                    }
                )
            }
    }

    override fun onResume() {
        super.onResume()

        //resume the VirtuosoSDK on activity resume
        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.P){
            //this is the recommended workaround for issuetracker.google.com/issues/110237673
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningAppProcessInfo = activityManager.runningAppProcesses?.first { it.pid == android.os.Process.myPid() }
            if (runningAppProcessInfo != null && runningAppProcessInfo.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                virtuoso.onResume()
            }
        }
        else{
            virtuoso.onResume()
        }

    }

    override fun onPause() {
        super.onPause()

        //pause the VirtuosoSDK on activity pause
        virtuoso.onPause()
    }

    private fun deleteAsset(assetId : Int){
        virtuoso.assetManager.delete(assetId)
    }

    private fun openAsset(assetId : String){
        startActivity(Intent(this,AssetDetailActivity::class.java).apply {
            action = ACTION_VIEW
            putExtra(AssetDetailActivity.ASSET_ID_KEY, assetId)
        })

       
    }

    private fun downloadAsset(index : Int){
        initVirtuosoSDK()

        val url : String
        val title  :  String
        val id : String

        when(index){
            0 -> {
                url = ASSET_URL_1
                title = ASSET_TITLE_1
                id = ASSET_ID_1
            }
            1 ->{
                url = ASSET_URL_2
                title = ASSET_TITLE_2
                id = ASSET_ID_2
            }
            2->{
                url = ASSET_URL_3
                title = ASSET_TITLE_3
                id = ASSET_ID_3
            }
            else ->{
                url = ASSET_URL_1
                title = ASSET_TITLE_1
                id = ASSET_ID_1
            }
        }


        if(url.endsWith("mpd")){
            val params = MPDAssetBuilder().apply {
                assetId(id)
                manifestUrl(URL(url))
                assetObserver(AssetParseObserver(this@MainActivity))
                addToQueue(true)
                desiredVideoBitrate(Long.MAX_VALUE)
                withMetadata(title)
            }.build()

            virtuoso.assetManager.createMPDSegmentedAssetAsync(params)
        }
        else{
            val params  = HLSAssetBuilder().apply {
                assetId(id)
                manifestUrl(URL(url))
                assetObserver(AssetParseObserver(this@MainActivity))
                addToQueue(true)
                desiredVideoBitrate(Long.MAX_VALUE)
                withMetadata(title)
            }.build()

            virtuoso.assetManager.createHLSSegmentedAssetAsync(params)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val uri = virtuoso.assetManager?.CONTENT_URI()

        return CursorLoader(this,uri!!, PROJECTION,null,null,null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        data?.let{
            it.setNotificationUri(contentResolver, virtuoso.assetManager?.CONTENT_URI())
            assetAdapter.setCursor(it)
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        assetAdapter.setCursor(null)
    }

    internal inner class AssetRecyclerAdapter(private val context: Context) :
        RecyclerView.Adapter<AssetRecyclerAdapter.AssetHolder>() {
        private var cursor: Cursor? = null
        fun setCursor(cursor: Cursor?) {
            this.cursor = cursor
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): AssetHolder {
            return AssetHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.asset_list_item, parent, false)
            )
        }

        override fun onBindViewHolder(
            holder: AssetHolder,
            position: Int
        ) {
            if (cursor != null) {
                cursor!!.moveToPosition(position)
                holder.bindItem(cursor!!)
            }
        }

        override fun getItemCount(): Int {
            return if (cursor != null) {
                cursor!!.count
            } else 0
        }

        internal inner class AssetHolder(itemView: View) :
            ViewHolder(itemView) {
            private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
            private val progressTextView: TextView = itemView.findViewById(R.id.progressTextView)
            private val idTextView: TextView = itemView.findViewById(R.id.idTextView)
            private val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
            private val sizeTextView: TextView = itemView.findViewById(R.id.sizeTextView)
            private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
            private var titleIndex = -1
            private var index = 0
            private var idIndex = 0
            private var statusIndex = 0
            private var currentSizeIndex = 0
            private var estimatedSizeIndex = 0
            private var progressIndex = 0
            private var itemId = 0
            private var assetId: String? = null
            fun bindItem(cursor: Cursor) {
                if (titleIndex < 0) {
                    fetchCursorIndexes(cursor)
                }
                itemId = cursor.getInt(index)
                assetId = cursor.getString(idIndex)
                titleTextView.text = cursor.getString(titleIndex)
                idTextView.text = assetId
                statusTextView.text = getStatusText(context, cursor.getInt(statusIndex))
                val currentSize = cursor.getLong(currentSizeIndex)
                val expectedSize = cursor.getLong(estimatedSizeIndex)
                sizeTextView.text = context.getString(R.string.asset_size, String.format("%.2f MB", currentSize / 1048576.00),
                    String.format("%.2f MB", expectedSize / 1048576.00))
                val progressPercent = cursor.getDouble(progressIndex)
                progressTextView.text = String.format("(%.2f)", progressPercent)
                progressBar.progress = (progressPercent * 100).toInt()
            }

            // For efficiency, fetch all the cursor indexes on the first execution and reuse
            private fun fetchCursorIndexes(cursor: Cursor) {
                index = cursor.getColumnIndex(AssetColumns._ID)
                titleIndex = cursor.getColumnIndex(AssetColumns.METADATA)
                idIndex = cursor.getColumnIndex(AssetColumns.ASSET_ID)
                statusIndex = cursor.getColumnIndex(AssetColumns.DOWNLOAD_STATUS)
                currentSizeIndex = cursor.getColumnIndex(AssetColumns.CURRENT_SIZE)
                estimatedSizeIndex = cursor.getColumnIndex(AssetColumns.EXPECTED_SIZE)
                progressIndex = cursor.getColumnIndex(AssetColumns.FRACTION_COMPLETE)
            }

            init {
                itemView.findViewById<View>(R.id.delete)
                    .setOnClickListener { deleteAsset(itemId) }
                itemView.setOnClickListener { openAsset(assetId!!) }
            }
        }

    }

    class AssetParseObserver(activity : AppCompatActivity) : ISegmentedAssetFromParserObserver{

        private var mActivty : AppCompatActivity = activity

        @SuppressLint("ShowToast")
        override fun complete(asset: ISegmentedAsset?, error : Int, addedToQueue : Boolean) {

            if(asset != null){
               Toast.makeText(mActivty, "Asset parsed and " + if(addedToQueue) "added" else "not added" + "to download queue", Toast.LENGTH_LONG  ).show()

            }
            else{
                Toast.makeText(mActivty, "Error $error while parsing asset", Toast.LENGTH_LONG).show()
            }
        }
    }



    companion object{

        const val LOADER_ID : Int = 1
        // Important: Asset ID should be unique across your video catalog
        const val ASSET_ID_1 : String = "TEST_ASSET_ID_1"
        lateinit var ASSET_TITLE_1 :String
        const val ASSET_URL_1: String = "https://storage.googleapis.com/wvmedia/clear/h264/tears/tears_sd.mpd"

        const val ASSET_ID_2: String = "TEST_ASSET_ID_2"
        lateinit var ASSET_TITLE_2 :String
        const val ASSET_URL_2: String = "https://virtuoso-demo-content.s3.amazonaws.com/Steve/steve.m3u8"

        const val ASSET_ID_3 : String = "TEST_ASSET_ID_3"
        lateinit var ASSET_TITLE_3 :String
        const val ASSET_URL_3: String = "https://virtuoso-demo-content.s3.amazonaws.com/College/college.m3u8"

        val PROJECTION = arrayOf(
            AssetColumns._ID
            , AssetColumns.UUID
            , AssetColumns.ASSET_ID
            , AssetColumns.DOWNLOAD_STATUS
            , AssetColumns.METADATA
            , AssetColumns.CURRENT_SIZE
            , AssetColumns.EXPECTED_SIZE
            , AssetColumns.DURATION_SECONDS
            , AssetColumns.FRACTION_COMPLETE
        )


        fun getStatusText(context: Context, downloadStatus: Int): String? {
            return when (downloadStatus) {
                AssetStatus.DOWNLOADING -> context.getString(R.string.asset_status_downloading)
                AssetStatus.DOWNLOAD_COMPLETE -> context.getString(R.string.asset_status_complete)
                AssetStatus.DOWNLOAD_PAUSED -> context.getString(R.string.asset_status_paused)
                AssetStatus.EXPIRED -> context.getString(R.string.asset_status_expired)
                AssetStatus.DOWNLOAD_DENIED_ASSET -> context.getString(R.string.asset_status_denied_mad)
                AssetStatus.DOWNLOAD_DENIED_ACCOUNT -> context.getString(R.string.asset_status_denied_mda)
                AssetStatus.DOWNLOAD_DENIED_EXTERNAL_POLICY -> context.getString(R.string.asset_status_denied_ext)
                AssetStatus.DOWNLOAD_DENIED_MAX_DEVICE_DOWNLOADS -> context.getString(R.string.asset_status_denied_mpd)
                AssetStatus.DOWNLOAD_DENIED_COPIES -> context.getString(R.string.asset_status_denied_copies)
                AssetStatus.DOWNLOAD_BLOCKED_AWAITING_PERMISSION -> context.getString(R.string.asset_status_await_permission)
                else -> context.getString(R.string.asset_status_pending)
            }
        }

        const val BACKPLANE_PUBLIC_KEY =  
        const val BACKPLANE_PRIVATE_KEY = 

    }
}
