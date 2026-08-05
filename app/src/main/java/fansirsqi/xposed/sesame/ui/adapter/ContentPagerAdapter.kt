package fansirsqi.xposed.sesame.ui.adapter

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.model.ModelConfig
import fansirsqi.xposed.sesame.model.ModelField
import java.util.ArrayList

class ContentPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    configMap: Map<String, ModelConfig>?
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    private val configs: MutableList<ModelConfig> = ArrayList()

    init {
        requireNotNull(configMap) { "ConfigMap cannot be null" }
        configs.addAll(configMap.values)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(configMap: Map<String, ModelConfig>?) {
        requireNotNull(configMap) { "ConfigMap cannot be null" }
        val newConfigs: List<ModelConfig> = ArrayList(configMap.values)
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return configs.size
            }

            override fun getNewListSize(): Int {
                return newConfigs.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return configs[oldItemPosition] == newConfigs[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return configs[oldItemPosition] == newConfigs[newItemPosition]
            }
        })
        configs.clear()
        configs.addAll(newConfigs)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun createFragment(position: Int): Fragment {
        try {
            if (position < 0 || position >= configs.size) {
                throw IndexOutOfBoundsException("Invalid position: $position")
            }
            val config = configs[position]
            val fields = config.fields
            if (fields == null) {
                throw IllegalStateException("Fields cannot be null for config at position: $position")
            }
            return ContentFragment(ArrayList(fields.values))
        } catch (e: Exception) {
            Log.e(TAG, "Error creating fragment at position: $position", e)
            throw e
        }
    }

    override fun getItemCount(): Int {
        return configs.size
    }

    class ContentFragment(private val modelFields: ArrayList<ModelField<*>>) : Fragment() {
        private var recyclerView: RecyclerView? = null

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            return inflater.inflate(R.layout.fragment_settings_list, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val rv = view.findViewById<RecyclerView>(R.id.rv_items)
            recyclerView = rv
            rv.layoutManager = LinearLayoutManager(requireContext())
            rv.adapter = ContentAdapter(modelFields)
        }

        override fun onDestroyView() {
            super.onDestroyView()
            recyclerView = null
        }

        fun scrollToTop() {
            recyclerView?.smoothScrollToPosition(0)
        }
    }

    private class ContentAdapter(private val modelFields: ArrayList<ModelField<*>>) :
        RecyclerView.Adapter<ContentAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_settings_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val container = holder.itemView as ViewGroup
            container.removeAllViews()
            val fieldView = modelFields[position].getView(container.context)
            if (fieldView != null) {
                container.addView(fieldView)
            }
        }

        override fun getItemCount(): Int {
            return modelFields.size
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }

    companion object {
        private const val TAG = "ContentPagerAdapter"
    }
}
