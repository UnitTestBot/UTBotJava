package org.utbot.framework.codegen.services.framework

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.plugin.api.UtAssembleModel

class SpyFrameworkManager(context: CgContext) : CgVariableConstructorComponent(context) {

    fun spyForVariable(model: UtAssembleModel, variable: CgVariable): CgVariable{
        variableConstructor.constructAssembleForVariable(model)
        return variable
    }

}