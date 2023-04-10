package org.utbot.fuzzing.demo

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.utbot.fuzzing.*
import java.util.concurrent.atomic.AtomicLong

private enum class Type {
    ANY, CONCRETE, MORE_CONCRETE
}

fun main(): Unit = runBlocking {
    launch {
        object : Fuzzing<Type, String, Description<Type>, Feedback<Type, String>> {

            private val runs = mutableMapOf<Type, AtomicLong>()

            override fun generate(description: Description<Type>, type: Type): Sequence<Seed<Type, String>> {
                return sequenceOf(Seed.Simple(type.name))
            }

            override suspend fun handle(description: Description<Type>, values: List<String>): Feedback<Type, String> {
                description.parameters.forEach {
                    runs[it]!!.incrementAndGet()
                }
                println(values)
                return emptyFeedback()
            }

            override suspend fun afterIteration(
                description: Description<Type>,
                stats: Statistic<Type, String>,
            ) {
                if (stats.totalRuns % 10 == 0L && description.parameters.size == 1) {
                    val newTypes = when (description.parameters[0]) {
                        Type.ANY -> listOf(Type.CONCRETE)
                        Type.CONCRETE -> listOf(Type.MORE_CONCRETE)
                        Type.MORE_CONCRETE -> listOf()
                    }
                    if (newTypes.isNotEmpty()) {
                        val d = Description(newTypes)
                        fork(d, stats)
                        // Description can be used as a transfer object,
                        // that collects information about the current running.
                        println("Fork ended: ${d.parameters}")
                    }
                }
            }

            override suspend fun isCancelled(description: Description<Type>, stats: Statistic<Type, String>): Boolean {
                println("info: ${description.parameters} runs ${stats.totalRuns}")
                return description.parameters.all { runs.computeIfAbsent(it) { AtomicLong(0) }.get() >= 10 }
            }
        }.fuzz(Description(listOf(Type.ANY)))
    }
//        .cancel()
}