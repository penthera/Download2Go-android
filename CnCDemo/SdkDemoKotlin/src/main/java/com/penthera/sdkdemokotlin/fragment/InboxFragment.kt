package com.penthera.sdkdemokotlin.fragment

import android.database.Cursor
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.activity.NavigationListener
import com.penthera.sdkdemokotlin.activity.OfflineVideoProvider
import com.penthera.sdkdemokotlin.engine.AssetsRecyclerAdapter
import com.penthera.sdkdemokotlin.engine.AssetsRecyclerAdapter.Companion.DOWNLOADED
import com.penthera.sdkdemokotlin.engine.AssetsRecyclerAdapter.Companion.EXPIRED
import com.penthera.sdkdemokotlin.engine.AssetsRecyclerAdapter.Companion.QUEUED
import com.penthera.sdkdemokotlin.engine.VirtuosoQueueViewModelFactory
import com.penthera.sdkdemokotlin.engine.VirtuosoQueuesViewModel
import com.penthera.sdkdemokotlin.view.EmptyRecyclerAdapter
import com.penthera.sdkdemokotlin.view.HeaderRecyclerAdapter
import com.penthera.virtuososdk.client.IAsset
import com.penthera.virtuososdk.client.ISegmentedAsset
import kotlinx.android.synthetic.main.fragment_inbox.*
import me.mvdw.recyclerviewmergeadapter.adapter.RecyclerViewMergeAdapter

/**
 * A single inbox fragment displays some of the different queues available from the SDK for:
 *  Downloaded Items, Queued Items, Expired Items.
 * To do so we use an off-the-shelf merge adapter on the recyclerview.
 */
class InboxFragment : Fragment(), AssetsRecyclerAdapter.AssetInboxActionListener, ActionMode.Callback {

    companion object {
        fun newInstance(): InboxFragment {
            return InboxFragment()
        }

        private val TAG = InboxFragment::class.java.simpleName

        private val DELETE = 0
        private val RESET = 1
        private val PAUSE = 2
        private val RESUME = 3
        private val REFRESH_DRM = 4
    }

    private lateinit var linearLayoutManager: LinearLayoutManager

    /** Merge adapter - combines the three adapters for the different queues plus headers */
    private var mergeAdapter : RecyclerViewMergeAdapter? = null

    /** Adapters for the three queues: downloaded, queued, and expired */
    private var downloadedAdapter: AssetsRecyclerAdapter? = null
    private var queuedAdapter: AssetsRecyclerAdapter? = null
    private var expiredAdapter: AssetsRecyclerAdapter? = null
    private var emptyAdapter: EmptyRecyclerAdapter? = null

    /** View model for obtaining the livedata to back the adapters */
    private var queuesViewModel: VirtuosoQueuesViewModel? = null

    private var navigationListener: NavigationListener? = null
    private var offlineVideoProvider: OfflineVideoProvider? = null

    /** Action mode  */
    private var actionMode: ActionMode? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_inbox, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        linearLayoutManager = LinearLayoutManager(context)
        inboxList.layoutManager = linearLayoutManager
        val itemDecoration = DividerItemDecoration(inboxList.context, linearLayoutManager.orientation)
        inboxList.addItemDecoration(itemDecoration)
        mergeAdapter?.let{
            inboxList.adapter = it
            setFooter()
        }

    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        navigationListener = activity as NavigationListener
        offlineVideoProvider = activity as OfflineVideoProvider

