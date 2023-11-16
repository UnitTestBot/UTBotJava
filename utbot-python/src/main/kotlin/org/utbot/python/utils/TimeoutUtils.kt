package org.utbot.python.utils

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

fun separateUntil(until: Long, currentIndex: Int, itemsCount: Int): Long {
    return when (val itemsLeft = itemsCount - currentIndex) {
        0 -> 0
        1 -> until
        else -> {
            val now = System.currentTimeMillis()
            max((until - now) / itemsLeft + now, 0)
        }
    }
}

fun separateTimeout(until: Long, currentIndex: Int, itemsCount: Int): Long {
    return when (val itemsLeft = itemsCount - currentIndex) {
        0 -> 0
        1 -> {
            val now = System.currentTimeMillis()
            until - now
        }
        else -> {
            val now = System.currentTimeMillis()
            max((until - now) / itemsLeft, 0)
        }
    }
}

fun Long.convertToTime(): String {
    val date = Date(this)
    val format = SimpleDateFormat("HH:mm:ss.SSS")
    return format.format(date)
}