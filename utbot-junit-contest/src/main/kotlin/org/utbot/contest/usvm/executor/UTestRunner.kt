package org.utbot.contest.usvm.executor

import org.jacodb.api.JcClasspath
import org.usvm.instrumentation.executor.UTestConcreteExecutor
import org.usvm.instrumentation.instrumentation.JcRuntimeTraceInstrumenterFactory
import kotlin.time.Duration.Companion.seconds

// TODO usvm-sbft-refactoring: copied from `usvm/usvm-jvm/test`, extract this class back to USVM project
object UTestRunner {
    val CONTEST_TEST_EXECUTION_TIMEOUT = 1.seconds

    lateinit var runner: UTestConcreteExecutor

    fun isInitialized() = this::runner.isInitialized

    fun initRunner(pathToJars: List<String>, classpath: JcClasspath) {
        runner =
            UTestConcreteExecutor(
                JcRuntimeTraceInstrumenterFactory::class,
                pathToJars,
                classpath,
                CONTEST_TEST_EXECUTION_TIMEOUT
            )
    }
}