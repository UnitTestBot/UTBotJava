package org.utbot.engine.state

import kotlinx.collections.immutable.plus
import org.utbot.engine.MethodResult
import org.utbot.engine.SymbolicFailure
import org.utbot.engine.SymbolicValue
import org.utbot.engine.putIfAbsent
import org.utbot.engine.symbolic.SymbolicStateUpdate
import soot.SootMethod
import soot.jimple.Stmt

fun ExecutionState.createExceptionState(
    exception: SymbolicFailure,
    update: SymbolicStateUpdate
): ExecutionState {
    val last = executionStack.last()
    // go to negative indexing below CALL_DECISION_NUM for exceptions
    val edge = Edge(stmt, stmt, CALL_DECISION_NUM - (++outgoingEdges))
    val localMemory = last.update(update.localMemoryUpdates)
    return ExecutionState(
        stmt = stmt,
        symbolicState = symbolicState + update,
        executionStack = executionStack.set(executionStack.lastIndex, localMemory),
        path = path,
        visitedStatementsHashesToCountInPath = visitedStatementsHashesToCountInPath,
        decisionPath = decisionPath + edge.decisionNum,
        edges = edges + edge,
        stmts = stmts,
        pathLength = pathLength + 1,
        lastEdge = edge,
        lastMethod = executionStack.last().method,
        exception = exception,
        label = label,
        stateAnalyticsProperties = stateAnalyticsProperties.successorProperties(this)
    )
}

fun ExecutionState.update(
    edge: Edge,
    symbolicStateUpdate: SymbolicStateUpdate,
    doesntThrow: Boolean,
): ExecutionState {
    val last = executionStack.last()
    val stackElement = last.update(
        symbolicStateUpdate.localMemoryUpdates,
        last.doesntThrow || doesntThrow
    )
    outgoingEdges++

    val stmtHashCode = stmt.hashCode()
    val stmtCountInPath = (visitedStatementsHashesToCountInPath[stmtHashCode] ?: 0) + 1

    return ExecutionState(
        stmt = edge.dst,
        symbolicState = symbolicState + symbolicStateUpdate,
        executionStack = executionStack.set(executionStack.lastIndex, stackElement),
        path = path + stmt,
        visitedStatementsHashesToCountInPath = visitedStatementsHashesToCountInPath.put(
            stmtHashCode,
            stmtCountInPath
        ),
        decisionPath = decisionPath + edge.decisionNum,
        edges = edges + edge,
        stmts = stmts.putIfAbsent(stmt, pathLength),
        pathLength = pathLength + 1,
        lastEdge = edge,
        lastMethod = stackElement.method,
        label = label,
        stateAnalyticsProperties = stateAnalyticsProperties.successorProperties(this)
    )
}

fun ExecutionState.withLabel(newLabel: StateLabel) = copy(label = newLabel)


fun ExecutionState.update(
    stateUpdate: SymbolicStateUpdate
): ExecutionState {
    val last = executionStack.last()
    val stackElement = last.update(stateUpdate.localMemoryUpdates)
    return copy(
        symbolicState = symbolicState + stateUpdate,
        executionStack = executionStack.set(executionStack.lastIndex, stackElement)
    )
}

fun ExecutionState.push(
    stmt: Stmt,
    inputArguments: ArrayDeque<SymbolicValue>,
    update: SymbolicStateUpdate,
    method: SootMethod
): ExecutionState {
    val edge = Edge(this.stmt, stmt, CALL_DECISION_NUM)
    val stackElement = ExecutionStackElement(
        this.stmt,
        localVariableMemory.memoryForNestedMethod().update(update.localMemoryUpdates),
        inputArguments = inputArguments,
        method = method,
        doesntThrow = executionStack.last().doesntThrow
    )
    outgoingEdges++

    val stmtHashCode = this.stmt.hashCode()
    val stmtCountInPath = (visitedStatementsHashesToCountInPath[stmtHashCode] ?: 0) + 1

    return ExecutionState(
        stmt = stmt,
        symbolicState = symbolicState.stateForNestedMethod() + update,
        executionStack = executionStack + stackElement,
        path = path + this.stmt,
        visitedStatementsHashesToCountInPath = visitedStatementsHashesToCountInPath.put(
            stmtHashCode,
            stmtCountInPath
        ),
        decisionPath = decisionPath + edge.decisionNum,
        edges = edges + edge,
        stmts = stmts.putIfAbsent(this.stmt, pathLength),
        pathLength = pathLength + 1,
        lastEdge = edge,
        lastMethod = stackElement.method,
        label = label,
        stateAnalyticsProperties = stateAnalyticsProperties.successorProperties(this)
    )
}

fun ExecutionState.pop(methodResult: MethodResult): ExecutionState {
    val caller = executionStack.last().caller!!
    val edge = Edge(stmt, caller, RETURN_DECISION_NUM)

    val stmtHashcode = stmt.hashCode()
    val stmtCountInPath = (visitedStatementsHashesToCountInPath[stmtHashcode] ?: 0) + 1

    return ExecutionState(
        stmt = caller,
        symbolicState = symbolicState,
        executionStack = executionStack.removeAt(executionStack.lastIndex),
        path = path + stmt,
        visitedStatementsHashesToCountInPath = visitedStatementsHashesToCountInPath.put(
            stmtHashcode,
            stmtCountInPath
        ),
        decisionPath = decisionPath + edge.decisionNum,
        edges = edges + edge,
        stmts = stmts.putIfAbsent(stmt, pathLength),
        pathLength = pathLength + 1,
        lastEdge = edge,
        lastMethod = executionStack.last().method,
        methodResult = methodResult,
        label = label,
        stateAnalyticsProperties = stateAnalyticsProperties.successorProperties(this)
    )
}