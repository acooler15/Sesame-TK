package fansirsqi.xposed.sesame.ui.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.EmptyModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountOneModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectOneModelField
import fansirsqi.xposed.sesame.ui.extension.openUrl
import fansirsqi.xposed.sesame.ui.widget.ChoiceDialog
import fansirsqi.xposed.sesame.ui.widget.ListDialog
import fansirsqi.xposed.sesame.ui.widget.StringDialog
import org.json.JSONException
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
            val fieldView = createFieldView(container.context, modelFields[position])
            container.addView(fieldView)
        }

        /**
         * 根据字段类型创建配置项视图（传统 View 实现，交互与原 getView 保持一致）
         */
        private fun createFieldView(context: Context, modelField: ModelField<*>): View {
            return when (modelField.viewData.type) {
                "BOOLEAN" -> {
                    @SuppressLint("UseSwitchCompatOrMaterialCode")
                    val sw = Switch(context)
                    sw.text = modelField.viewData.name
                    sw.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    sw.minHeight = 150
                    sw.maxHeight = 180
                    sw.setPaddingRelative(40, 0, 40, 0)
                    sw.isChecked = modelField.value as? Boolean ?: false
                    sw.setTrackResource(R.drawable.switch_track)
                    sw.setOnClickListener { v -> modelField.setObjectValue((v as Switch).isChecked) }
                    sw
                }
                else -> createFieldButton(context, modelField)
            }
        }

        /**
         * 创建按钮视图并绑定点击交互
         */
        private fun createFieldButton(context: Context, modelField: ModelField<*>): View {
            val btn = Button(context)
            btn.text = modelField.viewData.name
            btn.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            btn.setTextColor(ContextCompat.getColor(context, R.color.selection_color))
            btn.background = ContextCompat.getDrawable(context, R.drawable.dialog_list_button)
            btn.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            btn.minHeight = 150
            btn.maxHeight = 180
            btn.setPaddingRelative(40, 0, 40, 0)
            btn.isAllCaps = false
            when (modelField.viewData.type) {
                "INTEGER", "MULTIPLY_INTEGER", "STRING", "LIST" -> {
                    btn.setOnClickListener { v -> StringDialog.showEditDialog(v.context, (v as Button).text, modelField) }
                }
                "TEXT", "READ_TEXT" -> {
                    btn.setOnClickListener { v -> StringDialog.showReadDialog(v.context, (v as Button).text, modelField) }
                }
                "URL_TEXT" -> {
                    btn.setOnClickListener { v ->
                        val innerContext = v.context
                        val url = modelField.configValue
                        innerContext.openUrl(url)
                    }
                }
                "CHOICE" -> {
                    btn.setOnClickListener { v -> ChoiceDialog.show(v.context, (v as Button).text, modelField as ChoiceModelField) }
                }
                "SELECT" -> {
                    btn.setOnClickListener { v ->
                        try {
                            ListDialog.show(v.context, (v as Button).text, modelField as SelectModelField)
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
                        }
                    }
                }
                "SELECT_AND_COUNT" -> {
                    btn.setOnClickListener { v -> ListDialog.show(v.context, (v as Button).text, modelField as SelectAndCountModelField) }
                }
                "SELECT_ONE" -> {
                    btn.setOnClickListener { v -> ListDialog.show(v.context, (v as Button).text, modelField as SelectOneModelField, ListDialog.ListType.RADIO) }
                }
                "SELECT_AND_COUNT_ONE" -> {
                    btn.setOnClickListener { v -> ListDialog.show(v.context, (v as Button).text, modelField as SelectAndCountOneModelField, ListDialog.ListType.RADIO) }
                }
                "EMPTY" -> {
                    val runner = (modelField as EmptyModelField).clickRunner
                    if (runner != null) {
                        btn.setOnClickListener {
                            AlertDialog.Builder(context)
                                .setTitle("警告")
                                .setMessage("确认执行该操作？")
                                .setPositiveButton(R.string.ok) { _, _ -> runner.run() }
                                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                                .create()
                                .show()
                        }
                    } else {
                        btn.setOnClickListener { Toast.makeText(context, "无配置项", Toast.LENGTH_SHORT).show() }
                    }
                }
                else -> {
                    btn.setOnClickListener { Toast.makeText(context, "无配置项", Toast.LENGTH_SHORT).show() }
                }
            }
            return btn
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
