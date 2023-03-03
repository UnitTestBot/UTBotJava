package org.utbot.summary.ast

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.CastExpr
import com.github.javaparser.ast.expr.CharLiteralExpr
import com.github.javaparser.ast.expr.ConditionalExpr
import com.github.javaparser.ast.expr.EnclosedExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.InstanceOfExpr
import com.github.javaparser.ast.expr.LiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.CatchClause
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.stmt.SwitchEntry
import com.github.javaparser.ast.stmt.SwitchStmt
import com.github.javaparser.ast.stmt.WhileStmt
import org.utbot.framework.plugin.api.Step
import org.utbot.summary.comment.isLoopStatement
import java.util.LinkedList
import java.util.Queue
import kotlin.Int.Companion.MAX_VALUE
import kotlin.math.abs
import kotlin.streams.toList
import soot.Unit
import soot.Value
import soot.jimple.internal.JCaughtExceptionRef
import soot.jimple.internal.JEqExpr
import soot.jimple.internal.JGeExpr
import soot.jimple.internal.JGotoStmt
import soot.jimple.internal.JGtExpr
import soot.jimple.internal.JIdentityStmt
import soot.jimple.internal.JIfStmt
import soot.jimple.internal.JLeExpr
import soot.jimple.internal.JLtExpr
import soot.jimple.internal.JNeExpr
import soot.jimple.internal.JReturnStmt

class JimpleToASTMap(stmts: Iterable<Unit>, methodDeclaration: MethodDeclaration) {
    val stmtToASTNode = mutableMapOf<Unit, Node?>()

    init {
        removeComments(methodDeclaration)
        mapTernaryConditional(methodDeclaration, stmts)
        val ifStmtToNodeMap = createIfStmtToASTNodesMap(methodDeclaration)
        for (stmt in stmts) {
            val line = stmt.javaSourceStartLineNumber
            if (line == -1) {
                stmtToASTNode[stmt] = methodDeclaration
            } else if (stmt !in stmtToASTNode.keys) {
                var ASTNode = getASTNodeByLine(methodDeclaration, line)

                if (ASTNode != null) {
                    if (ASTNode is IfStmt && stmt is JIfStmt) {
                        val nodes = ifStmtToNodeMap[ASTNode]
                        if(!nodes.isNullOrEmpty()) ASTNode = nodes.remove()
                    } else if (stmt is JReturnStmt) {
                        ASTNode = validateReturnASTNode(ASTNode)
                    }
                }
                stmtToASTNode[stmt] = ASTNode
            }
        }
        remapMultilineLoops(methodDeclaration)
        addCatchASTNodes()
        alignNonAlignedReturns()
    }

    /**
     * Function that maps statements inside of ternary conditions to correct AST nodes
     */
    private fun mapTernaryConditional(methodDeclaration: MethodDeclaration, stmts: Iterable<Unit>) {
        for (condExpr in methodDeclaration.stream().toList().filterIsInstance<ConditionalExpr>()) {
            val begin = condExpr.begin.orElse(null)
            val end = condExpr.end.orElse(null)
            if (begin == null || end == null) continue
            val lines = begin.line..end.line
            val allStmtsInRange = stmts.filter { it.javaSourceStartLineNumber in lines }
            val lastJGotoStmt = allStmtsInRange.lastOrNull { it is JGotoStmt } ?: return
            val lastJGotoStmtTarget = (lastJGotoStmt as JGotoStmt).target
            val founded = stmts.find { it == lastJGotoStmtTarget }
            val conditionalStmtsInRange = if (founded !in allStmtsInRange) allStmtsInRange else allStmtsInRange.slice(
                0 until allStmtsInRange.indexOf(founded)
            )
            val queue = LinkedList<Node>()
            var currentCond = condExpr
            while (true) {
                queue.add(currentCond.condition)
                queue.add(currentCond.thenExpr)
                val elseExpr = currentCond.elseExpr
                if (elseExpr is ConditionalExpr) {
                    currentCond = elseExpr
                } else {
                    queue.add(elseExpr)
                    break
                }
            }
            var currNode = queue.removeLast()
            for (stmt in conditionalStmtsInRange.reversed()) {
                if (stmt is JGotoStmt || stmt is JIfStmt) {
                    if (queue.isEmpty()) {
                        stmtToASTNode[stmt] = currNode
                        break
                    }
                    currNode = queue.removeLast()
                }
                stmtToASTNode[stmt] = currNode
            }
        }
    }

    /**
     * Node is valid if there is only one return statement inside of it
     */
    private fun validateReturnASTNode(returnNode: Node): Node {
        val returns = returnNode.stream().filter { it is ReturnStmt }.toList()
        if (returns.size == 1) return returns[0]
        return returnNode
    }

    /**
     * Removes all comments from AST
     */
    private fun removeComments(node: Node) {
        for (comment in node.allContainedComments) {
            comment.remove()
        }
    }

