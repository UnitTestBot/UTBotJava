package org.utbot.engine.greyboxfuzzer.quickcheck.internal

class Weighted<T>(item: T, weight: Int) {
    val item: T
    val weight: Int

    init {
        require(weight > 0) { "non-positive weight: $weight" }
        this.item = item
        this.weight = weight
    }
}