package com.penthera.sdkdemokotlin.fragment

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.activity.NavigationListener
import com.penthera.sdkdemokotlin.util.inflate
import kotlinx.android.synthetic.main.basic_listrow.view.*
import kotlinx.android.synthetic.main.fragment_other.*

/**
 *
 */
class OtherViewFragment : Fragment(), View.OnClickListener {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var itemDecoration: DividerItemDecoration
    private lateinit var adapter: MenuRecyclerAdapter
    private lateinit var navigationListener: NavigationListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_other, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        linearLayoutManager = LinearLayoutManager(context)
        optionsList.layoutManager = linearLayoutManager
        optionsList.hasFixedSize()
        itemDecoration = DividerItemDecoration(optionsList.context, linearLayoutManager.orientation)
        optionsList.addItemDecoration(itemDecoration)
        adapter = MenuRecyclerAdapter(resources.getStringArray(R.array.other_menu), this)
        optionsList.adapter = adapter
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        navigationListener = activity as NavigationListener
    }

    override fun onClick(v: View) {

        when (v.tag){
            0 -> { navigationListener.addFragment(SettingsFragment(), "settings") }
            1 -> { navigationListener.addFragment(DevicesFragment(), "devices") }
            2 -> { navigationListener.addFragment(DiagnosticsFragment(), "diagnostics") }
            3 -> { navigationListener.addFragment(AboutFragment(), "about") }
        }
    }

    class MenuRecyclerAdapter(private val items : Array<String>, private val listener: View.OnClickListener) : RecyclerView.Adapter<MenuRecyclerAdapter.MenuItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
            val inflatedView = parent.inflate(R.layout.basic_listrow, false)
            return MenuItemViewHolder(inflatedView)
        }

        override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
            holder.textView.text = items[position]
            holder.textView.tag = position
            holder.textView.setOnClickListener(listener)
        }

        override fun getItemCount(): Int  = items.size

        class MenuItemViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val textView: TextView = v.rowText
        }
    }
}