package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.builtin.TestClassUtilMethodProvider
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.AnnotationTarget.*
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
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtSpringContextModel
import org.utbot.framework.plugin.api.util.SpringModelUtils.getBeanNameOrNull
import org.utbot.framework.plugin.api.util.id
import java.lang.Exception
import java.util.Collections.max

abstract class CgAbstractSpringTestClassConstructor(context: CgContext) :
    CgAbstractTestClassConstructor<SpringTestClassModel>(context) {

    protected val variableConstructor: CgSpringVariableConstructor =
        CgComponents.getVariableConstructorBy(context) as CgSpringVariableConstructor

    override fun constructTestClassBody(testClassModel: SpringTestClassModel): CgClassBody {
        return buildClassBody(currentTestClass) {

            // TODO: support inner classes here

            fields += constructClassFields(testClassModel)
            clearUnwantedVariableModels()

            constructAdditionalTestMethods()?.let { methodRegions += it }

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

            constructAdditionalUtilMethods()?.let { methodRegions += it }

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

    /**
     * Here "additional" means that these tests are not obtained from
     * [UtExecution]s generated by engine or fuzzer, but have another origin.
     */
    open fun constructAdditionalTestMethods(): CgMethodsCluster? = null

    open fun constructAdditionalUtilMethods(): CgMethodsCluster? = null

    protected fun constructFieldsWithAnnotation(
        fieldManager: CgClassFieldManager,
        groupedModelsByClassId: TypedModelWrappers,
    ): List<CgFieldDeclaration> {
        val annotationClassId = fieldManager.annotationType
        val annotation = addAnnotation(annotationClassId, Field)

        val constructedDeclarations = mutableListOf<CgFieldDeclaration>()
        for ((classId, modelWrappers) in groupedModelsByClassId) {

            val fieldWithAnnotationIsRequired = fieldManager.fieldWithAnnotationIsRequired(modelWrappers)
            if (!fieldWithAnnotationIsRequired) {
                continue
            }

            val modelWrapper = modelWrappers.firstOrNull() ?: continue
            val model = modelWrapper.model

            val baseVarName = model.getBeanNameOrNull()

            val createdVariable = variableConstructor.getOrCreateVariable(model, baseVarName) as? CgVariable
                ?: error("`UtCompositeModel` model was expected, but $model was found")

            val declaration = CgDeclaration(classId, variableName = createdVariable.name, initializer = null)

            constructedDeclarations += CgFieldDeclaration(
                ownerClassId = currentTestClass,
                declaration,
                annotation
            )

            modelWrappers
                .forEach { modelWrapper ->
                    modelWrapper.let {
                        valueByUtModelWrapper[modelWrapper] = createdVariable
                        variableConstructor.annotatedModelGroups.getOrPut(annotationClassId) { mutableSetOf() } += modelWrapper
                    }
                }
        }

        return constructedDeclarations
    }

    /**
     * Clears the results of variable instantiations that occurred
     * when we create class variables with specific annotations.
     * Actually, only mentioned variables should be stored in `valueByModelId`.
     *
     * This is a kind of HACK.
     * It is better to distinguish creating variable by model with all
     * related side effects and just creating a variable definition,
     * but it will take very long time to do it now.
     */
    private fun clearUnwantedVariableModels() {
        val trustedListOfModels =
            variableConstructor.annotatedModelGroups.values.flatten() + listOf(UtSpringContextModel.wrap())

        valueByUtModelWrapper
            .filterNot { it.key in trustedListOfModels }
            .forEach { valueByUtModelWrapper.remove(it.key) }
    }

    protected fun constructBeforeMethod(statements: List<CgStatement>): CgFrameworkUtilMethod {
        val beforeAnnotation = addAnnotation(context.testFramework.beforeMethodId, Method)
        return CgFrameworkUtilMethod(
            name = "setUp",
            statements = statements,
            exceptions = emptySet(),
            annotations = listOf(beforeAnnotation),
        )
    }

    protected fun constructAfterMethod(statements: List<CgStatement>): CgFrameworkUtilMethod {
        val afterAnnotation = addAnnotation(context.testFramework.afterMethodId, Method)
        return CgFrameworkUtilMethod(
            name = "tearDown",
            statements = statements,
            exceptions = setOf(Exception::class.id),
            annotations = listOf(afterAnnotation),
        )
    }
}