package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.UtModelWrapper
import org.utbot.framework.codegen.domain.builtin.injectMocksClassId
import org.utbot.framework.codegen.domain.builtin.mockClassId
import org.utbot.framework.codegen.domain.builtin.spyClassId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.services.framework.SpyFrameworkManager
import org.utbot.framework.codegen.tree.MockitoInjectionUtils.canBeInjectedByTypeInto
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtModelWithCompositeOrigin
import org.utbot.framework.plugin.api.UtSpringEntityManagerModel
import org.utbot.framework.plugin.api.isMockModel
import org.utbot.framework.plugin.api.canBeSpied
import org.utbot.framework.plugin.api.util.SpringModelUtils.autowiredClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.getBeanNameOrNull
import org.utbot.framework.plugin.api.util.SpringModelUtils.isAutowiredFromContext
import org.utbot.framework.plugin.api.util.SpringModelUtils.persistenceContextClassIds
import org.utbot.framework.plugin.api.util.allDeclaredFieldIds
import org.utbot.framework.plugin.api.util.isSubtypeOf

interface CgClassFieldManager : CgContextOwner {

    val annotationType: ClassId

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

    private val nameGenerator = CgComponents.getNameGeneratorBy(context)

    fun findCgValueByModel(model: UtModel, setOfModels: Set<UtModelWrapper>?): CgValue? {
        val key = setOfModels?.find { it == model.wrap() } ?: return null
        return valueByUtModelWrapper[key]
    }

    override fun constructBaseVarName(model: UtModel): String? = nameGenerator.nameFrom(model.classId)
}

class CgInjectingMocksFieldsManager(val context: CgContext) : CgAbstractClassFieldManager(context) {

    override val annotationType = injectMocksClassId

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

    override fun constructFieldsForVariable(model: UtModel, variable: CgValue) {
        return when(model) {
            is UtSpringEntityManagerModel -> {}
            else -> error("Trying to use @PersistenceContext for model $model but it is not appropriate")
        }
    }

    override fun fieldWithAnnotationIsRequired(classId: ClassId): Boolean = true
}

class ClassFieldManagerFacade(context: CgContext) : CgContextOwner by context {

    private val injectingMocksFieldsManager = CgInjectingMocksFieldsManager(context)
    private val mockedFieldsManager = CgMockedFieldsManager(context)
    private val spiedFieldsManager = CgSpiedFieldsManager(context)
    private val autowiredFieldsManager = CgAutowiredFieldsManager(context)
    private val persistenceContextFieldsManager = CgPersistenceContextFieldsManager.createIfPossible(context)

    fun constructVariableForField(
        model: UtModel,
    ): CgValue? {
        val annotationManagers = listOfNotNull(
            injectingMocksFieldsManager,
            mockedFieldsManager,
            spiedFieldsManager,
            autowiredFieldsManager,
            persistenceContextFieldsManager,
        )

        annotationManagers.forEach { manager ->
            val annotatedModelGroups = manager.variableConstructor.annotatedModelGroups

            val alreadyCreatedVariable = manager.findCgValueByModel(model, annotatedModelGroups[manager.annotationType])

            if (alreadyCreatedVariable != null) {
                manager.constructFieldsForVariable(model, alreadyCreatedVariable)
                return alreadyCreatedVariable
            }
        }

        return null
    }
}

object MockitoInjectionUtils {
    /*
     * If count of fields of the same type is 1, then we mock/spy variable by @Mock/@Spy annotation,
     * otherwise we will create this variable by simple variable constructor.
     */
    fun ClassId.canBeInjectedByTypeInto(classToInjectInto: ClassId): Boolean =
        classToInjectInto.allDeclaredFieldIds.filter { isSubtypeOf(it.type) }.toList().size == 1
}