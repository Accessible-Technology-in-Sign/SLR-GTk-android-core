package com.ccg.slrcore.system

import android.util.Log
import com.ccg.slrcore.common.SLREventHandler
import java.util.LinkedList
import java.util.HashMap

/**
 * A buffer that accumulates objects until a specified capacity is reached.
 * When the buffer is full, it removes the oldest objects to maintain the capacity,
 * and triggers callbacks for processing the accumulated objects.
 *
 * @param <T> The type of elements stored in the buffer.
 */
class Buffer<T> {
    /**
     * The internal buffer for storing objects.
     */
    private val internalBuffer: LinkedList<T> = LinkedList()

    /**
     * Callbacks to be triggered when the buffer reaches its trigger condition.
     */
    private val callbacks: HashMap<String, SLREventHandler<List<T>>> = HashMap()

    /**
     * Strategy to determine when the buffer should trigger callbacks.
     */
    var trigger: BufferTriggerStrategy<T> = CapacityFullTrigger<T>(60)

    /**
     * Strategy to determine how the buffer should fill when new elements are added.
     */
    var filler: BufferFillStrategy<T> = CapacityFill<T>()

    /**
     * Adds an element to the buffer. If the trigger condition is met, the callbacks are executed.
     *
     * @param elem The element to add to the buffer.
     */
    fun addElement(elem: T) {
        Log.d("BUFFER", "Add Element")
        val triggered = trigger.check(this.internalBuffer)
        filler.fill(this.internalBuffer, elem, triggered)
        if (triggered) triggerCallbacks()
    }

    /**
     * Triggers all registered callbacks with the current state of the buffer.
     */
    fun triggerCallbacks() {
        Log.d("BUFFER", "Trigger Callbacks")
        callbacks.forEach { (_, cb) -> cb.handle(this.internalBuffer.toList()) }
    }

    /**
     * Adds a callback to be executed when the buffer trigger condition is met.
     *
     * @param name     The name of the callback.
     * @param callback The callback handler to execute.
     */
    fun addCallback(name: String, callback: SLREventHandler<List<T>>) {
        this.callbacks[name] = callback
    }

    /**
     * Removes a previously registered callback.
     *
     * @param name The name of the callback to remove.
     */
    fun removeCallback(name: String) {
        if (name in this.callbacks) this.callbacks.remove(name)
    }


    /**
     * Clears all registered callbacks.
     */
    fun clearCallbacks() {
        this.callbacks.clear()
    }

    /**
     * Clears the buffer, removing all elements.
     */
    fun clear() {
        Log.d("BUFFER", "Clear")
        this.internalBuffer.clear()
    }

    /**
     * Gets the current size of the buffer.
     *
     * @return The number of elements in the buffer.
     */
    val size: Int
        get() {
            return this.internalBuffer.size
        }
}