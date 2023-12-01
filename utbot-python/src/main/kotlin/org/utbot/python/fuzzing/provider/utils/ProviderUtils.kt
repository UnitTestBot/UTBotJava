package org.utbot.python.fuzzing.provider.utils

import org.utbot.python.newtyping.PythonAnyTypeDescription
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.PythonDefinition
import org.utbot.python.newtyping.PythonTypeHintsStorage
import org.utbot.python.newtyping.PythonVariableDescription
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.getPythonAttributeByName

fun UtType.isAny(): Boolean {
    return meta is PythonAnyTypeDescription
}

fun isConcreteType(type: UtType): Boolean {
    return (type.meta as? PythonConcreteCompositeTypeDescription)?.isAbstract == false
}

fun PythonDefinition.isProtected(): Boolean {
    val name = this.meta.name
    return name.startsWith("_") && !this.isMagic()
}

fun PythonDefinition.isPrivate(): Boolean {
    val name = this.meta.name
    return name.startsWith("__") && !this.isMagic()
}

fun PythonDefinition.isMagic(): Boolean {
    return this.meta.name.startsWith("__") && this.meta.name.endsWith("__") && this.meta.name.length >= 4
}

fun PythonDefinition.isProperty(): Boolean {
    return (this.meta as? PythonVariableDescription)?.isProperty == true
}

fun PythonDefinition.isCallable(typeStorage: PythonTypeHintsStorage): Boolean {
    return this.type.getPythonAttributeByName(typeStorage, "__call__") != null
}