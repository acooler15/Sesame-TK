package fansirsqi.xposed.sesame.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.entity.CooperateEntity
import fansirsqi.xposed.sesame.entity.MapperEntity
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountOneModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectOneModelField
import fansirsqi.xposed.sesame.ui.adapter.ListAdapter
import org.json.JSONException

class ListDialog {

    enum class ListType {
        RADIO, CHECK, SHOW
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var listDialog: AlertDialog? = null

        @SuppressLint("StaticFieldLeak")
        private var btn_find_last: Button? = null

        @SuppressLint("StaticFieldLeak")
        private var btn_find_next: Button? = null

        @SuppressLint("StaticFieldLeak")
        private var btn_select_all: Button? = null

        @SuppressLint("StaticFieldLeak")
        private var btn_select_invert: Button? = null

        @SuppressLint("StaticFieldLeak")
        private var searchText: EditText? = null

        @SuppressLint("StaticFieldLeak")
        private var lv_list: ListView? = null

        private var selectModelFieldFunc: SelectModelFieldFunc? = null
        private var hasCount: Boolean? = null
        private var listType: ListType? = null

        @SuppressLint("StaticFieldLeak")
        private var layout_batch_process: RelativeLayout? = null

        fun show(c: Context, title: CharSequence, selectModelField: SelectOneModelField, listType: ListType) {
            show(c, title, selectModelField.expandValue, selectModelField, false, listType)
        }

        fun show(c: Context, title: CharSequence, selectModelField: SelectAndCountOneModelField, listType: ListType) {
            show(c, title, selectModelField.expandValue, selectModelField, false, listType)
        }

        @Throws(JSONException::class)
        fun show(c: Context, title: CharSequence, selectModelField: SelectModelField) {
            show(c, title, selectModelField, ListType.CHECK)
        }

        fun show(c: Context, title: CharSequence, selectModelField: SelectAndCountModelField) {
            show(c, title, selectModelField, ListType.CHECK)
        }

        @Throws(JSONException::class)
        fun show(c: Context, title: CharSequence, selectModelField: SelectModelField, listType: ListType) {
            show(c, title, selectModelField.expandValue, selectModelField, false, listType)
        }

        fun show(c: Context, title: CharSequence, selectModelField: SelectAndCountModelField, listType: ListType) {
            show(c, title, selectModelField.expandValue, selectModelField, true, listType)
        }

        fun show(c: Context, title: CharSequence, bl: List<out MapperEntity>?, selectModelFieldFunc: SelectModelFieldFunc, hasCount: Boolean) {
            show(c, title, bl, selectModelFieldFunc, hasCount, ListType.CHECK)
        }

        fun show(c: Context, title: CharSequence, bl: List<out MapperEntity>?, selectModelFieldFunc: SelectModelFieldFunc, hasCount: Boolean, listType: ListType) {
            ListDialog.selectModelFieldFunc = selectModelFieldFunc
            ListDialog.hasCount = hasCount
            val la = ListAdapter.getClear(c, listType)
            @Suppress("UNCHECKED_CAST")
            la.setBaseList(bl as MutableList<out MapperEntity>?)
            la.setSelectedList(selectModelFieldFunc)
            showListDialog(c, title)
            ListDialog.listType = listType
        }

        private fun showListDialog(c: Context, title: CharSequence) {
            if (listDialog == null || listDialog!!.context !== c) {
                listDialog = MaterialAlertDialogBuilder(c)
                    .setTitle(title)
                    .setView(getListView(c))
                    .setPositiveButton(c.getString(R.string.close), null)
                    .create()
            }
            listDialog!!.setOnShowListener { p1 ->
                val d = p1 as AlertDialog
                layout_batch_process = d.findViewById(R.id.layout_batch_process)
                assert(layout_batch_process != null)
                layout_batch_process!!.visibility = if (listType == ListType.CHECK && hasCount != true) View.VISIBLE else View.GONE
                ListAdapter.get(c).notifyDataSetChanged()
            }
            listDialog!!.show()
            val positiveButton = listDialog!!.getButton(DialogInterface.BUTTON_POSITIVE)
            if (positiveButton != null) {
                positiveButton.setTextColor(ContextCompat.getColor(c, R.color.selection_color))
            }
        }

