package org.utbot.python.newtyping.general

interface Type {
    val parameters: List<Type>
    val meta: TypeMetaData
}

// arguments and returnValue of FunctionType instance can recursively refer to it and its parameters
interface FunctionType: Type {
    val arguments: List<Type>
    val returnValue: Type
}

interface StatefulType: Type {
    val members: List<Type>
}

// members and supertypes of CompositeType instance can recursively refer to it and its parameters
interface CompositeType: StatefulType {
    val supertypes: Collection<Type>
}

open class TypeMetaData
open class TypeMetaDataWithName(val name: Name): TypeMetaData()

class TypeParameter(val definedAt: Type): Type {
    // tricky case with cyclic dependency; constraints may be changed after substitution
    var constraints: Set<TypeParameterConstraint> = emptySet()
    override var meta: TypeMetaData = TypeMetaData()
    override val parameters: List<Type> = emptyList()
}

class TypeRelation(
    val name: String
)

open class TypeParameterConstraint(
    val relation: TypeRelation,
    val boundary: Type
)

class Name(val prefix: List<String>, val name: String)