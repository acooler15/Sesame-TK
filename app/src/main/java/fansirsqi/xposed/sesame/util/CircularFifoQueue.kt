package fansirsqi.xposed.sesame.util

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serial
import java.io.Serializable
import java.util.NoSuchElementException
import java.util.Queue

/** 源码来自：Apache Commons Collections 4.4 少量定制 */
class CircularFifoQueue<E>(size: Int) : AbstractCollection<E>(), Queue<E>, Serializable {
    /** Underlying storage array. */
    @Transient
    private var elements: Array<Any?>

    /** Array index of first (oldest) queue element. */
    @Transient
    private var start = 0

    /**
     * Index mod maxElements of the array position following the last queue element. Queue elements
     * start at elements[start] and "wrap around" elements[maxElements-1], ending at
     * elements[decrement(end)]. For example, elements = {c,a,b}, start=1, end=1 corresponds to the
     * queue [a,b,c].
     */
    @Transient
    private var end = 0

    /** Flag to indicate if the queue is currently full. */
    @Transient
    private var full = false

    /** Capacity of the queue. */
    private val maxElements: Int

    init {
        /**
         * Constructor that creates a queue with the specified size.
         *
         * @param size the size of the queue (cannot be changed)
         * @throws IllegalArgumentException if the size is &lt; 1
         */
        require(size > 0) { "The size must be greater than 0" }
        elements = arrayOfNulls(size)
        maxElements = elements.size
    }

    // -----------------------------------------------------------------------

