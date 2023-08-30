package org.utbot.python.newtyping.ast.visitor.hints

import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.CompositeTypeCreator
import org.utbot.python.newtyping.general.FunctionTypeCreator
import org.utbot.python.newtyping.general.Name
import org.utbot.python.newtyping.general.UtType

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