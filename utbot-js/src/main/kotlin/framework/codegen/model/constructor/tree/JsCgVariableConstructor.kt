package framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.tree.CgTestClassConstructor
import org.utbot.framework.codegen.model.constructor.tree.CgVariableConstructor
import org.utbot.framework.codegen.model.tree.CgLiteral
import org.utbot.framework.codegen.model.tree.CgValue
import org.utbot.framework.codegen.model.util.nullLiteral
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.js.JsPrimitiveModel

class JsCgVariableConstructor(ctx: CgContext) : CgVariableConstructor(ctx) {

    private val nameGenerator = CgTestClassConstructor.CgComponents.getNameGeneratorBy(ctx)

    override fun getOrCreateVariable(model: UtModel, name: String?): CgValue {
        val baseName = name ?: nameGenerator.nameFrom(model.classId)
        return if (model is UtReferenceModel) valueByModelId.getOrPut(model.id) {
            when (model) {
                is UtCompositeModel -> TODO()
                is UtAssembleModel -> constructAssemble(model, baseName)
                is UtArrayModel -> TODO()
                else -> TODO()
            }
        } else valueByModel.getOrPut(model) {
            when (model) {
                is JsPrimitiveModel -> CgLiteral(model.classId, model.value)
                else -> nullLiteral()
            }
        }
    }
}