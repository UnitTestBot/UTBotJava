package org.utbot.framework.codegen.tree.fieldmanager

import org.utbot.framework.codegen.domain.UtModelWrapper
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtSpringContextModel

class ClassFieldManagerFacade(context: CgContext) : CgContextOwner by context {

    private val injectingMocksFieldsManager = CgInjectingMocksFieldsManager(context)
    private val mockedFieldsManager = CgMockedFieldsManager(context)
    private val spiedFieldsManager = CgSpiedFieldsManager(context)
    private val autowiredFieldsManager = CgAutowiredFieldsManager(context)
    private val persistenceContextFieldsManager = CgPersistenceContextFieldsManager.createIfPossible(context)

    private val annotationManagers = listOfNotNull(
        injectingMocksFieldsManager,
        mockedFieldsManager,
        spiedFieldsManager,
        autowiredFieldsManager,
        persistenceContextFieldsManager,
    )

    fun constructVariableForField(model: UtModel): CgValue? {
        annotationManagers.forEach { manager ->
            val managedModels = manager.annotatedModelGroups[manager.annotationType]

            val alreadyCreatedVariable = manager.findCgValueByModel(model, managedModels)
            if (alreadyCreatedVariable != null) {
                manager.constructFieldsForVariable(model, alreadyCreatedVariable)
                return alreadyCreatedVariable
            }
        }

        return null
    }

    fun findTrustedModels(): List<UtModelWrapper> {
        val trustedModels = mutableListOf<UtModelWrapper>()
         annotationManagers.forEach { manager ->
            val managedModels = manager.annotatedModelGroups[manager.annotationType]
            trustedModels += managedModels ?: emptyList()
        }

        trustedModels += listOf(UtSpringContextModel.wrap())

        return trustedModels
    }
}