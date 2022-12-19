package org.utbot.contest

import mu.KotlinLogging
import org.objectweb.asm.Type
import org.utbot.common.FileUtil
import org.utbot.common.bracket
import org.utbot.common.filterWhen
import org.utbot.common.info
import org.utbot.common.isAbstract
import org.utbot.engine.EngineController
import org.utbot.framework.TestSelectionStrategyType
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.junitByVersion
import org.utbot.framework.codegen.CodeGenerator
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.framework.util.isKnownImplicitlyDeclaredMethod
import org.utbot.fuzzer.UtFuzzedExecution
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.ConcreteExecutorPool
import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.warmup.Warmup
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Paths
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KCallable
import kotlin.reflect.jvm.isAccessible
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.utbot.engine.Mocker
import org.utbot.engine.greyboxfuzzer.util.CoverageCollector
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.isSynthetic
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.appendText


class GreyBoxFuzzerContest {

    private val logger = KotlinLogging.logger {}

    @ObsoleteCoroutinesApi
    @SuppressWarnings
    fun runGeneration(
        project: String,
        cut: ClassUnderTest,
        timeLimitSec: Long,
        fuzzingRatio: Double,
        classpathString: String,
        runFromEstimator: Boolean,
        methodNameFilter: String? = null // For debug purposes you can specify method name
    ): StatsForClass = runBlocking {
        CoverageCollector.clear()
        val timeBudgetMs = timeLimitSec * 1000
        val generationTimeout: Long =
            timeBudgetMs - timeBudgetMs * 15 / 100 // 4000 ms for terminate all activities and finalize code in file

        logger.debug { "-----------------------------------------------------------------------------" }
        logger.info(
            "Contest.runGeneration: Time budget: $timeBudgetMs ms, Generation timeout=$generationTimeout ms, " +
                    "classpath=$classpathString, methodNameFilter=$methodNameFilter"
        )

        if (runFromEstimator) {
            setOptions()
            //will not be executed in real contest
            logger.info()
                .bracket("warmup: 1st optional soot initialization and executor warmup (not to be counted in time budget)") {
                    TestCaseGenerator(
                        listOf(cut.classfileDir.toPath()),
                        classpathString,
                        dependencyPath,
                        JdkInfoService.provide()
                    )
                }
            logger.info().bracket("warmup (first): kotlin reflection :: init") {
                prepareClass(ConcreteExecutorPool::class.java, "")
                prepareClass(Warmup::class.java, "")
            }
        }

        logger.info("$cut")

        if (cut.classLoader.javaClass != URLClassLoader::class.java) {
            logger.error("Seems like classloader for cut not valid (maybe it was backported to system): ${cut.classLoader}")
        }

        val statsForClass = StatsForClass(project, cut.fqn)

        val codeGenerator = CodeGenerator(
            cut.classId,
            testFramework = junitByVersion(junitVersion),
            staticsMocking = staticsMocking,
            forceStaticMocking = forceStaticMocking,
            generateWarningsForStaticMocking = false
        )

        logger.info().bracket("class ${cut.fqn}", { statsForClass }) {

            val filteredMethods = logger.info().bracket("preparation class ${cut.clazz}: kotlin reflection :: run") {
                prepareClass(cut.clazz, methodNameFilter)
            }

            statsForClass.methodsCount = filteredMethods.size

            // nothing to process further
            if (filteredMethods.isEmpty()) return@runBlocking statsForClass

            val testCaseGenerator =
                logger.info().bracket("2nd optional soot initialization") {
                    TestCaseGenerator(
                        listOf(cut.classfileDir.toPath()),
                        classpathString,
                        dependencyPath,
                        JdkInfoService.provide()
                    )
                }

            val testSet = testCaseGenerator.generate(
                filteredMethods,
                MockStrategyApi.NO_MOCKS,
                methodsGenerationTimeout = generationTimeout
            )

            var testsCounter = 0
            for (test in testSet) {
                val method = test.method
                val statsForMethod = StatsForMethod(
                    "${method.classId.simpleName}#${method.name}#${method.signature}",
                    Type.getInternalName(method.classId.jClass)
                )
                statsForClass.statsForMethods.add(statsForMethod)
                for (result in test.executions) {
                    try {
                        val testMethodName = testMethodName(method.toString(), ++testsCounter)
                        val className = Type.getInternalName(method.classId.jClass)
                        logger.debug { "--new testCase collected, to generate: $testMethodName" }
                        statsForMethod.testsGeneratedCount++
                        result.coverage?.let {
                            statsForClass.updateCoverage(
                                newCoverage = it,
                                isNewClass = !statsForClass.testedClassNames.contains(className),
                                fromFuzzing = result is UtFuzzedExecution
                            )
                        }
                        statsForClass.testedClassNames.add(className)
                    } catch (e: Throwable) {
                        //Here we need isolation
                        logger.error(e) { "Code generation failed" }
                    }
                }
            }

            logger.info().bracket("Flushing tests for [${cut.simpleName}] on disk") {
                writeTestClass(cut, codeGenerator.generateAsString(testSet))
            }
        }
        statsForClass
    }

}