package com.ccg.slrcore.system

/**
 * Functional interface for defining a strategy to fill a buffer with elements.
 *
 * @param <T> The type of elements in the buffer.
 */
fun interface BufferFillStrategy<T> {
    /**
     * Defines how to add a new element to the buffer.
     *
     * @param buffer    The buffer to which the element is added.
     * @param elem      The element to add to the buffer.
     * @param triggered Indicates whether the buffer's trigger condition has been met.
     */
    fun fill(buffer: MutableList<T>, elem: T, triggered: Boolean)
}

/**
 * A buffer fill strategy that removes the oldest element when the buffer's trigger condition is met.
 *
 * @param <T> The type of elements in the buffer.
 */
class CapacityFill<T>: BufferFillStrategy<T> {
    /**
     * Adds an element to the buffer and removes the oldest element if the trigger condition is met.
     *
     * @param buffer    The buffer to which the element is added.
     * @param elem      The element to add to the buffer.
     * @param triggered Indicates whether the buffer's trigger condition has been met.
     */
    override fun fill(buffer: MutableList<T>, elem: T, triggered: Boolean) {
        buffer.add(elem)
        if (triggered) buffer.removeAt(0)
    }
}

/**
 * A buffer fill strategy that maintains a sliding window of a fixed size.
 *
 * @param <T> The type of elements in the buffer.
 */
open class SlidingWindowFill<T>(private val windowSize: Int): BufferFillStrategy<T> {
    /**
     * Constructs a {@code SlidingWindowFill} with the specified window size.
     *
     * @param windowSize The maximum number of elements the buffer can hold.
     */
    override fun fill(buffer: MutableList<T>, elem: T, triggered: Boolean) {
        buffer.add(elem)
        while (buffer.size > windowSize) {
            buffer.removeAt(0)
        }
    }
}