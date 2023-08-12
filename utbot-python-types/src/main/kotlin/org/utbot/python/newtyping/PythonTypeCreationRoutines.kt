package org.utbot.python.newtyping

import org.utbot.python.newtyping.general.CompositeTypeCreator
import org.utbot.python.newtyping.general.FunctionTypeCreator
import org.utbot.python.newtyping.general.Name
import org.utbot.python.newtyping.general.UtType


fun createIterableWithCustomReturn(returnType: UtType): UtType =
    createUnaryProtocolWithCustomReturn("__iter__", returnType)

fun supportsBoolProtocol(storage: PythonTypeHintsStorage): UtType =
    createUnaryProtocolWithCustomReturn("__bool__", storage.pythonBool)

fun createProtocolWithAttribute(attributeName: String, attributeType: UtType): UtType =
    createPythonProtocol(
        Name(emptyList(), "Supports_$attributeName"),
        0,
        listOf(PythonVariableDescription(attributeName)),
        listOf(attributeName)
    ) {
        CompositeTypeCreator.InitializationData(
            members = listOf(attributeType),
            supertypes = emptyList()
        )
    }

fun createBinaryProtocol(methodName: String, argType: UtType, returnType: UtType): UtType =
    createPythonProtocol(
        Name(emptyList(), "Supports_$methodName"),
        0,
        listOf(PythonVariableDescription(methodName)),
        listOf(methodName)
    ) { self ->
        CompositeTypeCreator.InitializationData(
            members = listOf(
                createPythonCallableType(
                    0,
                    listOf(PythonCallableTypeDescription.ArgKind.ARG_POS, PythonCallableTypeDescription.ArgKind.ARG_POS),
                    listOf("self", "")
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

fun createUnaryProtocolWithCustomReturn(methodName: String, returnType: UtType): UtType =
    createPythonProtocol(
        Name(emptyList(), "Supports_$methodName"),
        0,
        listOf(PythonVariableDescription(methodName)),
        listOf(methodName)
    ) { self ->
        CompositeTypeCreator.InitializationData(
            members = listOf(
                createPythonCallableType(
                    0,
                    listOf(PythonCallableTypeDescription.ArgKind.ARG_POS),
                    listOf("self")
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

fun createCallableProtocol(argBounds: List<UtType>, returnBound: UtType): UtType =
    createPythonProtocol(
        Name(emptyList(), "SupportsCall"),
        0,
        listOf(PythonVariableDescription("__call__")),
        listOf("__call__")
    ) {
        CompositeTypeCreator.InitializationData(
            members = listOf(
                createPythonCallableType(
                    0,
                    List(argBounds.size) { PythonCallableTypeDescription.ArgKind.ARG_POS },
                    List(argBounds.size) { "" }
                ) {
                    FunctionTypeCreator.InitializationData(argBounds, returnBound)
                }
            ),
            supertypes = emptyList()
        )
    }