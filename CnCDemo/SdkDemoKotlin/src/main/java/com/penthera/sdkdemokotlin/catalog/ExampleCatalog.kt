package com.penthera.sdkdemokotlin.catalog

import android.content.Context
import android.util.Log
import com.penthera.sdkdemokotlin.util.SingletonHolder
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.*
import java.lang.Exception

/**
 * A simple example catalog, persisted to a json file, used to demonstrate loading assets into the
 * SDK from an external catalog. Reads and writes the json as a single string as simple example
 * that is only suitable for demonstration purposes.
 */
class ExampleCatalog(context: Context) {

    interface CatalogObserver{
        fun catalogChanged()
    }

    companion object : SingletonHolder<ExampleCatalog, Context>(::ExampleCatalog){
        @JvmField val TAG = ExampleCatalog::class.java.simpleName
    }

    private val directory: String = "/example_catalog"

    private val fileName: String = "catalogfile.json"

    private var c: Context = context

    var currentCatalog: List<ExampleCatalogItem> = ArrayList()

    private var observers : ArrayList<CatalogObserver> = ArrayList()

    init {
        loadStore()
    }

    private fun loadStore() {
        var catalogItemsString: String?
        // Look for a stored file first
        catalogItemsString = readStore()

        // If missing load from resource
        if (catalogItemsString == null) {
            try {
                catalogItemsString = c.assets.open(fileName).bufferedReader(Charsets.UTF_8).use {
                    val result = it.readText()
                    Log.d(TAG, "Using assets from resource")
                    result
                }
            } catch (ioe: IOException) {
                Log.w(TAG, "Could not read catalog from app assets", ioe)
            }
        }

        catalogItemsString?.let {
            try {
                val moshi = Moshi.Builder()
                        .add(KotlinJsonAdapterFactory())
                        .build()

                val listOfItemsType = Types.newParameterizedType(List::class.java, ExampleCatalogItem::class.java)
                val catalogItemAdapter: JsonAdapter<List<ExampleCatalogItem>> = moshi.adapter(listOfItemsType)
                val catalogList = catalogItemAdapter.fromJson(it)

                // check ok

                catalogList?.let {currentCatalog = catalogList}
            } catch (e : Exception) {
                Log.w(TAG, "Error instantiating example catalog", e)
            }
        }
    }

    fun addAndStore(item: ExampleCatalogItem) {
        val updatedCatalog: ArrayList<ExampleCatalogItem> = ArrayList(currentCatalog)
        updatedCatalog.add(item)

        currentCatalog = updatedCatalog

        // save
        try {
            val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()

            val listOfItemsType = Types.newParameterizedType(List::class.java, ExampleCatalogItem::class.java)
            val catalogItemAdapter: JsonAdapter<List<ExampleCatalogItem>> = moshi.adapter(listOfItemsType)
            val json = catalogItemAdapter.toJson(currentCatalog)
            writeStore(json)

            for(observer : CatalogObserver in this.observers){
                observer.catalogChanged()
            }

        } catch (e : Exception) {

        }

    }

    fun registerObserver(observer : CatalogObserver){
        observers.add(observer)
    }

    fun unregisterObserver(observer: CatalogObserver){
        if(observers.contains(observer)) {
            observers.remove(observer)
        }
    }

    private fun readStore(): String? {
        var json : String? = null

        try {
            val sdMain = File(c.filesDir, directory)
            var success = true
            if (!sdMain.exists()) {
                success = sdMain.mkdir()
            }
            if (success) {
                json = File(sdMain, fileName).bufferedReader(Charsets.UTF_8).use {
                    it.readText()
                }
            }
        } catch (ioe : IOException) {
            Log.i(TAG, "Did not find catalog store")
        }

        return json
    }

    fun findItemById( id : String): ExampleCatalogItem ?{

        return currentCatalog.let{ it.filter {item -> item.exampleAssetId == id} }.first()

    }
    private fun writeStore(json: String) {
        val sdMain = File(c.filesDir, directory)
        var success = true
        if (!sdMain.exists()){
            success = sdMain.mkdir()
        }
        if (success) {
            try {
                File(sdMain, fileName).bufferedWriter(Charsets.UTF_8).use {
                    it.write(json)
                }
            } catch (e : Exception) {
                Log.w(TAG, "Could not write to catalog store ", e)
            }
        }
    }
}