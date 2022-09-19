@file:Suppress("NestedLambdaShadowedImplicitParameter")

package org.utbot.tests.infrastructure

import org.junit.jupiter.api.Assertions.assertTrue
import org.utbot.common.ClassLocation
import org.utbot.common.FileUtil.clearTempDirectory
import org.utbot.common.FileUtil.findPathToClassFiles
import org.utbot.common.FileUtil.locateClass
import org.utbot.engine.prettify
import org.utbot.framework.UtSettings
import org.utbot.framework.UtSettings.checkCoverageInCodeGenerationTests
import org.utbot.framework.UtSettings.daysLimitForTempFiles
import org.utbot.framework.UtSettings.testDisplayName
import org.utbot.framework.UtSettings.testName
import org.utbot.framework.UtSettings.testSummary
import org.utbot.framework.coverage.Coverage
import org.utbot.framework.coverage.counters
import org.utbot.framework.coverage.methodCoverage
import org.utbot.framework.coverage.toAtLeast
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.DocClassLinkStmt
import org.utbot.framework.plugin.api.DocCodeStmt
import org.utbot.framework.plugin.api.DocCustomTagStatement
import org.utbot.framework.plugin.api.DocMethodLinkStmt
import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.FieldMockTarget
import org.utbot.framework.plugin.api.MockId
import org.utbot.framework.plugin.api.MockInfo
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.MockStrategyApi.NO_MOCKS
import org.utbot.framework.plugin.api.ObjectMockTarget
import org.utbot.framework.plugin.api.ParameterMockTarget
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.framework.plugin.api.UtInstrumentation
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.UtMockValue
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtValueExecution
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.declaringClazz
import org.utbot.framework.plugin.api.util.enclosingClass
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.kClass
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.framework.util.Conflict
import org.utbot.framework.util.toValueTestCase
import org.utbot.summary.summarize
import org.utbot.testcheckers.ExecutionsNumberMatcher
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3
import kotlin.reflect.KFunction4
import kotlin.reflect.KFunction5

