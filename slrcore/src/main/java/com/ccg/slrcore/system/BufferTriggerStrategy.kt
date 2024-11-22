package com.ccg.slrcore.system

/**
 * Functional interface for defining a strategy to determine when a buffer should trigger an action.
 *
 * @param <T> The type of elements in the buffer.
 */
fun interface BufferTriggerStrategy<T>{
    /**
     * Checks if the buffer meets the trigger condition.
     *
     * @param buffer The buffer to evaluate.
     * @return {@code true} if the trigger condition is met, {@code false} otherwise.
     */
    fun check(buffer: List<T>): Boolean
}

/**
 * A trigger strategy that activates when the buffer reaches its specified capacity.
 *
 * @param <T> The type of elements in the buffer.
 */
class CapacityFullTrigger<T>(private val capacity: Int): BufferTriggerStrategy<T> {
    /**
     * Checks if the buffer size equals the specified capacity.
     *
     * @param buffer The buffer to evaluate.
     * @return {@code true} if the buffer size equals the capacity, {@code false} otherwise.
     */
    override fun check(buffer: List<T>): Boolean {
        return buffer.size == capacity
    }
}

/**
 * A trigger strategy that never activates, regardless of the buffer's state.
 *
 * @param <T> The type of elements in the buffer.
 */
class NoTrigger<T>: BufferTriggerStrategy<T> {
    /**
     * Always returns {@code false}, indicating that the trigger condition is never met.
     *
     * @param buffer The buffer to evaluate.
     * @return {@code false}, indicating no trigger.
     */
    override fun check(buffer: List<T>): Boolean {
        return false
    }
}
