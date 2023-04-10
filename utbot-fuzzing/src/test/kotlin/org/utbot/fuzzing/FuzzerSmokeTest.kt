package org.utbot.fuzzing

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.utbot.fuzzing.seeds.BitVectorValue
import org.utbot.fuzzing.seeds.Signed
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
        assertThrows<NoSeedValueException> {
            runBlocking {
                var count = 0
                runFuzzing<Unit, Unit, Description<Unit>, BaseFeedback<Unit, Unit, Unit>>(
                    provider = { _, _ -> sequenceOf() },
                    description = Description(listOf(Unit)),
                    configuration = Configuration(
                        generateEmptyCollectionsForMissedTypes = false
                    )
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

    @Test
    fun `fuzzer generate same result when random is seeded`() {
        data class B(var a: Int)
        val provider = ValueProvider<Unit, B, Description<Unit>> { _, _ ->
            sequenceOf(
                Seed.Simple(B(0)) { p, r -> B(p.a + r.nextInt()) },
                Seed.Known(BitVectorValue(32, Signed.POSITIVE)) { B(it.toInt()) },
                Seed.Recursive(
                    construct = Routine.Create(listOf(Unit)) { _ -> B(2) },
                    modify = sequenceOf(
                        Routine.Call(listOf(Unit)) { self, _ -> self.a = 3 },
                        Routine.Call(listOf(Unit)) { self, _ -> self.a = 4 },
                        Routine.Call(listOf(Unit)) { self, _ -> self.a = 5 },
                        Routine.Call(listOf(Unit)) { self, _ -> self.a = 6 },
                    ),
                    empty = Routine.Empty { B(7) }
                ),
                Seed.Collection(
                    construct = Routine.Collection { size -> B(size) },
                    modify = Routine.ForEach(listOf(Unit)) { self, ind, v -> self.a = ind * self.a * v.first().a }
                )
            )
        }
        fun createValues(): MutableList<Int> {
            val result = mutableListOf<Int>()
            val probes = 1_000
            runBlocking {
                runFuzzing(provider, Description(listOf(Unit))) { _, v ->
                    result.add(v.first().a)
                    BaseFeedback(Unit, if (result.size >= probes) Control.STOP else Control.CONTINUE)
                }
            }
            return result
        }
        val firstRun = createValues()
        val secondRun = createValues()
        val thirdRun = createValues()
        Assertions.assertEquals(firstRun, secondRun)
        Assertions.assertEquals(firstRun, thirdRun)
        Assertions.assertEquals(secondRun, thirdRun)
    }

    @Test
    fun `check flow invariant is not violated`() {
        val timeConsumer: (Long) -> Unit = {}
        runBlocking {
            val deferred = async {
                val start = System.currentTimeMillis()
                flow {
                    runFuzzing(
                        { _, _ -> sequenceOf(Seed.Simple(Unit)) },
                        Description(listOf(Unit))
                    ) { _, _ ->
                        if (System.currentTimeMillis() - start > 10_000) {
                            error("Fuzzer didn't stopped in 10_000 ms")
                        }
                        emit(System.currentTimeMillis())
                        BaseFeedback(Unit, Control.CONTINUE)
                    }
                }.collect {
                    timeConsumer(it)
                }
            }
            delay(1000)
            deferred.cancel()
        }
    }
}