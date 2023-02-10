package org.utbot.python.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgConstructorCall
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.tree.CgComponents
import org.utbot.framework.codegen.tree.CgVariableConstructor
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.python.framework.api.python.*
import org.utbot.python.framework.codegen.model.tree.*

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
                is PythonDictModel -> CgPythonDict(model.stores.map {
                    getOrCreateVariable(it.key) to getOrCreateVariable(
                        it.value
                    )
                }.toMap())

                is PythonListModel -> CgPythonList(model.stores.map { getOrCreateVariable(it) })
                is PythonSetModel -> CgPythonSet(model.stores.map { getOrCreateVariable(it) }.toSet())
                is PythonTupleModel -> CgPythonTuple(model.stores.map { getOrCreateVariable(it) })
                is PythonDefaultModel -> CgPythonRepr(model.classId, model.repr)
                is PythonModel -> error("Unexpected PythonModel: ${model::class}")
                else -> super.getOrCreateVariable(model, name)
            }
        }
    }

    private fun constructInitObjectModel(model: PythonInitObjectModel, baseName: String): CgVariable {
        return newVar(model.classId, baseName) {
            CgConstructorCall(
                ConstructorId(model.classId, model.initValues.map { it.classId }),
                model.initValues.map { getOrCreateVariable(it) }
            )
        }
    }
}