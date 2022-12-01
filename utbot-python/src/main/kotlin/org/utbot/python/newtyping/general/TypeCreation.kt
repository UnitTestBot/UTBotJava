package org.utbot.python.newtyping.general

object TypeCreator {
    fun create(numberOfParameters: Int, meta: TypeMetaData, initialization: (Original) -> Unit): Type {
        val result = Original(numberOfParameters, meta)
        initialization(result)
        return result
    }
    class Original(
        numberOfParameters: Int,
        override val meta: TypeMetaData
    ): Type {
        override val parameters: MutableList<TypeParameter> =
            List(numberOfParameters) { TypeParameter(this) }.toMutableList()
    }
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

/*
object StatefulTypeCreator {
    fun create(parameters: List<Type>, members: List<Type>, meta: TypeMetaData): StatefulType {
        return Original(parameters, members, meta)
    }
    class Original(
        override val parameters: List<Type>,
        override val members: List<Type>,
        override val meta: TypeMetaData
    ): StatefulType
}
 */

object CompositeTypeCreator {
    fun create(
        numberOfParameters: Int,
        meta: TypeMetaData,
        initialization: (Original) -> InitializationData
    ): CompositeType {
        val result = Original(numberOfParameters, meta)
        val data = initialization(result)
        result.members = data.members
        result.supertypes = data.supertypes
        return result
    }
    open class Original(
        numberOfParameters: Int,
        override val meta: TypeMetaData
    ): CompositeType {
        override val parameters: MutableList<TypeParameter> =
            List(numberOfParameters) { TypeParameter(this) }.toMutableList()
        override lateinit var members: List<Type>
        override lateinit var supertypes: List<Type>
    }
    data class InitializationData(
        val members: List<Type>,
        val supertypes: List<Type>
    )
}
