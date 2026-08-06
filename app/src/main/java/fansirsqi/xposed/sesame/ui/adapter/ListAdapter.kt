package fansirsqi.xposed.sesame.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.entity.MapperEntity
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc
import fansirsqi.xposed.sesame.ui.widget.ListDialog
import fansirsqi.xposed.sesame.core.log.Log
import java.util.ArrayList
import java.util.Collections
import kotlin.math.max

class ListAdapter private constructor(private val context: Context) : BaseAdapter() {
    private var list: MutableList<out MapperEntity>? = null
    private var selectModelFieldFunc: SelectModelFieldFunc? = null
    private var findIndex = -1
    private var findWord: String? = null

    fun setBaseList(l: MutableList<out MapperEntity>?) {
        if (l !== list) {
            exitFind()
        }
        list = l
    }

    fun setSelectedList(selectModelFieldFunc: SelectModelFieldFunc?) {
        this.selectModelFieldFunc = selectModelFieldFunc
        try {
            Collections.sort(list!!) { o1, o2 ->
                val contains1 = java.lang.Boolean.TRUE == selectModelFieldFunc!!.contains(o1.id)
                val contains2 = java.lang.Boolean.TRUE == selectModelFieldFunc.contains(o2.id)
                if (contains1 == contains2) {
                    return@sort o1.compareTo(o2)
                }
                if (contains1) -1 else 1
            }
        } catch (e: Exception) {
            Log.record(TAG, "ListAdapter error")
            Log.printStackTrace(e)
        }
    }

    fun findLast(findThis: String): Int {
        return findItem(findThis, false)
    }

    fun findNext(findThis: String): Int {
        return findItem(findThis, true)
    }

    private fun findItem(findThis: String, forward: Boolean): Int {
        val l = list
        if (l == null || l.isEmpty()) {
            return -1
        }
        val word = findThis.lowercase()
        if (word != findWord) {
            resetFindState()
            findWord = word
        }
        var current = max(findIndex, 0)
        val size = l.size
        val start = current
        do {
            current = if (forward) (current + 1) % size else (current - 1 + size) % size
            if (l[current].name.lowercase().contains(word)) {
                findIndex = current
                notifyDataSetChanged()
                return findIndex
            }
        } while (current != start)
        return -1
    }

    fun resetFindState() {
        findIndex = -1
        findWord = null
    }

    fun exitFind() {
        resetFindState()
    }

    fun selectAll() {
        selectModelFieldFunc!!.clear()
        for (item in list!!) {
            selectModelFieldFunc!!.add(item.id, 0)
        }
        notifyDataSetChanged()
    }

    fun SelectInvert() {
        for (item in list!!) {
            if (java.lang.Boolean.FALSE == selectModelFieldFunc!!.contains(item.id)) {
                selectModelFieldFunc!!.add(item.id, 0)
            } else {
                selectModelFieldFunc!!.remove(item.id)
            }
        }
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return list?.size ?: 0
    }

    override fun getItem(position: Int): Any {
        return list!![position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        val vh: ViewHolder
        if (view == null) {
            vh = ViewHolder()
            view = View.inflate(context, R.layout.list_item, null)
            vh.tv = view.findViewById(R.id.tv_idn)
            vh.cb = view.findViewById(R.id.cb_list)
            if (listType == ListDialog.ListType.SHOW) {
                vh.cb!!.visibility = View.GONE
            }
            view.tag = vh
            viewHolderList.add(vh)
        } else {
            vh = view.tag as ViewHolder
        }
        val item = list!![position]
        vh.tv.text = item.name
        val textColorPrimary = ContextCompat.getColor(context, R.color.textColorPrimary)
        vh.tv.setTextColor(if (findIndex == position) Color.RED else textColorPrimary)
        vh.cb!!.isChecked = selectModelFieldFunc != null && java.lang.Boolean.TRUE == selectModelFieldFunc!!.contains(item.id)
        return view
    }

    /**
     * 内部 ViewHolder 类，用于缓存列表项视图。
     */
    class ViewHolder {
        lateinit var tv: TextView
        @JvmField
        var cb: CheckBox? = null
    }

    companion object {
        private const val TAG = "ListAdapter"

        @SuppressLint("StaticFieldLeak")
        private var adapter: ListAdapter? = null
        private var listType: ListDialog.ListType? = null

        @JvmField
        val viewHolderList: MutableList<ViewHolder> = ArrayList()

        @JvmStatic
        fun get(c: Context): ListAdapter {
            if (adapter == null) {
                adapter = ListAdapter(c.applicationContext)  // 使用 ApplicationContext
            }
            return adapter!!
        }

        @JvmStatic
        fun getClear(c: Context): ListAdapter {
            val adapter = get(c)
            adapter.resetFindState()
            return adapter
        }

        @JvmStatic
        fun getClear(c: Context, listType: ListDialog.ListType?): ListAdapter {
            val adapter = get(c)
            this.listType = listType
            adapter.resetFindState()
            return adapter
        }
    }
}
