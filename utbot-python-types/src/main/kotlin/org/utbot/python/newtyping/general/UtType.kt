package org.utbot.python.newtyping.general

interface UtType {
    val parameters: List<UtType>
    val meta: TypeMetaData
}

// arguments and returnValue of FunctionType instance can recursively refer to it and its parameters
interface FunctionType: UtType {
    val arguments: List<UtType>
    val returnValue: UtType
}

/*
interface StatefulType: Type {
    val members: List<Type>
}
 */

// members and supertypes of CompositeType instance can recursively refer to it and its parameters
interface CompositeType: UtType {
    val supertypes: List<UtType>
    val members: List<UtType>
}

open class TypeMetaData
open class TypeMetaDataWithName(val name: Name): TypeMetaData() {
    override fun toString(): String {
        return name.toString()
    }
}

class TypeParameter(val definedAt: UtType): UtType {
    // tricky case with cyclic dependency; constraints may be changed after substitution
    var constraints: Set<TypeParameterConstraint> = emptySet()
    override var meta: TypeMetaData = TypeMetaData()
    override val parameters: List<UtType> = emptyList()
}

class TypeRelation(
    val name: String
)

open class TypeParameterConstraint(
    val relation: TypeRelation,
    val boundary: UtType
)

class Name(val prefix: List<String>, val name: String) {
    override fun toString(): String {
        return if (prefix.isEmpty()) {
            name
        } else {
            "${prefix.joinToString(".")}.$name"
        }
    }
}
