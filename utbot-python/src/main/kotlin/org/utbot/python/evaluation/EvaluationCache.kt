package org.utbot.python.evaluation

import mu.KotlinLogging
import org.utbot.python.framework.api.python.PythonTreeWrapper
import org.utbot.python.fuzzing.PythonExecutionResult
import org.utbot.python.fuzzing.PythonMethodDescription

private const val MAX_CACHE_SIZE = 200

class EvaluationCache {
    private val cache = mutableMapOf<Pair<PythonMethodDescription, List<PythonTreeWrapper>>, PythonExecutionResult>()

    fun add(key: Pair<PythonMethodDescription, List<PythonTreeWrapper>>, result: PythonExecutionResult) {
        cache[key] = result
        if (cache.size > MAX_CACHE_SIZE) {
            val elemToDelete = cache.keys.maxBy { (_, args) ->
                args.fold(0) { acc, arg -> arg.commonDiversity(acc) }
            }
            cache.remove(elemToDelete)
        }
    }

    fun get(key: Pair<PythonMethodDescription, List<PythonTreeWrapper>>): PythonExecutionResult? {
        return cache[key]
    }
}