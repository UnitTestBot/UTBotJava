package org.utbot.python.newtyping.ast.visitor.hints

import org.utbot.python.newtyping.PythonCallableTypeDescription
import org.utbot.python.newtyping.createPythonCallableType
import org.utbot.python.newtyping.createPythonProtocol
import org.utbot.python.newtyping.general.CompositeTypeCreator
import org.utbot.python.newtyping.general.FunctionTypeCreator
import org.utbot.python.newtyping.general.Name
import org.utbot.python.newtyping.general.Type


fun createIterableWithCustomReturn(returnType: Type): Type =
    createPythonProtocol(
        Name(emptyList(), ""),  // TODO: normal names?
        0,
        listOf("__iter__"),
        listOf("__iter__")
    ) { self ->
        CompositeTypeCreator.InitializationData(
            members = listOf(
                createPythonCallableType(
                    0,
                    listOf(PythonCallableTypeDescription.ArgKind.Positional),
                    isClassMethod = false,
                    isStaticMethod = false
                ) {
                    FunctionTypeCreator.InitializationData(
                        arguments = listOf(self),
                        returnType
                    )
                }
            ),
            supertypes = emptyList()
        )
    }