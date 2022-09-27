package org.utbot.kotlin.examples.arithmetics

class CheckedAddition(var x: Int) {
    fun addWithCheck(y: Int): Int? {
        val res = x.toLong() + y.toLong()
        if (res >= Int.MIN_VALUE && res <= Int.MAX_VALUE) {
            this.x = res.toInt()
            return res.toInt()
        }
        return null
    }
}