package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.tree.fieldmanager.ClassFieldManagerFacade
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtSpringContextModel
import org.utbot.framework.plugin.api.UtSpringEntityManagerModel
import org.utbot.framework.plugin.api.util.stringClassId

class CgSpringVariableConstructor(context: CgContext) : CgVariableConstructor(context) {

    private val fieldManagerFacade = ClassFieldManagerFacade(context)

    override fun getOrCreateVariable(model: UtModel, name: String?): CgValue {
        val variable = fieldManagerFacade.constructVariableForField(model)

        variable?.let { return it }

        return when (model) {
            is UtSpringContextModel, is UtSpringEntityManagerModel -> createDummyVariable(model)
            else -> super.getOrCreateVariable(model, name)
        }
    }

    private fun createDummyVariable(model: UtModel): CgVariable {
        // This is a kind of HACK
        // Actually, this value is not supposed to be used in the generated code.
        // However, this value existence is required for fields declaration process.
        val dummyVariable = newVar(model.classId) {
            CgLiteral(stringClassId, "dummy")
        }

        valueByUtModelWrapper[model.wrap()] = dummyVariable

        return dummyVariable
    }
}