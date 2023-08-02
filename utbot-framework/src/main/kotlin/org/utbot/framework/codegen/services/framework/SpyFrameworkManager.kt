package org.utbot.framework.codegen.services.framework

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.plugin.api.UtAssembleModel

class SpyFrameworkManager(context: CgContext) : CgVariableConstructorComponent(context) {

    fun spyForVariable(model: UtAssembleModel){
        variableConstructor.constructAssembleForVariable(model)
    }

}