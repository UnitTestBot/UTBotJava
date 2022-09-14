package org.utbot.summary.tag

import org.utbot.framework.plugin.api.Step
import org.utbot.summary.clustering.SplitSteps
import soot.RefType
import soot.jimple.Expr
import soot.jimple.IfStmt
import soot.jimple.InvokeExpr
import soot.jimple.Stmt
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JCaughtExceptionRef
import soot.jimple.internal.JIdentityStmt
import soot.jimple.internal.JInvokeStmt
import soot.jimple.internal.JLookupSwitchStmt
import soot.jimple.internal.JNewExpr
import soot.jimple.internal.JReturnStmt
import soot.jimple.internal.JReturnVoidStmt
import soot.jimple.internal.JTableSwitchStmt
import soot.jimple.internal.JThrowStmt

private const val ERROR_NAME = "Error"
private const val EXCEPTION_NAME = "Exception"

/**
 * creates statements order tag: first call, second call, or many
 * @param number - number call
 */
fun getCallOrderTag(number: Int?): CallOrderTag {
    if (number == 1)
        return CallOrderTag.First
    if (number == 2)
        return CallOrderTag.Second
    return CallOrderTag.Many
}


fun statementFrequencyTag(step: Step, splitSteps: SplitSteps): UniquenessTag =
    when {
        splitSteps.commonSteps.contains(step) -> {
            UniquenessTag.Common
        }
        splitSteps.uniqueSteps.contains(step) -> {
            UniquenessTag.Unique
        }
        else -> UniquenessTag.Partly
    }


fun getBasicTypeTag(stmt: Stmt): BasicTypeTag = when (stmt) {
    is JIdentityStmt -> basicIdentityTag(stmt)
    is IfStmt -> BasicTypeTag.Condition
    is JReturnStmt -> BasicTypeTag.Return
    is JReturnVoidStmt -> BasicTypeTag.Return
    is JAssignStmt -> basicAssignmentTag(stmt)
    is JThrowStmt -> BasicTypeTag.ExceptionThrow
    is JInvokeStmt -> BasicTypeTag.Invoke
    is JTableSwitchStmt -> BasicTypeTag.SwitchCase
    is JLookupSwitchStmt -> BasicTypeTag.SwitchCase
    else -> BasicTypeTag.Basic
}

fun basicIdentityTag(stmt: JIdentityStmt): BasicTypeTag {
    if (stmt.rightOp is JCaughtExceptionRef) {
        return BasicTypeTag.CaughtException
    }
    return BasicTypeTag.Initialization
}

fun basicAssignmentTag(stmt: JAssignStmt): BasicTypeTag {
    val rightValue = stmt.rightOp
    val rightType = stmt.rightOp.type
    if (rightValue is JNewExpr && rightType is RefType) {
        if (rightType.className.contains(ERROR_NAME) || rightType.className.contains(EXCEPTION_NAME))
            return BasicTypeTag.ExceptionAssignment
    }
    if (rightValue is Expr && rightValue is InvokeExpr) {
        return BasicTypeTag.Invoke
    }
    return BasicTypeTag.Assignment
}

fun getExecutionTag(step: Step): ExecutionTag {
    return when (step.stmt) {
        is IfStmt -> conditionExecutionTag(step.decision)
        else -> ExecutionTag.Executed
    }
}

fun conditionExecutionTag(decision: Int): ExecutionTag = if (decision == 1) ExecutionTag.True else ExecutionTag.False
