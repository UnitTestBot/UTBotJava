package org.utbot.python.newtyping

import org.utbot.python.newtyping.general.*

object PythonTypeSubstitutionProvider: SubstitutionProvider<Type, Type>() {
    override fun substitute(type: Type, params: Map<TypeParameter, Type>): Type {
        return when (type) {
            is PythonCallable -> PythonCallableSubstitutionProvider(this).substitute(type, params)
            is PythonConcreteCompositeType -> PythonConcreteCompositeSubstitutionProvider(this).substitute(type, params)
            is PythonProtocol -> PythonProtocolSubstitutionProvider(this).substitute(type, params)
            else -> DefaultSubstitutionProvider.substitute(type, params)
        }
    }
}

class PythonCallableSubstitutionProvider(
    private val provider: SubstitutionProvider<Type, Type>
): SubstitutionProvider<PythonCallable, PythonCallable>() {
    override fun substitute(type: PythonCallable, params: Map<TypeParameter, Type>): PythonCallable {
        return Substitution(type, params, provider)
    }
    class Substitution(
        override val rawOrigin: PythonCallable,
        override val params: Map<TypeParameter, Type>,
        provider: SubstitutionProvider<Type, Type>
    ): PythonCallable, FunctionTypeSubstitutionProvider.Substitution(rawOrigin, params, provider) {
        override val argumentKinds: List<PythonCallable.ArgKind>
            get() = rawOrigin.argumentKinds
    }
}

class PythonConcreteCompositeSubstitutionProvider(
    private val provider: SubstitutionProvider<Type, Type>
): SubstitutionProvider<PythonConcreteCompositeType, PythonConcreteCompositeType>() {
    override fun substitute(
        type: PythonConcreteCompositeType,
        params: Map<TypeParameter, Type>
    ): PythonConcreteCompositeType {
        return Substitution(type, params, provider)
    }
    class Substitution(
        override val rawOrigin: PythonConcreteCompositeType,
        override val params: Map<TypeParameter, Type>,
        provider: SubstitutionProvider<Type, Type>
    ): PythonConcreteCompositeType, CompositeTypeSubstitutionProvider.Substitution(rawOrigin, params, provider) {
        override val memberNames: List<String>
            get() = rawOrigin.memberNames
    }
}

class PythonProtocolSubstitutionProvider(
    private val provider: SubstitutionProvider<Type, Type>
): SubstitutionProvider<PythonProtocol, PythonProtocol>() {
    override fun substitute(type: PythonProtocol, params: Map<TypeParameter, Type>): PythonProtocol {
        return Substitution(type, params, provider)
    }
    class Substitution(
        override val rawOrigin: PythonProtocol,
        override val params: Map<TypeParameter, Type>,
        provider: SubstitutionProvider<Type, Type>
    ): PythonProtocol, CompositeTypeSubstitutionProvider.Substitution(rawOrigin, params, provider) {
        override val memberNames: List<String>
            get() = rawOrigin.memberNames
        override val protocolMemberNames: List<String>
            get() = rawOrigin.protocolMemberNames
    }
}