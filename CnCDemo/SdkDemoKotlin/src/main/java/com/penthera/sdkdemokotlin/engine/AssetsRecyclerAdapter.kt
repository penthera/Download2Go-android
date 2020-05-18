package com.penthera.sdkdemokotlin.engine

import android.content.Context
import android.database.Cursor
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.catalog.ExampleMetaData
import com.penthera.sdkdemokotlin.util.TextUtils
import com.penthera.sdkdemokotlin.util.inflate
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.database.AssetColumns
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.listrow_inbox.view.*
import java.util.*

/**
 *
 */
class AssetsRecyclerAdapter (private val context: Context, var cursor: Cursor, private val type: Int, private val listener: AssetInboxActionListener) : RecyclerView.Adapter<AssetsRecyclerAdapter.AssetHolder>() {

    interface AssetInboxActionListener {
        fun openDetailView(assetId: Int)
        fun moveUpInQueue(assetId: Int, currentPosition: Int)
        fun moveDownInQueue(assetId: Int, currentPosition: Int)
        fun selectionUpdated()
    }

    companion object {
        const val DOWNLOADED = 0
        const val QUEUED = 1
        const val EXPIRED = 2
    }

    /** Keep track of selections  */
    val checked : Hashtable<Int, Int> = Hashtable()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetHolder {
        val inflatedView = parent.inflate(R.layout.listrow_inbox, false)
        return AssetHolder(inflatedView, type, listener, this, context)
    }

    override fun getItemCount(): Int = cursor.count

    override fun onBindViewHolder(holder: AssetHolder, position: Int) {
        cursor.moveToPosition(position)
        holder.bindItem(cursor, position)
    }

    fun toggleCheck(id: Int) {
        val contains = checked.containsKey(id)

        if (!contains) {
            checked[id] = Integer.valueOf(1)
        } else {
            checked.remove(id)
        }
        notifyDataSetChanged()
        listener.selectionUpdated()
    }

