package org.utbot.examples.benchmark

import org.utbot.examples.samples.benchmark.Fibonacci
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.instrumentation.InvokeInstrumentation
import org.utbot.instrumentation.instrumentation.coverage.CoverageInstrumentation
import org.utbot.instrumentation.instrumentation.coverage.collectCoverage
import org.utbot.instrumentation.util.Isolated
import kotlin.system.measureNanoTime

fun getBasicCoverageTime_fib(count: Int): Double {
    var time: Long
    ConcreteExecutor(
        CoverageInstrumentation.Factory(),
        Fibonacci::class.java.protectionDomain.codeSource.location.path
    ).use {
        val fib = Isolated(Fibonacci::calc, it)
        for (i in 0..20_000) {
            fib(1, 1, 13)
        }

        time = measureNanoTime {
            for (i in 0..count) {
                fib(1, 1, 13)
            }
            it.collectCoverage(Fibonacci::class.java)
        }
    }
    return time / 1e6
}

fun getNativeCallTime_fib(count: Int): Double {
    for (i in 0..20_000) {
        Fibonacci.calc(1, 1, 13)

    }
    val time = measureNanoTime {
        for (i in 0..count) {
            Fibonacci.calc(1, 1, 13)
        }
    }
    return time / 1e6
}

fun getJustResultTime_fib(count: Int): Double {
    var time: Long
    ConcreteExecutor(
        InvokeInstrumentation.Factory(),
        Fibonacci::class.java.protectionDomain.codeSource.location.path
    ).use {
        val fib = Isolated(Fibonacci::calc, it)

        for (i in 0..20_000) {
            fib(1, 1, 13)
        }

        time = measureNanoTime {
            for (i in 0..count) {
                fib(1, 1, 13)
            }
        }
    }
    return time / 1e6
}

fun main() {
    withUtContext(UtContext(ClassLoader.getSystemClassLoader())) {
        val callsCount = 300_000

        val nativeCallTime = getNativeCallTime_fib(callsCount)
        val basicCoverageTime = getBasicCoverageTime_fib(callsCount)
        val justResultTime = getJustResultTime_fib(callsCount)

        println("Running results on $callsCount method calls")
        println("nativeCall: $nativeCallTime ms")
        println("basicCoverage: $basicCoverageTime ms, overhead per call: ${(basicCoverageTime - nativeCallTime) / callsCount} ms")
        println("justResult: $justResultTime ms, overhead per call: ${(justResultTime - nativeCallTime) / callsCount} ms")
    }
}