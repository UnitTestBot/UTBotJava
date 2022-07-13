package org.utbot.framework.coverage

import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtValueExecution
import org.utbot.framework.plugin.api.UtMethodValueTestSet
import org.utbot.framework.plugin.api.util.signature
import org.utbot.framework.util.anyInstance
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.execute
import org.utbot.instrumentation.instrumentation.coverage.CoverageInfo
import org.utbot.instrumentation.instrumentation.coverage.CoverageInstrumentation
import org.utbot.instrumentation.instrumentation.coverage.collectCoverage
import org.utbot.instrumentation.util.StaticEnvironment
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.instanceParameter
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.analysis.IClassCoverage
import org.jacoco.core.analysis.ICounter
import org.jacoco.core.analysis.IMethodCoverage
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.data.SessionInfoStore
import org.jacoco.core.instr.Instrumenter
import org.jacoco.core.runtime.IRuntime
import org.jacoco.core.runtime.LoggerRuntime
import org.jacoco.core.runtime.RuntimeData

fun instrument(clazz: KClass<*>, instrumenter: Instrumenter): ByteArray =
    clazz.asInputStream().use {
        instrumenter.instrument(it, clazz.qualifiedName)
    }

fun calculateClassCoverage(targetClass: KClass<*>, testClass: KClass<*>): Coverage {
    val targetName = targetClass.qualifiedName!!
    val testClassName = testClass.qualifiedName!!

    // IRuntime instance to collect execution data
    val runtime: IRuntime = LoggerRuntime()

    // create a modified version of target class with probes
    val instrumenter = Instrumenter(runtime)
    val instrumentedTarget = instrument(targetClass, instrumenter)
    val instrumentedTestClass = instrument(testClass, instrumenter)

    // startup the runtime
    val data = RuntimeData()
    runtime.startup(data)

    // load class from byte[] instances
    val memoryClassLoader = MemoryClassLoader()
    memoryClassLoader.addDefinition(targetName, instrumentedTarget)
    memoryClassLoader.addDefinition(testClassName, instrumentedTestClass)

    val instrumentedTests = memoryClassLoader.loadClass(testClassName).kotlin

    val tests = instrumentedTests.declaredFunctions
    val testClassInstance = instrumentedTests.createInstance()
    tests.forEach {
        it.call(testClassInstance)
    }

    // shutdown the runtime
    val executionData = ExecutionDataStore()
    val sessionInfos = SessionInfoStore()
    data.collect(executionData, sessionInfos, false)
    runtime.shutdown()

    // get coverage builder
    val coverageBuilder = CoverageBuilder().apply {
        val analyzer = Analyzer(executionData, this)
        targetClass.asInputStream().use {
            analyzer.analyzeClass(it, targetName)
        }
    }

    val methodsCoverage = coverageBuilder.classes
        .single { it.qualifiedName == targetName }
        .methods
        .map { it.toMethodCoverage() }

    return methodsCoverage.toClassCoverage()
}

fun calculateCoverage(clazz: KClass<*>, block: (KClass<*>) -> Unit): CoverageBuilder {
    val targetName = clazz.qualifiedName!!

    // IRuntime instance to collect execution data
    val runtime: IRuntime = LoggerRuntime()

    // create a modified version of target class with probes
    val instrumenter = Instrumenter(runtime)
    val instrumented = instrument(clazz, instrumenter)

    // startup the runtime
    val data = RuntimeData()
    runtime.startup(data)

    // load class from byte[] instances
    val memoryClassLoader = MemoryClassLoader()
    memoryClassLoader.addDefinition(targetName, instrumented)
    val targetClass = memoryClassLoader.loadClass(targetName).kotlin

    // execute code
    block(targetClass)

    // shutdown the runtime
    val executionData = ExecutionDataStore()
    val sessionInfos = SessionInfoStore()
    data.collect(executionData, sessionInfos, false)
    runtime.shutdown()

    // calculate coverage
    return CoverageBuilder().apply {
        val analyzer = Analyzer(executionData, this)
        clazz.asInputStream().use {
            analyzer.analyzeClass(it, targetName)
        }
    }
}

fun KClass<*>.asInputStream(): InputStream =
    java.getResourceAsStream("/${qualifiedName!!.replace('.', '/')}.class")!!

class MemoryClassLoader : ClassLoader() {
    private val definitions: MutableMap<String, ByteArray> = HashMap()

