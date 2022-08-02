package org.utbot.framework.codegen.model.constructor.tree

import org.utbot.common.appendHtmlLine
import org.utbot.engine.displayName
import org.utbot.framework.codegen.ParametrizedTestSource
import org.utbot.framework.codegen.model.constructor.CgMethodTestSet
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.framework.codegen.model.constructor.util.CgComponents
import org.utbot.framework.codegen.model.constructor.util.CgStatementConstructor
import org.utbot.framework.codegen.model.tree.CgMethod
import org.utbot.framework.codegen.model.tree.CgExecutableUnderTestCluster
import org.utbot.framework.codegen.model.tree.CgParameterDeclaration
import org.utbot.framework.codegen.model.tree.CgRegion
import org.utbot.framework.codegen.model.tree.CgSimpleRegion
import org.utbot.framework.codegen.model.tree.CgStaticsRegion
import org.utbot.framework.codegen.model.tree.CgTestClass
import org.utbot.framework.codegen.model.tree.CgTestClassFile
import org.utbot.framework.codegen.model.tree.CgTestMethod
import org.utbot.framework.codegen.model.tree.CgTestMethodCluster
import org.utbot.framework.codegen.model.tree.CgTestMethodType.*
import org.utbot.framework.codegen.model.tree.CgTripleSlashMultilineComment
import org.utbot.framework.codegen.model.tree.CgUtilMethod
import org.utbot.framework.codegen.model.tree.buildTestClass
import org.utbot.framework.codegen.model.tree.buildTestClassBody
import org.utbot.framework.codegen.model.tree.buildTestClassFile
import org.utbot.framework.codegen.model.visitor.importUtilMethodDependencies
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.codegen.model.constructor.TestClassModel
import org.utbot.framework.plugin.api.util.description
import org.utbot.framework.plugin.api.util.kClass
import kotlin.reflect.KClass

