package org.utbot.examples.benchmark

import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.instrumentation.InvokeInstrumentation
import org.utbot.instrumentation.instrumentation.coverage.CoverageInstrumentation
import org.utbot.instrumentation.instrumentation.coverage.collectCoverage
import org.utbot.instrumentation.util.Isolated
import kotlin.system.measureNanoTime
import org.junit.jupiter.api.Assertions.assertEquals


fun getBasicCoverageTime(count: Int): Double {
    var time: Long
    ConcreteExecutor(
        CoverageInstrumentation.Factory,
        Repeater::class.java.protectionDomain.codeSource.location.path
    ).use { executor ->
        val dc0 = Repeater(", ")
        val concat = Isolated(Repeater::concat, executor)

        for (i in 0..20000) {
            val res = concat(dc0, "flex", "mega-", 10)
            assertEquals("mega-mega-mega-mega-mega-mega-mega-mega-mega-mega-flex", res.getOrNull())
        }

        time = measureNanoTime {
            for (i in 0..count) {
                val res = concat(dc0, "flex", "mega-", 10)
                assertEquals("mega-mega-mega-mega-mega-mega-mega-mega-mega-mega-flex", res.getOrNull())
            }
            executor.collectCoverage(Repeater::class.java)
        }
    }
    return time / 1e6
}

fun getNativeCallTime(count: Int): Double {
    val dc0 = Repeater(", ")
    for (i in 0..20000) {
        val res0 = dc0.concat("flex", "mega-", 10)
        assertEquals("mega-mega-mega-mega-mega-mega-mega-mega-mega-mega-flex", res0)
    }
    val time = measureNanoTime {
        for (i in 0..count) {
            dc0.concat("flex", "mega-", 10)
        }
    }
    return time / 1e6
}

fun getJustResultTime(count: Int): Double {
    var time: Long
    ConcreteExecutor(
        InvokeInstrumentation.Factory,
        Repeater::class.java.protectionDomain.codeSource.location.path
    ).use {
        val dc0 = Repeater(", ")
        val concat = Isolated(Repeater::concat, it)

        for (i in 0..20000) {
            val res = concat(dc0, "flex", "mega-", 10)
            assertEquals("mega-mega-mega-mega-mega-mega-mega-mega-mega-mega-flex", res.getOrNull())
        }

        time = measureNanoTime {
            for (i in 0..count) {
                concat(dc0, "flex", "mega-", 10)
            }
        }
    }
    return time / 1e6
}

fun main() {
    val callsCount = 400_000

    val nativeCallTime = getNativeCallTime(callsCount)
    val basicCoverageTime = getBasicCoverageTime(callsCount)
    val justResultTime = getJustResultTime(callsCount)

    println("Running results on $callsCount method calls")
    println("nativeCall: $nativeCallTime ms")
    println("basicCoverage: $basicCoverageTime ms, overhead per call: ${(basicCoverageTime - nativeCallTime) / callsCount} ms")
    println("justResult: $justResultTime ms, overhead per call: ${(justResultTime - nativeCallTime) / callsCount} ms")
}