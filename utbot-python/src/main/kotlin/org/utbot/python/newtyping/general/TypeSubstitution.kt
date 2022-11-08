package org.utbot.python.newtyping.general

object DefaultSubstitutionProvider: SubstitutionProvider<Type, Type>() {
    override fun substitute(type: Type, params: Map<TypeParameter, Type>): Type =
        when (type) {
            is TypeParameter -> TypeParameterSubstitutionProvider(this).substitute(type, params)
            is FunctionType -> FunctionTypeSubstitutionProvider(this).substitute(type, params)
            is CompositeType -> CompositeTypeSubstitutionProvider(this).substitute(type, params)
            is StatefulType -> StatefulTypeSubstitutionProvider(this).substitute(type, params)
            is NamedType -> NamedTypeSubstitutionProvider(this).substitute(type, params)
            else -> TypeSubstitutionProvider(this).substitute(type, params)
        }
}

abstract class SubstitutionProvider<I : Type, O: Type> {
    abstract fun substitute(type: I, params: Map<TypeParameter, Type>): O
}

class TypeSubstitutionProvider(
    val provider: SubstitutionProvider<Type, Type>
): SubstitutionProvider<Type, Type>() {
    override fun substitute(type: Type, params: Map<TypeParameter, Type>): Type {
        return Substitution(type, params, provider)
    }
    open class Substitution(
        originForBoundedParameters: Type,
        rawParams: Map<TypeParameter, Type>,
        provider: SubstitutionProvider<Type, Type>
    ): Type, TypeSubstitution {
        val newBoundedTypeParameters: Map<TypeParameter, TypeParameter> =
            originForBoundedParameters.parameters.mapNotNull {
                if (
                    it is TypeParameter &&
                    !rawParams.keys.contains(it) &&
                    it.definedAt === originForBoundedParameters
                ) {
                    it to TypeParameter(this).let { newParam ->
                        newParam.constraints = substituteConstraints(it.constraints, provider, rawParams)
                        newParam
                    }
                } else {
                    null
                }
            }.associate { it }
        override val rawOrigin: Type = originForBoundedParameters
        override val params: Map<TypeParameter, Type> = rawParams
        override val parameters: List<Type> by lazy {
            rawOrigin.parameters.map {
                provider.substitute(it, params + newBoundedTypeParameters)
            }
        }
    }
}

class NamedTypeSubstitutionProvider(
    val provider: SubstitutionProvider<Type, Type>
): SubstitutionProvider<NamedType, NamedType>() {
    override fun substitute(type: NamedType, params: Map<TypeParameter, Type>): NamedType {
        return Substitution(type, params, provider)
    }
    open class Substitution(
        override val rawOrigin: NamedType,
        override val params: Map<TypeParameter, Type>,
        provider: SubstitutionProvider<Type, Type>
    ): NamedType, TypeSubstitutionProvider.Substitution(rawOrigin, params, provider) {
        override val name: Name
            get() = rawOrigin.name
    }
}

class FunctionTypeSubstitutionProvider(
    val provider: SubstitutionProvider<Type, Type>
): SubstitutionProvider<FunctionType, FunctionType>() {
    override fun substitute(type: FunctionType, params: Map<TypeParameter, Type>): FunctionType {
        return Substitution(type, params, provider)
    }
    open class Substitution(
        override val rawOrigin: FunctionType,
        override val params: Map<TypeParameter, Type>,
        provider: SubstitutionProvider<Type, Type>
    ): FunctionType, TypeSubstitutionProvider.Substitution(rawOrigin, params, provider) {
        override val arguments: List<Type> by lazy {
            rawOrigin.arguments.map { provider.substitute(it, newBoundedTypeParameters + params) }
        }
        override val returnValue: Type by lazy {
            provider.substitute(rawOrigin.returnValue, newBoundedTypeParameters + params)
        }
    }
}

class StatefulTypeSubstitutionProvider(
    val provider: SubstitutionProvider<Type, Type>
): SubstitutionProvider<StatefulType, StatefulType>() {
    override fun substitute(type: StatefulType, params: Map<TypeParameter, Type>): StatefulType {
        return Substitution(type, params, provider)
    }
    open class Substitution(
        override val rawOrigin: StatefulType,
        override val params: Map<TypeParameter, Type>,
        provider: SubstitutionProvider<Type, Type>
    ): StatefulType, NamedTypeSubstitutionProvider.Substitution(rawOrigin, params, provider) {
        override val members: List<Type> by lazy {
            rawOrigin.members.map { provider.substitute(it, newBoundedTypeParameters + params) }
        }
    }
}

class CompositeTypeSubstitutionProvider(
    val provider: SubstitutionProvider<Type, Type>
): SubstitutionProvider<CompositeType, CompositeType>() {
    override fun substitute(type: CompositeType, params: Map<TypeParameter, Type>): CompositeType {
        return Substitution(type, params, provider)
    }
    open class Substitution(
        override val rawOrigin: CompositeType,
        override val params: Map<TypeParameter, Type>,
        provider: SubstitutionProvider<Type, Type>
    ): CompositeType, StatefulTypeSubstitutionProvider.Substitution(rawOrigin, params, provider) {
        override val supertypes: List<Type> by lazy {
            rawOrigin.supertypes.map { provider.substitute(it, newBoundedTypeParameters + params) }
        }
    }
}

class TypeParameterSubstitutionProvider(
    val provider: SubstitutionProvider<Type, Type>
): SubstitutionProvider<TypeParameter, Type>() {
    override fun substitute(type: TypeParameter, params: Map<TypeParameter, Type>): Type {
        return params[type] ?: run {
            type.constraints = substituteConstraints(type.constraints, provider, params)
            type
        }
    }
}

private fun substituteConstraints(
    constraints: Set<TypeParameterConstraint>,
    provider: SubstitutionProvider<Type, Type>,
    params: Map<TypeParameter, Type>
) =
    constraints.map {
        TypeParameterConstraint(
            relation = it.relation,
            boundary = provider.substitute(it.boundary, params)
        )
    }.toSet()

interface TypeSubstitution {
    val params: Map<TypeParameter, Type>
    val rawOrigin: Type
}
