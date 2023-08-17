package org.utbot.python.newtyping.general

object DefaultSubstitutionProvider: SubstitutionProvider<UtType, UtType>() {
    override fun substitute(type: UtType, params: Map<TypeParameter, UtType>): UtType =
        when (type) {
            is TypeParameter -> TypeParameterSubstitutionProvider(this).substitute(type, params)
            is FunctionType -> FunctionTypeSubstitutionProvider(this).substitute(type, params)
            is CompositeType -> CompositeTypeSubstitutionProvider(this).substitute(type, params)
            // is StatefulType -> StatefulTypeSubstitutionProvider(this).substitute(type, params)
            else -> TypeSubstitutionProvider(this).substitute(type, params)
        }
}

abstract class SubstitutionProvider<I : UtType, O: UtType> {
    abstract fun substitute(type: I, params: Map<TypeParameter, UtType>): O
    fun substituteByIndex(type: I, index: Int, newParamValue: UtType): O {
        val param = type.parameters[index] as? TypeParameter
            ?: error("Cannot substitute parameter at index $index of type $type")
        return substitute(type, mapOf(param to newParamValue))
    }
    fun substituteAll(type: I, newParamValues: List<UtType>): O {
        val params = type.parameters.map {
            it as? TypeParameter ?: error("Can apply substituteAll only to types without any substitutions")
        }
        return substitute(type, (params zip newParamValues).associate { it })
    }
}

class TypeSubstitutionProvider(
    private val provider: SubstitutionProvider<UtType, UtType>
): SubstitutionProvider<UtType, UtType>() {
    override fun substitute(type: UtType, params: Map<TypeParameter, UtType>): UtType {
        return Substitution(type, params, provider)
    }
    open class Substitution(
        originForInitialization: UtType,
        rawParams: Map<TypeParameter, UtType>,
        provider: SubstitutionProvider<UtType, UtType>
    ): UtType, TypeSubstitution {
        val newBoundedTypeParameters: Map<TypeParameter, TypeParameter> =
            originForInitialization.parameters.mapNotNull {
                if (
                    it is TypeParameter &&
                    !rawParams.keys.contains(it) &&
                    it.definedAt == originForInitialization
                ) {
                    it to TypeParameter(this).let { newParam ->
                        newParam.constraints = substituteConstraints(it.constraints, provider, rawParams)
                        newParam.meta = it.meta
                        newParam
                    }
                } else {
                    null
                }
            }.associate { it }
        override val rawOrigin: UtType = originForInitialization
        override val params: Map<TypeParameter, UtType> = rawParams
        override val parameters: List<UtType> by lazy {
            rawOrigin.parameters.map {
                provider.substitute(it, params + newBoundedTypeParameters)
            }
        }
        override val meta: TypeMetaData
            get() = rawOrigin.meta
    }
}

class FunctionTypeSubstitutionProvider(
    private val provider: SubstitutionProvider<UtType, UtType>
): SubstitutionProvider<FunctionType, FunctionType>() {
    override fun substitute(type: FunctionType, params: Map<TypeParameter, UtType>): FunctionType {
        return Substitution(type, params, provider)
    }
    open class Substitution(
        override val rawOrigin: FunctionType,
        override val params: Map<TypeParameter, UtType>,
        provider: SubstitutionProvider<UtType, UtType>
    ): FunctionType, TypeSubstitutionProvider.Substitution(rawOrigin, params, provider) {
        override val arguments: List<UtType> by lazy {
            rawOrigin.arguments.map { provider.substitute(it, newBoundedTypeParameters + params) }
        }
        override val returnValue: UtType by lazy {
            provider.substitute(rawOrigin.returnValue, newBoundedTypeParameters + params)
        }
    }
}

/*
class StatefulTypeSubstitutionProvider(
    private val provider: SubstitutionProvider<Type, Type>
): SubstitutionProvider<StatefulType, StatefulType>() {
    override fun substitute(type: StatefulType, params: Map<TypeParameter, Type>): StatefulType {
        return Substitution(type, params, provider)
    }
    open class Substitution(
        override val rawOrigin: StatefulType,
        override val params: Map<TypeParameter, Type>,
        provider: SubstitutionProvider<Type, Type>
    ): StatefulType, TypeSubstitutionProvider.Substitution(rawOrigin, params, provider) {
        override val members: List<Type> by lazy {
            rawOrigin.members.map { provider.substitute(it, newBoundedTypeParameters + params) }
        }
    }
}
 */

class CompositeTypeSubstitutionProvider(
    private val provider: SubstitutionProvider<UtType, UtType>
): SubstitutionProvider<CompositeType, CompositeType>() {
    override fun substitute(type: CompositeType, params: Map<TypeParameter, UtType>): CompositeType {
        return Substitution(type, params, provider)
    }
    open class Substitution(
        override val rawOrigin: CompositeType,
        override val params: Map<TypeParameter, UtType>,
        provider: SubstitutionProvider<UtType, UtType>
    ): CompositeType, TypeSubstitutionProvider.Substitution(rawOrigin, params, provider) {
        override val supertypes: List<UtType> by lazy {
            rawOrigin.supertypes.map { provider.substitute(it, newBoundedTypeParameters + params) }
        }
        override val members: List<UtType> by lazy {
            rawOrigin.members.map { provider.substitute(it, newBoundedTypeParameters + params) }
        }
    }
}

class TypeParameterSubstitutionProvider(
    private val provider: SubstitutionProvider<UtType, UtType>
): SubstitutionProvider<TypeParameter, UtType>() {
    override fun substitute(type: TypeParameter, params: Map<TypeParameter, UtType>): UtType {
        return params[type] ?: run {
            type.constraints = substituteConstraints(type.constraints, provider, params)
            type
        }
    }
}

private fun substituteConstraints(
    constraints: Set<TypeParameterConstraint>,
    provider: SubstitutionProvider<UtType, UtType>,
    params: Map<TypeParameter, UtType>
) =
    constraints.map {
        TypeParameterConstraint(
            relation = it.relation,
            boundary = provider.substitute(it.boundary, params)
        )
    }.toSet()

interface TypeSubstitution {
    val params: Map<TypeParameter, UtType>
    val rawOrigin: UtType
}
