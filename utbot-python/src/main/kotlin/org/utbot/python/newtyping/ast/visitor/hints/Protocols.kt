package org.utbot.python.newtyping.ast.visitor.hints

import org.utbot.python.newtyping.PythonCallableTypeDescription
import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.createPythonCallableType
import org.utbot.python.newtyping.createPythonProtocol
import org.utbot.python.newtyping.general.CompositeTypeCreator
import org.utbot.python.newtyping.general.FunctionTypeCreator
import org.utbot.python.newtyping.general.Name
import org.utbot.python.newtyping.general.Type

fun operationToMagicMethod(op: String): String? =
    when (op) {
        "+" -> "__add__"
        "-" -> "__sub__"
        "*" -> "__mul__"
        else -> null
    }

fun createIterableWithCustomReturn(returnType: Type): Type =
    createUnaryProtocolWithCustomReturn("__iter__", returnType)

fun supportsBoolProtocol(storage: PythonTypeStorage): Type =
    createUnaryProtocolWithCustomReturn("__bool__", storage.pythonBool)

fun createBinaryProtocol(methodName: String, argType: Type, returnType: Type): Type =
    createPythonProtocol(
        Name(emptyList(), ""),  // TODO: name?
        0,
        listOf(methodName),
        listOf(methodName)
    ) { self ->
        CompositeTypeCreator.InitializationData(
            members = listOf(
                createPythonCallableType(
                    0,
                    listOf(PythonCallableTypeDescription.ArgKind.Positional, PythonCallableTypeDescription.ArgKind.Positional),
                    listOf("self", ""),
                    isClassMethod = false,
                    isStaticMethod = false
                ) {
                    FunctionTypeCreator.InitializationData(
                        arguments = listOf(self, argType),
                        returnValue = returnType
                    )
                }
            ),
            supertypes = emptyList()
        )
    }

fun createUnaryProtocolWithCustomReturn(methodName: String, returnType: Type): Type =
    createPythonProtocol(
        Name(emptyList(), ""),  // TODO: normal names?
        0,
        listOf(methodName),
        listOf(methodName)
    ) { self ->
        CompositeTypeCreator.InitializationData(
            members = listOf(
                createPythonCallableType(
                    0,
                    listOf(PythonCallableTypeDescription.ArgKind.Positional),
                    listOf("self"),
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