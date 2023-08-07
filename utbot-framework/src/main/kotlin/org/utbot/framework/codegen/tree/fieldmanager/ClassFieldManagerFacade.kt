package org.utbot.framework.codegen.tree.fieldmanager

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.plugin.api.UtModel

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