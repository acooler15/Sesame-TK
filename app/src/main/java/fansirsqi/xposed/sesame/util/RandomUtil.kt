package fansirsqi.xposed.sesame.util

import java.util.Random

/**
 * 随机数工具类，提供生成随机数和随机字符串的方法。
 */
object RandomUtil {
    private val rnd = Random()

    /**
     * 生成一个随机延迟时间（100到300毫秒之间）。
     *
     * @return 生成的随机延迟时间（毫秒）。
     */
    @JvmStatic
    fun delay(): Int = nextInt(100, 300)

    /**
     * 生成一个指定范围内的随机整数。
     *
     * @param min 最小值（包含）。
     * @param max 最大值（不包含）。
     * @return 生成的随机整数。
     */
    @JvmStatic
    fun nextInt(min: Int, max: Int): Int {
        if (min >= max) return min
        return rnd.nextInt(max - min) + min
    }

    /**
     * 生成一个随机的长整数。
     *
     * @return 生成的随机长整数。
     */
    @JvmStatic
    fun nextLong(): Long = rnd.nextLong()

    /**
     * 生成一个指定范围内的随机长整数。
     *
     * @param min 最小值（包含）。
     * @param max 最大值（不包含）。
     * @return 生成的随机长整数。
     */
    @JvmStatic
    fun nextLong(min: Long, max: Long): Long {
        if (min >= max) return min
        val o = max - min
        return (rnd.nextLong() % o) + min
    }

    /**
     * 生成一个随机的双精度浮点数。
     *
     * @return 生成的随机双精度浮点数。
     */
    @JvmStatic
    fun nextDouble(): Double = rnd.nextDouble()

    /**
     * 生成一个指定长度的随机数字字符串。
     *
     * @param len 随机字符串的长度。
     * @return 生成的随机数字字符串。
     */
    @JvmStatic
    fun getRandomInt(len: Int): String {
        val rs = StringBuilder()
        for (i in 0 until len) {
            rs.append(rnd.nextInt(10))
        }
        return rs.toString()
    }

    /**
     * 生成一个指定长度的随机字符串，包含小写字母和数字。
     *
     * @param length 随机字符串的长度。
     * @return 生成的随机字符串。
     */
    @JvmStatic
    fun getRandomString(length: Int): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val sb = StringBuilder()
        for (i in 0 until length) {
            val number = nextInt(0, chars.length)
            sb.append(chars[number])
        }
        return sb.toString()
    }

    @JvmStatic
    fun getRandomTag(): String = "_" + System.currentTimeMillis() + "_" + getRandomString(8)
}
