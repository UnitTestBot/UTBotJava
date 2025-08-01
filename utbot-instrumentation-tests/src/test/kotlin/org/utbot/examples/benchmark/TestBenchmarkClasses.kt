package org.utbot.examples.benchmark

import org.utbot.examples.samples.benchmark.Fibonacci
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.execute
import org.utbot.instrumentation.instrumentation.InvokeInstrumentation
import org.utbot.instrumentation.instrumentation.coverage.InstructionCoverageInstrumentation
import java.math.BigInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.test.util.UtPair

class TestBenchmarkClasses {
    lateinit var utContext: AutoCloseable

    @Test
    @Disabled("Ask Sergey to check")
    fun testRepeater() {
        ConcreteExecutor(
            InstructionCoverageInstrumentation.Factory,
            Repeater::class.java.protectionDomain.codeSource.location.path
        ).use {
            val dc0 = Repeater(", ")
            val res0 = it.execute(Repeater::concat, arrayOf(dc0, "flex", "mega-", 2))
            assertEquals("mega-mega-flex", res0.getOrNull())


            val dc1 = Unzipper()
            val arr = arrayOf(UtPair(1, 'h'), UtPair(1, 'e'), UtPair(2, 'l'), UtPair(1, 'o'))
            val res1 = it.execute(Unzipper::unzip, arrayOf(dc1, arr))
            assertEquals("h-e-ll-o-", res1.getOrNull())
        }
    }

    @Test
    fun testFibonacci() {
        ConcreteExecutor(
            InvokeInstrumentation.Factory,
            Fibonacci::class.java.protectionDomain.codeSource.location.path
        ).use {
            val res =
                it.execute(
                    Fibonacci::calc,
                    arrayOf(1, 1, 10)
                )
            assertEquals(Result.success(BigInteger.valueOf(89)), res)
        }
    }
}

