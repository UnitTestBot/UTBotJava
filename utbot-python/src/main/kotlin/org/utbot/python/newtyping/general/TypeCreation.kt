package org.utbot.python.newtyping.general

object TypeCreator {
    fun create(parameters: List<Type>): Type {
        return Original(parameters)
    }
    class Original(
        override val parameters: List<Type>
    ): Type
}

object NamedTypeCreator {
    fun create(parameters: List<Type>, name: Name): NamedType {
        return Original(parameters, name)
    }
    class Original(
        override val parameters: List<Type>,
        override val name: Name,
    ): NamedType
}

object FunctionTypeCreator {
    fun create(numberOfParameters: Int, initialization: (Original) -> InitializationData): FunctionType {
        val result = Original(numberOfParameters)
        val data = initialization(result)
        result.arguments = data.arguments
        result.returnValue = data.returnValue
        return result
    }
    open class Original(
        numberOfParameters: Int
    ): FunctionType {
        override val parameters: List<TypeParameter> = List(numberOfParameters) { TypeParameter(this) }
        override lateinit var arguments: List<Type>
        override lateinit var returnValue: Type
    }
    data class InitializationData(
        val arguments: List<Type>,
        val returnValue: Type
    )
}

object StatefulTypeCreator {
    fun create(parameters: List<Type>, name: Name, members: List<Type>): StatefulType {
        return Original(parameters, name, members)
    }
    class Original(
        override val parameters: List<Type>,
        override val name: Name,
        override val members: List<Type>
    ): StatefulType
}

object CompositeTypeCreator {
    fun create(
        name: Name,
        numberOfParameters: Int,
        initialization: (Original) -> InitializationData
    ): CompositeType {
        val result = Original(name, numberOfParameters)
        result.initialize(initialization(result))
        return result
    }
    open class Original(
        override val name: Name,
        numberOfParameters: Int,
    ): CompositeType {
        override val parameters: List<TypeParameter> = List(numberOfParameters) { TypeParameter(this) }
        lateinit var membersHolder: List<Type>
        lateinit var supertypesHolder: Collection<Type>
        var initialized = false
        fun initialize(data: InitializationData) {
            this.membersHolder = data.members
            this.supertypesHolder = data.supertypes
            this.parameters.forEach { it.constraints = data.typeParameterConstraints[it] ?: it.constraints }
            initialized = true
        }
        override val members: List<Type>
            get() = membersHolder
        override val supertypes: Collection<Type>
            get() = supertypesHolder
    }
    data class InitializationData(
        val members: List<Type>,
        val supertypes: Collection<Type>,
        val typeParameterConstraints: Map<TypeParameter, Set<TypeParameterConstraint>>
    )
}
