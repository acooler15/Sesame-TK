package fansirsqi.xposed.sesame.hook.view

import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * View 树节点包装：缓存父子关系，供 [XpathParser] 执行 XPath 查找
 */
class ViewImage(val originView: View) {

    private var children: Array<ViewImage?>? = null

    companion object {
        const val TEXT = "text"
        const val CONTENT_DESCRIPTION = "contentDescription"
    }

    /**
     * 获取文本内容
     */
    fun getText(): String? {
        return if (originView is TextView) {
            originView.text?.toString()
        } else {
            originView.contentDescription?.toString()
        }
    }

    /**
     * 获取子节点数量
     */
    fun childCount(): Int {
        if (originView !is ViewGroup) {
            return 0
        }
        return originView.childCount
    }

    /**
     * 获取指定索引的子节点
     */
    fun childAt(index: Int): ViewImage {
        if (childCount() < 0) {
            throw IllegalStateException("can not parse child node for none ViewGroup object!!")
        }
        if (children == null) {
            children = arrayOfNulls(childCount())
        }
        var viewImage = children!![index]
        if (viewImage != null) {
            return viewImage
        }
        val viewGroup = originView as ViewGroup
        viewImage = ViewImage(viewGroup.getChildAt(index))
        children!![index] = viewImage
        return viewImage
    }

    /**
     * 获取所有子节点
     */
    fun children(): List<ViewImage> {
        if (childCount() <= 0) {
            return emptyList()
        }
        val ret = ArrayList<ViewImage>(childCount())
        for (i in 0 until childCount()) {
            ret.add(childAt(i))
        }
        return ret
    }

    /**
     * 获取视图类型
     */
    fun getType(): String = originView.javaClass.simpleName

    /**
     * 获取属性值
     */
    fun attribute(key: String): Any? {
        return when (key) {
            TEXT -> getText()
            CONTENT_DESCRIPTION -> originView.contentDescription?.toString()
            else -> null
        }
    }
}
