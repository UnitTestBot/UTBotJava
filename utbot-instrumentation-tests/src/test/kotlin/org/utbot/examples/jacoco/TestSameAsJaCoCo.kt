package org.utbot.examples.jacoco

import org.utbot.common.Reflection
import org.utbot.examples.samples.jacoco.ExceptionExamples
import org.utbot.examples.samples.jacoco.MonitorUsage
import org.utbot.examples.samples.jacoco.Recursion
import org.utbot.framework.plugin.api.util.signature
import org.utbot.instrumentation.execute
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.coverage.CoverageInfo
import org.utbot.instrumentation.instrumentation.coverage.CoverageInstrumentation
import org.utbot.instrumentation.instrumentation.coverage.collectCoverage
import org.utbot.instrumentation.withInstrumentation
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.instanceParameter
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.analysis.IClassCoverage
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.data.SessionInfoStore
import org.jacoco.core.instr.Instrumenter
import org.jacoco.core.runtime.IRuntime
import org.jacoco.core.runtime.LoggerRuntime
import org.jacoco.core.runtime.RuntimeData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class TestSameAsJaCoCo {
    private fun checkSame(kClass: KClass<*>, method: KCallable<*>, executions: List<ArgumentList>) {
        val methodCoverageJaCoCo = methodCoverageWithJaCoCo(kClass, method, executions)
        val methodCoverageOur = methodCoverage(kClass, method, executions)

        Assertions.assertTrue(methodCoverageJaCoCo.first <= methodCoverageOur.first) {
            "JaCoCo (${methodCoverageJaCoCo.first} should not cover more instructions than we cover" +
                    "(${methodCoverageOur.first})"
        }
        Assertions.assertEquals(methodCoverageJaCoCo, methodCoverageOur)
    }

    @Test
    @Disabled(
        "Synchronized causes the creation of extra bytecode instructions by java compiler." +
                "JaCoCo ignores them, but we do not."
    )
    fun testSimpleMonitor() {
        val monitorUsage = MonitorUsage()
        val executions = listOf(listOf(monitorUsage, 1), listOf(monitorUsage, -1), listOf(monitorUsage, 0))

        checkSame(MonitorUsage::class, MonitorUsage::simpleMonitor, executions)
    }

    @Test
    @Disabled(
        "Finally block is copied into each catch branch, so instructions are duplicated." +
                "JaCoCo treats such instructions as one instruction, but we treat them as separate."
    )
    fun testFinallyChanging() {
        val exceptionExamples = ExceptionExamples()
        val executions = listOf(listOf(exceptionExamples, 0))

        checkSame(ExceptionExamples::class, ExceptionExamples::finallyChanging, executions)
    }

    @Test
    @Disabled("If an exception happens, JaCoCo ignores passed instructions before the exception, but we do not.")
    fun testThrowException() {
        val exceptionExamples = ExceptionExamples()
        val executions = listOf(listOf(exceptionExamples, -1), listOf(exceptionExamples, 1))

        checkSame(ExceptionExamples::class, ExceptionExamples::throwException, executions)
    }


    @Test
    @Disabled("For some reason (?), JaCoCo sometimes ignores instructions.")
    fun recursionWithExceptionTest() {
        val recursion = Recursion()
        val executions = listOf(listOf(recursion, 41), listOf(recursion, 42), listOf(recursion, 43))

        checkSame(Recursion::class, Recursion::recursionWithException, executions)
    }

    @Test
    @Disabled("For some reason (?), JaCoCo doesn't count instructions, even they were executed successfully.")
    fun infiniteRecursionTest() {
        val recursion = Recursion()
        val executions = listOf(listOf(recursion, 0))

        checkSame(Recursion::class, Recursion::infiniteRecursion, executions)
    }
}

private fun methodCoverageWithJaCoCo(kClass: KClass<*>, method: KCallable<*>, executions: List<ArgumentList>): Pair<Int, Int> {
    val methodSignature = method.signature
    val coverage = calculateCoverage(kClass) { clazz ->
        val instrumentedMethod = clazz.declaredFunctions.single { it.signature == methodSignature }
        val onInstance = instrumentedMethod.instanceParameter != null
        for (execution in executions) {
            try {
                if (onInstance) {
                    instrumentedMethod.call(clazz.java.anyInstance, *execution.drop(1).toTypedArray())
                } else {
                    instrumentedMethod.call(*execution.toTypedArray())
                }
            } catch (_: InvocationTargetException) {
            }
        }
    }
    val methodCoverage = coverage.classes
        .single { it.qualifiedName == kClass.qualifiedName }
        .methods
        .single {
            "${it.name}${it.desc}" == methodSignature
        }

    return methodCoverage.instructionCounter.let { it.coveredCount to it.coveredCount }
}

private fun methodCoverage(kClass: KClass<*>, method: KCallable<*>, executions: List<ArgumentList>): Pair<Int, Int> {
    return withInstrumentation(
        CoverageInstrumentation.Factory,
        kClass.java.protectionDomain.codeSource.location.path
    ) { executor ->
        for (execution in executions) {
            executor.execute(method, execution.toTypedArray())
        }

        val methodSignature = method.signature
        val coverage = executor.collectCoverage(kClass.java)
        coverage.toMethodCoverage(methodSignature)
    }
}

private fun CoverageInfo.toMethodCoverage(methodSignature: String): Pair<Int, Int> {
    val methodRange = methodToInstrRange[methodSignature]!!
    val visitedCount = visitedInstrs.filter { it in methodRange }.size
    return visitedCount to methodRange.count()
}

// The following helper functions for counting JaCoCo coverage were copied from `utbot-framework`.

private fun calculateCoverage(clazz: KClass<*>, block: (KClass<*>) -> Unit): CoverageBuilder {
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

private fun instrument(clazz: KClass<*>, instrumenter: Instrumenter): ByteArray =
    clazz.asInputStream().use {
        instrumenter.instrument(it, clazz.qualifiedName)
    }

private fun KClass<*>.asInputStream(): InputStream =
    java.getResourceAsStream("/${qualifiedName!!.replace('.', '/')}.class")!!

private class MemoryClassLoader : ClassLoader() {
    private val definitions: MutableMap<String, ByteArray> = mutableMapOf()

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

private val IClassCoverage.qualifiedName: String
    get() = this.name.replace('/', '.')

@Suppress("DEPRECATION")
private val Class<*>.anyInstance: Any
    get() {
        return Reflection.unsafe.allocateInstance(this)
    }