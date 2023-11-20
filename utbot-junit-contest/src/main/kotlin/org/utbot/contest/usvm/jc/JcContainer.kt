package org.utbot.contest.usvm.jc

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jacodb.api.JcClasspath
import org.jacodb.api.JcDatabase
import org.jacodb.impl.JcSettings
import org.jacodb.impl.features.classpaths.UnknownClasses
import org.jacodb.impl.jacodb
import org.usvm.UMachineOptions
import org.usvm.instrumentation.executor.UTestConcreteExecutor
import org.usvm.instrumentation.instrumentation.JcRuntimeTraceInstrumenterFactory
import org.usvm.machine.JcMachine
import java.io.File
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

// TODO usvm-sbft-refactoring: copied from `usvm/usvm-jvm/test`, extract this class back to USVM project
class JcContainer private constructor(
    classpath: List<File>,
    machineOptions: UMachineOptions,
    builder: JcSettings.() -> Unit,
) : AutoCloseable {
    val db: JcDatabase
    val cp: JcClasspath
    val machine: JcMachine
    val runner: UTestConcreteExecutor

    init {
        val (db, cp) = runBlocking {
            val db = jacodb(builder)
            // TODO usvm-sbft: use classpathWithApproximations here when approximation decoders are finished
            val cp = db.classpath(classpath, listOf(UnknownClasses))
            db to cp
        }
        this.db = db
        this.cp = cp
        this.machine = JcMachine(cp, machineOptions)
        this.runner = UTestConcreteExecutor(
            JcRuntimeTraceInstrumenterFactory::class,
            classpath.map { it.absolutePath },
            cp,
            CONTEST_TEST_EXECUTION_TIMEOUT
        )
        runBlocking {
            db.awaitBackgroundJobs()
        }
    }

    override fun close() {
        cp.close()
        db.close()
        machine.close()
        runner.close()
    }

    companion object : AutoCloseable {
        val CONTEST_TEST_EXECUTION_TIMEOUT = 1.seconds

        private val cache = HashMap<Pair<List<File>, UMachineOptions>, JcContainer>()

        operator fun invoke(
            classpath: List<File>,
            machineOptions: UMachineOptions,
            builder: JcSettings.() -> Unit,
        ): JcContainer {
            val cacheKey = classpath to machineOptions
            return cache[cacheKey] ?: run {
                // TODO usvm-sbft: right now max cache size is 1, do we need to increase it?
                logger.info { "JcContainer cache miss" }
                close()
                JcContainer(classpath, machineOptions, builder).also { cache[cacheKey] = it }
            }
        }

        override fun close() {
            cache.values.forEach { it.close() }
            cache.clear()
        }
    }
}
