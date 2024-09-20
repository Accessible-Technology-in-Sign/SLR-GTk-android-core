package com.ccg.slrcore.common

/**
 * An event handler will fire when an event triggers it (frame captured, hand result generated, sign detected etc.)
 * and call the handle function with the correct context.
 * @author Ananay Vikram Gupta
 * @version 1.0.0
 */
fun interface SLREventHandler<T> {
    fun handle(context: T)
}