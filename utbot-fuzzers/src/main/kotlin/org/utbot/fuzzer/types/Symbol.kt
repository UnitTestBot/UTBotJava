package org.utbot.fuzzer.types

class TypeParameter(
    constraints: Set<TypeParameterConstraint>
)
open class TypeParameterConstraint
open class Type(
    val parameters: List<TypeParameter>
)
sealed class Name
data class Atom(val name: String): Name()
data class FqName(val prefix: List<String>, val name: String): Name()
open class ConcreteType(val name: Name) : Type(emptyList())
open class PrimitiveType(name: Atom) : ConcreteType(name)

open class ParameterizedType (
    parameters: List<TypeParameter>,
    val type: Type
) : Type(parameters)

open class DeclaredType(
    fqName: FqName,
    val libraryType: Boolean
): ConcreteType(fqName)

open class FunctionType(
    parameters: List<TypeParameter>,
    val arguments: List<Type>,
    val returnValue: Type
) : Type(parameters)

open class Field(
    val name: String,
    val type: Type
)

open class Function(
    val name: String,
    val type: FunctionType
)

open class CompositeType(
    fqName: FqName,
    libraryType: Boolean,
    val supertypes: Set<Type>,
    val fields: List<Field>,
    val methods: List<Function>,
) : DeclaredType(fqName, libraryType)