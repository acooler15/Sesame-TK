package fansirsqi.xposed.sesame.core.util
/** 列表工具类，提供对列表的常用操作。 */
object ListUtil {
    /**
     * 创建一个新的ArrayList实例，并使用提供的元素进行初始化。
     * 这是一个泛型方法，可以用于创建并初始化任何类型的列表。
     *
     * @param objects 要添加到列表中的元素。
     * @param T 列表元素的类型。
     * @return 返回包含所有提供元素的新ArrayList。
     */
    @JvmStatic
    fun <T> newArrayList(vararg objects: T): MutableList<T> {
        val list = ArrayList<T>()
        list.addAll(objects)
        return list
    }
}
