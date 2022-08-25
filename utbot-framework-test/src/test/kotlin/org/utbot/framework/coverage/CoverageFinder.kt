package org.utbot.framework.coverage

import org.utbot.examples.controlflow.CycleDependedCondition
import org.utbot.framework.plugin.api.UtMethod
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberFunctions
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.data.SessionInfoStore
import org.jacoco.core.instr.Instrumenter
import org.jacoco.core.runtime.IRuntime
import org.jacoco.core.runtime.LoggerRuntime
import org.jacoco.core.runtime.RuntimeData

fun main() {
    println("test ints from -100 to 100")
    val result = coverage(CycleDependedCondition::twoCondition, -100..100)
    result.forEach { (range, coverage) ->
        println("${range.prettify()} -> ${coverage.toAtLeast()} $coverage")
    }
}

private fun coverage(
    method: KFunction<*>,
    range: IntRange
): List<IncrementalCoverage<IntRange>> {
    val coverageSeq = coverage(method, range.asSequence().map { arrayOf(it) })
    return coverageSeq.fold(mutableListOf()) { acc, (params, coverage) ->
        val value = params[0] as Int
        if (acc.isEmpty() || coverage > acc.last().coverage) {
            acc.add(IncrementalCoverage(value..value, coverage))
        } else {
            acc[acc.lastIndex] = acc.last().copy(params = acc.last().params.first..value)
        }
        acc
    }
}

private fun coverage(
    method: KFunction<*>,
    paramSeq: Sequence<Array<*>>
): Sequence<IncrementalCoverage<Array<*>>> {
    val (_, clazz) = UtMethod.from(method)
    val calculator = CoverageCalculator(clazz, method.name)
    val targetClass = calculator.instrumentedClass
    val targetMethod = targetClass.memberFunctions.first { it.name == method.name }
    return paramSeq.map { params ->
        try {
            targetMethod.call(targetClass.createInstance(), *params)
        } catch (_: InvocationTargetException) {
        }
        val coverage = calculator.calc()
        IncrementalCoverage(params, coverage)
    }
}

private fun IntRange.prettify(): String = if (this.first == this.last) "${this.first}" else "$this"

private class CoverageCalculator(val clazz: KClass<*>, private val methodName: String) {
    private val targetName: String = clazz.qualifiedName!!
    private val data: RuntimeData
    val instrumentedClass: KClass<*>

    init {
        val runtime: IRuntime = LoggerRuntime()

        val instrumented = clazz.asInputStream().use {
            Instrumenter(runtime).instrument(it, targetName)
        }

        data = RuntimeData()
        runtime.startup(data)

        instrumentedClass = MemoryClassLoader().apply {
            addDefinition(targetName, instrumented)
        }.loadClass(targetName).kotlin
    }

    fun calc(): Coverage {
        val methodCoverage = collectCoverage().classes.first { it.name == instrumentedClass.simpleName }
            .methods.first { it.name == methodName }
        return methodCoverage.toMethodCoverage()
    }

    private fun collectCoverage() = CoverageBuilder().apply {
        val eD = ExecutionDataStore()
        val sI = SessionInfoStore()
        data.collect(eD, sI, false)
        val analyzer = Analyzer(eD, this)
        clazz.asInputStream().use {
            analyzer.analyzeClass(it, targetName)
        }
    }
}

data class IncrementalCoverage<T>(val params: T, val coverage: Coverage)