package org.utbot.python.newtyping.general

object TypeCreator {
    fun create(parameters: List<Type>, meta: TypeMetaData): Type {
        return Original(parameters, meta)
    }
    class Original(
        override val parameters: List<Type>,
        override val meta: TypeMetaData
    ): Type
}

object NamedTypeCreator {
    fun create(parameters: List<Type>, name: Name, meta: TypeMetaData): NamedType {
        return Original(parameters, name, meta)
    }
    class Original(
        override val parameters: List<Type>,
        override val name: Name,
        override val meta: TypeMetaData
    ): NamedType
}

object FunctionTypeCreator {
    fun create(
        numberOfParameters: Int,
        meta: TypeMetaData,
        initialization: (Original) -> InitializationData
    ): FunctionType {
        val result = Original(numberOfParameters, meta)
        val data = initialization(result)
        result.arguments = data.arguments
        result.returnValue = data.returnValue
        return result
    }
    open class Original(
        numberOfParameters: Int,
        override val meta: TypeMetaData
    ): FunctionType {
        override val parameters: MutableList<TypeParameter> =
            List(numberOfParameters) { TypeParameter(this) }.toMutableList()
        override lateinit var arguments: List<Type>
        override lateinit var returnValue: Type
    }
    data class InitializationData(
        val arguments: List<Type>,
        val returnValue: Type
    )
}

object StatefulTypeCreator {
    fun create(parameters: List<Type>, name: Name, members: List<Type>, meta: TypeMetaData): StatefulType {
        return Original(parameters, name, members, meta)
    }
    class Original(
        override val parameters: List<Type>,
        override val name: Name,
        override val members: List<Type>,
        override val meta: TypeMetaData
    ): StatefulType
}

object CompositeTypeCreator {
    fun create(
        name: Name,
        numberOfParameters: Int,
        meta: TypeMetaData,
        initialization: (Original) -> InitializationData
    ): CompositeType {
        val result = Original(name, numberOfParameters, meta)
        val data = initialization(result)
        result.members = data.members
        result.supertypes = data.supertypes
        return result
    }
    open class Original(
        override val name: Name,
        numberOfParameters: Int,
        override val meta: TypeMetaData
    ): CompositeType {
        override val parameters: MutableList<TypeParameter> =
            List(numberOfParameters) { TypeParameter(this) }.toMutableList()
        override lateinit var members: List<Type>
        override lateinit var supertypes: Collection<Type>
    }
    data class InitializationData(
        val members: List<Type>,
        val supertypes: Collection<Type>
    )
}