internal class CgTestClassConstructor(val context: CgContext) :
    CgContextOwner by context,
    CgStatementConstructor by CgComponents.getStatementConstructorBy(context) {

    private val methodConstructor = CgComponents.getMethodConstructorBy(context)
    private val nameGenerator = CgComponents.getNameGeneratorBy(context)
    private val testFrameworkManager = CgComponents.getTestFrameworkManagerBy(context)

    private val testsGenerationReport: TestsGenerationReport = TestsGenerationReport()

    /**
     * Given a testClass model  constructs CgTestClass
     */
    fun construct(testClassModel: TestClassModel): CgTestClassFile {
        return buildTestClassFile {
            this.testClass = withTestClassScope { constructTestClass(testClassModel) }
            imports += context.collectedImports
            testsGenerationReport = this@CgTestClassConstructor.testsGenerationReport
        }
    }

    private fun constructTestClass(testClassModel: TestClassModel): CgTestClass {
        return buildTestClass {
            id = currentTestClass

            if (currentTestClass != outerMostTestClass) {
                isNested = true
                isStatic = testFramework.nestedClassesShouldBeStatic
                testFrameworkManager.annotationForNestedClasses?.let {
                    currentTestClassContext.collectedTestClassAnnotations += it
                }
            }
            if (testClassModel.nestedClasses.isNotEmpty()) {
                testFrameworkManager.annotationForOuterClasses?.let {
                    currentTestClassContext.collectedTestClassAnnotations += it
                }
            }

            body = buildTestClassBody {
                for (nestedClass in testClassModel.nestedClasses) {
                    nestedClassRegions += CgSimpleRegion(
                        "Tests for ${nestedClass.classUnderTest.simpleName}",
                        listOf(
                            withNestedClassScope(nestedClass) { constructTestClass(nestedClass) }
                        )
                    )
                }

                for (testSet in testClassModel.methodTestSets) {
                    updateCurrentExecutable(testSet.executableId)
                    val currentMethodUnderTestRegions = constructTestSet(testSet) ?: continue
                    val executableUnderTestCluster = CgExecutableUnderTestCluster(
                        "Test suites for executable $currentExecutable",
                        currentMethodUnderTestRegions
                    )
                    testMethodRegions += executableUnderTestCluster
                }

                val utilMethods = if (currentTestClass == outerMostTestClass)
                    createUtilMethods()
                else
                    emptyList()

                val additionalMethods = currentTestClassContext.cgDataProviderMethods + utilMethods

                dataProvidersAndUtilMethodsRegion += CgStaticsRegion(
                    "Data providers and utils methods",
                    additionalMethods
                )
            }
            // It is important that annotations, superclass and interfaces assignment is run after
            // all methods are generated so that all necessary info is already present in the context
            with (currentTestClassContext) {
                annotations += collectedTestClassAnnotations
                superclass = testClassSuperclass
                interfaces += collectedTestClassInterfaces
            }
        }
    }

    private fun constructTestSet(testSet: CgMethodTestSet): List<CgRegion<CgMethod>>? {
        if (testSet.executions.isEmpty()) {
            return null
        }

        val (methodUnderTest, _, _, clustersInfo) = testSet
        val regions = mutableListOf<CgRegion<CgMethod>>()
        val requiredFields = mutableListOf<CgParameterDeclaration>()

        when (context.parameterizedTestSource) {
            ParametrizedTestSource.DO_NOT_PARAMETRIZE -> {
                for ((clusterSummary, executionIndices) in clustersInfo) {
                    val currentTestCaseTestMethods = mutableListOf<CgTestMethod>()
                    emptyLineIfNeeded()
                    for (i in executionIndices) {
                        runCatching {
                            currentTestCaseTestMethods += methodConstructor.createTestMethod(methodUnderTest, testSet.executions[i])
                        }.onFailure { e -> processFailure(testSet, e) }
                    }
                    val clusterHeader = clusterSummary?.header
                    val clusterContent = clusterSummary?.content
                        ?.split('\n')
                        ?.let { CgTripleSlashMultilineComment(it) }
                    regions += CgTestMethodCluster(clusterHeader, clusterContent, currentTestCaseTestMethods)

                    testsGenerationReport.addTestsByType(testSet, currentTestCaseTestMethods)
                }
            }
            ParametrizedTestSource.PARAMETRIZE -> {
                for (splitByExecutionTestSet in testSet.splitExecutionsByResult()) {
                    for (splitByChangedStaticsTestSet in splitByExecutionTestSet.splitExecutionsByChangedStatics()) {
                        createParametrizedTestAndDataProvider(
                            splitByChangedStaticsTestSet,
                            requiredFields,
                            regions,
                            methodUnderTest
                        )
                    }
                }
            }
        }

        val errors = testSet.allErrors
        if (errors.isNotEmpty()) {
            regions += methodConstructor.errorMethod(testSet.executableId, errors)
            testsGenerationReport.addMethodErrors(testSet, errors)
        }

        return regions
    }

    private fun processFailure(testSet: CgMethodTestSet, failure: Throwable) {
        codeGenerationErrors
            .getOrPut(testSet) { mutableMapOf() }
            .merge(failure.description, 1, Int::plus)
    }

    private fun createParametrizedTestAndDataProvider(
        testSet: CgMethodTestSet,
        requiredFields: MutableList<CgParameterDeclaration>,
        regions: MutableList<CgRegion<CgMethod>>,
        methodUnderTest: ExecutableId,
    ) {
        runCatching {
            val dataProviderMethodName = nameGenerator.dataProviderMethodNameFor(testSet.executableId)

            val parameterizedTestMethod =
                methodConstructor.createParameterizedTestMethod(testSet, dataProviderMethodName)

            requiredFields += parameterizedTestMethod.requiredFields

            testFrameworkManager.addDataProvider(
                methodConstructor.createParameterizedTestDataProvider(testSet, dataProviderMethodName)
            )

            regions += CgSimpleRegion(
                "Parameterized test for method ${methodUnderTest.displayName}",
                listOf(parameterizedTestMethod),
            )
        }.onFailure { error -> processFailure(testSet, error) }
    }

    // TODO: collect imports of util methods
    private fun createUtilMethods(): List<CgUtilMethod> {
        val utilMethods = mutableListOf<CgUtilMethod>()
        // some util methods depend on the others
        // using this loop we make sure that all the
        // util methods dependencies are taken into account
        while (requiredUtilMethods.isNotEmpty()) {
            val method = requiredUtilMethods.first()
            requiredUtilMethods.remove(method)
            if (method.name !in existingMethodNames) {
                utilMethods += CgUtilMethod(method)
                importUtilMethodDependencies(method)
                existingMethodNames += method.name
                requiredUtilMethods += method.dependencies()
            }
        }
        return utilMethods
    }

    /**
     * If @receiver is an util method, then returns a list of util method ids that @receiver depends on
     * Otherwise, an empty list is returned
     */
    private fun MethodId.dependencies(): List<MethodId> = when (this) {
        createInstance -> listOf(getUnsafeInstance)
        deepEquals -> listOf(arraysDeepEquals, iterablesDeepEquals, streamsDeepEquals, mapsDeepEquals, hasCustomEquals)
        arraysDeepEquals, iterablesDeepEquals, streamsDeepEquals, mapsDeepEquals -> listOf(deepEquals)
        else -> emptyList()
    }

    /**
     * Engine errors + codegen errors for a given [UtMethodTestSet]
     */
    private val CgMethodTestSet.allErrors: Map<String, Int>
        get() = errors + codeGenerationErrors.getOrDefault(this, mapOf())
}

