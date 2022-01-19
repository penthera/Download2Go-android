package com.penthera.sdkdemokotlin.engine

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.catalog.ExampleMetaData
import com.penthera.sdkdemokotlin.databinding.ListrowInboxBinding
import com.penthera.sdkdemokotlin.util.TextUtils
import com.penthera.virtuososdk.Common
import com.penthera.virtuososdk.client.database.AssetColumns
import com.squareup.picasso.Picasso
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
        val itemBinding = ListrowInboxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AssetHolder(itemBinding, type, listener, this, context)
    }

    override fun getItemCount(): Int = cursor.count

    override fun onBindViewHolder(holder: AssetHolder, position: Int) {
        cursor.moveToPosition(position)
        holder.bindItem(cursor, position)
    }

    fun toggleCheck(id: Int) {
        if (checked.containsKey(id)) {
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
    class AssetHolder(private val itemBinding: ListrowInboxBinding, private val type: Int, private val listener: AssetInboxActionListener,
                      private val adapter: AssetsRecyclerAdapter, private val context: Context) :
                        RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener, View.OnLongClickListener {

        private var metaData: ExampleMetaData? = null

        private var position: Int? = null

        private var virtuosoId: Int? = null
        private var assetId: String? = null
        private var uuid: String? = null

        init {
            itemBinding.root.setOnClickListener(this)
            itemBinding.btnUp.setOnClickListener(this)
            itemBinding.btnDown.setOnClickListener(this)
            itemBinding.root.setOnLongClickListener(this)
        }

        fun bindItem(cursor: Cursor, position: Int) {

            this.position = position

            virtuosoId = cursor.getInt(cursor.getColumnIndexOrThrow(AssetColumns._ID))
            uuid = cursor.getString(cursor.getColumnIndexOrThrow(AssetColumns.UUID))
            assetId = cursor.getString(cursor.getColumnIndexOrThrow(AssetColumns.ASSET_ID))

            metaData = ExampleMetaData().fromJson(cursor.getString(cursor.getColumnIndexOrThrow(AssetColumns.METADATA)))

            itemBinding.title.text = metaData?.title

            metaData?.thumbnailUri?.let {
                if(it.isNotEmpty()) {
                    Picasso.get()
                            .load(it)
                            .placeholder(R.drawable.cloud)
                            .error(R.drawable.no_image)
                            .into(itemBinding.img)
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
            itemBinding.root.background.alpha = if (checked) 255 else 0
        }

        private fun bindDownloaded(cursor: Cursor) {
            itemBinding.btnUp.visibility = GONE
            itemBinding.btnDown.visibility = GONE
            itemBinding.downloadStatus.text = context.getString(R.string.downloaded)
            itemBinding.rowErrorCount.visibility = VISIBLE
            itemBinding.errorCount.text = cursor.getInt(cursor.getColumnIndexOrThrow(AssetColumns.ERROR_COUNT)).toString()
            updateExpiry(cursor)
        }

        private fun bindQueued(cursor: Cursor) {
            // Work out up/down buttons
            val count = cursor.count

            // Up
            if (showUp(count, position!!)) {
                itemBinding.btnUp.visibility = VISIBLE
            } else {
                itemBinding.btnUp.visibility = GONE
            }

            // Down
            if (showDown(count, position!!)) {
                itemBinding.btnDown.visibility = VISIBLE
            } else {
                itemBinding.btnDown.visibility = GONE
            }

            // Download status and error count
            val downloadStatus = cursor.getInt(cursor.getColumnIndexOrThrow(AssetColumns.DOWNLOAD_STATUS))
            itemBinding.downloadStatus.text = TextUtils().getAssetStatusDescription(downloadStatus)
            itemBinding.rowErrorCount.visibility = VISIBLE
            val errors = cursor.getInt(cursor.getColumnIndexOrThrow(AssetColumns.ERROR_COUNT))
            var retryString = ""
            if (errors >= Common.ASSET_RETRY_ERROR_LIMIT) {
                retryString = " " + context.getString(R.string.no_retry)
            }
            itemBinding.errorCount.text = String.format(context.getString(R.string.error_count_value), errors, retryString)

            // Progress bar if downloading
            if (downloadStatus == Common.AssetStatus.DOWNLOADING || downloadStatus == Common.AssetStatus.EARLY_DOWNLOADING) {
                itemBinding.downloadProgress.visibility = VISIBLE
                val fraction = cursor.getDouble(12)
                val percent = (fraction * 100).toInt()
                itemBinding.downloadProgress.progress = percent
            } else {
                itemBinding.downloadProgress.visibility = GONE
            }

            updateExpiry(cursor)
        }

        private fun bindExpired() {
            itemBinding.btnUp.visibility = GONE
            itemBinding.btnDown.visibility = GONE
            itemBinding.downloadStatus.text = context.getString(R.string.expired)
            itemBinding.rowErrorCount.visibility = GONE
            itemBinding.rowExpiration.visibility = GONE
        }

        private fun updateExpiry(cursor: Cursor) {
            val completionTime = cursor.getLong(cursor.getColumnIndexOrThrow(AssetColumns.COMPLETION_TIME))
            val endWindow = cursor.getLong(cursor.getColumnIndexOrThrow(AssetColumns.END_WINDOW))
            val firstPlayTime = cursor.getLong(cursor.getColumnIndexOrThrow(AssetColumns.FIRST_PLAY_TIME))
            val eap = cursor.getLong(cursor.getColumnIndexOrThrow(AssetColumns.EAP))
            val ead = cursor.getLong(cursor.getColumnIndexOrThrow(AssetColumns.EAD))

            val date = TextUtils().getExpirationString(completionTime, endWindow, firstPlayTime, eap, ead)

            if (!android.text.TextUtils.isEmpty(date)) {
                itemBinding.rowExpiration.visibility = VISIBLE
                itemBinding.expiration.text = date
            } else {
                itemBinding.rowExpiration.visibility = GONE
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