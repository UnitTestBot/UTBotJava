package framework.codegen.model.constructor.tree

import framework.api.js.JsPrimitiveModel
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.tree.CgComponents
import org.utbot.framework.codegen.tree.CgVariableConstructor
import org.utbot.framework.codegen.util.nullLiteral
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtReferenceModel

class JsCgVariableConstructor(ctx: CgContext) : CgVariableConstructor(ctx) {

    private val nameGenerator = CgComponents.getNameGeneratorBy(ctx)

    override fun getOrCreateVariable(model: UtModel, name: String?): CgValue {
        val baseName = name ?: nameGenerator.nameFrom(model.classId)
        return if (model is UtReferenceModel) valueByModelId.getOrPut(model.id) {
            // TODO SEVERE: May lead to unexpected behavior in case of changes to the original method
            super.getOrCreateVariable(model, name)
        } else valueByModel.getOrPut(model) {
            when (model) {
                is JsPrimitiveModel -> CgLiteral(model.classId, model.value)
                else -> nullLiteral()
            }
        }
    }
}