abstract class UtValueTestCaseChecker(
    testClass: KClass<*>,
    testCodeGeneration: Boolean = true,
    languagePipelines: List<CodeGenerationLanguageLastStage> = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN)
    )
) : CodeGenerationIntegrationTest(testClass, testCodeGeneration, languagePipelines) {
    // contains already analyzed by the engine methods
    private val analyzedMethods: MutableMap<MethodWithMockStrategy, MethodResult> = mutableMapOf()

    val searchDirectory: Path = Paths.get("../utbot-sample/src/main/java")

    init {
        UtSettings.checkSolverTimeoutMillis = 0
        UtSettings.checkNpeInNestedMethods = true
        UtSettings.checkNpeInNestedNotPrivateMethods = true
        UtSettings.substituteStaticsWithSymbolicVariable = true
        UtSettings.useAssembleModelGenerator = true
        UtSettings.saveRemainingStatesForConcreteExecution = false
        UtSettings.useFuzzing = false
        UtSettings.useCustomJavaDocTags = false
    }

    // checks paramsBefore and result
    protected inline fun <reified R> check(
        method: KFunction1<*, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified R> check(
        method: KFunction2<*, T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified R> check(
        method: KFunction3<*, T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified R> check(
        method: KFunction4<*, T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4, reified R> check(
        method: KFunction5<*, T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    // check paramsBefore and Result<R>, suitable to check exceptions
    protected inline fun <reified R> checkWithException(
        method: KFunction1<*, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage,
        arguments = ::withException,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified R> checkWithException(
        method: KFunction2<*, T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withException,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified R> checkWithException(
        method: KFunction3<*, T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class,
        arguments = ::withException,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified R> checkWithException(
        method: KFunction4<*, T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class,
        arguments = ::withException,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4, reified R> checkWithException(
        method: KFunction5<*, T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withException,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    // check this, paramsBefore and result value
    protected inline fun <reified T, reified R> checkWithThis(
        method: KFunction1<T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withThisAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified R> checkWithThis(
        method: KFunction2<T, T1, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class,
        arguments = ::withThisAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified T2, reified R> checkWithThis(
        method: KFunction3<T, T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, T2, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class, T2::class,
        arguments = ::withThisAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified T2, reified T3, reified R> checkWithThis(
        method: KFunction4<T, T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, T2, T3, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class, T2::class, T3::class,
        arguments = ::withThisAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified T2, reified T3, reified T4, reified R> checkWithThis(
        method: KFunction5<T, T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, T2, T3, T4, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withThisAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified R> checkWithThisAndException(
        method: KFunction1<T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withThisAndException,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified R> checkWithThisAndException(
        method: KFunction2<T, T1, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class,
        arguments = ::withThisAndException,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified T2, reified R> checkWithThisAndException(
        method: KFunction3<T, T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, T2, Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class, T2::class,
        arguments = ::withThisAndException,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified T2, reified T3, reified R> checkWithThisAndException(
        method: KFunction4<T, T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, T2, T3, Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class, T2::class, T3::class,
        arguments = ::withThisAndException,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified T2, reified T3, reified T4, reified R> checkWithThisAndException(
        method: KFunction5<T, T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, T2, T3, T4, Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withThisAndException,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    // checks paramsBefore, mocks and result value
    protected inline fun <reified R> checkMocksInStaticMethod(
        method: KFunction0<R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (Mocks, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage,
        arguments = ::withMocks,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified R> checkMocksInStaticMethod(
        method: KFunction1<T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, Mocks, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withMocks,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified R> checkMocksInStaticMethod(
        method: KFunction2<T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, Mocks, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class,
        arguments = ::withMocks,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified R> checkMocksInStaticMethod(
        method: KFunction3<T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, Mocks, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class,
        arguments = ::withMocks,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4, reified R> checkMocksInStaticMethod(
        method: KFunction4<T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, Mocks, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withMocks,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    // checks paramsBefore, mocks and result value
    protected inline fun <reified R> checkMocks(
        method: KFunction1<*, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (Mocks, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage,
        arguments = ::withMocks,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified R> checkMocks(
        method: KFunction2<*, T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, Mocks, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withMocks,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified R> checkMocks(
        method: KFunction3<*, T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, Mocks, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class,
        arguments = ::withMocks,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified R> checkMocks(
        method: KFunction4<*, T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, Mocks, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class,
        arguments = ::withMocks,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4, reified R> checkMocks(
        method: KFunction5<*, T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, Mocks, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withMocks,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    // check paramsBefore, mocks and instrumentation and result value
    protected inline fun <reified R> checkMocksAndInstrumentation(
        method: KFunction1<*, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (Mocks, Instrumentation, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage,
        arguments = ::withMocksAndInstrumentation,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified R> checkMocksAndInstrumentation(
        method: KFunction2<*, T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, Mocks, Instrumentation, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withMocksAndInstrumentation,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified R> checkMocksAndInstrumentation(
        method: KFunction3<*, T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, Mocks, Instrumentation, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class,
        arguments = ::withMocksAndInstrumentation,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified R> checkMocksAndInstrumentation(
        method: KFunction4<*, T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, Mocks, Instrumentation, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class,
        arguments = ::withMocksAndInstrumentation,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4, reified R> checkMocksAndInstrumentation(
        method: KFunction5<*, T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, Mocks, Instrumentation, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withMocksAndInstrumentation,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    // check this, paramsBefore, mocks, instrumentation and return value
    protected inline fun <reified T, reified R> checkMocksInstrumentationAndThis(
        method: KFunction1<T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, Mocks, Instrumentation, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withMocksInstrumentationAndThis,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified R> checkMocksInstrumentationAndThis(
        method: KFunction2<T, T1, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, Mocks, Instrumentation, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class,
        arguments = ::withMocksInstrumentationAndThis,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified T2, reified R> checkMocksInstrumentationAndThis(
        method: KFunction3<T, T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, T2, Mocks, Instrumentation, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class, T2::class,
        arguments = ::withMocksInstrumentationAndThis,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified T2, reified T3, reified R> checkMocksInstrumentationAndThis(
        method: KFunction4<T, T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, T2, T3, Mocks, Instrumentation, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class, T2::class, T3::class,
        arguments = ::withMocksInstrumentationAndThis,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified T2, reified T3, reified T4, reified R> checkMocksInstrumentationAndThis(
        method: KFunction5<T, T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, T2, T3, T4, Mocks, Instrumentation, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withMocksInstrumentationAndThis,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    // checks paramsBefore and return value for static methods
    protected inline fun <reified R> checkStaticMethod(
        method: KFunction0<R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified R> checkStaticMethod(
        method: KFunction1<T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified R> checkStaticMethod(
        method: KFunction2<T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified R> checkStaticMethod(
        method: KFunction3<T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4, reified R> checkStaticMethod(
        method: KFunction4<T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    // checks paramsBefore and Result<R>, suitable for exceptions check
    protected inline fun <reified R> checkStaticMethodWithException(
        method: KFunction0<R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage,
        arguments = ::withException,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified R> checkStaticMethodWithException(
        method: KFunction1<T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withException,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified R> checkStaticMethodWithException(
        method: KFunction2<T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class,
        arguments = ::withException,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified R> checkStaticMethodWithException(
        method: KFunction3<T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class,
        arguments = ::withException,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4, reified R> checkStaticMethodWithException(
        method: KFunction4<T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withException,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    // check arguments, statics and return value
    protected inline fun <reified R> checkStatics(
        method: KFunction1<*, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage,
        arguments = ::withStaticsBefore,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified R> checkStatics(
        method: KFunction2<*, T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withStaticsBefore,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified R> checkStatics(
        method: KFunction3<*, T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class,
        arguments = ::withStaticsBefore,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified R> checkStatics(
        method: KFunction4<*, T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class,
        arguments = ::withStaticsBefore,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4, reified R> checkStatics(
        method: KFunction5<*, T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withStaticsBefore,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    // check arguments, statics and Result<R> for exceptions check
    protected inline fun <reified R> checkStaticsAndException(
        method: KFunction1<*, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (StaticsType, Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage,
        arguments = ::withStaticsBeforeAndExceptions,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified R> checkStaticsAndException(
        method: KFunction2<*, T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, StaticsType, Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withStaticsBeforeAndExceptions,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified R> checkStaticsAndException(
        method: KFunction3<*, T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, StaticsType, Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class,
        arguments = ::withStaticsBeforeAndExceptions,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified R> checkStaticsAndException(
        method: KFunction4<*, T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, StaticsType, Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class,
        arguments = ::withStaticsBeforeAndExceptions,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4, reified R> checkStaticsAndException(
        method: KFunction5<*, T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, StaticsType, Result<R>) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withStaticsBeforeAndExceptions,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified R> checkStaticsAfter(
        method: KFunction1<*, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage,
        arguments = ::withStaticsAfter,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified R> checkStaticsAfter(
        method: KFunction2<*, T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withStaticsAfter,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified R> checkStaticsAfter(
        method: KFunction3<*, T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class,
        arguments = ::withStaticsAfter,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified R> checkStaticsAfter(
        method: KFunction4<*, T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class,
        arguments = ::withStaticsAfter,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4, reified R> checkStaticsAfter(
        method: KFunction5<*, T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withStaticsAfter,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified R> checkThisAndStaticsAfter(
        method: KFunction1<T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withThisAndStaticsAfter,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified R> checkThisAndStaticsAfter(
        method: KFunction2<T, T1, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class,
        arguments = ::withThisAndStaticsAfter,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified T2, reified R> checkThisAndStaticsAfter(
        method: KFunction3<T, T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, T2, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class, T2::class,
        arguments = ::withThisAndStaticsAfter,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified T2, reified T3, reified R> checkThisAndStaticsAfter(
        method: KFunction4<T, T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, T2, T3, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class, T2::class, T3::class,
        arguments = ::withThisAndStaticsAfter,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified T2, reified T3, reified T4, reified R> checkThisAndStaticsAfter(
        method: KFunction5<T, T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, T2, T3, T4, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withThisAndStaticsAfter,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    // checks paramsBefore, staticsBefore and return value for static methods
    protected inline fun <reified R> checkStaticsInStaticMethod(
        method: KFunction0<R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage,
        arguments = ::withStaticsBefore,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified R> checkStaticsInStaticMethod(
        method: KFunction1<T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withStaticsBefore,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified R> checkStaticsInStaticMethod(
        method: KFunction2<T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class,
        arguments = ::withStaticsBefore,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified R> checkStaticsInStaticMethod(
        method: KFunction3<T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class,
        arguments = ::withStaticsBefore,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4, reified R> checkStaticsInStaticMethod(
        method: KFunction4<T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withStaticsBefore,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4, reified T5, reified R> checkStaticsInStaticMethod(
        method: KFunction5<T1, T2, T3, T4, T5, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, T5, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withStaticsBefore,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    // check this, arguments and result value
    protected inline fun <reified T, reified R> checkStaticsWithThis(
        method: KFunction1<T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withThisStaticsBeforeAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified R> checkStaticsWithThis(
        method: KFunction2<T, T1, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class,
        arguments = ::withThisStaticsBeforeAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified T2, reified R> checkStaticsWithThis(
        method: KFunction3<T, T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, T2, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class, T2::class,
        arguments = ::withThisStaticsBeforeAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified T2, reified T3, reified R> checkStaticsWithThis(
        method: KFunction4<T, T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, T2, T3, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class, T2::class, T3::class,
        arguments = ::withThisStaticsBeforeAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified T2, reified T3, reified T4, reified R> checkStaticsWithThis(
        method: KFunction5<T, T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, T2, T3, T4, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withThisStaticsBeforeAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified R> checkParamsMutationsAndResult(
        method: KFunction2<*, T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withParamsMutationsAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified R> checkParamsMutationsAndResult(
        method: KFunction3<*, T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T1, T2, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class,
        arguments = ::withParamsMutationsAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified R> checkParamsMutationsAndResult(
        method: KFunction4<*, T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T1, T2, T3, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class,
        arguments = ::withParamsMutationsAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4, reified R> checkParamsMutationsAndResult(
        method: KFunction5<*, T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, T1, T2, T3, T4, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withParamsMutationsAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    // checks mutations in the parameters
    protected inline fun <reified T> checkParamsMutations(
        method: KFunction2<*, T, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withParamsMutations,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2> checkParamsMutations(
        method: KFunction3<*, T1, T2, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T1, T2) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class,
        arguments = ::withParamsMutations,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3> checkParamsMutations(
        method: KFunction4<*, T1, T2, T3, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T1, T2, T3) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class,
        arguments = ::withParamsMutations,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4> checkParamsMutations(
        method: KFunction5<*, T1, T2, T3, T4, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, T1, T2, T3, T4) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withParamsMutations,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    // checks mutations in the parameters and statics for static method
    protected fun checkStaticMethodMutation(
        method: KFunction0<*>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (StaticsType, StaticsType) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage,
        arguments = ::withMutations,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T> checkStaticMethodMutation(
        method: KFunction1<T, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, StaticsType, T, StaticsType) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withMutations,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2> checkStaticMethodMutation(
        method: KFunction2<T1, T2, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, StaticsType, T1, T2, StaticsType) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class,
        arguments = ::withMutations,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3> checkStaticMethodMutation(
        method: KFunction3<T1, T2, T3, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, StaticsType, T1, T2, T3, StaticsType) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class,
        arguments = ::withMutations,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4> checkStaticMethodMutation(
        method: KFunction4<T1, T2, T3, T4, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, StaticsType, T1, T2, T3, T4, StaticsType) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withMutations,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified R> checkStaticMethodMutationAndResult(
        method: KFunction0<R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (StaticsType, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage,
        arguments = ::withMutationsAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified R> checkStaticMethodMutationAndResult(
        method: KFunction1<T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, StaticsType, T, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withMutationsAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified R> checkStaticMethodMutationAndResult(
        method: KFunction2<T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, StaticsType, T1, T2, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class,
        arguments = ::withMutationsAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified R> checkStaticMethodMutationAndResult(
        method: KFunction3<T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, StaticsType, T1, T2, T3, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class,
        arguments = ::withMutationsAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4, reified R> checkStaticMethodMutationAndResult(
        method: KFunction4<T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, StaticsType, T1, T2, T3, T4, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withMutationsAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )


    // checks mutations in the parameters and statics
    protected inline fun <reified T> checkMutations(
        method: KFunction2<*, T, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, StaticsType, T, StaticsType) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withMutations,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2> checkMutations(
        method: KFunction3<*, T1, T2, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, StaticsType, T1, T2, StaticsType) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class,
        arguments = ::withMutations,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3> checkMutations(
        method: KFunction4<*, T1, T2, T3, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, StaticsType, T1, T2, T3, StaticsType) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class,
        arguments = ::withMutations,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4> checkMutations(
        method: KFunction5<*, T1, T2, T3, T4, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, StaticsType, T1, T2, T3, T4, StaticsType) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withMutations,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    // checks mutations in the parameters and statics
    protected inline fun <reified R> checkMutationsAndResult(
        method: KFunction1<*, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (StaticsType, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage,
        arguments = ::withMutationsAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified R> checkMutationsAndResult(
        method: KFunction2<*, T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, StaticsType, T, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withMutationsAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified R> checkMutationsAndResult(
        method: KFunction3<*, T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, StaticsType, T1, T2, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class,
        arguments = ::withMutationsAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified R> checkMutationsAndResult(
        method: KFunction4<*, T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, StaticsType, T1, T2, T3, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class,
        arguments = ::withMutationsAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4, reified R> checkMutationsAndResult(
        method: KFunction5<*, T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, StaticsType, T1, T2, T3, T4, StaticsType, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withMutationsAndResult,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    // checks mutations in this, parameters and statics
    protected inline fun <reified T> checkAllMutationsWithThis(
        method: KFunction1<T, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, StaticsType, T, StaticsType) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        arguments = ::withMutationsAndThis,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1> checkAllMutationsWithThis(
        method: KFunction2<T, T1, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, StaticsType, T, T1, StaticsType) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class,
        arguments = ::withMutationsAndThis,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified T2> checkAllMutationsWithThis(
        method: KFunction3<T, T1, T2, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, T2, StaticsType, T, T1, T2, StaticsType) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class, T2::class,
        arguments = ::withMutationsAndThis,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified T2, reified T3> checkAllMutationsWithThis(
        method: KFunction4<T, T1, T2, T3, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, T2, T3, StaticsType, T, T1, T2, T3, StaticsType) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class, T2::class, T3::class,
        arguments = ::withMutationsAndThis,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified T1, reified T2, reified T3, reified T4> checkAllMutationsWithThis(
        method: KFunction5<T, T1, T2, T3, T4, *>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, T1, T2, T3, T4, StaticsType, T, T1, T2, T3, T4, StaticsType) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class, T1::class, T2::class, T3::class, T4::class,
        arguments = ::withMutationsAndThis,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    //region checks substituting statics with symbolic variable or not
    protected inline fun <reified R> checkWithoutStaticsSubstitution(
        method: KFunction1<*, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T, reified R> checkWithoutStaticsSubstitution(
        method: KFunction2<*, T, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T::class,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified R> checkWithoutStaticsSubstitution(
        method: KFunction3<*, T1, T2, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified R> checkWithoutStaticsSubstitution(
        method: KFunction4<*, T1, T2, T3, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    protected inline fun <reified T1, reified T2, reified T3, reified T4, reified R> checkWithoutStaticsSubstitution(
        method: KFunction5<*, T1, T2, T3, T4, R>,
        branches: ExecutionsNumberMatcher,
        vararg matchers: (T1, T2, T3, T4, R?) -> Boolean,
        coverage: CoverageMatcher = Full,
        mockStrategy: MockStrategyApi = NO_MOCKS,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) = internalCheck(
        method, mockStrategy, branches, matchers, coverage, T1::class, T2::class, T3::class, T4::class,
        additionalDependencies = additionalDependencies,
        summaryTextChecks = summaryTextChecks,
        summaryNameChecks = summaryNameChecks,
        summaryDisplayNameChecks = summaryDisplayNameChecks
    )

    //endregion

    /**
     * @param method method under test
     * @param generateWithNested a flag indicating if we need to generate nested test classes
     * or just generate one top-level test class
     * @see [ClassWithStaticAndInnerClassesTest]
     */
    fun checkAllCombinations(method: KFunction<*>, generateWithNested: Boolean = false) {
        val failed = mutableListOf<TestFrameworkConfiguration>()
        val succeeded = mutableListOf<TestFrameworkConfiguration>()

        allTestFrameworkConfigurations
            .filterNot { it.isDisabled }
            .forEach { config ->
                runCatching {
                    internalCheckForCodeGeneration(method, config, generateWithNested)
                }.onFailure {
                    failed += config
                }.onSuccess {
                    succeeded += config
                }
            }

        // TODO check that all generated classes have different content JIRA:1415

        logger.info { "Total configurations: ${succeeded.size + failed.size}. Failed: ${failed.size}." }

        require(failed.isEmpty()) {
            val separator = System.lineSeparator()
            val failedConfigurations = failed.joinToString(prefix = separator, separator = separator)

            "Failed configurations: $failedConfigurations"
        }
    }

    @Suppress("ControlFlowWithEmptyBody", "UNUSED_VARIABLE")
    private fun internalCheckForCodeGeneration(
        method: KFunction<*>,
        testFrameworkConfiguration: TestFrameworkConfiguration,
        generateWithNested: Boolean
    ) {
        withSettingsFromTestFrameworkConfiguration(testFrameworkConfiguration) {
            with(testFrameworkConfiguration) {

                val executableId = method.executableId
                computeAdditionalDependenciesClasspathAndBuildDir(method.declaringClazz, emptyArray())
                val utContext = UtContext(method.declaringClazz.classLoader)

                clearTempDirectory(daysLimitForTempFiles)

                withUtContext(utContext) {
                    val methodWithStrategy =
                        MethodWithMockStrategy(executableId, mockStrategy, resetNonFinalFieldsAfterClinit)

                    val (testSet, coverage) = analyzedMethods.getOrPut(methodWithStrategy) {
                        walk(executableId, mockStrategy)
                    }

                    // if force mocking took place in parametrized test generation,
                    // we do not need to process this [testSet]
                    if (TestCodeGeneratorPipeline.currentTestFrameworkConfiguration.isParametrizedAndMocked) {
                        conflictTriggers.reset(Conflict.ForceMockHappened, Conflict.ForceStaticMockHappened)
                        return
                    }

                    if (checkCoverageInCodeGenerationTests) {
                        // TODO JIRA:1407
                    }

                    val methodUnderTestOwnerId = testSet.method.classId
                    val classUnderTest = if (generateWithNested) {
                        generateSequence(methodUnderTestOwnerId) { clazz -> clazz.enclosingClass }.last().kClass
                    } else {
                        methodUnderTestOwnerId.kClass
                    }

                    val stageStatusCheck = StageStatusCheck(
                        firstStage = CodeGeneration,
                        lastStage = TestExecution,
                        status = ExecutionStatus.SUCCESS
                    )
                    val classStages = listOf(ClassStages(classUnderTest, stageStatusCheck, listOf(testSet)))

                    TestCodeGeneratorPipeline(testFrameworkConfiguration).runClassesCodeGenerationTests(classStages)
                }
            }
        }
    }

    inline fun <reified R> internalCheck(
        method: KFunction<R>,
        mockStrategy: MockStrategyApi,
        branches: ExecutionsNumberMatcher,
        matchers: Array<out Function<Boolean>>,
        coverageMatcher: CoverageMatcher,
        vararg classes: KClass<*>,
        noinline arguments: (UtValueExecution<*>) -> List<Any?> = ::withResult,
        additionalDependencies: Array<Class<*>> = emptyArray(),
        summaryTextChecks: List<(List<DocStatement>?) -> Boolean> = listOf(),
        summaryNameChecks: List<(String?) -> Boolean> = listOf(),
        summaryDisplayNameChecks: List<(String?) -> Boolean> = listOf()
    ) {
        if (UtSettings.checkAllCombinationsForEveryTestInSamples) {
            checkAllCombinations(method)
        }

        val executableId = method.executableId

        withUtContext(UtContext(method.declaringClazz.classLoader)) {
            val additionalDependenciesClassPath =
                computeAdditionalDependenciesClasspathAndBuildDir(executableId.classId.jClass, additionalDependencies)

            val (testSet, coverage) = if (coverageMatcher is DoNotCalculate) {
                MethodResult(executions(executableId, mockStrategy, additionalDependenciesClassPath), Coverage())
            } else {
                walk(executableId, mockStrategy, additionalDependenciesClassPath)
            }
            testSet.summarize(searchDirectory)
            val valueTestCase = testSet.toValueTestCase()

            assertTrue(testSet.errors.isEmpty()) {
                "We have errors: ${
                    testSet.errors.entries.map { "${it.value}: ${it.key}" }.prettify()
                }"
            }

            // if force mocking took place in parametrized test generation,
            // we do not need to process this [testSet]
            if (TestCodeGeneratorPipeline.currentTestFrameworkConfiguration.isParametrizedAndMocked) {
                conflictTriggers.reset(Conflict.ForceMockHappened, Conflict.ForceStaticMockHappened)
                return
            }

            val valueExecutions = valueTestCase.executions
            assertTrue(branches(valueExecutions.size)) {
                "Branch count matcher '$branches' fails for ${valueExecutions.size}: ${valueExecutions.prettify()}"
            }

            valueExecutions.checkTypes(R::class, classes.toList())

            if (testSummary) {
                valueExecutions.checkSummaryMatchers(summaryTextChecks)
                // todo: Ask Zarina to take a look (Java 11 transition)
                // valueExecutions.checkCommentsForBasicErrors()
            }
            if (testName) {
                valueExecutions.checkNameMatchers(summaryNameChecks)
                valueExecutions.checkNamesForBasicErrors()
            }
            if (testDisplayName) {
                valueExecutions.checkDisplayNameMatchers(summaryDisplayNameChecks)
            }

            valueExecutions.checkMatchers(matchers, arguments)
            assertTrue(coverageMatcher(coverage)) {
                "Coverage matcher '$coverageMatcher' fails for $coverage (at least: ${coverage.toAtLeast()})"
            }

            processTestCase(testSet)
        }
    }

    fun List<UtValueExecution<*>>.checkTypes(
        resultType: KClass<*>,
        argumentTypes: List<KClass<*>>
    ) {
        for (execution in this) {
            val typesWithArgs = argumentTypes.zip(execution.stateBefore.params.map { it.type })

            assertTrue(typesWithArgs.all { it.second::class.isInstance(it.first::class) }) { "Param types do not match" }

            execution.returnValue.getOrNull()?.let { returnValue ->
                assertTrue(resultType::class.isInstance(returnValue::class)) { "Return type does not match" }
            }
        }
    }

    fun List<UtValueExecution<*>>.checkMatchers(
        matchers: Array<out Function<Boolean>>,
        arguments: (UtValueExecution<*>) -> List<Any?>
    ) {
        val notMatched = matchers.indices.filter { i ->
            this.none { ex ->
                runCatching { invokeMatcher(matchers[i], arguments(ex)) }.getOrDefault(false)
            }
        }

        val matchersNumbers = notMatched.map { it + 1 }

        assertTrue(notMatched.isEmpty()) { "Execution matchers $matchersNumbers not match to ${this.prettify()}" }

        // Uncomment if you want to check that each value matches at least one matcher.
//        val notMatchedValues = this.filter { ex ->
//            matchers.none { matcher ->
//                runCatching {
//                    invokeMatcher(matcher, arguments(ex))
//                }.getOrDefault(false)
//            }
//        }
//        assertTrue(notMatchedValues.isEmpty()) { "Values not match to matchers, ${notMatchedValues.prettify()}" }
    }

    fun List<UtValueExecution<*>>.checkSummaryMatchers(summaryTextChecks: List<(List<DocStatement>?) -> Boolean>) {
        val notMatched = summaryTextChecks.indices.filter { i ->
            this.none { ex -> summaryTextChecks[i](ex.summary) }
        }
        assertTrue(notMatched.isEmpty()) {
            "Summary matchers ${notMatched.map { it + 1 }} not match for \n${
                this.joinToString(separator = "\n\n") {
                    "Next summary start ".padEnd(50, '-') + "\n" +
                            prettifyDocStatementList(it.summary ?: emptyList())
                }
            }"
        }
    }

    private fun prettifyDocStatementList(docStmts: List<DocStatement>): String {
        val flattenStmts = flattenDocStatements(docStmts)
        return flattenStmts.joinToString(separator = "\n") { "${it.javaClass.simpleName.padEnd(20)} content:$it;" }
    }

    fun List<UtValueExecution<*>>.checkNameMatchers(nameTextChecks: List<(String?) -> Boolean>) {
        val notMatched = nameTextChecks.indices.filter { i ->
            this.none { execution -> nameTextChecks[i](execution.testMethodName) }
        }
        assertTrue(notMatched.isEmpty()) {
            "Test method name matchers ${notMatched.map { it + 1 }} not match for ${map { it.testMethodName }.prettify()}"
        }
    }

    fun List<UtValueExecution<*>>.checkDisplayNameMatchers(displayNameTextChecks: List<(String?) -> Boolean>) {
        val notMatched = displayNameTextChecks.indices.filter { i ->
            this.none { ex -> displayNameTextChecks[i](ex.displayName) }
        }
        assertTrue(notMatched.isEmpty()) {
            "Test display name matchers ${notMatched.map { it + 1 }} not match for ${map { it.displayName }.prettify()}"
        }
    }

//    fun List<UtValueExecution<*>>.checkCommentsForBasicErrors() {
//        val emptyLines = this.filter {
//            it.summary?.contains("\n\n") ?: false
//        }
//        assertTrue(emptyLines.isEmpty()) { "Empty lines in the comments: ${emptyLines.map { it.summary }.prettify()}" }
//    }

    fun List<UtValueExecution<*>>.checkNamesForBasicErrors() {
        val wrongASTNodeConversion = this.filter {
            it.testMethodName?.contains("null") ?: false
        }
        assertTrue(wrongASTNodeConversion.isEmpty()) {
            "Null in AST node conversion in the names: ${wrongASTNodeConversion.map { it.testMethodName }.prettify()}"
        }
    }

    fun walk(
        method: ExecutableId,
        mockStrategy: MockStrategyApi,
        additionalDependenciesClassPath: String = ""
    ): MethodResult {
        val testSet = executions(method, mockStrategy, additionalDependenciesClassPath)
        val methodCoverage = methodCoverage(
            method,
            testSet.toValueTestCase().executions,
            buildDir.toString() + File.pathSeparator + additionalDependenciesClassPath
        )
        return MethodResult(testSet, methodCoverage)
    }

    fun executions(
        method: ExecutableId,
        mockStrategy: MockStrategyApi,
        additionalDependenciesClassPath: String
    ): UtMethodTestSet {
        val buildInfo = CodeGenerationIntegrationTest.Companion.BuildInfo(buildDir, additionalDependenciesClassPath)

        val testCaseGenerator = testCaseGeneratorCache
            .getOrPut(buildInfo) {
                TestSpecificTestCaseGenerator(
                    buildDir,
                    additionalDependenciesClassPath,
                    System.getProperty("java.class.path")
                )
            }
        return testCaseGenerator.generate(method, mockStrategy)
    }

    fun executionsModel(
        method: ExecutableId,
        mockStrategy: MockStrategyApi,
        additionalDependencies: Array<Class<*>> = emptyArray()
    ): UtMethodTestSet {
        val additionalDependenciesClassPath =
            computeAdditionalDependenciesClasspathAndBuildDir(method.classId.jClass, additionalDependencies)
        withUtContext(UtContext(method.classId.jClass.classLoader)) {
            val buildInfo = CodeGenerationIntegrationTest.Companion.BuildInfo(buildDir, additionalDependenciesClassPath)
            val testCaseGenerator = testCaseGeneratorCache
                .getOrPut(buildInfo) {
                    TestSpecificTestCaseGenerator(
                        buildDir,
                        additionalDependenciesClassPath,
                        System.getProperty("java.class.path")
                    )
                }
            return testCaseGenerator.generate(method, mockStrategy)
        }
    }

    companion object {
        private var previousClassLocation: ClassLocation? = null
        private lateinit var buildDir: Path

        fun computeAdditionalDependenciesClasspathAndBuildDir(
            clazz: Class<*>,
            additionalDependencies: Array<Class<*>>
        ): String {
            val additionalDependenciesClassPath = additionalDependencies
                .map { locateClass(it) }
                .joinToString(File.pathSeparator) { findPathToClassFiles(it).toAbsolutePath().toString() }
            val classLocation = locateClass(clazz)
            if (classLocation != previousClassLocation) {
                buildDir = findPathToClassFiles(classLocation)
                previousClassLocation = classLocation
            }
            return additionalDependenciesClassPath
        }
    }

    data class MethodWithMockStrategy(
        val method: ExecutableId,
        val mockStrategy: MockStrategyApi,
        val substituteStatics: Boolean
    )

    data class MethodResult(val testSet: UtMethodTestSet, val coverage: Coverage)
}

@Suppress("UNCHECKED_CAST")
// TODO please use matcher.reflect().call(...) when it will be ready, currently call isn't supported in kotlin reflect
public fun invokeMatcher(matcher: Function<Boolean>, params: List<Any?>) = when (matcher) {
    is Function1<*, *> -> (matcher as Function1<Any?, Boolean>).invoke(params[0])
    is Function2<*, *, *> -> (matcher as Function2<Any?, Any?, Boolean>).invoke(params[0], params[1])
    is Function3<*, *, *, *> -> (matcher as Function3<Any?, Any?, Any?, Boolean>).invoke(
        params[0], params[1], params[2]
    )
    is Function4<*, *, *, *, *> -> (matcher as Function4<Any?, Any?, Any?, Any?, Boolean>).invoke(
        params[0], params[1], params[2], params[3]
    )
    is Function5<*, *, *, *, *, *> -> (matcher as Function5<Any?, Any?, Any?, Any?, Any?, Boolean>).invoke(
        params[0], params[1], params[2], params[3], params[4],
    )
    is Function6<*, *, *, *, *, *, *> -> (matcher as Function6<Any?, Any?, Any?, Any?, Any?, Any?, Boolean>).invoke(
        params[0], params[1], params[2], params[3], params[4], params[5],
    )
    is Function7<*, *, *, *, *, *, *, *> -> (matcher as Function7<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Boolean>).invoke(
        params[0], params[1], params[2], params[3], params[4], params[5], params[6],
    )
    else -> error("Function with arity > 7 not supported")
}

fun between(bounds: IntRange) = ExecutionsNumberMatcher("$bounds") { it in bounds }
val ignoreExecutionsNumber = ExecutionsNumberMatcher("Do not calculate") { it > 0 }

fun atLeast(percents: Int) = AtLeast(percents)

fun signatureOf(function: Function<*>): String {
    function as KFunction
    return function.executableId.signature
}

fun MockInfo.mocksMethod(method: Function<*>) = signatureOf(method) == this.method.signature

fun List<MockInfo>.singleMockOrNull(field: String, method: Function<*>): MockInfo? =
    singleOrNull { (it.mock as? FieldMockTarget)?.field == field && it.mocksMethod(method) }

fun List<MockInfo>.singleMock(field: String, method: Function<*>): MockInfo =
    single { (it.mock as? FieldMockTarget)?.field == field && it.mocksMethod(method) }

fun List<MockInfo>.singleMock(id: MockId, method: Function<*>): MockInfo =
    single { (it.mock as? ObjectMockTarget)?.id == id && it.mocksMethod(method) }

inline fun <reified T> MockInfo.value(index: Int = 0): T =
    when (val value = (values[index] as? UtConcreteValue<*>)?.value) {
        is T -> value
        else -> error("Unsupported type: ${values[index]}")
    }

fun MockInfo.mockValue(index: Int = 0): UtMockValue = values[index] as UtMockValue

fun MockInfo.isParameter(num: Int): Boolean = (mock as? ParameterMockTarget)?.index == num

inline fun <reified T> Result<*>.isException(): Boolean = exceptionOrNull() is T

inline fun <reified T> UtCompositeModel.mockValues(methodName: String): List<T> =
    mocks.filterKeys { it.name == methodName }.values.flatten().map { it.primitiveValue() }

inline fun <reified T> UtModel.primitiveValue(): T =
    (this as? UtPrimitiveModel)?.value as? T ?: error("Can't transform $this to ${T::class}")

sealed class CoverageMatcher(private val description: String, private val cmp: (Coverage) -> Boolean) {
    operator fun invoke(c: Coverage) = cmp(c)
    override fun toString() = description
}

object Full : CoverageMatcher("full coverage", { it.counters.all { it.total == it.covered } })

class AtLeast(percents: Int) : CoverageMatcher("at least $percents% coverage",
    { it.counters.all { 100 * it.covered >= percents * it.total } })

object DoNotCalculate : CoverageMatcher("Do not calculate", { true })

class FullWithAssumptions(assumeCallsNumber: Int) : CoverageMatcher(
    "full coverage except failed assume calls",
    { it.instructionCounter.let { it.covered >= it.total - assumeCallsNumber } }
) {
    init {
        require(assumeCallsNumber > 0) {
            "Non-positive number of assume calls $assumeCallsNumber passed (for zero calls use Full coverage matcher"
        }
    }
}

// simple matchers
fun withResult(ex: UtValueExecution<*>) = ex.paramsBefore + ex.evaluatedResult
fun withException(ex: UtValueExecution<*>) = ex.paramsBefore + ex.returnValue
fun withStaticsBefore(ex: UtValueExecution<*>) = ex.paramsBefore + ex.staticsBefore + ex.evaluatedResult
fun withStaticsBeforeAndExceptions(ex: UtValueExecution<*>) = ex.paramsBefore + ex.staticsBefore + ex.returnValue
fun withStaticsAfter(ex: UtValueExecution<*>) = ex.paramsBefore + ex.staticsAfter + ex.evaluatedResult
fun withThisAndStaticsAfter(ex: UtValueExecution<*>) = listOf(ex.callerBefore) + ex.paramsBefore + ex.staticsAfter + ex.evaluatedResult
fun withThisAndResult(ex: UtValueExecution<*>) = listOf(ex.callerBefore) + ex.paramsBefore + ex.evaluatedResult
fun withThisStaticsBeforeAndResult(ex: UtValueExecution<*>) =
    listOf(ex.callerBefore) + ex.paramsBefore + ex.staticsBefore + ex.evaluatedResult

fun withThisAndException(ex: UtValueExecution<*>) = listOf(ex.callerBefore) + ex.paramsBefore + ex.returnValue
fun withMocks(ex: UtValueExecution<*>) = ex.paramsBefore + listOf(ex.mocks) + ex.evaluatedResult
fun withMocksAndInstrumentation(ex: UtValueExecution<*>) =
    ex.paramsBefore + listOf(ex.mocks) + listOf(ex.instrumentation) + ex.evaluatedResult

fun withMocksInstrumentationAndThis(ex: UtValueExecution<*>) =
    listOf(ex.callerBefore) + ex.paramsBefore + listOf(ex.mocks) + listOf(ex.instrumentation) + ex.evaluatedResult

// mutations
fun withParamsMutations(ex: UtValueExecution<*>) = ex.paramsBefore + ex.paramsAfter
fun withMutations(ex: UtValueExecution<*>) = ex.paramsBefore + ex.staticsBefore + ex.paramsAfter + ex.staticsAfter
fun withParamsMutationsAndResult(ex: UtValueExecution<*>) = ex.paramsBefore + ex.paramsAfter + ex.evaluatedResult
fun withMutationsAndResult(ex: UtValueExecution<*>) =
    ex.paramsBefore + ex.staticsBefore + ex.paramsAfter + ex.staticsAfter + ex.evaluatedResult

fun withMutationsAndThis(ex: UtValueExecution<*>) =
    mutableListOf<Any?>().apply {
        add(ex.callerBefore)
        addAll(ex.paramsBefore)
        add(ex.staticsBefore)

        add(ex.callerAfter)
        addAll(ex.paramsAfter)
        add(ex.staticsAfter)

        add(ex.returnValue)
    }

private val UtValueExecution<*>.callerBefore get() = stateBefore.caller!!.value
private val UtValueExecution<*>.paramsBefore get() = stateBefore.params.map { it.value }
private val UtValueExecution<*>.staticsBefore get() = stateBefore.statics

private val UtValueExecution<*>.callerAfter get() = stateAfter.caller!!.value
private val UtValueExecution<*>.paramsAfter get() = stateAfter.params.map { it.value }
private val UtValueExecution<*>.staticsAfter get() = stateAfter.statics

private val UtValueExecution<*>.evaluatedResult get() = returnValue.getOrNull()

fun keyContain(vararg keys: String) = { summary: String? ->
    if (summary != null) {
        keys.all { it in summary }
    } else false
}

fun keyMatch(keyText: String) = { summary: String? ->
    keyText == summary
}


private fun flattenDocStatements(summary: List<DocStatement>): List<DocStatement> {
    val flatten = mutableListOf<DocStatement>()
    for (s in summary) {
        when (s) {
            is DocPreTagStatement -> flatten.addAll(flattenDocStatements(s.content))
            is DocClassLinkStmt -> flatten.add(s)
            is DocMethodLinkStmt -> flatten.add(s)
            is DocCodeStmt -> flatten.add(s)
            is DocRegularStmt -> flatten.add(s)
            is DocCustomTagStatement -> flatten.add(s)
        }
    }
    return flatten
}

fun keyContain(vararg keys: DocStatement) = { summary: List<DocStatement>? ->
    summary?.let { keys.all { key -> key in flattenDocStatements(it) } } ?: false
}

fun keyMatch(keyStmt: List<DocStatement>) = { summary: List<DocStatement>? ->
    summary?.let { keyStmt == summary } ?: false
}


fun Map<FieldId, UtConcreteValue<*>>.findByName(name: String) = entries.single { name == it.key.name }.value.value
fun Map<FieldId, UtConcreteValue<*>>.singleValue() = values.single().value

typealias StaticsType = Map<FieldId, UtConcreteValue<*>>
private typealias Mocks = List<MockInfo>
private typealias Instrumentation = List<UtInstrumentation>


inline fun <reified T> withSettingsFromTestFrameworkConfiguration(
    config: TestFrameworkConfiguration,
    block: () -> T
): T {
    val substituteStaticsWithSymbolicVariable = UtSettings.substituteStaticsWithSymbolicVariable
    UtSettings.substituteStaticsWithSymbolicVariable = config.resetNonFinalFieldsAfterClinit

    val previousConfig = TestCodeGeneratorPipeline.currentTestFrameworkConfiguration
    TestCodeGeneratorPipeline.currentTestFrameworkConfiguration = config
    try {
        return block()
    } finally {
        UtSettings.substituteStaticsWithSymbolicVariable = substituteStaticsWithSymbolicVariable
        TestCodeGeneratorPipeline.currentTestFrameworkConfiguration = previousConfig
    }
}
