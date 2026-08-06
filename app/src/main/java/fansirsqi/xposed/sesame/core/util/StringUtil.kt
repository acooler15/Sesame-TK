package fansirsqi.xposed.sesame.core.util
import java.util.Objects

object StringUtil {
    @JvmStatic
    fun isEmpty(str: String?): Boolean = str == null || str.isEmpty()

    fun collectionJoinString(conjunction: CharSequence, collection: Collection<*>): String {
        if (collection.isNotEmpty()) {
            val b = StringBuilder()
            val iterator = collection.iterator()
            b.append(toStringOrEmpty(iterator.next()))
            while (iterator.hasNext()) {
                b.append(conjunction).append(toStringOrEmpty(iterator.next()))
            }
            return b.toString()
        }
        return ""
    }

    fun arrayJoinString(conjunction: CharSequence, vararg array: Any?): String {
        val length = array.size
        if (length > 0) {
            val b = StringBuilder()
            b.append(toStringOrEmpty(array[0]))
            for (i in 1 until length) {
                b.append(conjunction).append(toStringOrEmpty(array[i]))
            }
            return b.toString()
        }
        return ""
    }

    fun arrayToString(vararg array: Any?): String = arrayJoinString(",", *array)

    private fun toStringOrEmpty(obj: Any?): String = Objects.toString(obj, "")

    fun padLeft(str: Int, totalWidth: Int, padChar: Char): String =
        padLeft(str.toString(), totalWidth, padChar)

    fun padRight(str: Int, totalWidth: Int, padChar: Char): String =
        padRight(str.toString(), totalWidth, padChar)

    fun padLeft(str: String, totalWidth: Int, padChar: Char): String {
        val sb = StringBuilder(str)
        while (sb.length < totalWidth) {
            sb.insert(0, padChar)
        }
        return sb.toString()
    }

    fun padRight(str: String, totalWidth: Int, padChar: Char): String {
        val sb = StringBuilder(str)
        while (sb.length < totalWidth) {
            sb.append(padChar)
        }
        return sb.toString()
    }

    fun getSubString(text: String, left: String?, right: String?): String {
        val zLen: Int
        if (left.isNullOrEmpty()) {
            zLen = 0
        } else {
            val index = text.indexOf(left)
            zLen = if (index > -1) index + left.length else 0
        }
        var yLen = text.indexOf(right!!, zLen)
        if (yLen < 0 || right.isEmpty()) {
            yLen = text.length
        }
        return text.substring(zLen, yLen)
    }
}
