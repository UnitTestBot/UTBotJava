package org.utbot.python.newtyping

import org.utbot.python.newtyping.general.CompositeTypeCreator
import org.utbot.python.newtyping.general.FunctionTypeCreator
import org.utbot.python.newtyping.general.Name
import org.utbot.python.newtyping.general.UtType


fun createIterableWithCustomReturn(returnType: UtType): UtType =
    createUnaryProtocol("__iter__", returnType)

fun supportsBoolProtocol(storage: PythonTypeHintsStorage): UtType =
    createUnaryProtocol("__bool__", storage.pythonBool)

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


fun createProtocolWithFunction(methodName: String, argTypes: List<UtType>, returnType: UtType): UtType =
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
                    List(argTypes.size + 1) { PythonCallableTypeDescription.ArgKind.ARG_POS },
                    listOf("self") + List(argTypes.size) { "" }
                ) {
                    FunctionTypeCreator.InitializationData(
                        arguments = listOf(self) + argTypes,
                        returnValue = returnType
                    )
                }
            ),
            supertypes = emptyList()
        )
    }

fun createBinaryProtocol(methodName: String, argType: UtType, returnType: UtType): UtType =
    createProtocolWithFunction(methodName, listOf(argType), returnType)

fun createUnaryProtocol(methodName: String, returnType: UtType): UtType =
    createProtocolWithFunction(methodName, emptyList(), returnType)

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