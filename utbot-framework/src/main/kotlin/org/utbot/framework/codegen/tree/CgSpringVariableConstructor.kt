package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.UtModelWrapper
import org.utbot.framework.codegen.domain.builtin.injectMocksClassId
import org.utbot.framework.codegen.domain.builtin.mockClassId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtSpringContextModel
import org.utbot.framework.plugin.api.isMockModel
import org.utbot.framework.plugin.api.util.SpringModelUtils.autowiredClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.isAutowiredFromContext
import org.utbot.framework.plugin.api.util.stringClassId

class CgSpringVariableConstructor(context: CgContext) : CgVariableConstructor(context) {
    val annotatedModelVariables: MutableMap<ClassId, MutableSet<UtModelWrapper>> = mutableMapOf()

    private val fieldManagerFacade = FieldManagerFacade(context, annotatedModelVariables)

    override fun getOrCreateVariable(model: UtModel, name: String?): CgValue {
        val variable = fieldManagerFacade.constructVariableForField(model)

        if(variable != null){
            return variable
        }

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