        private fun getListView(c: Context): View {
            @SuppressLint("InflateParams") val v = LayoutInflater.from(c).inflate(R.layout.dialog_list, null)
            btn_find_last = v.findViewById(R.id.btn_find_last)
            btn_find_next = v.findViewById(R.id.btn_find_next)
            btn_select_all = v.findViewById(R.id.btn_select_all)
            btn_select_invert = v.findViewById(R.id.btn_select_invert)
            val onBtnClickListener = View.OnClickListener { v1 ->
                if (searchText!!.length() <= 0) return@OnClickListener
                val la = ListAdapter.get(v1.context)
                var index = -1
                if (v1.id == R.id.btn_find_last) index = la.findLast(searchText!!.text.toString())
                else if (v1.id == R.id.btn_find_next) index = la.findNext(searchText!!.text.toString())
                if (index < 0) Toast.makeText(v1.context, "未搜到", Toast.LENGTH_SHORT).show()
                else lv_list!!.setSelection(index)
            }
            btn_find_last!!.setOnClickListener(onBtnClickListener)
            btn_find_next!!.setOnClickListener(onBtnClickListener)

            val batchBtnOnClickListener = View.OnClickListener { v1 ->
                val la = ListAdapter.get(v1.context)
                if (v1.id == R.id.btn_select_all) la.selectAll()
                else if (v1.id == R.id.btn_select_invert) la.SelectInvert()
            }
            btn_select_all!!.setOnClickListener(batchBtnOnClickListener)
            btn_select_invert!!.setOnClickListener(batchBtnOnClickListener)

            searchText = v.findViewById(R.id.edt_find)
            lv_list = v.findViewById(R.id.lv_list)
            lv_list!!.setAdapter(ListAdapter.getClear(c))

            lv_list!!.setOnItemClickListener { p1, p2, p3, _ ->
                if (listType == ListType.SHOW) return@setOnItemClickListener
                val cur = p1.adapter.getItem(p3) as MapperEntity
                val holder = p2.tag as ListAdapter.ViewHolder
                if (hasCount != true) {
                    if (listType == ListType.RADIO) {
                        selectModelFieldFunc!!.clear()
                        if (holder.cb!!.isChecked) holder.cb!!.isChecked = false
                        else {
                            for (vh in ListAdapter.viewHolderList) vh.cb!!.isChecked = false
                            holder.cb!!.isChecked = true
                            selectModelFieldFunc!!.add(cur.id, 0)
                        }
                    } else {
                        if (holder.cb!!.isChecked) {
                            selectModelFieldFunc!!.remove(cur.id)
                            holder.cb!!.isChecked = false
                        } else {
                            if (java.lang.Boolean.FALSE == selectModelFieldFunc!!.contains(cur.id)) selectModelFieldFunc!!.add(cur.id, 0)
                            holder.cb!!.isChecked = true
                        }
                    }
                } else {
                    val edt = EditText(c)
                    val edtDialog = MaterialAlertDialogBuilder(c)
                        .setTitle(cur.name)
                        .setView(edt)
                        .setPositiveButton(c.getString(R.string.ok)) { _, _ ->
                            if (edt.length() > 0) {
                                try {
                                    val count = edt.text.toString().toInt()
                                    if (count > 0) {
                                        selectModelFieldFunc!!.add(cur.id, count)
                                        holder.cb!!.isChecked = true
                                    } else {
                                        selectModelFieldFunc!!.remove(cur.id)
                                        holder.cb!!.isChecked = false
                                    }
                                } catch (ignored: Exception) {
                                }
                            }
                            ListAdapter.get(c).notifyDataSetChanged()
                        }
                        .setNegativeButton(c.getString(R.string.cancel), null)
                        .create()
                    edt.setHint(if (cur is CooperateEntity) "浇水克数" else "次数")
                    val value = selectModelFieldFunc!!.get(cur.id)
                    if (value != null && value >= 0) edt.setText(value.toString())
                    edtDialog.show()
                }
            }

            return v
        }
    }
}
