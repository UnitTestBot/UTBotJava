package org.utbot.python.newtyping.ast.visitor.hints

import org.utbot.python.newtyping.*
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

enum class ComparisonSign(val method: String) {
    Lt("__lt__"),
    Le("__le__"),
    Eq("__eq__"),
    Ne("__ne__"),
    Gt("__gt__"),
    Ge("__ge__")
}

fun getComparison(op: String): ComparisonSign? =
    when (op) {
        "<" -> ComparisonSign.Lt
        "<=" -> ComparisonSign.Le
        "==" -> ComparisonSign.Eq
        "!=" -> ComparisonSign.Ne
        ">" -> ComparisonSign.Gt
        ">=" -> ComparisonSign.Ge
        else -> null
    }

fun reverseComparison(comp: ComparisonSign): ComparisonSign =
    when (comp) {
        ComparisonSign.Lt -> ComparisonSign.Gt
        ComparisonSign.Le -> ComparisonSign.Ge
        ComparisonSign.Eq -> ComparisonSign.Eq
        ComparisonSign.Ne -> ComparisonSign.Ne
        ComparisonSign.Gt -> ComparisonSign.Lt
        ComparisonSign.Ge -> ComparisonSign.Le
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

fun createProtocolWithAttribute(attributeName: String, attributeType: Type): Type =
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

fun createBinaryProtocol(methodName: String, argType: Type, returnType: Type): Type =
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

fun createUnaryProtocolWithCustomReturn(methodName: String, returnType: Type): Type =
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

fun createCallableProtocol(argBounds: List<Type>, returnBound: Type): Type =
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