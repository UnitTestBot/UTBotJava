package org.utbot.framework.codegen.tree

import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.Junit4
import org.utbot.framework.codegen.domain.Junit5
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.TestNg
import org.utbot.framework.codegen.domain.builtin.TestClassUtilMethodProvider
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgAuxiliaryClass
import org.utbot.framework.codegen.domain.models.CgClassBody
import org.utbot.framework.codegen.domain.models.CgMethod
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.CgMethodsCluster
import org.utbot.framework.codegen.domain.models.CgRealNestedClassesRegion
import org.utbot.framework.codegen.domain.models.CgRegion
import org.utbot.framework.codegen.domain.models.CgSimpleRegion
import org.utbot.framework.codegen.domain.models.CgStaticsRegion
import org.utbot.framework.codegen.domain.models.CgTestMethod
import org.utbot.framework.codegen.domain.models.CgTestMethodCluster
import org.utbot.framework.codegen.domain.models.CgTripleSlashMultilineComment
import org.utbot.framework.codegen.domain.models.CgUtilEntity
import org.utbot.framework.codegen.domain.models.CgUtilMethod
import org.utbot.framework.codegen.domain.models.SimpleTestClassModel
import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.codegen.renderer.importUtilMethodDependencies
import org.utbot.framework.codegen.reports.TestsGenerationReport
import org.utbot.framework.codegen.tree.CgComponents.clearContextRelatedStorage
import org.utbot.framework.codegen.tree.CgComponents.getMethodConstructorBy
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.util.description
import org.utbot.framework.plugin.api.util.humanReadableName
import org.utbot.fuzzer.UtFuzzedExecution

/**
 * This test class constructor is used for pure Java/Kotlin applications.
 */
open class CgSimpleTestClassConstructor(context: CgContext): CgTestClassConstructorBase<SimpleTestClassModel>(context) {

    init {
        clearContextRelatedStorage()
    }

    override val methodConstructor = getMethodConstructorBy(context)

    val testsGenerationReport = TestsGenerationReport()