        offlineVideoProvider?.getOfflineEngine()?.let {
            queuesViewModel = ViewModelProvider(this, VirtuosoQueueViewModelFactory(it))
                    .get(VirtuosoQueuesViewModel::class.java)
            queuesViewModel?.combinedQueuesLiveData?.observe(viewLifecycleOwner, Observer<List<Cursor?>> {
                it?.let {
                    if (inboxList.adapter != null) {
                        it[0]?.let {
                            downloadedAdapter?.cursor = it
                            downloadedAdapter?.notifyDataSetChanged()
                        }
                        it[1]?.let {
                            queuedAdapter?.cursor = it
                            queuedAdapter?.notifyDataSetChanged()
                        }
                        it[2]?.let {
                            expiredAdapter?.cursor = it
                            expiredAdapter?.notifyDataSetChanged()
                        }
                        setFooter()
                    } else if (it.get(0) != null && it.get(1) != null && it.get(2) != null) {
                        // Wait until we have all three cursors. Simple demo approach.
                        prepareAdapters(it[0]!!,it[1]!!,it[2]!!)
                        setFooter()
                    }
                }
            })
        }
    }

    /**
     * Construct a merge adapter for the recyclerview containing the adapters from
     * download, queued, and expired queues of the SDK. This is done for demonstration purposes only,
     * in a normal application it is unlikely that these queues would be merged into a single UI element.
     * Note: method only called once all adapters are constructed
     */
    fun prepareAdapters(downloadedCursor : Cursor, queuedCursor: Cursor, expiredCursor: Cursor) {

        val adaptersList = ArrayList<AssetsRecyclerAdapter>()

        downloadedAdapter = AssetsRecyclerAdapter(requireContext(), downloadedCursor, DOWNLOADED, this)
        adaptersList.add(downloadedAdapter!!)
        val downloadedHeader = HeaderRecyclerAdapter(getString(R.string.downloaded), downloadedAdapter!!)

        queuedAdapter = AssetsRecyclerAdapter(requireContext(), queuedCursor, QUEUED, this)
        adaptersList.add(queuedAdapter!!)
        val queuedHeader = HeaderRecyclerAdapter(getString(R.string.queued), queuedAdapter!!)

        expiredAdapter = AssetsRecyclerAdapter(requireContext(), expiredCursor, EXPIRED, this)
        adaptersList.add(expiredAdapter!!)
        val expiredHeader = HeaderRecyclerAdapter(getString(R.string.expired), expiredAdapter!!)

        emptyAdapter = EmptyRecyclerAdapter(getString(R.string.no_download), adaptersList)

        mergeAdapter = RecyclerViewMergeAdapter()
        mergeAdapter?.addAdapter(downloadedHeader)
        mergeAdapter?.addAdapter(downloadedAdapter!!)
        mergeAdapter?.addAdapter(queuedHeader)
        mergeAdapter?.addAdapter(queuedAdapter!!)
        mergeAdapter?.addAdapter(expiredHeader)
        mergeAdapter?.addAdapter(expiredAdapter!!)
        mergeAdapter?.addAdapter(emptyAdapter!!)

        inboxList.adapter = mergeAdapter
    }

    private fun setFooter() {
        if (queuedAdapter!!.itemCount > 0 || downloadedAdapter!!.itemCount > 0 || expiredAdapter!!.itemCount > 0) {
            inboxFooter.setVisibility(View.VISIBLE)
        } else {
            inboxFooter.setVisibility(View.GONE)
        }
    }

    override fun openDetailView(assetId: Int) {
        val asset: IAsset? = offlineVideoProvider?.getOfflineEngine()?.getVirtuoso()?.assetManager?.get(assetId) as IAsset
        asset?.let{ navigationListener?.showInboxDetailsView(asset)}
    }

    override fun moveUpInQueue(assetId: Int, currentPosition: Int) {
        offlineVideoProvider?.getOfflineEngine()?.getVirtuoso()?.assetManager?.getQueue()?.move(assetId, currentPosition - 1)
    }

    override fun moveDownInQueue(assetId: Int, currentPosition: Int) {
        offlineVideoProvider?.getOfflineEngine()?.getVirtuoso()?.assetManager?.getQueue()?.move(assetId, currentPosition + 1)
    }

    override fun selectionUpdated() {
        var count = 0
        downloadedAdapter?.let {count += it.checked.count()}
        queuedAdapter?.let {count += it.checked.count()}
        expiredAdapter?.let {count += it.checked.count()}

        if (count > 0 && actionMode == null) {
            actionMode = activity?.startActionMode(this)
        }
        if (count <= 0 && actionMode != null) {
            actionMode?.finish()
        }
    }

    // Action Mode handling for context menus

    // oncreateActionMode
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        val inflater = mode.menuInflater
        inflater.inflate(R.menu.context_inbox, menu)
        return true
    }

    // onDestroyActionMode
    override fun onDestroyActionMode(mode: ActionMode) {
        if (actionMode === mode) {
            actionMode = null
        }
        queuedAdapter?.checked?.clear()
        downloadedAdapter?.checked?.clear()
        expiredAdapter?.checked?.clear()
        queuedAdapter?.notifyDataSetChanged()
        downloadedAdapter?.notifyDataSetChanged()
        expiredAdapter?.notifyDataSetChanged()
    }

    // onActionItemClicked
    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        try {
            when (item.itemId) {
                R.id.menu_delete -> handleOp(DELETE)
                R.id.menu_reset -> handleOp(RESET)
                R.id.menu_pause -> handleOp(PAUSE)
                R.id.menu_resume -> handleOp(RESUME)
                R.id.menu_refresh_drm -> handleOp(REFRESH_DRM)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mode.finish()
        return true
    }

    // onPrepareActionMode
    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        queuedAdapter?.notifyDataSetChanged()
        return false
    }

    /**
     * Handle Delete
     */
    private fun handleOp(op: Int) {
        // Downloaded
        downloadedAdapter?.let { runOp(op, it) }
        queuedAdapter?.let { runOp(op, it) }
        expiredAdapter?.let { runOp(op, it) }
    }

    private fun runOp(op: Int, adapter: AssetsRecyclerAdapter) {
        var idSet = adapter.checked.keys
        when (op) {
            DELETE -> deleteSet(idSet)
            RESET -> resetSet(idSet)
            PAUSE -> pauseSet(idSet)
            RESUME -> resumeSet(idSet)
            REFRESH_DRM -> refreshDrm(idSet)
        }
    }

    private fun refreshDrm(idSet: Set<Int>){
        for(id in idSet){
            (offlineVideoProvider?.getOfflineEngine()?.getVirtuoso()?.assetManager?.get(id) as ISegmentedAsset).manualDrmRefresh()
        }
    }

    private fun deleteSet(idSet: Set<Int>) {
        for (id in idSet) {
            offlineVideoProvider?.getOfflineEngine()?.getVirtuoso()?.assetManager?.delete(id)
        }
    }

    private fun resetSet(idSet: Set<Int>) {
        for (id in idSet) {
            offlineVideoProvider?.getOfflineEngine()?.getVirtuoso()?.assetManager?.getQueue()?.clearRetryCount(id)
        }
    }

    private fun pauseSet(idSet: Set<Int>) {
        for (id in idSet) {
            offlineVideoProvider?.getOfflineEngine()?.getVirtuoso()?.assetManager?.pauseDownload(id)
        }
    }

    private fun resumeSet(idSet: Set<Int>) {
        for (id in idSet) {
            offlineVideoProvider?.getOfflineEngine()?.getVirtuoso()?.assetManager?.resumeDownload(id)
        }
    }
}
