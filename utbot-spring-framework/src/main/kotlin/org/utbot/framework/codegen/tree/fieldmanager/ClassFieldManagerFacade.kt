package org.utbot.framework.codegen.tree.fieldmanager

import org.utbot.common.getOrPut
import org.utbot.framework.codegen.domain.UtModelWrapper
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.context.CgContextProperty
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtSpringContextModel

object RelevantFieldManagersProperty : CgContextProperty<MutableList<CgAbstractClassFieldManager>>

/**
 * Managers to process annotated fields of the class under test
 * relevant for the current generation type.
 */
val CgContextOwner.relevantFieldManagers: MutableList<CgAbstractClassFieldManager>
    get() = properties.getOrPut(RelevantFieldManagersProperty) { mutableListOf() }

class ClassFieldManagerFacade(context: CgContext) : CgContextOwner by context {

    private val alreadyInitializedModels = mutableSetOf<UtModelWrapper>()

    fun clearAlreadyInitializedModels() {
        alreadyInitializedModels.clear()
    }

    fun constructVariableForField(model: UtModel): CgValue? {
        relevantFieldManagers.forEach { manager ->
            val alreadyCreatedVariable = manager.findCgValueByModel(model, manager.annotatedModels)
            if (alreadyCreatedVariable != null) {
                if (alreadyInitializedModels.add(model.wrap()))
                    manager.useVariableForModel(model, alreadyCreatedVariable)
                return alreadyCreatedVariable
            }
        }

        return null
    }

    fun findTrustedModels(): List<UtModelWrapper> {
        val trustedModels = mutableListOf(UtSpringContextModel.wrap())

         relevantFieldManagers.forEach { manager ->
            trustedModels += manager.annotatedModels
        }

        return trustedModels
    }
}