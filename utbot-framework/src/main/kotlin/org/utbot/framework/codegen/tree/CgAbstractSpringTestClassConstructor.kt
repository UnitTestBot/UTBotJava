package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.builtin.TestClassUtilMethodProvider
import org.utbot.framework.codegen.domain.builtin.injectMocksClassId
import org.utbot.framework.codegen.domain.builtin.mockClassId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgClassBody
import org.utbot.framework.codegen.domain.models.CgDeclaration
import org.utbot.framework.codegen.domain.models.CgFieldDeclaration
import org.utbot.framework.codegen.domain.models.CgFrameworkUtilMethod
import org.utbot.framework.codegen.domain.models.CgMethod
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.CgMethodsCluster
import org.utbot.framework.codegen.domain.models.CgRegion
import org.utbot.framework.codegen.domain.models.CgStatement
import org.utbot.framework.codegen.domain.models.CgStaticsRegion
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.domain.models.SpringTestClassModel
import org.utbot.framework.codegen.domain.models.builders.TypedModelWrappers
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.id
import java.lang.Exception

abstract class CgAbstractSpringTestClassConstructor(context: CgContext):
    CgAbstractTestClassConstructor<SpringTestClassModel>(context) {

    protected val variableConstructor: CgSpringVariableConstructor =
        CgComponents.getVariableConstructorBy(context) as CgSpringVariableConstructor
    protected val statementConstructor: CgStatementConstructor = CgComponents.getStatementConstructorBy(context)

    override fun constructTestClassBody(testClassModel: SpringTestClassModel): CgClassBody {
        return buildClassBody(currentTestClass) {

            // TODO: support inner classes here

            fields += constructClassFields(testClassModel)
            clearUnwantedVariableModels()

            methodRegions += constructAdditionalMethods()

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

    abstract fun constructClassFields(testClassModel: SpringTestClassModel): List<CgFieldDeclaration>

    abstract fun constructAdditionalMethods(): CgMethodsCluster

    protected fun constructFieldsWithAnnotation(
        groupedModelsByClassId: TypedModelWrappers,
        annotationClassId: ClassId
    ): List<CgFieldDeclaration> {
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
    protected fun clearUnwantedVariableModels() {
        val whiteListOfModels =
            listOf(
                variableConstructor.mockedModelsVariables,
                variableConstructor.injectedMocksModelsVariables
            ).flatten()

        valueByUtModelWrapper
            .filter { it.key !in whiteListOfModels }
            .forEach { valueByUtModelWrapper.remove(it.key) }
    }

    protected fun constructBeforeMethod(statements: List<CgStatement>) = CgFrameworkUtilMethod(
        name = "setUp",
        statements = statements,
        exceptions = emptySet(),
        annotations = listOf(statementConstructor.annotation(context.testFramework.beforeMethodId)),
    )

    protected fun constructAfterMethod(statements: List<CgStatement>) = CgFrameworkUtilMethod(
        name = "tearDown",
        statements = statements,
        exceptions = setOf(Exception::class.id),
        annotations = listOf(statementConstructor.annotation(context.testFramework.afterMethodId)),
    )
}