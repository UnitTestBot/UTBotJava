package org.utbot.python

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel

sealed class PythonModel {
    abstract val model: UtModel
    abstract fun render(): String
}

class PythonIntModel() : PythonModel() {
    override fun render(): String {
        return ""
    }
}