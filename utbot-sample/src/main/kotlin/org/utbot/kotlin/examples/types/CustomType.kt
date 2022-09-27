package org.utbot.kotlin.examples.types

class PositivesContainer(
    private var first: Long,
    second: Long,
) {
    fun setFirst(value: Long) = require(value > 0).also { first = value }
    fun getFirst(): Long = first

    // Kotlin's property syntax is not fully supported yet, so there won't be tests for these getters and setters
    var second: Long = second
        set(value) = require(value > 0).also { field = value }
}
