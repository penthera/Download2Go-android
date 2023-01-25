package com.penthera.download2go7

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cursoradapter.widget.CursorAdapter
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.penthera.download2go7.databinding.PlaylistActivityBinding
import com.penthera.download2go7.databinding.PlaylistitemRowBinding
import com.penthera.virtuososdk.client.Virtuoso
import com.penthera.virtuososdk.client.autodownload.IPlaylistManager
import com.penthera.virtuososdk.client.database.PlaylistItemColumns
import java.text.SimpleDateFormat
import java.util.*

class PlaylistItemsActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor> {
    val TAG: String = PlaylistItemsActivity::class.java.getName()

    val PROJECTION = arrayOf(
        PlaylistItemColumns._ID,
        PlaylistItemColumns.ASSET_ID,
        PlaylistItemColumns.STATUS,
        PlaylistItemColumns.DOWNLOADED,
        PlaylistItemColumns.DELETED,
        PlaylistItemColumns.PLAYED_BACK,
        PlaylistItemColumns.PENDING,
        PlaylistItemColumns.LAST_PENDING
    )

    private val LOADER_ID = 1

    private lateinit var binding : PlaylistActivityBinding
    private var playlistAdapter: PlaylistItemAdapter? = null
    var mVirtuoso: Virtuoso? = null

    private var playlistName: String? = null

    private var mEmptyView: View? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = PlaylistActivityBinding.inflate(layoutInflater)

        setContentView(binding.root)

        mVirtuoso = Virtuoso(this)
        playlistAdapter = PlaylistItemAdapter(this, null)


        binding.playlists .apply{
            setEmptyView(binding.emptyList)
            setAdapter(playlistAdapter)
        }


        val intent = intent
        playlistName = intent.getStringExtra(PLAYLIST_NAME)
        if (playlistName != null) {
            supportLoaderManager.initLoader(LOADER_ID, null, this)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }


    // onCreateLoader
    override fun onCreateLoader(id: Int, bundle: Bundle?): Loader<Cursor?> {
        val playlist =
            mVirtuoso!!.assetManager.playlistManager.find(playlistName)
        Log.i(TAG, "onCreateLoader for playlist $playlistName")
        return CursorLoader(
            this,
            if (playlist != null) playlist.itemsContentUri else Uri.parse("content://pass_something_to_prevent_crash"),
            PROJECTION, null, null, null
        )
    }

    // onLoadFinished
    override fun onLoadFinished(loader: Loader<Cursor?>, cursor: Cursor?) {
        val playlist =
            mVirtuoso!!.assetManager.playlistManager.find(playlistName)
        Log.i(TAG, "onCreateLoader")
        if (cursor != null) {
            cursor.setNotificationUri(
                contentResolver,
                if (playlist != null) playlist.itemsContentUri else Uri.parse("content://pass_something_to_prevent_crash")
            )
            playlistAdapter!!.swapCursor(cursor)
        }
    }

    // onLoaderReset
    override fun onLoaderReset(arg0: Loader<Cursor?>) {
        Log.d(TAG, "onLoaderReset")
        if (playlistAdapter != null) playlistAdapter!!.swapCursor(null)
    }


    inner class PlaylistItemAdapter(context: Context?, c: Cursor?) :
        CursorAdapter(context, c, 0) {
        private val mInflater: LayoutInflater
        private val playlistManager: IPlaylistManager
        private val dateFormat: SimpleDateFormat
        @SuppressLint("Range")
        override fun bindView(
            view: View,
            context: Context,
            cursor: Cursor
        ) {
            val assetId =
                cursor.getString(cursor.getColumnIndex(PlaylistItemColumns.ASSET_ID))
            val status = cursor.getInt(cursor.getColumnIndex(PlaylistItemColumns.STATUS))
            val downloadedTimestamp =
                cursor.getLong(cursor.getColumnIndex(PlaylistItemColumns.DOWNLOADED))
            val deleted =
                cursor.getInt(cursor.getColumnIndex(PlaylistItemColumns.DELETED)) > 0
            val expired =
                cursor.getInt(cursor.getColumnIndex(PlaylistItemColumns.EXPIRED)) > 0
            val playedBackTimestamp =
                cursor.getLong(cursor.getColumnIndex(PlaylistItemColumns.PLAYED_BACK))
            val pending =
                cursor.getInt(cursor.getColumnIndex(PlaylistItemColumns.PENDING)) > 0
            val lastPending =
                cursor.getLong(cursor.getColumnIndex(PlaylistItemColumns.LAST_PENDING))

            val itemBinding : PlaylistitemRowBinding= view.tag as PlaylistitemRowBinding

            itemBinding.playlistitemName.text = assetId
            itemBinding.playlistitemStatus.text = playlistManager.PlaylistItemStatusAsString(status)
            itemBinding.playlistitemDownloaded.text = formattedStringFromTimestamp(downloadedTimestamp, true)
            itemBinding.playlistitemDeleted.text = if (deleted) "yes" else "no"
            itemBinding.playlistitemExpired.text = if (expired) "yes" else "no"
            itemBinding.playlistitemPlayback.text = formattedStringFromTimestamp(playedBackTimestamp, true)
            itemBinding.playlistitemPending.text = if (pending) "yes" else "no"
            itemBinding.playlistitemLastpending.text = formattedStringFromTimestamp(lastPending, false)
        }

        private fun formattedStringFromTimestamp(
            timestamp: Long,
            showYesNo: Boolean
        ): String {
            return if (timestamp > 0) {
                (if (showYesNo) "yes " else "") + dateFormat.format(Date(timestamp))
            } else {
                if (showYesNo) "no" else ""
            }
        }

        override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
            val itemBinding =  PlaylistitemRowBinding.inflate(mInflater, parent, false)
            itemBinding.root.tag = itemBinding
            return itemBinding.root
        }

        init {
            mInflater = LayoutInflater.from(context)
            playlistManager = mVirtuoso?.assetManager?.playlistManager!!
            dateFormat = SimpleDateFormat("MM/dd/yyyy',' HH:mm:ss a")
        }
    }

    companion object{
        const val PLAYLIST_NAME = "PLAYLIST_NAME"
    }
}