package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.UtModelWrapper
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtSpringContextModel
import org.utbot.framework.plugin.api.util.stringClassId

class CgSpringVariableConstructor(context: CgContext) : CgVariableConstructor(context) {
    val annotatedModelGroups: MutableMap<ClassId, MutableSet<UtModelWrapper>> = mutableMapOf()

    private val fieldManagerFacade = ClassFieldManagerFacade(context)

    override fun getOrCreateVariable(model: UtModel, name: String?): CgValue {
        val variable = fieldManagerFacade.constructVariableForField(model)

        variable?.let { return it }

        return when (model) {
            is UtSpringContextModel -> createApplicationContextVariable()
            else -> super.getOrCreateVariable(model, name)
        }
    }

    private fun createApplicationContextVariable(): CgValue {
        // This is a kind of HACK
        // Actually, applicationContext variable is useless as it is not used in the generated code.
        // However, this variable existence is required for autowired fields creation process.
        val applicationContextVariable = CgLiteral(stringClassId, "applicationContext")

        return applicationContextVariable.also {
            valueByUtModelWrapper[UtSpringContextModel.wrap()] = it
        }
    }
}