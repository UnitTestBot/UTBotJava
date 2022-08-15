package org.utbot.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.util.CgComponents
import org.utbot.framework.codegen.model.tree.*
import org.utbot.framework.plugin.api.*

class PythonCgVariableConstructor(context_: CgContext) : CgVariableConstructor(context_) {
    private val nameGenerator = CgComponents.getNameGeneratorBy(context)

    override fun getOrCreateVariable(model: UtModel, name: String?): CgValue {
        val baseName = name ?: nameGenerator.nameFrom(model.classId)
        return valueByModel.getOrPut(model) {
            when (model) {
                is PythonBoolModel -> CgLiteral(model.classId, model.value)
                is PythonPrimitiveModel -> CgLiteral(model.classId, model.value)
                is PythonTreeModel -> CgPythonTree(model.classId, model.tree)
                is PythonInitObjectModel -> constructInitObjectModel(model, baseName)
                is PythonDictModel -> CgPythonRepr(model.classId, model.toString())
                is PythonListModel -> CgPythonRepr(model.classId, model.toString())
                is PythonSetModel -> CgPythonRepr(model.classId, model.toString())
                is PythonTupleModel -> CgPythonRepr(model.classId, model.toString())
                is PythonDefaultModel -> CgPythonRepr(model.classId, model.repr)
                is PythonModel -> error("Unexpected PythonModel: ${model::class}")
                else -> super.getOrCreateVariable(model, name)
            }
        }
    }

    private fun constructInitObjectModel(model: PythonInitObjectModel, baseName: String): CgVariable {
        return newVar(model.classId, baseName) { CgConstructorCall(
            ConstructorId(model.classId, model.initValues.map { it.classId }),
            model.initValues.map { getOrCreateVariable(it) }
        ) }
    }
}