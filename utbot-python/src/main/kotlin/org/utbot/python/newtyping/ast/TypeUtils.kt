package org.utbot.python.newtyping.ast

import org.parsers.python.PythonConstants
import org.parsers.python.ast.NumericalLiteral
import org.utbot.python.newtyping.PythonTypeStorage
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.pythonAnyType

fun typeOfNumericalLiteral(node: NumericalLiteral, storage: PythonTypeStorage): UtType =
    when (node.type) {
        PythonConstants.TokenType.DECNUMBER,
        PythonConstants.TokenType.HEXNUMBER,
        PythonConstants.TokenType.OCTNUMBER -> storage.pythonInt
        PythonConstants.TokenType.FLOAT -> storage.pythonFloat
        PythonConstants.TokenType.COMPLEX -> storage.pythonComplex
        else -> pythonAnyType
    }