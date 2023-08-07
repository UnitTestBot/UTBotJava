package org.utbot.framework.codegen.tree.fieldmanager

import org.utbot.framework.codegen.domain.UtModelWrapper
import org.utbot.framework.codegen.domain.builtin.injectMocksClassId
import org.utbot.framework.codegen.domain.builtin.mockClassId
import org.utbot.framework.codegen.domain.builtin.spyClassId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.AnnotationTarget
import org.utbot.framework.codegen.domain.models.CgDeclaration
import org.utbot.framework.codegen.domain.models.CgFieldDeclaration
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.codegen.domain.models.builders.TypedModelWrappers
import org.utbot.framework.codegen.services.framework.SpyFrameworkManager
import org.utbot.framework.codegen.tree.CgComponents
import org.utbot.framework.codegen.tree.CgSpringVariableConstructor
import org.utbot.framework.codegen.tree.fieldmanager.MockitoInjectionUtils.canBeInjectedByTypeInto
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtModelWithCompositeOrigin
import org.utbot.framework.plugin.api.UtSpringEntityManagerModel
import org.utbot.framework.plugin.api.canBeSpied
import org.utbot.framework.plugin.api.isMockModel
import org.utbot.framework.plugin.api.util.SpringModelUtils.autowiredClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.getBeanNameOrNull
import org.utbot.framework.plugin.api.util.SpringModelUtils.isAutowiredFromContext
import org.utbot.framework.plugin.api.util.SpringModelUtils.persistenceContextClassIds

interface CgClassFieldManager : CgContextOwner {

    val annotationType: ClassId

    fun createFieldDeclarations(testClassModel: TestClassModel): List<CgFieldDeclaration>

    fun constructFieldsForVariable(model: UtModel, variable: CgValue)

    fun fieldWithAnnotationIsRequired(classId: ClassId): Boolean

    fun constructBaseVarName(model: UtModel): String?
}

abstract class CgAbstractClassFieldManager(context: CgContext) :
    CgClassFieldManager,
    CgContextOwner by context {

    val variableConstructor: CgSpringVariableConstructor by lazy {
        CgComponents.getVariableConstructorBy(context) as CgSpringVariableConstructor
    }

    private val statementConstructor = CgComponents.getStatementConstructorBy(context)

    protected val fieldManagerUtils = FieldManagerUtils(context)

    fun findCgValueByModel(model: UtModel, setOfModels: Set<UtModelWrapper>?): CgValue? {
        val key = setOfModels?.find { it == model.wrap() } ?: return null
        return valueByUtModelWrapper[key]
    }

    protected fun constructFieldsWithAnnotation(modelWrappers: Set<UtModelWrapper>): List<CgFieldDeclaration> {
        val groupedModelsByClassId = modelWrappers.groupByClassId()
        val annotation = statementConstructor.addAnnotation(annotationType, AnnotationTarget.Field)

        val constructedDeclarations = mutableListOf<CgFieldDeclaration>()
        for ((classId, modelWrappers) in groupedModelsByClassId) {

            val modelWrapper = modelWrappers.firstOrNull() ?: continue
            val model = modelWrapper.model

            val fieldWithAnnotationIsRequired = fieldWithAnnotationIsRequired(model.classId)
            if (!fieldWithAnnotationIsRequired) {
                continue
            }

            val baseVarName = constructBaseVarName(model)

            val createdVariable = variableConstructor.getOrCreateVariable(model, baseVarName) as? CgVariable
                ?: error("`CgVariable` cannot be constructed from a $model model")

            val declaration = CgDeclaration(classId, variableName = createdVariable.name, initializer = null)

            constructedDeclarations += CgFieldDeclaration(
                ownerClassId = currentTestClass,
                declaration,
                annotation
            )

            modelWrappers
                .forEach { modelWrapper ->

                    valueByUtModelWrapper[modelWrapper] = createdVariable

                    variableConstructor.annotatedModelGroups
                        .getOrPut(annotationType) { mutableSetOf() } += modelWrapper
                }
        }

        return constructedDeclarations
    }

    private fun Set<UtModelWrapper>.groupByClassId(): TypedModelWrappers {
        val classModels = mutableMapOf<ClassId, Set<UtModelWrapper>>()

        for (modelGroup in this.groupBy { it.model.classId }) {
            classModels[modelGroup.key] = modelGroup.value.toSet()
        }

        return classModels
    }

    override fun constructBaseVarName(model: UtModel): String? = nameGenerator.nameFrom(model.classId)

    private val nameGenerator = CgComponents.getNameGeneratorBy(context)
}

class CgInjectingMocksFieldsManager(val context: CgContext) : CgAbstractClassFieldManager(context) {

    override val annotationType = injectMocksClassId

    override fun createFieldDeclarations(testClassModel: TestClassModel): List<CgFieldDeclaration> {
        val modelsByOrigin = fieldManagerUtils.collectModelsByOrigin(testClassModel)
        return constructFieldsWithAnnotation(modelsByOrigin.thisInstanceModels)
    }

    override fun constructFieldsForVariable(model: UtModel, variable: CgValue) {
        val modelFields = when (model) {
            is UtCompositeModel -> model.fields
            is UtModelWithCompositeOrigin -> model.origin?.fields
            else -> null
        }

        modelFields?.forEach { (fieldId, fieldModel) ->
            // creating variables for modelVariable fields
            val variableForField = variableConstructor.getOrCreateVariable(fieldModel)

            // is variable mocked by @Mock annotation
            val isMocked = findCgValueByModel(fieldModel, variableConstructor.annotatedModelGroups[mockClassId]) != null

            // is variable spied by @Spy annotation
            val isSpied = findCgValueByModel(fieldModel, variableConstructor.annotatedModelGroups[spyClassId]) != null

            // If field model is a mock model and is mocked by @Mock annotation in classFields or is spied by @Spy annotation,
            // it is set in the connected with instance under test automatically via @InjectMocks.
            // Otherwise, we need to set this field manually.
            if ((!fieldModel.isMockModel() || !isMocked) && !isSpied) {
                variableConstructor.setFieldValue(variable, fieldId, variableForField)
            }
        }
    }

