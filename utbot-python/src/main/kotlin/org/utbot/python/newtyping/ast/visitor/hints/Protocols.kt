package org.utbot.python.newtyping.ast.visitor.hints

import org.utbot.python.newtyping.PythonCallableTypeDescription
import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.createPythonCallableType
import org.utbot.python.newtyping.createPythonProtocol
import org.utbot.python.newtyping.general.CompositeTypeCreator
import org.utbot.python.newtyping.general.FunctionTypeCreator
import org.utbot.python.newtyping.general.Name
import org.utbot.python.newtyping.general.Type

enum class Operation(val method: String) {
    Add("__add__"),
    Sub("__sub__"),
    Mul("__mul__"),
    TrueDiv("__truediv__"),
    FloorDiv("__floordiv__"),
    Mod("__mod__"),
    Pow("__pow__"),
    MatMul("__matmul__"),
    And("__and__")
}

fun getOperationOfOperator(op: String): Operation? =
    when (op) {
        "+" -> Operation.Add
        "-" -> Operation.Sub
        "*" -> Operation.Mul
        "/" -> Operation.TrueDiv
        "//" -> Operation.FloorDiv
        "%" -> Operation.Mod
        "**" -> Operation.Pow
        "@" -> Operation.MatMul
        "&" -> Operation.And
        else -> null
    }

fun getOperationOfOpAssign(op: String): Operation? {
    if (op.last() != '=')
        return null
    return getOperationOfOperator(op.dropLast(1))
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