package org.utbot.contest.usvm.executor

import org.jacodb.api.JcClasspath
import org.usvm.instrumentation.executor.UTestConcreteExecutor
import org.usvm.instrumentation.instrumentation.JcRuntimeTraceInstrumenterFactory
import org.usvm.instrumentation.util.InstrumentationModuleConstants

// TODO usvm-sbft-refactoring: copied from `usvm/usvm-jvm/test`, extract this class back to USVM project
object UTestRunner {

    lateinit var runner: UTestConcreteExecutor

    fun isInitialized() = this::runner.isInitialized

    fun initRunner(pathToJars: List<String>, classpath: JcClasspath) {
        runner =
            UTestConcreteExecutor(
                JcRuntimeTraceInstrumenterFactory::class,
                pathToJars,
                classpath,
                InstrumentationModuleConstants.testExecutionTimeout
            )
    }
}