    override fun fieldWithAnnotationIsRequired(classId: ClassId): Boolean = true
}

class CgMockedFieldsManager(context: CgContext) : CgAbstractClassFieldManager(context) {

    private val mockFrameworkManager = CgComponents.getMockFrameworkManagerBy(context)

    override val annotationType = mockClassId
    override fun createFieldDeclarations(testClassModel: TestClassModel): List<CgFieldDeclaration> {
        val modelsByOrigin = fieldManagerUtils.collectModelsByOrigin(testClassModel)

        val dependentMockModels =
            modelsByOrigin.thisInstanceDependentModels
                .filterTo(mutableSetOf()) { cgModel ->
                    cgModel.model.isMockModel() && cgModel !in modelsByOrigin.thisInstanceModels
                }

        return constructFieldsWithAnnotation(dependentMockModels)
    }

    override fun constructFieldsForVariable(model: UtModel, variable: CgValue) {
        if (!model.isMockModel()) {
            error("$model does not represent a mock")
        }

        mockFrameworkManager.createMockForVariable(
            model as UtCompositeModel,
            variable as CgVariable,
        )

        for ((fieldId, fieldModel) in model.fields) {
            val variableForField = variableConstructor.getOrCreateVariable(fieldModel)
            variableConstructor.setFieldValue(variable, fieldId, variableForField)
        }
    }

    override fun fieldWithAnnotationIsRequired(classId: ClassId): Boolean =
        classId.canBeInjectedByTypeInto(classUnderTest)
}

class CgSpiedFieldsManager(context: CgContext) : CgAbstractClassFieldManager(context) {

    private val spyFrameworkManager = SpyFrameworkManager(context)

    override val annotationType = spyClassId

    override fun createFieldDeclarations(testClassModel: TestClassModel): List<CgFieldDeclaration> {
        val modelsByOrigin = fieldManagerUtils.collectModelsByOrigin(testClassModel)

        val dependentMockModels =
            modelsByOrigin.thisInstanceDependentModels
                .filterTo(mutableSetOf()) { cgModel ->
                    cgModel.model.isMockModel() && cgModel !in modelsByOrigin.thisInstanceModels
                }

        val dependentSpyModels =
            modelsByOrigin.thisInstanceDependentModels
                .filterTo(mutableSetOf()) { cgModel ->
                    cgModel.model.canBeSpied() &&
                            cgModel !in modelsByOrigin.thisInstanceModels &&
                            cgModel !in dependentMockModels
                }

        return constructFieldsWithAnnotation(dependentSpyModels)
    }

    override fun constructFieldsForVariable(model: UtModel, variable: CgValue) {
        if (!model.canBeSpied()) {
            error("$model does not represent a spy")
        }
        spyFrameworkManager.spyForVariable(
            model as UtAssembleModel,
        )
    }

    override fun fieldWithAnnotationIsRequired(classId: ClassId): Boolean =
        classId.canBeInjectedByTypeInto(classUnderTest)

    override fun constructBaseVarName(model: UtModel): String = super.constructBaseVarName(model) + "Spy"
}

class CgAutowiredFieldsManager(context: CgContext) : CgAbstractClassFieldManager(context) {

    override val annotationType = autowiredClassId

    override fun createFieldDeclarations(testClassModel: TestClassModel): List<CgFieldDeclaration> {
        val modelsByOrigin = fieldManagerUtils.collectModelsByOrigin(testClassModel)
        val autowiredFromContextModels =  modelsByOrigin
            .stateBeforeDependentModels
            .filterTo(HashSet()) { it.model.isAutowiredFromContext() }

        return constructFieldsWithAnnotation(autowiredFromContextModels)
    }

    override fun constructFieldsForVariable(model: UtModel, variable: CgValue) {
        when {
            model.isAutowiredFromContext() -> {
                variableConstructor.constructAssembleForVariable(model as UtAssembleModel)
            }

            else -> error("Trying to autowire model $model but it is not appropriate")
        }
    }

    override fun constructBaseVarName(model: UtModel): String? = model.getBeanNameOrNull()

    override fun fieldWithAnnotationIsRequired(classId: ClassId): Boolean = true
}

class CgPersistenceContextFieldsManager private constructor(
    context: CgContext,
    override val annotationType: ClassId,
) : CgAbstractClassFieldManager(context) {
    companion object {
        fun createIfPossible(context: CgContext): CgPersistenceContextFieldsManager? = persistenceContextClassIds.firstOrNull()
            ?.let { persistenceContextClassId -> CgPersistenceContextFieldsManager(context, persistenceContextClassId) }
    }

    override fun createFieldDeclarations(testClassModel: TestClassModel): List<CgFieldDeclaration> {
        val modelsByOrigin = fieldManagerUtils.collectModelsByOrigin(testClassModel)
        val entityManagerModels = modelsByOrigin
            .stateBeforeDependentModels
            .filterTo(HashSet()) { it.model is UtSpringEntityManagerModel }

        return constructFieldsWithAnnotation(entityManagerModels)
    }

    override fun constructFieldsForVariable(model: UtModel, variable: CgValue) {
        return when(model) {
            is UtSpringEntityManagerModel -> {}
            else -> error("Trying to use @PersistenceContext for model $model but it is not appropriate")
        }
    }

    override fun fieldWithAnnotationIsRequired(classId: ClassId): Boolean = true
}

