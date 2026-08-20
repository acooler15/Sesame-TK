package fansirsqi.xposed.sesame.core.reflect

import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * 框架无关的字符串反射工具（纯 java.lang.reflect）。
 *
 * 承接 XposedHelpers 字符串反射族，兼容 Xposed 82 / libxposed 102 双后端：
 * - 按名找字段/方法并自动沿类继承链向上查找；
 * - 自动 setAccessible(true)（hook 场景目标类多为私有）；
 * - 参数匹配兼容基本类型装箱与继承兼容，null 实参按通配处理；
 * - 语义对齐 XposedHelpers：找不到抛 [NoSuchMethodException] / [NoSuchFieldException]，
 *   调用异常解包 [InvocationTargetException] 直抛真实原因。
 */
object ReflectUtil {

    // ---------- 方法调用 ----------

    fun callMethod(obj: Any?, name: String, vararg args: Any?): Any? =
        invoke(findMethodExact(obj!!.javaClass, name, *getParameterTypes(*args)), obj, *args)

    fun callStaticMethod(clazz: Class<*>, name: String, vararg args: Any?): Any? =
        invoke(findMethodExact(clazz, name, *getParameterTypes(*args)), null, *args)

    /** 实例化对象（自动匹配构造器，参数兼容装箱） */
    fun newInstance(clazz: Class<*>, vararg args: Any?): Any? {
        val types = getParameterTypes(*args)
        var current: Class<*>? = clazz
        while (current != null) {
            for (ctor in current.declaredConstructors) {
                if (isMatch(ctor.parameterTypes, types)) {
                    return try {
                        ctor.isAccessible = true
                        ctor.newInstance(*args)
                    } catch (e: InvocationTargetException) {
                        throw (e.cause ?: e)
                    }
                }
            }
            current = current.superclass
        }
        throw NoSuchMethodException("$clazz.<init>(${types.joinToString()})")
    }

    // ---------- 字段访问 ----------

    fun getObjectField(obj: Any, name: String): Any? =
        findField(obj.javaClass, name).get(obj)

    fun getStaticObjectField(clazz: Class<*>, name: String): Any? =
        findField(clazz, name).get(null)

    fun setObjectField(obj: Any, name: String, value: Any?) {
        findField(obj.javaClass, name).set(obj, value)
    }

    fun setStaticObjectField(clazz: Class<*>, name: String, value: Any?) {
        findField(clazz, name).set(null, value)
    }

    // 基本类型取值族（保持 XposedHelpers getXxxField 语义）
    fun getIntField(obj: Any, name: String): Int = findField(obj.javaClass, name).getInt(obj)
    fun getLongField(obj: Any, name: String): Long = findField(obj.javaClass, name).getLong(obj)
    fun getBooleanField(obj: Any, name: String): Boolean = findField(obj.javaClass, name).getBoolean(obj)
    fun getFloatField(obj: Any, name: String): Float = findField(obj.javaClass, name).getFloat(obj)
    fun getDoubleField(obj: Any, name: String): Double = findField(obj.javaClass, name).getDouble(obj)
    fun getByteField(obj: Any, name: String): Byte = findField(obj.javaClass, name).getByte(obj)
    fun getShortField(obj: Any, name: String): Short = findField(obj.javaClass, name).getShort(obj)
    fun getCharField(obj: Any, name: String): Char = findField(obj.javaClass, name).getChar(obj)

    fun getStaticIntField(clazz: Class<*>, name: String): Int = findField(clazz, name).getInt(null)
    fun getStaticLongField(clazz: Class<*>, name: String): Long = findField(clazz, name).getLong(null)
    fun getStaticBooleanField(clazz: Class<*>, name: String): Boolean = findField(clazz, name).getBoolean(null)
    fun getStaticFloatField(clazz: Class<*>, name: String): Float = findField(clazz, name).getFloat(null)
    fun getStaticDoubleField(clazz: Class<*>, name: String): Double = findField(clazz, name).getDouble(null)
    fun getStaticByteField(clazz: Class<*>, name: String): Byte = findField(clazz, name).getByte(null)
    fun getStaticShortField(clazz: Class<*>, name: String): Short = findField(clazz, name).getShort(null)
    fun getStaticCharField(clazz: Class<*>, name: String): Char = findField(clazz, name).getChar(null)

    // ---------- 方法 / 字段查找 ----------

    /** 精确查方法（沿继承链向上，自动 isAccessible） */
    fun findMethodExact(clazz: Class<*>, name: String, vararg types: Class<*>): Method {
        var current: Class<*>? = clazz
        while (current != null) {
            for (m in current.declaredMethods) {
                if (m.name == name && isMatch(m.parameterTypes, types)) {
                    m.isAccessible = true
                    return m
                }
            }
            current = current.superclass
        }
        throw NoSuchMethodException("$clazz.$name(${types.joinToString()})")
    }

    /** 精确查字段（沿继承链向上，自动 isAccessible） */
    fun findField(clazz: Class<*>, name: String): Field {
        var current: Class<*>? = clazz
        while (current != null) {
            for (f in current.declaredFields) {
                if (f.name == name) {
                    f.isAccessible = true
                    return f
                }
            }
            current = current.superclass
        }
        throw NoSuchFieldException("$clazz.$name")
    }

    // ---------- 内部工具 ----------

    private fun invoke(m: Method, obj: Any?, vararg args: Any?): Any? = try {
        m.invoke(obj, *args)
    } catch (e: InvocationTargetException) {
        throw (e.cause ?: e)
    }

    /** 实参 → 形参类型；null 实参以 Object 作通配标记 */
    private fun getParameterTypes(vararg args: Any?): Array<Class<*>> =
        args.map { it?.javaClass ?: java.lang.Object::class.java }.toTypedArray()

    /** 参数匹配：等类型 / 装箱等类型 / 继承兼容（形参类型须能接受实参类型）；Object 通配任意 */
    private fun isMatch(actual: Array<out Class<*>>, expected: Array<out Class<*>>): Boolean {
        if (actual.size != expected.size) return false
        for (i in actual.indices) {
            val e = expected[i]
            if (e == java.lang.Object::class.java) continue
            val a = actual[i]
            if (a == e || box(a) == box(e) || a.isAssignableFrom(e)) continue
            return false
        }
        return true
    }

    private fun box(c: Class<*>): Class<*> = when (c) {
        java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
        java.lang.Byte.TYPE -> java.lang.Byte::class.java
        java.lang.Character.TYPE -> java.lang.Character::class.java
        java.lang.Short.TYPE -> java.lang.Short::class.java
        java.lang.Integer.TYPE -> java.lang.Integer::class.java
        java.lang.Long.TYPE -> java.lang.Long::class.java
        java.lang.Float.TYPE -> java.lang.Float::class.java
        java.lang.Double.TYPE -> java.lang.Double::class.java
        else -> c
    }
}
