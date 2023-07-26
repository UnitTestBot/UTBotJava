package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.Junit4
import org.utbot.framework.codegen.domain.Junit5
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.TestNg
import org.utbot.framework.codegen.domain.builtin.TestClassUtilMethodProvider
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgClassBody
import org.utbot.framework.codegen.domain.models.CgMethod
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.CgMethodsCluster
import org.utbot.framework.codegen.domain.models.CgRealNestedClassesRegion
import org.utbot.framework.codegen.domain.models.CgRegion
import org.utbot.framework.codegen.domain.models.CgSimpleRegion
import org.utbot.framework.codegen.domain.models.CgStaticsRegion
import org.utbot.framework.codegen.domain.models.CgTestMethod
import org.utbot.framework.codegen.domain.models.SimpleTestClassModel
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.util.humanReadableName
import org.utbot.fuzzer.UtFuzzedExecution

/**
 * This test class constructor is used for pure Java/Kotlin applications.
 */
open class CgSimpleTestClassConstructor(context: CgContext): CgAbstractTestClassConstructor<SimpleTestClassModel>(context) {

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

            for ((testSetIndex, testSet) in notYetConstructedTestSets.withIndex()) {
                updateExecutableUnderTest(testSet.executableId)
                withTestSetIdScope(testSetIndex) {
                    val currentMethodUnderTestRegions = constructTestSet(testSet) ?: return@withTestSetIdScope
                    val executableUnderTestCluster = CgMethodsCluster(
                        "Test suites for executable $currentExecutableUnderTest",
                        currentMethodUnderTestRegions
                    )
                    methodRegions += executableUnderTestCluster
                }
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

    override fun constructTestSet(testSet: CgMethodTestSet): List<CgRegion<CgMethod>>? {
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
            .withIndex()
            .forEach { (index, execution) ->
                withExecutionIdScope(index) {
                    testMethods += methodConstructor.createTestMethod(testSet.executableId, execution)
                }
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
            .withIndex()
            .forEach { (index, execution) ->
                withExecutionIdScope(index) {
                    testMethods += methodConstructor.createTestMethod(testSet.executableId, execution)
                }
            }

        return testMethods
    }
}

