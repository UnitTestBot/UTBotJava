package org.utbot.framework.codegen.tree

import mu.KotlinLogging
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.builtin.TestClassUtilMethodProvider
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgAuxiliaryClass
import org.utbot.framework.codegen.domain.models.CgClass
import org.utbot.framework.codegen.domain.models.CgClassBody
import org.utbot.framework.codegen.domain.models.CgClassFile
import org.utbot.framework.codegen.domain.models.CgMethod
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.CgRegion
import org.utbot.framework.codegen.domain.models.CgTestMethod
import org.utbot.framework.codegen.domain.models.CgTestMethodCluster
import org.utbot.framework.codegen.domain.models.CgTripleSlashMultilineComment
import org.utbot.framework.codegen.domain.models.CgUtilEntity
import org.utbot.framework.codegen.domain.models.CgUtilMethod
import org.utbot.framework.codegen.domain.models.SimpleTestClassModel
import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.codegen.renderer.importUtilMethodDependencies
import org.utbot.framework.codegen.reports.TestsGenerationReport
import org.utbot.framework.codegen.services.CgNameGenerator
import org.utbot.framework.codegen.services.framework.TestFrameworkManager
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.util.description

abstract class CgAbstractTestClassConstructor<T : TestClassModel>(val context: CgContext):
    CgContextOwner by context,
    CgStatementConstructor by CgComponents.getStatementConstructorBy(context) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    init {
        CgComponents.clearContextRelatedStorage()
    }

    val testsGenerationReport = TestsGenerationReport()

    protected val methodConstructor: CgMethodConstructor = CgComponents.getMethodConstructorBy(context)
    protected val nameGenerator: CgNameGenerator = CgComponents.getNameGeneratorBy(context)
    protected val testFrameworkManager: TestFrameworkManager = CgComponents.getTestFrameworkManagerBy(context)

    /**
     * Constructs a file with the test class corresponding to [TestClassModel].
     */
    open fun construct(testClassModel: T): CgClassFile {
        return buildClassFile {
            this.declaredClass = withTestClassScope { constructTestClass(testClassModel) }
            imports += context.collectedImports
        }
    }

    /**
     * Constructs [CgClass] corresponding to [TestClassModel].
     */
    open fun constructTestClass(testClassModel: T): CgClass {
        return buildClass {
            id = currentTestClass

            if (currentTestClass != outerMostTestClass) {
                isNested = true
                isStatic = testFramework.nestedClassesShouldBeStatic
                testFrameworkManager.addAnnotationForNestedClasses()
            }

            body = constructTestClassBody(testClassModel)

            // It is important that annotations, superclass and interfaces assignment is run after
            // all methods are generated so that all necessary info is already present in the context
            with (currentTestClassContext) {
                annotations += collectedTestClassAnnotations
                superclass = testClassSuperclass
                interfaces += collectedTestClassInterfaces
            }
        }
    }

    abstract fun constructTestClassBody(testClassModel: T): CgClassBody

    abstract fun constructTestSet(testSet: CgMethodTestSet): List<CgRegion<CgMethod>>?

    protected fun createTest(
        testSet: CgMethodTestSet,
        regions: MutableList<CgRegion<CgMethod>>
    ) {
        val (_, _, clustersInfo) = testSet

        for ((clusterSummary, executionIndices) in clustersInfo) {
            val currentTestCaseTestMethods = mutableListOf<CgTestMethod>()
            emptyLineIfNeeded()
            val (checkedRange, needLimitExceedingComments) = if (executionIndices.last  - executionIndices.first >= UtSettings.maxTestsPerMethodInRegion) {
                IntRange(executionIndices.first, executionIndices.first + (UtSettings.maxTestsPerMethodInRegion - 1).coerceAtLeast(0)) to true
            } else {
                executionIndices to false
            }

            for (i in checkedRange) {
                withExecutionIdScope(i) {
                    currentTestCaseTestMethods += methodConstructor.createTestMethod(testSet, testSet.executions[i])
                }
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

    protected fun processFailure(testSet: CgMethodTestSet, failure: Throwable) {
        logger.warn(failure) { "Code generation error" }
        codeGenerationErrors
            .getOrPut(testSet) { mutableMapOf() }
            .merge(failure.description, 1, Int::plus)
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
    protected fun collectUtilEntities(): List<CgUtilEntity> {
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
     * Engine errors + codegen errors for a given [UtMethodTestSet]
     */
    protected val CgMethodTestSet.allErrors: Map<String, Int>
        get() = errors + codeGenerationErrors.getOrDefault(this, mapOf())

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
}