    /**
     * Write the queue out using a custom routine.
     *
     * @param out the output stream
     * @throws IOException if an I/O error occurs while writing to the output stream
     */
    @Serial
    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        out.defaultWriteObject()
        out.writeInt(size)
        for (e in this) {
            out.writeObject(e)
        }
    }

    /**
     * Read the queue in using a custom routine.
     *
     * @param in the input stream
     * @throws IOException if an I/O error occurs while writing to the input stream
     * @throws ClassNotFoundException if a serialized object can not be found
     */
    @Serial
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`in`: ObjectInputStream) {
        `in`.defaultReadObject()
        elements = arrayOfNulls(maxElements)
        val size = `in`.readInt()
        for (i in 0 until size) {
            @Suppress("UNCHECKED_CAST")
            elements[i] = `in`.readObject() as E
        }
        start = 0
        full = size == maxElements
        if (full) {
            end = 0
        } else {
            end = size
        }
    }

    // -----------------------------------------------------------------------

    /**
     * Returns the number of elements stored in the queue.
     *
     * @return this queue's size
     */
    override val size: Int
        get() {
            val size: Int
            if (end < start) {
                size = maxElements - start + end
            } else if (end == start) {
                size = if (full) maxElements else 0
            } else {
                size = end - start
            }
            return size
        }

    /**
     * Returns true if this queue is empty; false otherwise.
     *
     * @return true if this queue is empty
     */
    override fun isEmpty(): Boolean = size == 0

    /**
     * {@inheritDoc}
     *
     *
     * A `CircularFifoQueue` can never be full, thus this returns always
     * `false`.
     *
     * @return always returns `false`
     */
    fun isFull(): Boolean = false

    /**
     * Returns `true` if the capacity limit of this queue has been reached, i.e. the number of
     * elements stored in the queue equals its maximum size.
     *
     * @return `true` if the capacity limit has been reached, `false` otherwise
     * @since 4.1
     */
    fun isAtFullCapacity(): Boolean = size == maxElements

    /** Clears this queue. */
    override fun clear() {
        full = false
        start = 0
        end = 0
        elements.fill(null)
    }

    /**
     * Adds all of the elements in the specified collection to this queue.
     * 行为等价于 java.util.AbstractCollection#addAll（逐个调用 add）。
     */
    override fun addAll(elements: Collection<E>): Boolean {
        var modified = false
        for (e in elements) {
            if (add(e)) {
                modified = true
            }
        }
        return modified
    }

    /**
     * 行为等价于 java.util.AbstractCollection#removeAll（基于迭代器移除）。
     */
    override fun removeAll(elements: Collection<E>): Boolean {
        var modified = false
        val it = iterator()
        while (it.hasNext()) {
            if (elements.contains(it.next())) {
                it.remove()
                modified = true
            }
        }
        return modified
    }

    /**
     * 行为等价于 java.util.AbstractCollection#retainAll（基于迭代器移除）。
     */
    override fun retainAll(elements: Collection<E>): Boolean {
        var modified = false
        val it = iterator()
        while (it.hasNext()) {
            if (!elements.contains(it.next())) {
                it.remove()
                modified = true
            }
        }
        return modified
    }

    /**
     * 行为等价于 java.util.AbstractCollection#remove(Object)，原实现不支持按元素移除。
     */
    override fun remove(element: E): Boolean = throw UnsupportedOperationException("remove")

    fun push(element: E): E? {
        if (null == element) {
            throw NullPointerException("Attempted to add null object to queue")
        }
        val oldElement: E?
        if (isAtFullCapacity()) {
            oldElement = remove()
        } else {
            oldElement = null
        }
        elements[end] = element // 先写入
        end = increment(end) // 再移动 end（自动回绕）
        if (end == start) {
            full = true
        }
        return oldElement
    }

    /**
     * Adds the given element to this queue. If the queue is full, the least recently added element
     * is discarded so that a new element can be inserted.
     *
     * @param element the element to add
     * @return true, always
     * @throws NullPointerException if the given element is null
     */
    override fun add(element: E): Boolean {
        if (null == element) {
            throw NullPointerException("Attempted to add null object to queue")
        }
        if (isAtFullCapacity()) {
            remove()
        }

        elements[end] = element
        end = increment(end) // 使用 increment 方法

        if (end == start) {
            full = true
        }
        return true
    }

    /**
     * Returns the element at the specified position in this queue.
     *
     * @param index the position of the element in the queue
     * @return the element at position `index`
     * @throws NoSuchElementException if the requested position is outside the range [0, size)
     */
    operator fun get(index: Int): E {
        val sz = size
        if (index < 0 || index >= sz) {
            throw NoSuchElementException(
                String.format("The specified index (%1\$d) is outside the available range [0, %2\$d)",
                    index, sz))
        }
        val idx = (start + index) % maxElements
        @Suppress("UNCHECKED_CAST")
        return elements[idx] as E
    }

    // -----------------------------------------------------------------------

    /**
     * Adds the given element to this queue. If the queue is full, the least recently added element
     * is discarded so that a new element can be inserted.
     *
     * @param element the element to add
     * @return true, always
     * @throws NullPointerException if the given element is null
     */
    override fun offer(element: E): Boolean = add(element)

    override fun poll(): E? {
        return if (isEmpty()) {
            null
        } else remove()
    }

    override fun element(): E {
        if (isEmpty()) {
            throw NoSuchElementException("queue is empty")
        }
        return peek()!!
    }

    override fun peek(): E? {
        if (isEmpty()) {
            return null
        }
        @Suppress("UNCHECKED_CAST")
        return elements[start] as E
    }

    override fun remove(): E {
        if (isEmpty()) {
            throw NoSuchElementException("queue is empty")
        }
        @Suppress("UNCHECKED_CAST")
        val element = elements[start] as E
        if (null != element) {
            elements[start++] = null
            if (start >= maxElements) {
                start = 0
            }
            full = false
        }
        return element
    }

    // -----------------------------------------------------------------------

    /**
     * Increments the internal index.
     *
     * @param index the index to increment
     * @return the updated index
     */
    private fun increment(index: Int): Int {
        var idx = index
        idx++
        if (idx >= maxElements) {
            idx = 0
        }
        return idx
    }

    /**
     * Decrements the internal index.
     *
     * @param index the index to decrement
     * @return the updated index
     */
    private fun decrement(index: Int): Int {
        var idx = index
        idx--
        if (idx < 0) {
            idx = maxElements - 1
        }
        return idx
    }

    /**
     * Returns an iterator over this queue's elements.
     *
     * @return an iterator over this queue's elements
     */
    override fun iterator(): MutableIterator<E> {
        return object : MutableIterator<E> {
            private var index = start
            private var lastReturnedIndex = -1
            private var isFirst = full

            override fun hasNext(): Boolean = isFirst || index != end

            override fun next(): E {
                if (!hasNext()) {
                    throw NoSuchElementException()
                }
                isFirst = false
                lastReturnedIndex = index
                index = increment(index)
                @Suppress("UNCHECKED_CAST")
                return elements[lastReturnedIndex] as E
            }

            override fun remove() {
                if (lastReturnedIndex == -1) {
                    throw IllegalStateException()
                }
                // First element can be removed quickly
                if (lastReturnedIndex == start) {
                    this@CircularFifoQueue.remove()
                    lastReturnedIndex = -1
                    return
                }
                var pos = lastReturnedIndex + 1
                if (start < lastReturnedIndex && pos < end) {
                    // shift in one part
                    System.arraycopy(elements, pos, elements, lastReturnedIndex, end - pos)
                } else {
                    // Other elements require us to shift the subsequent elements
                    while (pos != end) {
                        if (pos >= maxElements) {
                            elements[pos - 1] = elements[0]
                            pos = 0
                        } else {
                            elements[decrement(pos)] = elements[pos]
                            pos = increment(pos)
                        }
                    }
                }
                lastReturnedIndex = -1
                end = decrement(end)
                elements[end] = null
                full = false
                index = decrement(index)
            }
        }
    }

    companion object {
        /** Serialization version. */
        @Serial
        private const val serialVersionUID: Long = -8423413834657610406L
    }
}
