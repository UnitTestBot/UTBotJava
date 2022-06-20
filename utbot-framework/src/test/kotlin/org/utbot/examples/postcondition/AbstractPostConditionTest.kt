package org.utbot.examples.postcondition

import org.utbot.common.ClassLocation
import org.utbot.common.FileUtil
import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.engine.prettify
import org.utbot.examples.CodeTestCaseGeneratorTest
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtBotTestCaseGenerator
import org.utbot.framework.plugin.api.UtBotTestCaseGenerator.jimpleBody
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtTestCase
import org.utbot.framework.plugin.api.getOrThrow
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.framework.synthesis.postcondition.constructors.EmptyPostCondition
import org.utbot.framework.synthesis.postcondition.constructors.PostConditionConstructor
import org.utbot.framework.synthesis.postcondition.checkers.ModelBasedPostConditionChecker
import org.utbot.framework.synthesis.postcondition.checkers.PostConditionChecker
import org.utbot.framework.synthesis.postcondition.constructors.ModelBasedPostConditionConstructor
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction2
import org.junit.jupiter.api.Assertions.assertTrue
import org.utbot.framework.plugin.api.toSootMethod

// TODO: Maybe we somehow should extract common logic (checks, internalCheck, executions) from:
//          `AbstractPostConditionTest`,
//          `AbstractModelBasedTest`
//          `AbstractTestCaseGeneratorTest`
//      to the common Superclass
internal abstract class AbstractPostConditionTest(
    testClass: KClass<*>,
    testCodeGeneration: Boolean = true,
    languagePipelines: List<CodeGenerationLanguageLastStage> = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN)
    )
) : CodeTestCaseGeneratorTest(testClass, testCodeGeneration, languagePipelines) {
    protected fun <T> buildAndCheckReturn(
        method: KFunction2<*, *, *>,
        mockStrategy: MockStrategyApi = MockStrategyApi.NO_MOCKS,
        postCondition: T
    ) where T : PostConditionConstructor, T : PostConditionChecker =
        internalCheck(method, mockStrategy, postCondition, postCondition)

    private fun internalCheck(
        method: KFunction<*>,
        mockStrategy: MockStrategyApi,
        constructor: PostConditionConstructor = EmptyPostCondition,
        checker: PostConditionChecker = PostConditionChecker { true },
        arguments: (UtExecution) -> UtModel = ::withSuccessfulResultOnly // TODO: refactor it to generalize to the entire state after
    ) {
        workaround(WorkaroundReason.HACK) {
            // @todo change to the constructor parameter
            UtSettings.checkSolverTimeoutMillis = 0
        }
        val utMethod = UtMethod.from(method)

        withUtContext(UtContext(utMethod.clazz.java.classLoader)) {
            val testCase = executions(utMethod, mockStrategy, constructor)

            assertTrue(testCase.errors.isEmpty()) {
                "We have errors: ${testCase.errors.entries.map { "${it.value}: ${it.key}" }.prettify()}"
            }

            val executions = testCase.executions
            assertTrue(executions.isNotEmpty()) {
                "At least one execution expected..."
            }

            executions.any { checkExecution(it, arguments, checker) }

            processTestCase(testCase)
        }
    }

    // TODO: refactor it to generalize to the entire state after
    private fun checkExecution(
        execution: UtExecution,
        arguments: (UtExecution) -> UtModel,
        checker: PostConditionChecker
    ): Boolean {
        val actual = arguments(execution)
        return checker.checkPostCondition(actual)
    }

    private fun executions(
        method: UtMethod<*>,
        mockStrategy: MockStrategyApi,
        postConditionConstructor: PostConditionConstructor
    ): UtTestCase {
        val classLocation = FileUtil.locateClass(method.clazz)
        if (classLocation != previousClassLocation) {
            buildDir = FileUtil.findPathToClassFiles(classLocation)
            previousClassLocation = classLocation
        }
        UtBotTestCaseGenerator.init(buildDir, classpath = null, dependencyPaths = System.getProperty("java.class.path"))
        val testCase = UtTestCase(
            method,
            UtBotTestCaseGenerator.generateWithPostCondition(method.toSootMethod(), mockStrategy, postConditionConstructor),
            jimpleBody(method)
        )
        return testCase
    }

    protected class ModelBasedPostCondition(expectedModel: UtModel) :
        PostConditionConstructor by ModelBasedPostConditionConstructor(expectedModel),
        PostConditionChecker by ModelBasedPostConditionChecker(expectedModel)

    companion object {
        private var previousClassLocation: ClassLocation? = null
        private lateinit var buildDir: Path
    }
}

private fun withSuccessfulResultOnly(ex: UtExecution) = ex.result.getOrThrow()