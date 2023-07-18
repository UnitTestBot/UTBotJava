package org.utbot.summary

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.ArrayCreationExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.CastExpr
import com.github.javaparser.ast.expr.CharLiteralExpr
import com.github.javaparser.ast.expr.ClassExpr
import com.github.javaparser.ast.expr.ConditionalExpr
import com.github.javaparser.ast.expr.DoubleLiteralExpr
import com.github.javaparser.ast.expr.EnclosedExpr
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.InstanceOfExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.NullLiteralExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.stmt.CatchClause
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.SwitchEntry
import com.github.javaparser.ast.stmt.SwitchStmt
import com.github.javaparser.ast.stmt.ThrowStmt
import com.github.javaparser.ast.stmt.WhileStmt
import org.utbot.framework.plugin.api.Step
import org.utbot.summary.SummarySentenceConstants.SEMI_COLON_SYMBOL
import org.utbot.summary.ast.JimpleToASTMap
import org.utbot.summary.comment.getTextIterationDescription
import soot.jimple.internal.JIfStmt
import soot.jimple.internal.JLookupSwitchStmt
import soot.jimple.internal.JReturnStmt
import soot.jimple.internal.JTableSwitchStmt
import kotlin.jvm.optionals.getOrNull


private const val STATEMENT_DECISION_TRUE = 1
private const val STATEMENT_DECISION_FALSE = 0

class NodeConverter {

