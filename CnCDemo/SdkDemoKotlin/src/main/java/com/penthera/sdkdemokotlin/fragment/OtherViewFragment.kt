package com.penthera.sdkdemokotlin.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.penthera.sdkdemokotlin.R
import com.penthera.sdkdemokotlin.activity.NavigationListener
import com.penthera.sdkdemokotlin.databinding.BasicListrowBinding
import com.penthera.sdkdemokotlin.databinding.FragmentOtherBinding

/**
 *
 */
class OtherViewFragment : Fragment(), View.OnClickListener {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var itemDecoration: DividerItemDecoration
    private lateinit var adapter: MenuRecyclerAdapter
    private lateinit var navigationListener: NavigationListener

    private var _binding: FragmentOtherBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentOtherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        linearLayoutManager = LinearLayoutManager(context)
        binding.optionsList.layoutManager = linearLayoutManager
        binding.optionsList.hasFixedSize()
        itemDecoration = DividerItemDecoration(binding.optionsList.context, linearLayoutManager.orientation)
        binding.optionsList.addItemDecoration(itemDecoration)
        adapter = MenuRecyclerAdapter(resources.getStringArray(R.array.other_menu), this)
        binding.optionsList.adapter = adapter
    }

    override fun onAttach(context: Context) {
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
            val itemBinding = BasicListrowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MenuItemViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
            holder.textView.text = items[position]
            holder.textView.tag = position
            holder.textView.setOnClickListener(listener)
        }

        override fun getItemCount(): Int  = items.size

        class MenuItemViewHolder(itemBinding: BasicListrowBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            val textView: TextView = itemBinding.rowText
        }
    }
}