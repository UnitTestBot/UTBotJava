package org.utbot.contest

import mu.KotlinLogging
import org.objectweb.asm.Type
import org.utbot.common.bracket
import org.utbot.common.info
import org.utbot.framework.codegen.domain.junitByVersion
import org.utbot.framework.codegen.CodeGenerator
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.fuzzer.UtFuzzedExecution
import org.utbot.instrumentation.ConcreteExecutorPool
import org.utbot.instrumentation.warmup.Warmup
import java.net.URLClassLoader
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.utbot.framework.plugin.api.*


class GreyBoxFuzzerContest {

    private val logger = KotlinLogging.logger {}

    @ObsoleteCoroutinesApi
    @SuppressWarnings
    fun runGeneration(
        project: String,
        cut: ClassUnderTest,
        timeLimitSec: Long,
        classpathString: String,
        runFromEstimator: Boolean,
        methodNameFilter: String? = null // For debug purposes you can specify method name){}
    ): StatsForClass = runBlocking {
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