    /**
     * After mapping, multiline loops are mapped in a wrong way
     * So initialization update and condition are mapped all over again
     */
    private fun remapMultilineLoops( methodDeclaration: MethodDeclaration) {
        val forLoops = methodDeclaration.stream().filter { it is ForStmt || it is WhileStmt || it is ForEachStmt }
        for (loop in forLoops) {
            val loopList = mutableListOf<Node>()
            when (loop) {
                is ForStmt -> {
                    loopList.addAll(loop.initialization.stream().toList())
                    val compare = loop.compare.orElse(null)?.stream()?.toList()
                    if (compare != null) loopList.addAll(compare)
                    loopList.addAll(loop.update.flatMap { it.stream().toList() })
                }
                is WhileStmt -> {
                    loopList.addAll(loop.condition.stream().toList())
                }
                is ForEachStmt -> {
                    loopList.addAll(loop.iterable.stream().toList())
                    loopList.addAll(loop.variable.stream().toList())
                }
            }
            for (stmt in stmtToASTNode.filter { it.value in loopList }.map { it.key }) stmtToASTNode[stmt] = loop
        }

    }
    private fun addCatchASTNodes() {
        val stmts = stmtToASTNode.keys.toTypedArray()
        for (index in stmts.indices) {
            val stmt = stmts[index]
            if (stmt is JIdentityStmt && stmt.rightOp is JCaughtExceptionRef) {
                if (index + 1 < stmts.size) {
                    val nextASTNode = stmtToASTNode[stmts[index + 1]]
                    if (nextASTNode != null) {
                        val nearestCatchClause = getNearestCatchClauseASTNode(nextASTNode)
                        if (nearestCatchClause != null) stmtToASTNode[stmts[index]] = nearestCatchClause
                    }
                }
            }
        }
    }

    /**
     * Aligns returns that are inside of ternary condition expr
     * it is needed bc such returns have -1 line in jimple so mapping by line doesn't work
     */
    private fun alignNonAlignedReturns() {
        val nonAlignedReturns = stmtToASTNode.keys.filter { it is JReturnStmt && it.javaSourceStartLineNumber == -1 }
        val stmts = stmtToASTNode.keys.toTypedArray()
        for (nonAlignedReturn in nonAlignedReturns) {
            val index = stmts.indexOf(nonAlignedReturn)
            val ternaryIfStmtIndex = stmts.indexOfLast { it is JIfStmt && stmts.indexOf(it) <= index }

            stmtToASTNode[nonAlignedReturn] = stmtToASTNode[stmts[ternaryIfStmtIndex]]
        }
    }

    private fun isValidExprType(ex: Node): Boolean {
        if (ex is NameExpr
            || ex is LiteralExpr
            || ex is ArrayAccessExpr
            || ex is FieldAccessExpr
            || ex is CastExpr
            || ex is BinaryExpr && ex.operator in doNotSplitOperators
            || ex is MethodCallExpr
            || ex is InstanceOfExpr
            || ex is UnaryExpr && ex.operator in doNotSplitOperators
        ) return true
        return false
    }

    private fun addASTNodesForElseStmts(ifStmtsToASTNodes: MutableMap<IfStmt, Queue<Node>>): MutableMap<IfStmt, Queue<Node>> {
        val ifStmts = ifStmtsToASTNodes.keys
        for (ifStmt in ifStmts) {
            if (ifStmt.elseStmt.isPresent) {
                val elseStmt = ifStmt.elseStmt.orElse(null)
                if (elseStmt is IfStmt) {
                    val elseConditions = ifStmtsToASTNodes[elseStmt]
                    if (elseConditions != null) ifStmtsToASTNodes[ifStmt]?.addAll(elseConditions)
                }
            }
        }
        return ifStmtsToASTNodes
    }

    private fun createIfStmtToASTNodesMap(rootNode: Node): MutableMap<IfStmt, Queue<Node>> {
        val ifStmtToASTNodes = mutableMapOf<IfStmt, Queue<Node>>()
        for (childNode in rootNode.stream()) {
            if (childNode is IfStmt) ifStmtToASTNodes[childNode] = decomposeConditionNode(childNode.condition)
        }
        return addASTNodesForElseStmts(ifStmtToASTNodes)
    }

    /**
     * Decomposes condition Node so it can be mapped to jimple stmts
     */
    private fun decomposeConditionNode(ASTNode: Node): Queue<Node> {
        val decomposed = LinkedList<Node>()
        val node = if (ASTNode is EnclosedExpr) unEncloseExpr(ASTNode) else ASTNode

        if (node is BinaryExpr) {
            var left = node.left
            var right = node.right
            if (left is EnclosedExpr) left = unEncloseExpr(left)
            if (right is EnclosedExpr) right = unEncloseExpr(right)

            if (isValidExprType(left) && isValidExprType(right)) {
                decomposed.add(node)
                return decomposed
            }
        }
        if (isValidExprType(node) || node is UnaryExpr) {
            decomposed.add(node)
        } else {
            for (child in node.childNodes) decomposed.addAll(decomposeConditionNode(child))
        }
        return decomposed
    }

    private fun getASTNodeByLine(node: Node, lineNumber: Int): Node? {
        val begin = node.begin.orElse(null)
        if (begin?.line == lineNumber) {
            return node
        } else {
            for (childNode in node.childNodes) {
                val childStmtToNode = getASTNodeByLine(childNode, lineNumber)
                if (childStmtToNode != null) return childStmtToNode
            }
        }
        return null
    }

