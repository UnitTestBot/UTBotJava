package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.UtModelWrapper
import org.utbot.framework.codegen.domain.builtin.TestClassUtilMethodProvider
import org.utbot.framework.codegen.domain.builtin.closeMethodId
import org.utbot.framework.codegen.domain.builtin.injectMocksClassId
import org.utbot.framework.codegen.domain.builtin.mockClassId
import org.utbot.framework.codegen.domain.builtin.openMocksMethodId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgAssignment
import org.utbot.framework.codegen.domain.models.CgClassBody
import org.utbot.framework.codegen.domain.models.CgDeclaration
import org.utbot.framework.codegen.domain.models.CgFieldDeclaration
import org.utbot.framework.codegen.domain.models.CgFrameworkUtilMethod
import org.utbot.framework.codegen.domain.models.CgMethod
import org.utbot.framework.codegen.domain.models.CgMethodCall
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.CgMethodsCluster
import org.utbot.framework.codegen.domain.models.CgRegion
import org.utbot.framework.codegen.domain.models.CgSimpleRegion
import org.utbot.framework.codegen.domain.models.CgStatementExecutableCall
import org.utbot.framework.codegen.domain.models.CgStaticsRegion
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.domain.models.SpringTestClassModel
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.objectClassId

class CgSpringTestClassConstructor(context: CgContext): CgAbstractTestClassConstructor<SpringTestClassModel>(context) {

    private val variableConstructor: CgSpringVariableConstructor =
        CgComponents.getVariableConstructorBy(context) as CgSpringVariableConstructor
    private val statementConstructor: CgStatementConstructor = CgComponents.getStatementConstructorBy(context)

    override fun constructTestClassBody(testClassModel: SpringTestClassModel): CgClassBody {
        return buildClassBody(currentTestClass) {

            // TODO: support inner classes here

            val mockedFields = constructClassFields(testClassModel.mockedModels, mockClassId)

            if (mockedFields.isNotEmpty()) {
                fields += constructClassFields(testClassModel.injectedMockModels, injectMocksClassId)
                fields += mockedFields

                clearUnwantedVariableModels()

                val (closeableField, closeableMethods) = constructMockitoCloseables()
                fields += closeableField
                methodRegions += closeableMethods
            }

            for ((testSetIndex, testSet) in testClassModel.methodTestSets.withIndex()) {
                updateCurrentExecutable(testSet.executableId)
                withTestSetIdScope(testSetIndex) {
                    val currentMethodUnderTestRegions = constructTestSet(testSet) ?: return@withTestSetIdScope
                    val executableUnderTestCluster = CgMethodsCluster(
                        "Test suites for executable $currentExecutable",
                        currentMethodUnderTestRegions
                    )
                    methodRegions += executableUnderTestCluster
                }
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
    }

    override fun constructTestSet(testSet: CgMethodTestSet): List<CgRegion<CgMethod>>? {
        val regions = mutableListOf<CgRegion<CgMethod>>()

        if (testSet.executions.any()) {
            runCatching {
                createTest(testSet, regions)
            }.onFailure { e -> processFailure(testSet, e) }
        }

        val errors = testSet.allErrors
        if (errors.isNotEmpty()) {
            regions += methodConstructor.errorMethod(testSet.executableId, errors)
            testsGenerationReport.addMethodErrors(testSet, errors)
        }

        return if (regions.any()) regions else null
    }

    private fun constructClassFields(
        groupedModelsByClassId: Map<ClassId, Set<UtModelWrapper>>,
        annotationClassId: ClassId
    ): MutableList<CgFieldDeclaration> {
        require(annotationClassId == injectMocksClassId || annotationClassId == mockClassId) {
            error("Unexpected annotation classId -- $annotationClassId")
        }

        val annotation = statementConstructor.annotation(annotationClassId)

        val constructedDeclarations = mutableListOf<CgFieldDeclaration>()
        for ((classId, listOfUtModels) in groupedModelsByClassId) {
            val model = listOfUtModels.firstOrNull() ?: continue
            val createdVariable = variableConstructor.getOrCreateVariable(model.model) as? CgVariable
                ?: error("`UtCompositeModel` model was expected, but $model was found")

            val declaration = CgDeclaration(classId, variableName = createdVariable.name, initializer = null)
            constructedDeclarations += CgFieldDeclaration(ownerClassId = currentTestClass, declaration, annotation)

            listOfUtModels.forEach { key ->
                valueByUtModelWrapper[key] = createdVariable
            }

            when (annotationClassId) {
                injectMocksClassId -> variableConstructor.injectedMocksModelsVariables += listOfUtModels
                mockClassId -> variableConstructor.mockedModelsVariables += listOfUtModels
            }
        }

        return constructedDeclarations
    }

    /**
     * Clears the results of variable instantiations that occured
     * when we create class variables with specific annotations.
     * Actually, only mentioned variables should be stored in `valueByModelId`.
     *
     * This is a kind of HACK.
     * It is better to distinguish creating variable by model with all
     * related side effects and just creating a variable definition,
     * but it will take very long time to do it now.
     */
    private fun clearUnwantedVariableModels() {
        val whiteListOfModels =
            listOf(
                variableConstructor.mockedModelsVariables,
                variableConstructor.injectedMocksModelsVariables
            ).flatten()

        valueByUtModelWrapper
            .filter { it.key !in whiteListOfModels }
            .forEach { valueByUtModelWrapper.remove(it.key) }
    }

    private fun constructMockitoCloseables(): Pair<CgFieldDeclaration, CgMethodsCluster> {
        val mockitoCloseableVarName = "mockitoCloseable"
        val mockitoCloseableVarType = java.lang.AutoCloseable::class.id

        val mockitoCloseableModel = UtCompositeModel(
            id = null,
            classId = mockitoCloseableVarType,
            isMock = false,
        )

        val mockitoCloseableVariable =
            variableConstructor.getOrCreateVariable(mockitoCloseableModel, mockitoCloseableVarName)
        val mockitoCloseableDeclaration = CgDeclaration(mockitoCloseableVarType, mockitoCloseableVarName, initializer = null)
        val mockitoCloseableFieldDeclaration = CgFieldDeclaration(ownerClassId = currentTestClass, mockitoCloseableDeclaration)

        importIfNeeded(openMocksMethodId)

        val openMocksCall = CgMethodCall(
            caller = null,
            executableId = openMocksMethodId,
            //TODO: this is a hack of this
            arguments = listOf(CgVariable("this", objectClassId))
        )

        val closeCall = CgMethodCall(
            caller = mockitoCloseableVariable,
            executableId = closeMethodId,
            arguments = emptyList(),
        )

        val openMocksStatement = CgAssignment(mockitoCloseableVariable, openMocksCall)
        val beforeMethod = CgFrameworkUtilMethod(
            name = "setUp",
            statements = listOf(openMocksStatement),
            exceptions = emptySet(),
            annotations = listOf(statementConstructor.annotation(context.testFramework.beforeMethodId)),
        )

        val closeStatement = CgStatementExecutableCall(closeCall)
        val afterMethod = CgFrameworkUtilMethod(
            name = "tearDown",
            statements = listOf(closeStatement),
            exceptions = setOf(java.lang.Exception::class.id),
            annotations = listOf(statementConstructor.annotation(context.testFramework.afterMethodId)),
        )

        val methodCluster = CgMethodsCluster(
            header = null,
            listOf(CgSimpleRegion(header = null, listOf(beforeMethod, afterMethod)))
        )

        return mockitoCloseableFieldDeclaration to methodCluster
    }
}