typealias MethodGeneratedTests = MutableMap<ExecutableId, MutableSet<CgTestMethod>>
typealias ErrorsCount = Map<String, Int>

data class TestsGenerationReport(
    val executables: MutableSet<ExecutableId> = mutableSetOf(),
    var successfulExecutions: MethodGeneratedTests = mutableMapOf(),
    var timeoutExecutions: MethodGeneratedTests = mutableMapOf(),
    var failedExecutions: MethodGeneratedTests = mutableMapOf(),
    var crashExecutions: MethodGeneratedTests = mutableMapOf(),
    var errors: MutableMap<ExecutableId, ErrorsCount> = mutableMapOf()
) {
    val classUnderTest: KClass<*>
        get() = executables.firstOrNull()?.classId?.kClass
            ?: error("No executables found in test report")

    val initialWarnings: MutableList<() -> String> = mutableListOf()
    val hasWarnings: Boolean
        get() = initialWarnings.isNotEmpty()

    val detailedStatistics: String
        get() = buildString {
            appendHtmlLine("Class: ${classUnderTest.qualifiedName}")
            val testMethodsStatistic = executables.map { it.countTestMethods() }
            val errors = executables.map { it.countErrors() }
            val overallErrors = errors.sum()

            appendHtmlLine("Successful test methods: ${testMethodsStatistic.sumBy { it.successful }}")
            appendHtmlLine(
                "Failing because of unexpected exception test methods: ${testMethodsStatistic.sumBy { it.failing }}"
            )
            appendHtmlLine(
                "Failing because of exceeding timeout test methods: ${testMethodsStatistic.sumBy { it.timeout }}"
            )
            appendHtmlLine(
                "Failing because of possible JVM crash test methods: ${testMethodsStatistic.sumBy { it.crashes }}"
            )
            appendHtmlLine("Not generated because of internal errors test methods: $overallErrors")
        }

    fun addMethodErrors(testSet: CgMethodTestSet, errors: Map<String, Int>) {
        this.errors[testSet.executableId] = errors
    }

    fun addTestsByType(testSet: CgMethodTestSet, testMethods: List<CgTestMethod>) {
        with(testSet.executableId) {
            executables += this

            testMethods.forEach {
                when (it.type) {
                    SUCCESSFUL -> updateExecutions(it, successfulExecutions)
                    FAILING -> updateExecutions(it, failedExecutions)
                    TIMEOUT -> updateExecutions(it, timeoutExecutions)
                    CRASH -> updateExecutions(it, crashExecutions)
                    PARAMETRIZED -> {
                        // Parametrized tests are not supported in the tests report yet
                        // TODO JIRA:1507
                    }
                }
            }
        }
    }

    fun toString(isShort: Boolean): String = buildString {
        appendHtmlLine("Target: ${classUnderTest.qualifiedName}")
        if (initialWarnings.isNotEmpty()) {
            initialWarnings.forEach { appendHtmlLine(it()) }
            appendHtmlLine()
        }

        val testMethodsStatistic = executables.map { it.countTestMethods() }
        val overallTestMethods = testMethodsStatistic.sumBy { it.count }

        appendHtmlLine("Overall test methods: $overallTestMethods")

        if (!isShort) {
            appendHtmlLine(detailedStatistics)
        }
    }

    override fun toString(): String = toString(false)

    private fun ExecutableId.countTestMethods(): TestMethodStatistic = TestMethodStatistic(
        testMethodsNumber(successfulExecutions),
        testMethodsNumber(failedExecutions),
        testMethodsNumber(timeoutExecutions),
        testMethodsNumber(crashExecutions)
    )

    private fun ExecutableId.countErrors(): Int = errors.getOrDefault(this, emptyMap()).values.sum()

    private fun ExecutableId.testMethodsNumber(executables: MethodGeneratedTests): Int =
        executables.getOrDefault(this, emptySet()).size

    private fun ExecutableId.updateExecutions(it: CgTestMethod, executions: MethodGeneratedTests) {
        executions.getOrPut(this) { mutableSetOf() } += it
    }

    private data class TestMethodStatistic(val successful: Int, val failing: Int, val timeout: Int, val crashes: Int) {
        val count: Int = successful + failing + timeout + crashes
    }
}
