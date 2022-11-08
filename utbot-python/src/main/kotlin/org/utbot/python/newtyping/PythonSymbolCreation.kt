package org.utbot.python.newtyping

import org.utbot.python.newtyping.general.CompositeTypeCreator
import org.utbot.python.newtyping.general.FunctionTypeCreator
import org.utbot.python.newtyping.general.Name

object PythonCallableCreator {
    fun create(
        numberOfParameters: Int,
        argumentKinds: List<PythonCallable.ArgKind>,
        initialization: (Original) -> FunctionTypeCreator.InitializationData
    ): PythonCallable {
        val result = Original(numberOfParameters, argumentKinds)
        val data = initialization(result)
        result.arguments = data.arguments
        result.returnValue = data.returnValue
        return result
    }
    class Original(
        numberOfParameters: Int,
        override val argumentKinds: List<PythonCallable.ArgKind>
    ): PythonCallable, FunctionTypeCreator.Original(numberOfParameters)
}

object PythonConcreteCompositeTypeCreator {
    fun create(
        name: Name,
        numberOfParameters: Int,
        memberNames: List<String>,
        initialization: (Original) -> CompositeTypeCreator.InitializationData
    ): PythonConcreteCompositeType {
        val result = Original(name, numberOfParameters, memberNames)
        result.initialize(initialization(result))
        return result
    }
    class Original(
        name: Name,
        numberOfParameters: Int,
        override val memberNames: List<String>
    ): PythonConcreteCompositeType, CompositeTypeCreator.Original(name, numberOfParameters)
}

object PythonProtocolCreator {
    fun create(
        name: Name,
        numberOfParameters: Int,
        memberNames: List<String>,
        protocolMemberNames: List<String>,
        initialization: (CompositeTypeCreator.Original) -> CompositeTypeCreator.InitializationData
    ): PythonProtocol {
        val result = Original(name, numberOfParameters, memberNames, protocolMemberNames)
        result.initialize(initialization(result))
        return result
    }
    class Original(
        name: Name,
        numberOfParameters: Int,
        override val memberNames: List<String>,
        override val protocolMemberNames: List<String>
    ): PythonProtocol, CompositeTypeCreator.Original(name, numberOfParameters)
}