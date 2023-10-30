package org.utbot.python.engine.utils

import org.utbot.framework.plugin.api.UtModel
import org.utbot.python.evaluation.serialization.MemoryDump
import org.utbot.python.evaluation.serialization.toPythonTree
import org.utbot.python.framework.api.python.PythonTreeModel

fun transformModelList(
    hasThisObject: Boolean,
    state: MemoryDump,
    modelListIds: List<String>
): Pair<UtModel?, List<UtModel>> {
    val (stateThisId, resultModelListIds) =
        if (hasThisObject) {
            Pair(modelListIds.first(), modelListIds.drop(1))
        } else {
            Pair(null, modelListIds)
        }
    val stateThisObject = stateThisId?.let {
        PythonTreeModel(
            state.getById(it).toPythonTree(state)
        )
    }
    val modelList = resultModelListIds.map {
        PythonTreeModel(
            state.getById(it).toPythonTree(state)
        )
    }
    return Pair(stateThisObject, modelList)
}

