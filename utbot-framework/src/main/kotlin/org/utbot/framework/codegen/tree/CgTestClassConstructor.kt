package org.utbot.framework.codegen.tree

import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.builtin.TestClassUtilMethodProvider
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgAuxiliaryClass
import org.utbot.framework.codegen.domain.models.CgClass
import org.utbot.framework.codegen.domain.models.CgClassFile
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
import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.codegen.renderer.importUtilMethodDependencies
import org.utbot.framework.codegen.reports.TestsGenerationReport
import org.utbot.framework.codegen.services.CgNameGenerator
import org.utbot.framework.codegen.services.access.CgCallableAccessManager
import org.utbot.framework.codegen.services.framework.MockFrameworkManager
import org.utbot.framework.codegen.services.framework.TestFrameworkManager
import org.utbot.framework.codegen.tree.CgTestClassConstructor.CgComponents.clearContextRelatedStorage
import org.utbot.framework.codegen.tree.CgTestClassConstructor.CgComponents.getMethodConstructorBy
import org.utbot.framework.codegen.tree.CgTestClassConstructor.CgComponents.getNameGeneratorBy
import org.utbot.framework.codegen.tree.CgTestClassConstructor.CgComponents.getStatementConstructorBy
import org.utbot.framework.codegen.tree.CgTestClassConstructor.CgComponents.getTestFrameworkManagerBy

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.util.description
import org.utbot.framework.plugin.api.util.humanReadableName
import org.utbot.fuzzer.UtFuzzedExecution

open class CgTestClassConstructor(val context: CgContext) :
    CgContextOwner by context,
    CgStatementConstructor by getStatementConstructorBy(context) {

    init {
        clearContextRelatedStorage()
    }

    private val methodConstructor = getMethodConstructorBy(context)
    private val nameGenerator = getNameGeneratorBy(context)
    private val testFrameworkManager = getTestFrameworkManagerBy(context)

    val testsGenerationReport = TestsGenerationReport()

    /**
     * Given a testClass model  constructs CgTestClass
     */
    open fun construct(testClassModel: TestClassModel): CgClassFile {
        return buildClassFile {
            this.declaredClass = withTestClassScope { constructTestClass(testClassModel) }
            imports += context.collectedImports
        }
    }

    open fun constructTestClass(testClassModel: TestClassModel): CgClass {
        return buildClass {
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

            body = buildClassBody(currentTestClass) {
                for (nestedClass in testClassModel.nestedClasses) {
                    nestedClassRegions += CgRealNestedClassesRegion(
                        "Tests for ${nestedClass.classUnderTest.simpleName}",
                        listOf(
                            withNestedClassScope(nestedClass) { constructTestClass(nestedClass) }
                        )
                    )
                }

                for (testSet in testClassModel.methodTestSets) {
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
            // It is important that annotations, superclass and interfaces assignment is run after
            // all methods are generated so that all necessary info is already present in the context
            with (currentTestClassContext) {
                annotations += collectedTestClassAnnotations
                superclass = testClassSuperclass
                interfaces += collectedTestClassInterfaces
            }
        }
    }

    fun constructTestSet(testSet: CgMethodTestSet): List<CgRegion<CgMethod>>? {
        if (testSet.executions.isEmpty()) {
            return null
        }

        successfulExecutionsModels = testSet
            .executions
            .filter { it.result is UtExecutionSuccess }
            .map { (it.result as UtExecutionSuccess).model }

        val regions = mutableListOf<CgRegion<CgMethod>>()

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

    private fun createTest(
        testSet: CgMethodTestSet,
        regions: MutableList<CgRegion<CgMethod>>
    ) {
        val (methodUnderTest, _, _, clustersInfo) = testSet

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
        val (methodUnderTest, _, _, _) = testSet

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
            "SYMBOLIC EXECUTION: additional tests for symbolic executions for method ${methodUnderTest.humanReadableName}",
            collectAdditionalSymbolicTestsForParametrizedMode(testSet),
        )

        regions += CgSimpleRegion(
            "FUZZER: Tests for method ${methodUnderTest.humanReadableName}",
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

    object CgComponents {
        /**
         * Clears all stored data for current [CgContext].
         * As far as context is created per class under test,
         * no related data is required after it's processing.
         */
        fun clearContextRelatedStorage() {
            nameGenerators.clear()
            statementConstructors.clear()
            callableAccessManagers.clear()
            testFrameworkManagers.clear()
            mockFrameworkManagers.clear()
            variableConstructors.clear()
            methodConstructors.clear()
        }

        private val nameGenerators: MutableMap<CgContext, CgNameGenerator> = mutableMapOf()
        private val statementConstructors: MutableMap<CgContext, CgStatementConstructor> = mutableMapOf()
        private val callableAccessManagers: MutableMap<CgContext, CgCallableAccessManager> = mutableMapOf()
        private val testFrameworkManagers: MutableMap<CgContext, TestFrameworkManager> = mutableMapOf()
        private val mockFrameworkManagers: MutableMap<CgContext, MockFrameworkManager> = mutableMapOf()

        private val variableConstructors: MutableMap<CgContext, CgVariableConstructor> = mutableMapOf()
        private val methodConstructors: MutableMap<CgContext, CgMethodConstructor> = mutableMapOf()

        fun getNameGeneratorBy(context: CgContext) = nameGenerators.getOrPut(context) {
            context.cgLanguageAssistant.getNameGeneratorBy(context)
        }
        fun getCallableAccessManagerBy(context: CgContext) = callableAccessManagers.getOrPut(context) {
            context.cgLanguageAssistant.getCallableAccessManagerBy(context)
        }
        fun getStatementConstructorBy(context: CgContext) = statementConstructors.getOrPut(context) {
            context.cgLanguageAssistant.getStatementConstructorBy(context)
        }

        fun getTestFrameworkManagerBy(context: CgContext) =
            testFrameworkManagers.getOrDefault(context, context.cgLanguageAssistant.getLanguageTestFrameworkManager().managerByFramework(context))

        fun getMockFrameworkManagerBy(context: CgContext) = mockFrameworkManagers.getOrPut(context) { MockFrameworkManager(context) }
        fun getVariableConstructorBy(context: CgContext) = variableConstructors.getOrPut(context) {
            context.cgLanguageAssistant.getVariableConstructorBy(context)
        }
        fun getMethodConstructorBy(context: CgContext) = methodConstructors.getOrPut(context) {
            context.cgLanguageAssistant.getMethodConstructorBy(context)
        }
    }
}

