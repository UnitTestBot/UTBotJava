@file:Suppress("NestedLambdaShadowedImplicitParameter")

package org.utbot.testing

import org.junit.jupiter.api.Assertions.assertTrue
import org.utbot.common.ClassLocation
import org.utbot.common.FileUtil.findPathToClassFiles
import org.utbot.common.FileUtil.locateClass
import org.utbot.common.WorkaroundReason.HACK
import org.utbot.common.workaround
import org.utbot.engine.prettify
import org.utbot.framework.UtSettings.checkSolverTimeoutMillis
import org.utbot.framework.UtSettings.useFuzzing
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.MockStrategyApi.NO_MOCKS
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.exceptionOrNull
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.declaringClazz
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.framework.util.Conflict
import org.utbot.testcheckers.ExecutionsNumberMatcher
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

abstract class UtModelTestCaseChecker(
    testClass: KClass<*>,
    testCodeGeneration: Boolean = true,
    configurations: List<Configuration> = standardTestingConfigurations,
) : CodeGenerationIntegrationTest(testClass, testCodeGeneration, configurations) {
    protected fun check(
        method: KFunction2<*, *, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (UtModel, UtExecutionResult) -> Boolean,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalMockAlwaysClasses: Set<ClassId> = emptySet()
    ) = internalCheck(
        method,
        mockStrategy,
        branches,
        matchers,
        additionalMockAlwaysClasses = additionalMockAlwaysClasses
    )

    protected fun check(
        method: KFunction3<*, *, *, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (UtModel, UtModel, UtExecutionResult) -> Boolean,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalMockAlwaysClasses: Set<ClassId> = emptySet()
    ) = internalCheck(
        method,
        mockStrategy,
        branches,
        matchers,
        additionalMockAlwaysClasses = additionalMockAlwaysClasses
    )

    protected fun checkStatic(
        method: KFunction1<*, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (UtModel, UtExecutionResult) -> Boolean,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalMockAlwaysClasses: Set<ClassId> = emptySet()
    ) = internalCheck(
        method,
        mockStrategy,
        branches,
        matchers,
        additionalMockAlwaysClasses = additionalMockAlwaysClasses
    )

    protected fun checkStaticsAfter(
        method: KFunction2<*, *, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (UtModel, StaticsModelType, UtExecutionResult) -> Boolean,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalMockAlwaysClasses: Set<ClassId> = emptySet()
    ) = internalCheck(
        method, mockStrategy, branches, matchers,
        arguments = ::withStaticsAfter,
        additionalMockAlwaysClasses = additionalMockAlwaysClasses
    )

    private fun internalCheck(
        method: KFunction<*>,
        mockStrategy: MockStrategyApi,
        branches: ExecutionsNumberMatcher,
        matchers: Array<out Function<Boolean>>,
        arguments: (UtExecution) -> List<Any?> = ::withResult,
        additionalMockAlwaysClasses: Set<ClassId> = emptySet()
    ) {
        workaround(HACK) {
            // @todo change to the constructor parameter
            checkSolverTimeoutMillis = 0
            useFuzzing = false
        }
        val executableId = method.executableId

        withUtContext(UtContext(method.declaringClazz.classLoader)) {
            val testSet = executions(executableId, mockStrategy, additionalMockAlwaysClasses)

            assertTrue(testSet.errors.isEmpty()) {
                "We have errors: ${testSet.errors.entries.map { "${it.value}: ${it.key}" }.prettify()}"
            }

            // if force mocking took place in parametrized test generation,
            // we do not need to process this [testSet]
            if (TestCodeGeneratorPipeline.currentTestInfrastructureConfiguration.isParametrizedAndMocked) {
                conflictTriggers.reset(Conflict.ForceMockHappened, Conflict.ForceStaticMockHappened)
                return
            }

            val executions = testSet.executions
            assertTrue(branches(executions.size)) {
                "Branch count matcher '$branches' fails for #executions=${executions.size}: ${executions.prettify()}"
            }
            executions.checkMatchers(matchers, arguments)

            processTestSet(testSet)
        }
    }

    private fun List<UtExecution>.checkMatchers(
        matchers: Array<out Function<Boolean>>,
        arguments: (UtExecution) -> List<Any?>
    ) {
        val exceptions = mutableMapOf<Int, List<Throwable>>()
        val notMatched = matchers.indices.filter { i ->
            this.none { ex ->
                runCatching { invokeMatcher(matchers[i], arguments(ex)) }
                    .onFailure { exceptions.merge(i, listOf(it)) { v1, v2 -> v1 + v2 } }
                    .getOrDefault(false)
            }
        }

        val matchersNumbers = notMatched.map { it + 1 }
        assertTrue(notMatched.isEmpty()) {
            "Execution matchers $matchersNumbers not match to ${this.prettify()}, found exceptions: ${exceptions.prettify()}"
        }
    }

    private fun executions(
        method: ExecutableId,
        mockStrategy: MockStrategyApi,
        additionalMockAlwaysClasses: Set<ClassId> = emptySet()
    ): UtMethodTestSet {
        val classLocation = locateClass(method.classId.jClass)
        if (classLocation != previousClassLocation) {
            buildDir = findPathToClassFiles(classLocation)
            previousClassLocation = classLocation
        }

        val buildInfo = CodeGenerationIntegrationTest.Companion.BuildInfo(buildDir, dependencyPath = null)
        val testCaseGenerator = testCaseGeneratorCache
            .getOrPut(buildInfo) {
                TestSpecificTestCaseGenerator(
                    buildDir,
                    classpath = null,
                    dependencyPaths = System.getProperty("java.class.path"),
                )
            }

        return testCaseGenerator.generate(method, mockStrategy, additionalMockAlwaysClasses)
    }

    protected inline fun <reified T> UtExecutionResult.isException(): Boolean = exceptionOrNull() is T

    /**
     * Finds mocked method and returns values.
     */
    protected fun UtModel.mocksMethod(method: KFunction<*>): List<UtModel>? {
        if (this !is UtCompositeModel) return null
        if (!isMock) return null
        return mocks[method.executableId]
    }

    protected inline fun <reified T> UtExecutionResult.primitiveValue(): T = getOrThrow().primitiveValue()

    /**
     * Finds field model in [UtCompositeModel] and [UtAssembleModel]. For assemble model supports direct field access only.
     */
    protected fun UtModel.findField(fieldName: String, declaringClass: ClassId = this.classId): UtModel =
        findField(FieldId(declaringClass, fieldName))

    /**
     * Finds field model in [UtCompositeModel] and [UtAssembleModel]. For assemble model supports direct field access only.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    protected fun UtModel.findField(fieldId: FieldId): UtModel = when (this) {
        is UtCompositeModel -> this.fields[fieldId]!!
        is UtAssembleModel -> {
            val fieldAccess = this.modificationsChain
                .filterIsInstance<UtDirectSetFieldModel>()
                .singleOrNull { it.fieldId == fieldId }
            fieldAccess?.fieldModel ?: fieldId.type.defaultValueModel()
        }

        else -> error("Can't get ${fieldId.name} from $this")
    }

    companion object {
        private var previousClassLocation: ClassLocation? = null
        private lateinit var buildDir: Path
    }
}

private fun withResult(ex: UtExecution) = ex.stateBefore.parameters + ex.result
private fun withStaticsAfter(ex: UtExecution) = ex.stateBefore.parameters + ex.stateAfter.statics + ex.result

private typealias StaticsModelType = Map<FieldId, UtModel>