package org.utbot.framework.codegen.tree.fieldmanager

import org.utbot.framework.codegen.domain.UtModelWrapper
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtSpringContextModel

class ClassFieldManagerFacade(context: CgContext) : CgContextOwner by context {

    fun constructVariableForField(model: UtModel): CgValue? {
        relevantFieldManagers.forEach { manager ->
            val managedModels = manager.getManagedModels()

            val alreadyCreatedVariable = manager.findCgValueByModel(model, managedModels)
            if (alreadyCreatedVariable != null) {
                manager.useVariableForModel(model, alreadyCreatedVariable)
                return alreadyCreatedVariable
            }
        }

        return null
    }

    fun findTrustedModels(): List<UtModelWrapper> {
        val trustedModels = mutableListOf(UtSpringContextModel.wrap())

         relevantFieldManagers.forEach { manager ->
            trustedModels += manager.getManagedModels()
        }

        return trustedModels
    }
}