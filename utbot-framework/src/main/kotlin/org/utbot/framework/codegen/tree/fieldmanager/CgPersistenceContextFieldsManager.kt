package org.utbot.framework.codegen.tree.fieldmanager

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgFieldDeclaration
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtSpringEntityManagerModel
import org.utbot.framework.plugin.api.util.SpringModelUtils

class CgPersistenceContextFieldsManager private constructor(
    context: CgContext,
    override val annotationType: ClassId,
) : CgAbstractClassFieldManager(context) {

    init {
        relevantFieldManagers += this
    }

    companion object {
        fun createIfPossible(context: CgContext): CgPersistenceContextFieldsManager? = SpringModelUtils.persistenceContextClassIds.firstOrNull()
            ?.let { persistenceContextClassId -> CgPersistenceContextFieldsManager(context, persistenceContextClassId) }
    }

    override fun createFieldDeclarations(testClassModel: TestClassModel): List<CgFieldDeclaration> {
        val modelsByOrigin = modelGroupsProvider.collectModelsByOrigin(testClassModel)
        val entityManagerModels = modelsByOrigin
            .stateBeforeDependentModels
            .filterTo(HashSet()) { it.model is UtSpringEntityManagerModel }

        return constructFieldsWithAnnotation(entityManagerModels)
    }

    override fun useVariableForModel(model: UtModel, variable: CgValue) {
        return when(model) {
            is UtSpringEntityManagerModel -> {}
            else -> error("Trying to use @PersistenceContext for model $model but it is not appropriate")
        }
    }

    override fun fieldWithAnnotationIsRequired(classId: ClassId): Boolean = true
}