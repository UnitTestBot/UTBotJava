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
import org.usvm.util.classpathWithApproximations
import java.io.File
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

// TODO usvm-sbft-refactoring: copied from `usvm/usvm-jvm/test`, extract this class back to USVM project
class JcContainer private constructor(
    usePersistence: Boolean,
    classpath: List<File>,
    machineOptions: UMachineOptions,
    builder: JcSettings.() -> Unit,
) : AutoCloseable {
    val db: JcDatabase
    val cp: JcClasspath
    val machine: JcMachine
    val runner: UTestConcreteExecutor

    init {
        val cpPath = classpath.map { it.absolutePath }.sorted()

        /**
         * Persist jacodb cp to achieve:
         * 1. Faster concrete executor initialization
         * 2. Faster analysis for classes from the same cp
         * */
        val persistenceLocation = if (usePersistence) {
            "jcdb-persistence-${cpPath.hashCode()}"
        } else {
            null
        }

        val (db, cp) = runBlocking {
            val db = jacodb {
                builder()

                if (persistenceLocation != null) {
                    persistent(location = persistenceLocation, clearOnStart = false)
                }
            }
            val cp = db.classpathWithApproximations(classpath, listOf(UnknownClasses))
            db to cp
        }
        this.db = db
        this.cp = cp
        this.machine = JcMachine(cp, machineOptions)
        this.runner = UTestConcreteExecutor(
            JcRuntimeTraceInstrumenterFactory::class,
            cpPath,
            cp,
            persistenceLocation,
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
            usePersistence: Boolean,
            classpath: List<File>,
            machineOptions: UMachineOptions,
            builder: JcSettings.() -> Unit,
        ): JcContainer {
            val cacheKey = classpath to machineOptions
            return cache[cacheKey] ?: run {
                // TODO usvm-sbft: right now max cache size is 1, do we need to increase it?
                logger.info { "JcContainer cache miss" }
                close()
                JcContainer(usePersistence, classpath, machineOptions, builder).also { cache[cacheKey] = it }
            }
        }

        override fun close() {
            cache.values.forEach { it.close() }
            cache.clear()
        }
    }
}
