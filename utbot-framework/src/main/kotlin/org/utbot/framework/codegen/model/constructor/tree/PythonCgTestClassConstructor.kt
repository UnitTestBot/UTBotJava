package org.utbot.framework.codegen.model.constructor.tree;

import org.utbot.framework.codegen.*
import org.utbot.framework.codegen.model.constructor.CgMethodTestSet
import org.utbot.framework.codegen.model.constructor.TestClassModel
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.util.importIfNeeded
import org.utbot.framework.codegen.model.tree.*
import org.utbot.framework.plugin.api.PythonClassId

internal class PythonCgTestClassConstructor(context: CgContext) : CgTestClassConstructor(context) {
    override fun construct(testClassModel: TestClassModel): CgTestClassFile {
        return buildTestClassFile {
            this.testClass = withTestClassScope { constructTestClass(testClassModel) }
            imports.addAll(context.collectedImports)
            existingVariableNames = existingVariableNames.addAll(context.collectedImports.map { it.qualifiedName })
            testsGenerationReport = this@PythonCgTestClassConstructor.testsGenerationReport
        }
    }

    override fun constructTestClass(testClassModel: TestClassModel): CgTestClass {
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

                val additionalMethods = currentTestClassContext.cgDataProviderMethods

                dataProvidersAndUtilMethodsRegion += CgStaticsRegion(
                    "Data providers and utils methods",
                    additionalMethods
                )
            }
            // It is important that annotations, superclass and interfaces assignment is run after
            // all methods are generated so that all necessary info is already present in the context
            with (currentTestClassContext) {
                annotations += collectedTestClassAnnotations
                superclass = testFramework.testSuperClass
                interfaces += collectedTestClassInterfaces
            }
        }
    }

    override fun constructTestSet(testSet: CgMethodTestSet): List<CgRegion<CgMethod>>? {
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
            ParametrizedTestSource.PARAMETRIZE -> {}
        }

        val errors = testSet.allErrors
        if (errors.isNotEmpty()) {
            regions += methodConstructor.errorMethod(testSet.executableId, errors)
            testsGenerationReport.addMethodErrors(testSet, errors)
        }

        return regions
    }
}
