package org.utbot.python.newtyping.mypy

import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.Type

sealed class MypyDefinition(val type: MypyAnnotation) {
    fun getUtBotType(): Type = type.asUtBotType
    abstract fun getUtBotDescription(): PythonDefinitionDescription?
    abstract fun getUtBotDefinition(): PythonDefinition?
}

class Variable(
    val name: String,
    val isProperty: Boolean,
    val isSelf: Boolean,
    val isInitializedInClass: Boolean,
    type: MypyAnnotation
): MypyDefinition(type) {
    override fun getUtBotDescription(): PythonVariableDescription =
        PythonVariableDescription(name, isProperty = isProperty, isSelf = isSelf, isInitializedInClass = isInitializedInClass)
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

val definitionAdapter: PolymorphicJsonAdapterFactory<MypyDefinition> =
    PolymorphicJsonAdapterFactory.of(MypyDefinition::class.java, "kind")
        .withSubtype(Variable::class.java, MypyDefinitionLabel.Variable.name)
        .withSubtype(ClassDef::class.java, MypyDefinitionLabel.ClassDef.name)
        .withSubtype(FuncDef::class.java, MypyDefinitionLabel.FuncDef.name)
        .withSubtype(OverloadedFuncDef::class.java, MypyDefinitionLabel.OverloadedFuncDef.name)