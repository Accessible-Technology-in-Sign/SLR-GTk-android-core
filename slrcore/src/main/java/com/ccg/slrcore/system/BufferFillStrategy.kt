package com.ccg.slrcore.system

fun interface BufferFillStrategy<T> {
    fun fill(buffer: MutableList<T>, elem: T, triggered: Boolean)
}

class CapacityFill<T>: BufferFillStrategy<T> {
    override fun fill(buffer: MutableList<T>, elem: T, triggered: Boolean) {
        buffer.add(elem)
        if (triggered) buffer.removeAt(0)
    }
}

open class SlidingWindowFill<T>(private val windowSize: Int): BufferFillStrategy<T> {
    override fun fill(buffer: MutableList<T>, elem: T, triggered: Boolean) {
        buffer.add(elem)
        while (buffer.size > windowSize) {
            buffer.removeAt(0)
        }
    }
}