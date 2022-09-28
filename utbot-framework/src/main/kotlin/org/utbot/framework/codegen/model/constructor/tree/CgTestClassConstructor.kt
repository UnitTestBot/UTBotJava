package org.utbot.framework.codegen.model.constructor.tree

import org.utbot.common.appendHtmlLine
import org.utbot.framework.codegen.ParametrizedTestSource
import org.utbot.framework.codegen.model.constructor.CgMethodTestSet
import org.utbot.framework.codegen.model.constructor.builtin.TestClassUtilMethodProvider
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
import org.utbot.framework.codegen.model.tree.CgAuxiliaryClass
import org.utbot.framework.codegen.model.tree.CgUtilEntity
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.description
import org.utbot.framework.plugin.api.util.humanReadableName
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
            this.declaredClass = withTestClassScope { constructTestClass(testClassModel) }
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

                val currentTestClassDataProviderMethods = currentTestClassContext.cgDataProviderMethods
                if (currentTestClassDataProviderMethods.isNotEmpty()) {
                    staticDeclarationRegions += CgStaticsRegion("Data providers", currentTestClassDataProviderMethods)
                }

                if (currentTestClass == outerMostTestClass) {
                    val utilEntities = collectUtilEntities()
                    // If utilMethodProvider is TestClassUtilMethodProvider, then util entities should be declared
                    // in the test class. Otherwise, util entities will be located elsewhere (e.g. another class).
                    if (utilMethodProvider is TestClassUtilMethodProvider && utilEntities.isNotEmpty()) {
                        staticDeclarationRegions += CgStaticsRegion("Util methods", utilEntities)
                    }
                }
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

        allExecutions = testSet.executions

        val (methodUnderTest, _, _, clustersInfo) = testSet
        val regions = mutableListOf<CgRegion<CgMethod>>()
        val requiredFields = mutableListOf<CgParameterDeclaration>()

        when (context.parametrizedTestSource) {
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
                "Parameterized test for method ${methodUnderTest.humanReadableName}",
                listOf(parameterizedTestMethod),
            )
        }.onFailure { error -> processFailure(testSet, error) }
    }

    /**
     * This method collects a list of util entities (methods and classes) needed by the class.
     * By the end of the test method generation [requiredUtilMethods] may not contain all the needed.
     * That's because some util methods may not be directly used in tests, but they may be used from other util methods.
     * We define such method dependencies in [MethodId.methodDependencies].
     *
     * Once all dependencies are collected, required methods are added back to [requiredUtilMethods],
     * because during the work of this method they are being removed from this list, so we have to put them back in.
     *
     * Also, some util methods may use some classes that also need to be generated.
     * That is why we collect information about required classes using [MethodId.classDependencies].
     *
     * @return a list of [CgUtilEntity] representing required util methods and classes (including their own dependencies).
     */
    private fun collectUtilEntities(): List<CgUtilEntity> {
        val utilMethods = mutableListOf<CgUtilMethod>()
        // Some util methods depend on other util methods or some auxiliary classes.
        // Using this loop we make sure that all the util method dependencies are taken into account.
        val requiredClasses = mutableSetOf<ClassId>()
        while (requiredUtilMethods.isNotEmpty()) {
            val method = requiredUtilMethods.first()
            requiredUtilMethods.remove(method)
            if (method.name !in existingMethodNames) {
                utilMethods += CgUtilMethod(method)
                // we only need imports from util methods if these util methods are declared in the test class
                if (utilMethodProvider is TestClassUtilMethodProvider) {
                    importUtilMethodDependencies(method)
                }
                existingMethodNames += method.name
                requiredUtilMethods += method.methodDependencies()
                requiredClasses += method.classDependencies()
            }
        }
        // Collect all util methods back into requiredUtilMethods.
        // Now there will also be util methods that weren't present in requiredUtilMethods at first,
        // but were needed for the present util methods to work.
        requiredUtilMethods += utilMethods.map { method -> method.id }

        val auxiliaryClasses = requiredClasses.map { CgAuxiliaryClass(it) }

        return utilMethods + auxiliaryClasses
    }

    /**
     * If @receiver is an util method, then returns a list of util method ids that @receiver depends on
     * Otherwise, an empty list is returned
     */
    private fun MethodId.methodDependencies(): List<MethodId> = when (this) {
        createInstance -> listOf(getUnsafeInstance)
        deepEquals -> listOf(arraysDeepEquals, iterablesDeepEquals, streamsDeepEquals, mapsDeepEquals, hasCustomEquals)
        arraysDeepEquals, iterablesDeepEquals, streamsDeepEquals, mapsDeepEquals -> listOf(deepEquals)
        buildLambda, buildStaticLambda -> listOf(
            getLookupIn, getSingleAbstractMethod, getLambdaMethod,
            getLambdaCapturedArgumentTypes, getInstantiatedMethodType, getLambdaCapturedArgumentValues
        )
        else -> emptyList()
    }

    /**
     * If @receiver is an util method, then returns a list of auxiliary class ids that @receiver depends on.
     * Otherwise, an empty list is returned.
     */
    private fun MethodId.classDependencies(): List<ClassId> = when (this) {
        buildLambda, buildStaticLambda -> listOf(capturedArgumentClass)
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