    companion object {
        /**
         * Converts ASTNode into String
         * @return String that can be a javadoc
         */
        fun convertNodeToString(ASTNode: Node, step: Step): String? {
            var res = ""
            var node = ASTNode
            if (node is EnclosedExpr) node = JimpleToASTMap.unEncloseExpr(node)
            convertNodeToStringRecursively(node, step)?.let {
                res += it
            }
            if (step.decision == STATEMENT_DECISION_TRUE) {
                if (node is BooleanLiteralExpr
                    || node is NameExpr
                    || node is MethodCallExpr
                    || node is CastExpr
                    || node is FieldAccessExpr
                    || node is InstanceOfExpr
                    || node is ArrayAccessExpr
                ) res = "Not$res"
                else if (node is UnaryExpr && node.operator == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
                    res =
                        res.removePrefix(convertUnaryOperator(UnaryExpr.Operator.LOGICAL_COMPLEMENT)) // double negative expression is positive expression
                }
            }
            return res.ifEmpty { null }
        }

        /**
         * Used in a conversion of Node into javadoc String
         */
        private fun convertNodeToStringRecursively(ASTNode: Node, step: Step): String? {
            val res = when (ASTNode) {
                is EnclosedExpr -> convertNodeToStringRecursively(JimpleToASTMap.unEncloseExpr(ASTNode), step)
                is BinaryExpr -> convertBinaryExpr(ASTNode, step)
                is UnaryExpr ->
                    convertUnaryOperator(ASTNode.operator) +
                            convertNodeToStringRecursively(ASTNode.expression, step)
                is NameExpr -> "${ASTNode.name}"
                is LiteralExpr -> convertLiteralExpression(ASTNode)
                is ArrayAccessExpr -> "${ASTNode.index.toString().capitalize()}Of${
                    ASTNode.name.toString().capitalize()
                }"
                is FieldAccessExpr -> {
                    if (ASTNode.scope is FieldAccessExpr) "${
                        convertNodeToStringRecursively(
                            ASTNode.scope,
                            step
                        )
                    }${ASTNode.name.toString().capitalize()}"
                    else "${ASTNode.scope.toString().capitalize()}${ASTNode.name.toString().capitalize()}"
                }
                is CastExpr -> ASTNode.expression.toString().capitalize()
                is MethodCallExpr -> {
                    if (ASTNode.scope.isPresent) "${
                        ASTNode.scope.get().toString().capitalize()
                    }${ASTNode.name.toString().capitalize()}"
                    else ASTNode.name.toString().capitalize()
                }
                is InstanceOfExpr -> {
                    if (step.decision == STATEMENT_DECISION_TRUE) "${
                        ASTNode.expression.toString().capitalize()
                    }NotInstanceOf${ASTNode.type.toString().capitalize()}"
                    else "${ASTNode.expression.toString().capitalize()}InstanceOf${
                        ASTNode.type.toString().capitalize()
                    }"
                }
                is ClassExpr -> "${ASTNode.type}Class"
                is ArrayCreationExpr -> "NewArrayOf${ASTNode.createdType().toString().capitalize()}"
                is ReturnStmt -> {
                    if (ASTNode.expression.isPresent) convertNodeToStringRecursively(
                        ASTNode.expression.get(),
                        step
                    )
                    else ""
                }
                is ConditionalExpr -> "${convertNodeToStringRecursively(ASTNode.condition, step)}"
                is VariableDeclarationExpr -> ASTNode.variables.joinToString(separator = "") { variable ->
                    convertNodeToStringRecursively(
                        variable,
                        step
                    ) ?: ""
                }

                is VariableDeclarator -> {
                    val initializer = ASTNode.initializer
                    if (initializer.isPresent) "${ASTNode.type.toString().capitalize()}${
                        ASTNode.name.toString().capitalize()
                    }InitializedBy${convertNodeToStringRecursively(ASTNode.initializer.get(), step)}"
                    else "${ASTNode.type.toString().capitalize()}${ASTNode.name.toString().capitalize()}IsInitialized"
                }
                is CatchClause -> ASTNode.parameter.type.toString()
                    .capitalize() //add ${ASTNode.parameter.name.toString().capitalize() to print variable name

                is WhileStmt -> convertNodeToStringRecursively(ASTNode.condition, step)
                is IfStmt -> convertNodeToStringRecursively(ASTNode.condition, step)
                is SwitchEntry -> convertSwitchEntry0(ASTNode, step, removeSpaces = true)
                is ThrowStmt -> "Throws${ASTNode.expression.toString().removePrefix("new").capitalize()}"
                is SwitchStmt -> convertSwitchStmt0(ASTNode, step, removeSpaces = true)
                is ExpressionStmt -> convertNodeToStringRecursively(ASTNode.expression, step)

                else -> {
                    null
                }
            } ?: return null
            return postProcessName(res)
        }

        /**
         * Converts ASTNode into String
         * @return String that can be a DisplayName
         */
        fun convertNodeToDisplayNameString(ASTNode: Node, step: Step): String {
            var node = ASTNode
            if (node is ExpressionStmt) node = node.expression
            if (node is EnclosedExpr) node = JimpleToASTMap.unEncloseExpr(node)
            var res = convertNodeToDisplayNameStringRecursively(node, step)
            if (node is ReturnStmt) node = node.expression.getOrNull() ?: node
            if (step.stmt is JReturnStmt) return res
            if (nodeContainsBooleanCondition(node)) {
                res += if (step.decision == STATEMENT_DECISION_TRUE) {
                    " : False"
                } else {
                    " : True"
                }
            }
            return res
        }

        /**
         * Checks if node contain any boolean condition
         */
        private fun nodeContainsBooleanCondition(node: Node): Boolean {
            if (node is ArrayAccessExpr
                || node is FieldAccessExpr
                || node is CastExpr
                || node is NameExpr
                || node is BinaryExpr
                || node is MethodCallExpr
                || node is InstanceOfExpr
                || node is UnaryExpr
                || node is BooleanLiteralExpr
                || node is VariableDeclarationExpr && node.variables.any { nodeContainsBooleanCondition(it) }
            ) return true
            if (node is VariableDeclarator) {
                val initializer = node.initializer.getOrNull()
                if (initializer != null) {
                    return nodeContainsBooleanCondition(initializer)
                }
            }
            return false
        }

        /**
         * Used in a conversion of Node into DisplayName String
         */
        private fun convertNodeToDisplayNameStringRecursively(ASTNode: Node, step: Step): String {
            val res = when (ASTNode) {
                is EnclosedExpr -> convertNodeToDisplayNameStringRecursively(
                    JimpleToASTMap.unEncloseExpr(ASTNode),
                    step
                )
                is WhileStmt -> getTextIterationDescription(ASTNode)
                is ForStmt -> getTextIterationDescription(ASTNode)
                is ForEachStmt -> getTextIterationDescription(ASTNode)
                is IfStmt -> convertNodeToDisplayNameStringRecursively(ASTNode.condition, step)
                is SwitchEntry -> convertSwitchEntry0(ASTNode, step, removeSpaces = false)
                is ThrowStmt -> "Throws ${ASTNode.expression.toString().removePrefix("new").capitalize()}"
                is SwitchStmt -> convertSwitchStmt0(ASTNode, step, removeSpaces = false)
                is ExpressionStmt -> convertNodeToDisplayNameStringRecursively(ASTNode.expression, step)
                is VariableDeclarationExpr -> ASTNode.variables.joinToString(separator = " ") { variable ->
                    convertNodeToDisplayNameStringRecursively(
                        variable,
                        step
                    )
                }
                else -> {
                    ASTNode.toString()
                }
            }
            return displayNamePostprocessor(res)
        }

        /**
         * Replaces one+ whitespaces with one whitespace
         */
        private fun displayNamePostprocessor(displayName: String) =
            displayName.replace("\\s+".toRegex(), " ").replace("$SEMI_COLON_SYMBOL", "")

        private fun convertBinaryExpr(binaryExpr: BinaryExpr, step: Step): String {
            var res = ""
            val left = convertNodeToStringRecursively(binaryExpr.left, step)
            if (left != null) res += left.capitalize()
            val stmt = step.stmt
            res += if (stmt is JIfStmt) {
                convertBinaryOperator(
                    binaryExpr.operator,
                    JimpleToASTMap.isOperatorReversed(stmt, binaryExpr),
                    step.decision
                ).capitalize()
            } else {
                convertBinaryOperator(binaryExpr.operator, false, step.decision).capitalize()
            }
            val right = convertNodeToStringRecursively(binaryExpr.right, step)
            if (right != null) res += right.capitalize()
            return res
        }

        fun convertSwitchStmt0(switchStmt: SwitchStmt, step: Step, removeSpaces: Boolean = true): String =
            convertSwitchLabel(switchStmt, step)
                ?.let { label ->
                    val selector = switchStmt.selector.toString()
                    formatSwitchLabel(label, selector, removeSpaces)
                }
                ?: convertToRawSwitchStmt(switchStmt)

        fun convertSwitchEntry0(switchEntry: SwitchEntry, step: Step, removeSpaces: Boolean = true): String =
            (switchEntry.parentNode.getOrNull() as? SwitchStmt)
                ?.let { switchStmt ->
                    val label = convertSwitchLabel(switchStmt, step) ?: convertSwitchEntry(switchEntry)
                    val selector = switchStmt.selector.toString()
                    formatSwitchLabel(label, selector, removeSpaces)
                }
                ?: switchEntry.toString()

        private fun convertToRawSwitchStmt(switchStmt: SwitchStmt): String =
            "switch(${switchStmt.selector})"

        private fun convertSwitchEntry(node: SwitchEntry): String {
            val case = node.labels.first
            return if (case.isPresent) "${case.get()}" else "default"
        }

        private fun formatSwitchLabel(label: String, selector: String, removeSpaces: Boolean = true): String {
            return if (removeSpaces) "Switch${selector.capitalize()}Case" + label.replace(" ", "")
            else "switch($selector) case: " + label
        }

        private fun convertSwitchLabel(switchStmt: SwitchStmt, step: Step): String? =
            when (val stmt = step.stmt) {
                is JLookupSwitchStmt -> {
                    val lookup = stmt.lookupValues
                    val case =
                        if (step.decision >= lookup.size) null
                        else lookup[step.decision].value

                    JimpleToASTMap.getSwitchCaseLabel(switchStmt, case)
                }

                is JTableSwitchStmt -> {
                    JimpleToASTMap
                        .mapSwitchCase(switchStmt, step)
                        ?.let { convertSwitchEntry(it) }
                }

                else -> null
            }

        /**
         * Converts literal into String
         */
        private fun convertLiteralExpression(literal: LiteralExpr): String {
            val res = when (literal) {
                is StringLiteralExpr -> literal.asString()
                is CharLiteralExpr -> if (isLegitSymbolForFunctionName(literal.asChar())) "${literal.asChar()}" else "Char"
                is DoubleLiteralExpr -> {
                    when (val literalAsDouble = literal.asDouble()) {
                        Double.NaN -> "NaN"
                        Double.MIN_VALUE -> "MinValue"
                        Double.MAX_VALUE -> "MaxValue"
                        Double.NEGATIVE_INFINITY -> "NegativeInfinity"
                        Double.POSITIVE_INFINITY -> "Infinity"
                        else -> {
                            when {
                                literalAsDouble != literalAsDouble -> "NaN"
                                literalAsDouble < 0 -> "Negative${literal.asDouble().toInt()}d"
                                literalAsDouble == 0.0 -> "Zero"
                                literalAsDouble == -0.0 -> "Zero"
                                else -> "${
                                    literal.asDouble().toInt()
                                }d" //seems kinda wrong, . can be replaced with "dot" or something else
                            }
                        }
                    }
                }
                is IntegerLiteralExpr -> {
                    var str = ""
                    if (literal.asNumber().toInt() < 0) str += "Negative"
                    str += if (literal.asNumber().toInt() == 0) "Zero" else "$literal"
                    str
                }
                is LongLiteralExpr -> {
                    var str = ""
                    if (literal.asNumber().toLong() < 0) str += "Negative"
                    str += if (literal.asNumber().toInt() == 0) "Zero" else "$literal"
                    str
                }
                is BooleanLiteralExpr -> literal.toString()
                is NullLiteralExpr -> "Null"
                else -> ""
            }
            return res.capitalize()
        }

        /**
         * Checks if symbol can be used in function name
         * @see <a href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.8">Java documentation: Identifiers</a>
         */
        private fun isLegitSymbolForFunctionName(ch: Char): Boolean {
            return (ch in '0'..'9'
                    || ch in 'a'..'z'
                    || ch in 'A'..'Z'
                    || ch == '_'
                    || ch == '$'
                    )
        }

        /**
         * Capitalizes method name.
         *
         * It splits the text by delimiters, capitalizes each part, removes special characters and concatenates result.
         */
        private fun postProcessName(name: String) =
            name.split(".", "(", ")", ",")
                .joinToString("") { it -> it.capitalize().filter { isLegitSymbolForFunctionName(it) } }

        /**
         * Converts Javaparser BinaryOperator and all of its children into a String
         */
        private fun convertBinaryOperator(
            binaryOperator: BinaryExpr.Operator,
            isOperatorReversed: Boolean,
            decision: Int
        ): String {
            var operator = binaryOperator
            if ((isOperatorReversed && decision == STATEMENT_DECISION_TRUE) || (!isOperatorReversed && decision == STATEMENT_DECISION_FALSE)) {
                operator = reverseBinaryOperator(operator) ?: binaryOperator
            }
            return when (operator) {
                BinaryExpr.Operator.OR -> "Or"
                BinaryExpr.Operator.AND -> "And"
                BinaryExpr.Operator.BINARY_OR -> "BitwiseOr"
                BinaryExpr.Operator.BINARY_AND -> "BitwiseAnd"
                BinaryExpr.Operator.XOR -> "Xor"
                BinaryExpr.Operator.EQUALS -> "Equals"
                BinaryExpr.Operator.NOT_EQUALS -> "NotEquals"
                BinaryExpr.Operator.LESS -> "LessThan"
                BinaryExpr.Operator.GREATER -> "GreaterThan"
                BinaryExpr.Operator.LESS_EQUALS -> "LessOrEqual"
                BinaryExpr.Operator.GREATER_EQUALS -> "GreaterOrEqual"
                BinaryExpr.Operator.LEFT_SHIFT -> "LeftShift"
                BinaryExpr.Operator.SIGNED_RIGHT_SHIFT -> "RightShift"
                BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT -> "UnsignedRightShift"
                BinaryExpr.Operator.PLUS -> "Plus"
                BinaryExpr.Operator.MINUS -> "Minus"
                BinaryExpr.Operator.MULTIPLY -> "Multiply"
                BinaryExpr.Operator.DIVIDE -> "Divide"
                BinaryExpr.Operator.REMAINDER -> "RemainderOf" //does it sounds strange? or is it ok
            }
        }

        /**
         * Reverts Javaparser binary operator if possible
         */
        private fun reverseBinaryOperator(operator: BinaryExpr.Operator) = when (operator) {
            BinaryExpr.Operator.EQUALS -> BinaryExpr.Operator.NOT_EQUALS
            BinaryExpr.Operator.NOT_EQUALS -> BinaryExpr.Operator.EQUALS
            BinaryExpr.Operator.LESS -> BinaryExpr.Operator.GREATER_EQUALS
            BinaryExpr.Operator.GREATER -> BinaryExpr.Operator.LESS_EQUALS
            BinaryExpr.Operator.LESS_EQUALS -> BinaryExpr.Operator.GREATER
            BinaryExpr.Operator.GREATER_EQUALS -> BinaryExpr.Operator.LESS
            else -> null
        }

        /**
         * Converts Javaparser unary operator to String
         */
        private fun convertUnaryOperator(unaryOperator: UnaryExpr.Operator) = when (unaryOperator) {
            UnaryExpr.Operator.PLUS -> "Plus"
            UnaryExpr.Operator.MINUS -> "Negative"
            UnaryExpr.Operator.PREFIX_INCREMENT -> "PrefixIncrement"
            UnaryExpr.Operator.PREFIX_DECREMENT -> "PrefixDecrement"
            UnaryExpr.Operator.LOGICAL_COMPLEMENT -> "Not" //! or LogicalComplement
            UnaryExpr.Operator.BITWISE_COMPLEMENT -> "BitwiseComplement"
            UnaryExpr.Operator.POSTFIX_INCREMENT -> "PostfixIncrement"
            UnaryExpr.Operator.POSTFIX_DECREMENT -> "PostfixDecrement"
        }
    }
}