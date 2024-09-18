package com.ccg.slrcore.system

fun interface BufferTriggerStrategy<T>{
    fun check(buffer: List<T>): Boolean
}

class CapacityFullTrigger<T>(private val capacity: Int): BufferTriggerStrategy<T> {
    override fun check(buffer: List<T>): Boolean {
        return buffer.size == capacity
    }
}

class NoTrigger<T>: BufferTriggerStrategy<T> {
    override fun check(buffer: List<T>): Boolean {
        return false
    }
}
