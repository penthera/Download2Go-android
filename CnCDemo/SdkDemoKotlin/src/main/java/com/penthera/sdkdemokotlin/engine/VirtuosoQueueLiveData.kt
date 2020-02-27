package com.penthera.sdkdemokotlin.engine

import android.database.Cursor
import com.penthera.virtuososdk.client.database.AssetColumns
import android.R.string.cancel
import android.content.Context
import android.util.Log
import android.database.ContentObserver
import android.net.Uri
import android.os.AsyncTask
import android.os.Handler
import androidx.core.content.ContentResolverCompat
import androidx.core.os.CancellationSignal
import androidx.core.os.OperationCanceledException
import androidx.lifecycle.LiveData
import java.lang.RuntimeException


/**
 *
 */
class VirtuosoQueueLiveData(private val context: Context, private val uri: Uri) : LiveData<Cursor>() {

    companion object {

        /** The fields we want for the Assets  */
        val PROJECTION = arrayOf(AssetColumns._ID,
                AssetColumns.UUID,
                AssetColumns.TYPE,
                AssetColumns.ASSET_ID,
                AssetColumns.DOWNLOAD_STATUS,
                AssetColumns.ERROR_COUNT,
                AssetColumns.METADATA,
                AssetColumns.EAP,
                AssetColumns.EAD,
                AssetColumns.END_WINDOW,
                AssetColumns.FIRST_PLAY_TIME,
                AssetColumns.COMPLETION_TIME,
                AssetColumns.FRACTION_COMPLETE)

        private val TAG = VirtuosoQueueLiveData::class.java.simpleName
    }

    private val observer: ForceLoadContentObserver = ForceLoadContentObserver()

    private var cancellationSignal: CancellationSignal? = null

    override fun onActive() {
        Log.d(TAG, "onActive")
        loadData(false)
    }

    override fun onInactive() {
        Log.d(TAG, "onInactive")
        synchronized(this@VirtuosoQueueLiveData) {
            cancellationSignal?.cancel()
        }
    }

    override fun setValue(newCursor: Cursor) {
        val oldCursor = value
        if (oldCursor != null) {
            Log.d(TAG, "setValue() oldCursor.close()")
            oldCursor.close()
        }

        super.setValue(newCursor)
    }

    private fun loadData(force: Boolean){
        Log.d(TAG, "loadData")

        if (!force) {
            val cursor = value
            if (cursor != null && !cursor.isClosed) {
                return
            }
        }

        val task = FetchCursorTask()
        task.execute()
    }

    inner class ForceLoadContentObserver : ContentObserver(Handler()) {

        override fun deliverSelfNotifications(): Boolean {
            return true
        }

        override fun onChange(selfChange: Boolean) {
            Log.d(TAG, "ForceLoadContentObserver.onChange()")
            loadData(true)
        }

    }

    inner class FetchCursorTask : AsyncTask<Void, Void, Cursor>() {
        override fun doInBackground(vararg params: Void?): Cursor? {
            synchronized(this@VirtuosoQueueLiveData) {
                cancellationSignal = CancellationSignal()
            }
            try {
                val cursor = ContentResolverCompat.query(
                        context.contentResolver,
                        uri,
                        PROJECTION,
                        null,
                        null,
                        null,
                        cancellationSignal
                )

                cursor?.let {
                    try {
                        it.count
                        it.setNotificationUri(context.contentResolver, uri)
                        it.registerContentObserver(observer)
                    } catch (ex: RuntimeException) {
                        it.close()
                        throw ex
                    }
                }

                return cursor
            } catch (oce : OperationCanceledException) {
                if (hasActiveObservers()){
                    throw oce
                }
                return null
            }
        }

        override fun onPostExecute(cursor: Cursor) {
            setValue(cursor)
        }
    }

}