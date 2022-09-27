package org.utbot.kotlin.examples.controlflow

fun lookupSwitch(x: Int): Int {
    return when (x) {
        0 -> 0
        10, 20 -> 20
        30 -> 30
        else -> -1
    }
}