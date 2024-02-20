package org.utbot.python.newtyping.ast

import org.parsers.python.PythonConstants
import org.parsers.python.ast.NumericalLiteral
import org.utpython.types.PythonTypeHintsStorage
import org.utpython.types.general.UtType
import org.utpython.types.pythonAnyType

fun typeOfNumericalLiteral(node: NumericalLiteral, storage: PythonTypeHintsStorage): UtType =
    when (node.type) {
        PythonConstants.TokenType.DECNUMBER,
        PythonConstants.TokenType.HEXNUMBER,
        PythonConstants.TokenType.OCTNUMBER -> storage.pythonInt
        PythonConstants.TokenType.FLOAT -> storage.pythonFloat
        PythonConstants.TokenType.COMPLEX -> storage.pythonComplex
        else -> pythonAnyType
    }