    override fun constructTestClassBody(testClassModel: SimpleTestClassModel): CgClassBody =
        buildClassBody(currentTestClass) {
            val notYetConstructedTestSets = testClassModel.methodTestSets.toMutableList()

            for (nestedClass in testClassModel.nestedClasses) {
                // It is not possible to run tests for both outer and inner class in JUnit4 at once,
                // so we locate all test methods in outer test class for JUnit4.
                // see https://stackoverflow.com/questions/69770700/how-to-run-tests-from-outer-class-and-nested-inner-classes-simultaneously-in-jun
                // or https://stackoverflow.com/questions/28230277/test-cases-in-inner-class-and-outer-class-with-junit4
                when (testFramework) {
                    Junit4 -> {
                        notYetConstructedTestSets += collectTestSetsFromInnerClasses(nestedClass)
                    }
                    Junit5,
                    TestNg -> {
                        nestedClassRegions += CgRealNestedClassesRegion(
                            "Tests for ${nestedClass.classUnderTest.simpleName}",
                            listOf(withNestedClassScope(nestedClass) { constructTestClass(nestedClass) })
                        )
                    }
                }
            }

            for (testSet in notYetConstructedTestSets) {
                updateCurrentExecutable(testSet.executableId)
                val currentMethodUnderTestRegions = constructTestSet(testSet) ?: continue
                val executableUnderTestCluster = CgMethodsCluster(
                    "Test suites for executable $currentExecutable",
                    currentMethodUnderTestRegions
                )
                methodRegions += executableUnderTestCluster
            }

            val currentTestClassDataProviderMethods = currentTestClassContext.cgDataProviderMethods
            if (currentTestClassDataProviderMethods.isNotEmpty()) {
                staticDeclarationRegions +=
                    CgStaticsRegion(
                        "Data provider methods for parametrized tests",
                        currentTestClassDataProviderMethods,
                    )
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

    private fun constructTestSet(testSet: CgMethodTestSet): List<CgRegion<CgMethod>>? {
        val regions = mutableListOf<CgRegion<CgMethod>>()

        if (testSet.executions.any()) {
            successfulExecutionsModels = testSet
                .executions
                .filter { it.result is UtExecutionSuccess }
                .map { (it.result as UtExecutionSuccess).model }

            runCatching {
                when (context.parametrizedTestSource) {
                    ParametrizedTestSource.DO_NOT_PARAMETRIZE -> createTest(testSet, regions)
                    ParametrizedTestSource.PARAMETRIZE ->
                        createParametrizedTestAndDataProvider(
                            testSet,
                            regions
                        )
                }
            }.onFailure { e -> processFailure(testSet, e) }
        }

        val errors = testSet.allErrors
        if (errors.isNotEmpty()) {
            regions += methodConstructor.errorMethod(testSet.executableId, errors)
            testsGenerationReport.addMethodErrors(testSet, errors)
        }

        return if (regions.any()) regions else null
    }

    private fun collectTestSetsFromInnerClasses(model: SimpleTestClassModel): List<CgMethodTestSet> {
        val testSets = model.methodTestSets.toMutableList()
        for (nestedClass in model.nestedClasses) {
            testSets += collectTestSetsFromInnerClasses(nestedClass)
        }

        return testSets
    }

    private fun processFailure(testSet: CgMethodTestSet, failure: Throwable) {
        codeGenerationErrors
            .getOrPut(testSet) { mutableMapOf() }
            .merge(failure.description, 1, Int::plus)
    }

    private fun createTest(
        testSet: CgMethodTestSet,
        regions: MutableList<CgRegion<CgMethod>>
    ) {
        val (methodUnderTest, _, clustersInfo) = testSet

        for ((clusterSummary, executionIndices) in clustersInfo) {
            val currentTestCaseTestMethods = mutableListOf<CgTestMethod>()
            emptyLineIfNeeded()
            val (checkedRange, needLimitExceedingComments) = if (executionIndices.last  - executionIndices.first >= UtSettings.maxTestsPerMethodInRegion) {
                IntRange(executionIndices.first, executionIndices.first + (UtSettings.maxTestsPerMethodInRegion - 1).coerceAtLeast(0)) to true
            } else {
                executionIndices to false
            }

            for (i in checkedRange) {
                currentTestCaseTestMethods += methodConstructor.createTestMethod(methodUnderTest, testSet.executions[i])
            }

            val comments = listOf("Actual number of generated tests (${executionIndices.last - executionIndices.first}) exceeds per-method limit (${UtSettings.maxTestsPerMethodInRegion})",
                "The limit can be configured in '{HOME_DIR}/.utbot/settings.properties' with 'maxTestsPerMethod' property")

            val clusterHeader = clusterSummary?.header
            var clusterContent = clusterSummary?.content
                ?.split('\n')
                ?.let { CgTripleSlashMultilineComment(if (needLimitExceedingComments) {it.toMutableList() + comments} else {it}) }
            if (clusterContent == null && needLimitExceedingComments) {
                clusterContent = CgTripleSlashMultilineComment(comments)
            }
            regions += CgTestMethodCluster(clusterHeader, clusterContent, currentTestCaseTestMethods)

            testsGenerationReport.addTestsByType(testSet, currentTestCaseTestMethods)
        }
    }

    private fun createParametrizedTestAndDataProvider(
        testSet: CgMethodTestSet,
        regions: MutableList<CgRegion<CgMethod>>
    ) {
        val (methodUnderTest, _, _) = testSet

        for (preparedTestSet in testSet.prepareTestSetsForParameterizedTestGeneration()) {
            val dataProviderMethodName = nameGenerator.dataProviderMethodNameFor(preparedTestSet.executableId)

            val parameterizedTestMethod =
                methodConstructor.createParameterizedTestMethod(preparedTestSet, dataProviderMethodName)

            testFrameworkManager.addDataProvider(
                methodConstructor.createParameterizedTestDataProvider(preparedTestSet, dataProviderMethodName)
            )

            regions += CgSimpleRegion(
                "Parameterized test for method ${methodUnderTest.humanReadableName}",
                listOf(parameterizedTestMethod),
            )
        }

        regions += CgSimpleRegion(
            "SYMBOLIC EXECUTION: additional tests for symbolic executions for method ${methodUnderTest.humanReadableName} that cannot be presented as parameterized",
            collectAdditionalSymbolicTestsForParametrizedMode(testSet),
        )

        regions += CgSimpleRegion(
            "FUZZER: Tests for method ${methodUnderTest.humanReadableName} that cannot be presented as parameterized",
            collectFuzzerTestsForParameterizedMode(testSet),
        )
    }

    /**
     * Collects standard tests for fuzzer executions in parametrized mode.
     * This is a requirement from [https://github.com/UnitTestBot/UTBotJava/issues/1137].
     */
    private fun collectFuzzerTestsForParameterizedMode(testSet: CgMethodTestSet): List<CgTestMethod> {
        val testMethods = mutableListOf<CgTestMethod>()

        testSet.executions
            .filterIsInstance<UtFuzzedExecution>()
            .forEach { execution ->
                testMethods += methodConstructor.createTestMethod(testSet.executableId, execution)
            }

        return testMethods
    }

    /**
     * Collects standard tests for symbolic executions that can't be included into parametrized tests.
     * This is a requirement from [https://github.com/UnitTestBot/UTBotJava/issues/1231].
     */
    private fun collectAdditionalSymbolicTestsForParametrizedMode(testSet: CgMethodTestSet): List<CgTestMethod> {
        val testMethods = mutableListOf<CgTestMethod>()

        testSet.executions
            .filterIsInstance<UtSymbolicExecution>()
            .filter { it.containsMocking }
            .forEach { execution ->
                testMethods += methodConstructor.createTestMethod(testSet.executableId, execution)
            }

        return testMethods
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
    protected val CgMethodTestSet.allErrors: Map<String, Int>
        get() = errors + codeGenerationErrors.getOrDefault(this, mapOf())

}

