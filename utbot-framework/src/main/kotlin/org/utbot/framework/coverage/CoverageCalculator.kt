package org.utbot.framework.coverage

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
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtValueExecution
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.instrumentation.coverage.CoverageInfo
import org.utbot.instrumentation.instrumentation.coverage.CoverageInstrumentation
import org.utbot.instrumentation.instrumentation.coverage.collectCoverage
import org.utbot.instrumentation.util.StaticEnvironment
import java.io.InputStream
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredFunctions
import kotlinx.coroutines.runBlocking

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

fun methodCoverage(executable: ExecutableId, executions: List<UtValueExecution<*>>, classpath: String): Coverage {
    val methodSignature = executable.signature
    val classId = executable.classId
    return ConcreteExecutor(CoverageInstrumentation, classpath).let { executor ->
        for (execution in executions) {
            val args = execution.stateBefore.params.map { it.value }.toMutableList()
            val caller = execution.stateBefore.caller
            if (caller != null) {
                args.add(0, caller.value)
            }
            val staticEnvironment = StaticEnvironment(
                execution.stateBefore.statics.map { it.key to it.value.value }
            )
            runBlocking {
                executor.executeAsync(
                    classId.name,
                    methodSignature,
                    args.toTypedArray(),
                    parameters = staticEnvironment
                )
            }
        }

        val coverage = executor.collectCoverage(classId.jClass)
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
