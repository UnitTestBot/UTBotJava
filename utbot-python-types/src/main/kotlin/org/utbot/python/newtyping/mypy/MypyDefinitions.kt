package org.utbot.python.newtyping.mypy

import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.utils.CustomPolymorphicJsonAdapterFactory

sealed class MypyDefinition(val type: MypyAnnotation) {
    fun getUtBotType(): UtType = type.asUtBotType
    abstract fun getUtBotDescription(): PythonDefinitionDescription?
    abstract fun getUtBotDefinition(): PythonDefinition?
}

class Variable(
    val name: String,
    val isProperty: Boolean,
    val isSelf: Boolean,
    type: MypyAnnotation
): MypyDefinition(type) {
    override fun getUtBotDescription(): PythonVariableDescription =
        PythonVariableDescription(name, isProperty = isProperty, isSelf = isSelf)
    override fun getUtBotDefinition(): PythonDefinition =
        PythonDefinition(getUtBotDescription(), getUtBotType())
}

class ClassDef(type: MypyAnnotation): MypyDefinition(type) {
    override fun getUtBotDefinition(): PythonDefinition? = null
    override fun getUtBotDescription(): PythonDefinitionDescription? = null
}

class FuncDef(
    type: MypyAnnotation,
    val args: List<MypyDefinition>,
    val name: String
): MypyDefinition(type) {
    override fun getUtBotDescription(): PythonFuncItemDescription =
        PythonFuncItemDescription(
            name,
            args.map {
                val variable = it as Variable
                PythonVariableDescription(variable.name, isProperty = variable.isProperty, isSelf = variable.isSelf)
            }
        )

    override fun getUtBotDefinition(): PythonFunctionDefinition =
        PythonFunctionDefinition(getUtBotDescription(), getUtBotType() as FunctionType)
}

class OverloadedFuncDef(
    type: MypyAnnotation,
    val items: List<MypyDefinition>,
    val name: String
): MypyDefinition(type) {
    override fun getUtBotDescription(): PythonOverloadedFuncDefDescription =
        PythonOverloadedFuncDefDescription(
            name,
            items.mapNotNull { it.getUtBotDescription() }
        )

    override fun getUtBotDefinition(): PythonDefinition =
        PythonDefinition(getUtBotDescription(), getUtBotType())
}


enum class MypyDefinitionLabel {
    Variable,
    ClassDef,
    FuncDef,
    OverloadedFuncDef
}

val definitionAdapter = CustomPolymorphicJsonAdapterFactory(
    MypyDefinition::class.java,
    contentLabel = "content",
    keyLabel = "kind",
    mapOf(
        MypyDefinitionLabel.Variable.name to Variable::class.java,
        MypyDefinitionLabel.ClassDef.name to ClassDef::class.java,
        MypyDefinitionLabel.FuncDef.name to FuncDef::class.java,
        MypyDefinitionLabel.OverloadedFuncDef.name to OverloadedFuncDef::class.java
    )
)