package org.utbot.fuzzing

import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.reflect.KClass

class FuzzerSmokeTest {

    @Test
    fun `fuzzing doesn't run with empty parameters`() {
        runBlocking {
            var count = 0
            runFuzzing<Unit, Unit, Description<Unit>, BaseFeedback<Unit, Unit, Unit>>(
                { _, _ -> sequenceOf() },
                Description(emptyList())
            ) { _, _ ->
                count += 1
                BaseFeedback(Unit, Control.STOP)
            }
            Assertions.assertEquals(0, count)
        }
    }

    @Test
    fun `fuzzing throws an exception if no values generated for some type`() {
        assertThrows<IllegalStateException> {
            runBlocking {
                var count = 0
                runFuzzing<Unit, Unit, Description<Unit>, BaseFeedback<Unit, Unit, Unit>>(
                    { _, _ -> sequenceOf() },
                    Description(listOf(Unit))
                ) { _, _ ->
                    count += 1
                    BaseFeedback(Unit, Control.STOP)
                }
                Assertions.assertEquals(0, count)
            }
        }
    }

    @Test
    fun `fuzzing stops on Control$STOP signal after one execution`() {
        runBlocking {
            var count = 0
            runFuzzing(
                { _, _ -> sequenceOf(Seed.Simple(Unit)) },
                Description(listOf(Unit))
            ) { _, _ ->
                count += 1
                BaseFeedback(Unit, Control.STOP)
            }
            Assertions.assertEquals(1, count)
        }
    }

    @Test
    fun `fuzzing stops on Control$STOP signal after 3 execution`() {
        runBlocking {
            var count = 3
            var executions = 0
            runFuzzing(
                { _, _ -> sequenceOf(Seed.Simple(Unit)) },
                Description(listOf(Unit))
            ) { _, _ ->
                executions++
                BaseFeedback(Unit, if (--count == 0) Control.STOP else Control.CONTINUE)
            }
            Assertions.assertEquals(0, count)
            Assertions.assertEquals(3, executions)
        }
    }

    @Test
    fun `fuzzing runs value generation once per type by default`() {
        runBlocking {
            var count = 10
            var generations = 0
            runFuzzing(
                { _, _ ->
                    generations++
                    sequenceOf(Seed.Simple(Unit))
                },
                Description(listOf(Unit))
            ) { _, _ ->
                BaseFeedback(Unit, if (--count == 0) Control.STOP else Control.CONTINUE)
            }
            Assertions.assertEquals(0, count)
            Assertions.assertEquals(1, generations)
        }
    }

    @Test
    fun `fuzzing runs value generation every type when cache is being reset`() {
        runBlocking {
            var count = 10
            var generations = 0
            runFuzzing(
                { _, _ ->
                    generations++
                    sequenceOf(Seed.Simple(Unit))
                },
                Description(listOf(Unit))
            ) { _, _ ->
                BaseFeedback(Unit, if (--count == 0) Control.STOP else Control.RESET_TYPE_CACHE_AND_CONTINUE)
            }
            Assertions.assertEquals(0, count)
            // fuzzing swaps generated and modified values,
            // therefore it can call generation only once,
            // but at the moment it should be called 5 times
            Assertions.assertEquals(5, generations)
        }
    }

    @Test
    fun `fuzzer rethrow exception from execution block`() {
        class SpecialException : Exception()
        runBlocking {
            assertThrows<SpecialException> {
                withTimeout(1000) {
                    runFuzzing(
                        { _, _ -> sequenceOf(Seed.Simple(Unit)) },
                        Description(listOf(Unit))
                    ) { _, _ ->
                        throw SpecialException()
                    }
                }
            }
        }
    }

    @Test
    fun `fuzzer generates recursive data with correct depth`() {
        data class Node(val left: Node?, val right: Node?)

        runBlocking {
            val configuration = Configuration(
                recursionTreeDepth = 10
            )
            var depth = 0
            var count = 0
            runFuzzing(
                ValueProvider<KClass<*>, Node?, Description<KClass<*>>> { _, _ ->
                    sequenceOf(Seed.Recursive(
                        construct = Routine.Create(listOf(Node::class, Node::class)) { v ->
                            Node(v[0], v[1])
                        },
                        empty = Routine.Empty { null }
                    ))
                },
                Description(listOf(Node::class)),
                configuration = configuration
            ) { _, v ->
                fun traverse(n: Node?, l: Int = 1) {
                    n ?: return
                    depth = maxOf(depth, l)
                    count++
                    traverse(n.left, l + 1)
                    traverse(n.right, l + 1)
                }
                traverse(v.first())
                Assertions.assertEquals(configuration.recursionTreeDepth, depth)
                Assertions.assertEquals((1 shl configuration.recursionTreeDepth) - 1, count)
                BaseFeedback(this, Control.STOP)
            }
        }
    }

    @Test
    fun `fuzzer can be cancelled by timeout`() {
        val start = System.currentTimeMillis()
        runBlocking {
            assertThrows<TimeoutCancellationException> {
                withTimeout(1000) {
                    runFuzzing(
                        { _, _ -> sequenceOf(Seed.Simple(Unit)) },
                        Description(listOf(Unit))
                    ) { _, _ ->
                        if (System.currentTimeMillis() - start > 10_000) {
                            error("Fuzzer didn't stopped in 10 000 ms")
                        }
                        BaseFeedback(Unit, Control.CONTINUE)
                    }
                }
            }
        }
    }

    @Test
    fun `fuzzer can be cancelled by coroutine`() {
        runBlocking {
            val deferred = async {
                val start = System.currentTimeMillis()
                runFuzzing(
                    { _, _ -> sequenceOf(Seed.Simple(Unit)) },
                    Description(listOf(Unit))
                ) { _, _ ->
                    if (System.currentTimeMillis() - start > 10_000) {
                        error("Fuzzer didn't stopped in 10_000 ms")
                    }
                    BaseFeedback(Unit, Control.CONTINUE)
                }
            }
            delay(1000)
            deferred.cancel()
        }
    }
}