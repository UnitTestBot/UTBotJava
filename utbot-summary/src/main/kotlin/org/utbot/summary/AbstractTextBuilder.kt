package org.utbot.summary

import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.stmt.SwitchStmt
import com.github.javaparser.ast.stmt.WhileStmt
import org.utbot.framework.plugin.api.Step
import org.utbot.summary.ast.JimpleToASTMap
import org.utbot.summary.comment.classic.symbolic.IterationDescription
import org.utbot.summary.comment.classic.symbolic.SimpleSentenceBlock
import org.utbot.summary.comment.classic.symbolic.StmtDescription
import org.utbot.summary.comment.classic.symbolic.StmtType
import org.utbot.summary.comment.getTextIterationDescription
import org.utbot.summary.comment.getTextTypeIterationDescription
import org.utbot.summary.comment.numberWithSuffix
import org.utbot.summary.tag.StatementTag
import org.utbot.summary.tag.TraceTagWithoutExecution
import soot.SootMethod
import soot.jimple.Stmt
import soot.jimple.internal.JIfStmt

abstract class AbstractTextBuilder(
    val traceTag: TraceTagWithoutExecution,
    val sootToAST: MutableMap<SootMethod, JimpleToASTMap>
) {


    protected val methodToNoIterationDescription: MutableMap<SootMethod, MutableList<IterationDescription>> =
        mutableMapOf()

    protected fun skippedIterations() {
        methodToNoIterationDescription.clear()
        for ((method, methodAST) in sootToAST) {
            val iterationDescriptions = traceTag.noIterationCall.mapNotNull { stmts ->
                val stmt = stmts.firstOrNull { stmt ->
                    (stmt is JIfStmt) && methodAST[stmt] is Statement
                }
                stmt?.let {
                    val toLine = stmts.maxOf { it.javaSourceStartLineNumber }
                    val fromLine = stmts.minOf { it.javaSourceStartLineNumber }
                    IterationDescription(
                        fromLine,
                        toLine,
                        getTextIterationDescription(methodAST[stmt] as Statement),
                        getTextTypeIterationDescription(methodAST[stmt] as Statement)
                    )
                }
            }.reversed().toMutableList()
            methodToNoIterationDescription[method] = iterationDescriptions
        }
    }


    protected fun textReturn(
        statementTag: StatementTag,
        sentenceBlock: SimpleSentenceBlock,
        stmt: Stmt,
        jimpleToASTMap: JimpleToASTMap
    ) {
        val numberReturn = traceTag.returnsToNumber?.get(stmt) ?: 0
        val prefixReturnText: String
        val returnType: StmtType

        if (numberReturn > 0) {
            val numberReturnWithSuffix = numberWithSuffix(numberReturn)
            prefixReturnText = "$numberReturnWithSuffix return statement: "
            returnType = StmtType.CountedReturn
        } else {
            prefixReturnText = ""
            returnType = StmtType.Return
        }
        jimpleToASTMap[statementTag.step.stmt]?.let {
            sentenceBlock.stmtTexts.add(
                StmtDescription(
                    returnType,
                    it.toString(),
                    prefix = prefixReturnText
                )
            )
        }
    }

    protected fun textSwitchCase(step: Step, jimpleToASTMap: JimpleToASTMap): String? =
        (jimpleToASTMap[step.stmt] as? SwitchStmt)
            ?.let { switchStmt ->
                NodeConverter.convertSwitchStmt0(switchStmt, step, removeSpaces = false)
            }

    protected fun textCondition(statementTag: StatementTag, jimpleToASTMap: JimpleToASTMap): String? {
        var reversed = true
        val jCondition = statementTag.step.stmt
        val astBinaryExpr = jimpleToASTMap[statementTag.step.stmt]
        if (jCondition is JIfStmt && astBinaryExpr is BinaryExpr) {
            reversed = JimpleToASTMap.isOperatorReversed(jCondition, astBinaryExpr)
        }
        // filters
        if (jCondition is JIfStmt
            && (astBinaryExpr is WhileStmt
                    || astBinaryExpr is ForStmt
                    || astBinaryExpr is ForEachStmt)
        ) {
            return null
        }
        if (astBinaryExpr is ExpressionStmt
            && astBinaryExpr.expression is VariableDeclarationExpr
        ) {
            return null
        }
        if (astBinaryExpr.toString().contains("return")) {
            return null
        }
        return conditionStep(statementTag.step, reversed, jimpleToASTMap)
    }

    protected open fun conditionStep(step: Step, reversed: Boolean, jimpleToASTMap: JimpleToASTMap): String {
        var description = "(${jimpleToASTMap[step.stmt]}): "
        description += if ((step.decision == 1 && reversed) || (step.decision == 0 && !reversed)) "False"
        else "True"
        return description
    }


    // In traceTags we have description of each not executed iterations but don't know after which statement we can
    // consider it skipped.
    // This method identifies iterations which can be considered as skipped after given statementTag
    //
    // it.from <= statementTag.line && it.to <= statementTag.line filters iteration descriptions
    // that described after given statement
    protected fun statementTagSkippedIteration(statementTag: StatementTag, sootMethod: SootMethod) =
        methodToNoIterationDescription.getOrDefault(sootMethod, emptyList()).filter { iterationDescription ->
            iterationDescription.from <= statementTag.line && iterationDescription.to <= statementTag.line
        }
}