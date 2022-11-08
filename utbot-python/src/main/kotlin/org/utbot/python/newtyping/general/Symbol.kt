package org.utbot.python.newtyping.general

interface Type {
    val parameters: List<Type>
}

interface NamedType: Type {
    val name: Name
}

interface FunctionType: Type {
    val arguments: List<Type>
    val returnValue: Type
}

interface StatefulType: NamedType {
    val members: List<Type>
}

interface CompositeType: StatefulType {
    val supertypes: Collection<Type>
}

open class TypeParameterMetaData

class TypeParameter(val definedAt: Type): Type {
    // tricky case with cyclic dependency; constraints may be changed after substitution
    var constraints: Set<TypeParameterConstraint> = emptySet()
    var meta: TypeParameterMetaData = TypeParameterMetaData()
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