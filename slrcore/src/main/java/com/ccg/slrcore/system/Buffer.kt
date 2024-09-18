package com.ccg.slrcore.system

import android.util.Log
import com.ccg.slrcore.common.SLREventHandler
import java.util.LinkedList
import java.util.HashMap

/**
 * Buffer will accumulate objects till it's capacity is reached, then it will drop oldest objects and keep newest ones to maintain the capacity.
 * @author Ananay Vikram Gupta
 * @version 1.1.0
 */
class Buffer<T> {
    // the internal buffer that actually stores the objects
    private val internalBuffer: LinkedList<T> = LinkedList()

    // callbacks that need to be made when the buffer is full
    private val callbacks: HashMap<String, SLREventHandler<List<T>>> = HashMap()

    var trigger: BufferTriggerStrategy<T> = CapacityFullTrigger<T>(60)
    var filler: BufferFillStrategy<T> = CapacityFill<T>()

    fun addElement(elem: T) {
        Log.d("BUFFER", "Add Element")
        val triggered = trigger.check(this.internalBuffer)
        filler.fill(this.internalBuffer, elem, triggered)
        if (triggered) triggerCallbacks()
    }

    fun triggerCallbacks() {
        Log.d("BUFFER", "Trigger Callbacks")
        callbacks.forEach { (_, cb) -> cb.handle(this.internalBuffer.toList()) }
    }

    /**
     * Add the callbacks required on buffer being full
     */
    fun addCallback(name: String, callback: SLREventHandler<List<T>>) {
        this.callbacks[name] = callback
    }

    fun removeCallback(name: String) {
        if (name in this.callbacks) this.callbacks.remove(name)
    }

    fun clearCallbacks() {
        this.callbacks.clear()
    }

    fun clear() {
        Log.d("BUFFER", "Clear")
        this.internalBuffer.clear()
    }

    val size: Int
        get() {
            return this.internalBuffer.size
        }
}