    fun addDefinition(name: String, bytes: ByteArray) {
        definitions[name] = bytes
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        val bytes = definitions[name]
        return if (bytes != null) {
            defineClass(name, bytes, 0, bytes.size)
        } else super.loadClass(name, resolve)
    }
}

fun classCoverage(testSets: List<UtMethodValueTestSet<*>>): Coverage =
    testSets.map { methodCoverageWithJaCoCo(it.method, it.executions) }.toClassCoverage()

fun methodCoverageWithJaCoCo(utMethod: UtMethod<*>, executions: List<UtValueExecution<*>>): Coverage {
    val methodSignature = utMethod.callable.signature
    val coverage = calculateCoverage(utMethod.clazz) { clazz ->
        val method = clazz.declaredFunctions.single { it.signature == methodSignature }
        val onInstance = method.instanceParameter != null
        for (execution in executions) {
            try {
                if (onInstance) {
                    method.call(clazz.java.anyInstance, *execution.stateBefore.params.map { it.value }.toTypedArray())
                } else {
                    method.call(*execution.stateBefore.params.map { it.value }.toTypedArray())
                }
            } catch (_: InvocationTargetException) {
            }
        }
    }
    val methodCoverage = coverage.classes
        .single { it.qualifiedName == utMethod.clazz.qualifiedName }
        .methods
        .single {
            "${it.name}${it.desc}" == methodSignature
        }

    return methodCoverage.toMethodCoverage()
}

fun methodCoverage(utMethod: UtMethod<*>, executions: List<UtValueExecution<*>>, classpath: String): Coverage {
    val methodSignature = utMethod.callable.signature
    val clazz = utMethod.clazz
    return ConcreteExecutor(CoverageInstrumentation, classpath).let { executor ->
        val method = clazz.declaredFunctions.single { it.signature == methodSignature }
        val onInstance = method.instanceParameter != null
        for (execution in executions) {
            val args = execution.stateBefore.params.map { it.value }.toMutableList()
            if (onInstance) {
                args.add(0, clazz.java.anyInstance)
            }
            val staticEnvironment = StaticEnvironment(
                execution.stateBefore.statics.map { it.key to it.value.value }
            )
            executor.execute(method, args.toTypedArray(), parameters = staticEnvironment)
        }

        val coverage = executor.collectCoverage(clazz.java)
        coverage.toMethodCoverage(methodSignature)
    }
}

fun CoverageInfo.toMethodCoverage(methodSignature: String): Coverage {
    val methodRange = methodToInstrRange[methodSignature]!!
    val visitedCount = visitedInstrs.count { it in methodRange }
    return Coverage(
        Counter(),
        Counter(methodRange.count(), visitedCount, methodRange.count() - visitedCount),
        Counter()
    )
}

fun IMethodCoverage.toMethodCoverage(): Coverage =
    Coverage(branchCounter.toCounter(), instructionCounter.toCounter(), lineCounter.toCounter())

private fun ICounter.toCounter(): Counter = Counter(totalCount, coveredCount, missedCount)

data class Coverage(
    val branchCounter: Counter = Counter(),
    val instructionCounter: Counter = Counter(),
    val lineCounter: Counter = Counter()
) {
    override fun toString() = "(branches: $branchCounter, instructions: $instructionCounter, lines: $lineCounter)"
}

fun List<Coverage>.toClassCoverage(): Coverage {
    var branchCounter = Counter()
    var instructionCounter = Counter()
    var lineCounter = Counter()
    forEach {
        branchCounter += it.branchCounter
        instructionCounter += it.instructionCounter
        lineCounter += it.lineCounter
    }
    return Coverage(branchCounter, instructionCounter, lineCounter)
}

operator fun Counter.plus(other: Counter): Counter =
    Counter(
        total + other.total,
        covered + other.covered,
        missed + other.missed
    )

private val IClassCoverage.qualifiedName: String
    get() = this.name.replace('/', '.')

data class Counter(val total: Int = 0, val covered: Int = 0, val missed: Int = 0) {
    override fun toString() = "$covered/$total"
}

val Coverage.counters: List<Counter>
    get() = listOf(branchCounter, instructionCounter, lineCounter)

operator fun Coverage.compareTo(other: Coverage) = this.sumCovered().compareTo(other.sumCovered())

fun Coverage.toAtLeast(): Int =
    this.counters.minOf { if (it.total == 0) 100 else (it.covered * 100 / it.total) }

fun Coverage.sumCovered(): Int = this.counters.sumBy { it.covered }
