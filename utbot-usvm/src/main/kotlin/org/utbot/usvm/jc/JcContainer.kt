package org.utbot.usvm.jc

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jacodb.api.JcClasspath
import org.jacodb.api.JcDatabase
import org.jacodb.impl.JcSettings
import org.jacodb.impl.features.classpaths.UnknownClasses
import org.jacodb.impl.jacodb
import org.usvm.instrumentation.executor.UTestConcreteExecutor
import org.usvm.instrumentation.instrumentation.JcRuntimeTraceInstrumenterFactory
import org.usvm.util.classpathWithApproximations
import java.io.File
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

// TODO usvm-sbft-refactoring: copied from `usvm/usvm-jvm/test`, extract this class back to USVM project
class JcContainer private constructor(
    usePersistence: Boolean,
    persistenceDir: File,
    classpath: List<File>,
    javaHome: File,
    builder: JcSettings.() -> Unit,
) : AutoCloseable {
    val db: JcDatabase
    val cp: JcClasspath
    val runner: UTestConcreteExecutor

    init {
        val cpPath = classpath.map { it.absolutePath }.sorted()

        /**
         * Persist jacodb cp to achieve:
         * 1. Faster concrete executor initialization
         * 2. Faster analysis for classes from the same cp
         * */
        val persistenceLocation = if (usePersistence) {
            persistenceDir.resolve("jcdb-persistence-${cpPath.hashCode()}").absolutePath
        } else {
            null
        }

        val (db, cp) = runBlocking {
            val db = jacodb {
                useJavaRuntime(javaHome)

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
        this.runner = UTestConcreteExecutor(
            JcRuntimeTraceInstrumenterFactory::class,
            cpPath,
            cp,
            javaHome.absolutePath,
            persistenceLocation,
            TEST_EXECUTION_TIMEOUT
        )
        runBlocking {
            db.awaitBackgroundJobs()
        }
    }

    override fun close() {
        cp.close()
        db.close()
        runner.close()
    }

    companion object : AutoCloseable {
        val TEST_EXECUTION_TIMEOUT = 1.seconds

        private val cache = HashMap<List<File>, JcContainer>()

        operator fun invoke(
            usePersistence: Boolean,
            persistenceDir: File,
            classpath: List<File>,
            javaHome: File,
            builder: JcSettings.() -> Unit,
        ): JcContainer {
            return cache[classpath] ?: run {
                // TODO usvm-sbft: right now max cache size is 1, do we need to increase it?
                logger.info { "JcContainer cache miss" }
                close()
                JcContainer(usePersistence, persistenceDir, classpath, javaHome, builder)
                    .also { cache[classpath] = it }
            }
        }

        override fun close() {
            cache.values.forEach { it.close() }
            cache.clear()
        }
    }
}