    /**
     * ViewHolder for catalog item view
     */
    class AssetHolder(v: View, private val type: Int, private val listener: AssetInboxActionListener,
                      private val adapter: AssetsRecyclerAdapter, private val context: Context) :
                        RecyclerView.ViewHolder(v), View.OnClickListener, View.OnLongClickListener {

        private var view: View = v

        private var metaData: ExampleMetaData? = null

        private var position: Int? = null

        private var virtuosoId: Int? = null
        private var assetId: String? = null
        private var uuid: String? = null

        init {
            v.setOnClickListener(this)
            v.btnUp.setOnClickListener(this)
            v.btnDown.setOnClickListener(this)
            v.setOnLongClickListener(this)
        }

        fun bindItem(cursor: Cursor, position: Int) {

            this.position = position

            virtuosoId = cursor.getInt(cursor.getColumnIndex(AssetColumns._ID))
            uuid = cursor.getString(cursor.getColumnIndex(AssetColumns.UUID))
            assetId = cursor.getString(cursor.getColumnIndex(AssetColumns.ASSET_ID))

            metaData = ExampleMetaData().fromJson(cursor.getString(cursor.getColumnIndex(AssetColumns.METADATA)))

            view.title.text = metaData?.title

            metaData?.thumbnailUri?.let {
                if(it.isNotEmpty()) {
                    Picasso.get()
                            .load(it)
                            .placeholder(R.drawable.cloud)
                            .error(R.drawable.no_image)
                            .into(view.img)
                }
            }

            when (type){
                DOWNLOADED -> { bindDownloaded(cursor) }
                QUEUED -> { bindQueued(cursor) }
                EXPIRED -> { bindExpired()
                }
            }

            // Update the check-marks for the CAB
            val checked = adapter.checked.containsKey(virtuosoId)
            view.background.alpha = if (checked) 255 else 0
        }

        private fun bindDownloaded(cursor: Cursor) {
            view.btnUp.visibility = GONE
            view.btnDown.visibility = GONE
            view.downloadStatus.text = context.getString(R.string.downloaded)
            view.rowErrorCount.visibility = VISIBLE
            view.errorCount.text = cursor.getInt(cursor.getColumnIndex(AssetColumns.ERROR_COUNT)).toString()
            updateExpiry(cursor)
        }

        private fun bindQueued(cursor: Cursor) {
            // Work out up/down buttons
            val count = cursor.count

            // Up
            if (showUp(count, position!!)) {
                view.btnUp.visibility = VISIBLE
            } else {
                view.btnUp.visibility = GONE
            }

            // Down
            if (showDown(count, position!!)) {
                view.btnDown.visibility = VISIBLE
            } else {
                view.btnDown.visibility = GONE
            }

            // Download status and error count
            val downloadStatus = cursor.getInt(cursor.getColumnIndex(AssetColumns.DOWNLOAD_STATUS))
            view.downloadStatus.text = TextUtils().getAssetStatusDescription(downloadStatus)
            view.rowErrorCount.visibility = VISIBLE
            val errors = cursor.getInt(cursor.getColumnIndex(AssetColumns.ERROR_COUNT))
            var retryString = ""
            if (errors >= Common.ASSET_RETRY_ERROR_LIMIT) {
                retryString = " " + context.getString(R.string.no_retry)
            }
            view.errorCount.text = String.format(context.getString(R.string.error_count_value), errors, retryString)

            // Progress bar if downloading
            if (downloadStatus == Common.AssetStatus.DOWNLOADING || downloadStatus == Common.AssetStatus.EARLY_DOWNLOADING) {
                view.downloadProgress.visibility = VISIBLE
                val fraction = cursor.getDouble(12)
                val percent = (fraction * 100).toInt()
                view.downloadProgress.progress = percent
            } else {
                view.downloadProgress.visibility = GONE
            }

            updateExpiry(cursor)
        }

        private fun bindExpired() {
            view.btnUp.visibility = GONE
            view.btnDown.visibility = GONE
            view.downloadStatus.text = context.getString(R.string.expired)
            view.rowErrorCount.visibility = GONE
            view.rowExpiration.visibility = GONE
        }

        private fun updateExpiry(cursor: Cursor) {
            val completionTime = cursor.getLong(cursor.getColumnIndex(AssetColumns.COMPLETION_TIME))
            val endWindow = cursor.getLong(cursor.getColumnIndex(AssetColumns.END_WINDOW))
            val firstPlayTime = cursor.getLong(cursor.getColumnIndex(AssetColumns.FIRST_PLAY_TIME))
            val eap = cursor.getLong(cursor.getColumnIndex(AssetColumns.EAP))
            val ead = cursor.getLong(cursor.getColumnIndex(AssetColumns.EAD))

            val date = TextUtils().getExpirationString(completionTime, endWindow, firstPlayTime, eap, ead)

            if (!android.text.TextUtils.isEmpty(date)) {
                view.rowExpiration.visibility = VISIBLE
                view.expiration.text = date
            } else {
                view.rowExpiration.visibility = GONE
            }
        }

        override fun onClick(v: View?) {
            when (v?.id) {
                R.id.btnUp -> {
                    virtuosoId?.let { listener.moveUpInQueue(it, position!!)}
                }
                R.id.btnDown -> {
                    virtuosoId?.let { listener.moveDownInQueue(it, position!!)}
                }
                else -> {
                    virtuosoId?.let { listener.openDetailView(it)}
                }
            }

        }

        override fun onLongClick(v: View?): Boolean {
            adapter.toggleCheck(virtuosoId!!)
            return true
        }

        private fun showUp(count: Int, position: Int): Boolean {
            return count > 0 && position != 0
        }

        private fun showDown(count: Int, position: Int): Boolean {
            return count > 0 && position != count - 1
        }
    }
}