    fun nearestIterationNode(node: Node?, line: Int): Statement? {
        if (node == null) return null
        val nodes = node.childNodes.toMutableList()
        if (node is MethodDeclaration && node.body.isPresent) {
            val body = node.body.orElse(null)
            if (body != null) nodes.addAll(body.childNodes)
        }
        if (node.hasParentNode()) {
            val parentNode = node.parentNode.orElse(null)
            if (parentNode != null) nodes.add(parentNode)
        }
        val nearbyLoops = nodes.filterIsInstance<Statement>().filter { isLoopStatement(it) }
        return nearbyLoops.minByOrNull {
            val beginLine = it.begin.orElse(null)?.line
            if (beginLine != null) abs(beginLine - line) else MAX_VALUE
        }

    }

    /**
     * Returns parent or grandparent node which is a CatchClause
     */
    private fun getNearestCatchClauseASTNode(node: Node): CatchClause? {
        val nodeParent = node.parentNode.orElse(null)
        if (nodeParent != null && nodeParent is BlockStmt) {
            val nodeGrandParent = nodeParent.parentNode.orElse(null)
            if (nodeGrandParent != null && nodeGrandParent is CatchClause) {
                return nodeGrandParent
            }
        }
        return null
    }

    operator fun get(unit: Unit) = stmtToASTNode[unit]

    companion object {
        /**
         * Operators where left and right statements can not be BinaryStmt
         */
        val doNotSplitOperators = arrayOf(
            BinaryExpr.Operator.LEFT_SHIFT,
            BinaryExpr.Operator.SIGNED_RIGHT_SHIFT,
            BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT,
            BinaryExpr.Operator.PLUS,
            BinaryExpr.Operator.MINUS,
            BinaryExpr.Operator.MULTIPLY,
            BinaryExpr.Operator.DIVIDE,
            BinaryExpr.Operator.REMAINDER,
            BinaryExpr.Operator.BINARY_AND,
            BinaryExpr.Operator.BINARY_OR,

            UnaryExpr.Operator.PLUS,
            UnaryExpr.Operator.MINUS,
            UnaryExpr.Operator.PREFIX_DECREMENT,
            UnaryExpr.Operator.PREFIX_INCREMENT,
            UnaryExpr.Operator.POSTFIX_DECREMENT,
            UnaryExpr.Operator.POSTFIX_INCREMENT,
            UnaryExpr.Operator.BITWISE_COMPLEMENT
        )

        /**
         * @return expression inside of EnclosedExpr
         */
        fun unEncloseExpr(expr: EnclosedExpr): Expression {
            val unEnclosed = expr.inner
            return if (unEnclosed is EnclosedExpr) unEncloseExpr(unEnclosed)
            else unEnclosed
        }

        /**
         * @return SwitchEntry
         */
        fun mapSwitchCase(switchStmt: SwitchStmt, step: Step): SwitchEntry? {
            val neededLine = step.stmt.unitBoxes[step.decision].unit.javaSourceStartLineNumber
            return switchStmt
                .childNodes
                .filterIsInstance<SwitchEntry>()
                .find { neededLine in (it.range.get().begin.line..it.range.get().end.line) }
        }

        /**
         * @return switch case label №case from the beginning
         */
        fun getSwitchCaseLabel(switchStmt: SwitchStmt, case: Int?): String {
            val switchStmts = switchStmt.childNodes.filterIsInstance<SwitchEntry>()
            val switchCases = switchStmts.mapNotNull {
                val label = it.labels.first.orElse(null)
                if (label != null) {
                    transformSwitchEntry(label) to it
                } else {
                    null
                }
            }.toMap()
            val entry = switchCases[case.toString()]?.labels?.first?.orElse(null)
            return entry?.toString() ?: "default"
        }

        private fun transformSwitchEntry(expr: Expression): String {
            if (expr is CharLiteralExpr) return expr.asChar().toInt().toString()
            return expr.toString()
        }

        /**
         * Maps Jimple operator to AST operator
         */
        private fun mapJimpleOperatorsToAstOperators(value: Value) = when (value) {
            is JEqExpr -> BinaryExpr.Operator.EQUALS
            is JGeExpr -> BinaryExpr.Operator.GREATER_EQUALS
            is JGtExpr -> BinaryExpr.Operator.GREATER
            is JLeExpr -> BinaryExpr.Operator.LESS_EQUALS
            is JLtExpr -> BinaryExpr.Operator.LESS
            is JNeExpr -> BinaryExpr.Operator.NOT_EQUALS
            else -> null
        }

        /**
         * Check whether JIfStmt and BinaryExpr.operator are the same or reversed
         */
        fun isOperatorReversed(jimpleIf: JIfStmt, binaryExpr: BinaryExpr): Boolean {
            val mappedJimpleOperator = mapJimpleOperatorsToAstOperators(jimpleIf.conditionBox.value)
            if (mappedJimpleOperator == binaryExpr.operator) return false
            return true
        }
    }
}