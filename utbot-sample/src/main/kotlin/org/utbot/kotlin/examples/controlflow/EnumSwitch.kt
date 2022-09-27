package org.utbot.kotlin.examples.controlflow

import java.math.RoundingMode
import java.math.RoundingMode.*

fun enumSwitch(m: RoundingMode?) =
    when (m) {
        HALF_DOWN, HALF_EVEN, HALF_UP -> 1
        DOWN -> 2
        CEILING -> 3
        else -> -1;
    }