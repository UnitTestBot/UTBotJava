package org.utbot.engine

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import org.utbot.common.md5
import org.utbot.engine.pc.UtSolver
import org.utbot.engine.pc.UtSolverStatusUNDEFINED
import org.utbot.engine.symbolic.SymbolicState
import org.utbot.engine.symbolic.SymbolicStateUpdate
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.Step
import soot.SootMethod
import soot.jimple.Stmt
import java.util.Objects
import org.utbot.engine.symbolic.Assumption
import org.utbot.framework.plugin.api.UtSymbolicExecution

const val RETURN_DECISION_NUM = -1
const val CALL_DECISION_NUM = -2

data class Edge(val src: Stmt, val dst: Stmt, val decisionNum: Int)

/**
 * Possible state types. Engine matches on them and processes differently.
 *
 * [INTERMEDIATE] is a label for an intermediate state which is suitable for further symbolic analysis.
 *
 * [TERMINAL] is a label for a terminal state from which we might (or might not) execute concretely and construct
 * [UtExecution]. This state represents the final state of the program execution, that is a throw or return from the outer
 * method.
 *
 * [CONCRETE] is a label for a state which is not suitable for further symbolic analysis, and it is also not a terminal
 * state. Such states are only suitable for a concrete execution and may result from [Assumption]s.
 */
enum class StateLabel {
    INTERMEDIATE,
    TERMINAL,
    CONCRETE
}

/**
 * The stack element of the [ExecutionState].
 * Contains properties, that are suitable for specified method in call stack.
 *
 * @param doesntThrow if true, then engine should drop states with throwing exceptions.
 * @param localVariableMemory the local memory associated with the current stack element.
 */
data class ExecutionStackElement(
    val caller: Stmt?,
    val localVariableMemory: LocalVariableMemory = LocalVariableMemory(),
    val parameters: MutableList<Parameter> = mutableListOf(),
    val inputArguments: ArrayDeque<SymbolicValue> = ArrayDeque(),
    val doesntThrow: Boolean = false,
    val method: SootMethod,
) {
    fun update(memoryUpdate: LocalMemoryUpdate, doesntThrow: Boolean = this.doesntThrow) =
        this.copy(localVariableMemory = localVariableMemory.update(memoryUpdate), doesntThrow = doesntThrow)
}

/**
 * Class that store all information about execution state that needed only for analytics module
 */
data class StateAnalyticsProperties(
    /**
     * Number of forks already performed along state's path, where fork is statement with multiple successors
     */
    val depth: Int = 0,
    var visitedAfterLastFork: Int = 0,
    var visitedBeforeLastFork: Int = 0,
    var stmtsSinceLastCovered: Int = 0,
    val parent: ExecutionState? = null,
) {
    var executingTime: Long = 0
    var reward: Double? = null
    val features: MutableList<Double> = mutableListOf()

    /**
     * Flag that indicates whether this state is fork or not. Fork here means that we have more than one successor
     */
    private var isFork: Boolean = false

    fun definitelyFork() {
        isFork = true
    }

    private var isVisitedNew: Boolean = false

    fun updateIsVisitedNew() {
        isVisitedNew = true
        stmtsSinceLastCovered = 0
        visitedAfterLastFork++
    }

    private val successorDepth: Int get() = depth + if (isFork) 1 else 0

    private val successorVisitedAfterLastFork: Int get() = if (!isFork) visitedAfterLastFork else 0
    private val successorVisitedBeforeLastFork: Int get() = visitedBeforeLastFork + if (isFork) visitedAfterLastFork else 0
    private val successorStmtSinceLastCovered: Int get() = 1 + stmtsSinceLastCovered

    fun successorProperties(parent: ExecutionState) = StateAnalyticsProperties(
        successorDepth,
        successorVisitedAfterLastFork,
        successorVisitedBeforeLastFork,
        successorStmtSinceLastCovered,
        if (UtSettings.enableFeatureProcess) parent else null
    )
}

/**
 * [visitedStatementsHashesToCountInPath] is a map representing how many times each instruction from the [path]
 * has occurred. It is required to calculate priority of the branches and decrease the priority for branches leading
 * inside a cycle. To improve performance it is a persistent map using state's hashcode to imitate an identity hashmap.
 *
 * @param symbolicState the current symbolic state.
 */
data class ExecutionState(
    val stmt: Stmt,
    val symbolicState: SymbolicState,
    val executionStack: PersistentList<ExecutionStackElement>,
    val path: PersistentList<Stmt> = persistentListOf(),
    val visitedStatementsHashesToCountInPath: PersistentMap<Int, Int> = persistentHashMapOf(),
    val decisionPath: PersistentList<Int> = persistentListOf(0),
    val edges: PersistentSet<Edge> = persistentHashSetOf(),
    val stmts: PersistentMap<Stmt, Int> = persistentHashMapOf(),
    val pathLength: Int = 0,
    val lastEdge: Edge? = null,
    val lastMethod: SootMethod? = null,
    val methodResult: MethodResult? = null,
    val exception: SymbolicFailure? = null,
    val label: StateLabel = StateLabel.INTERMEDIATE,
    private var stateAnalyticsProperties: StateAnalyticsProperties = StateAnalyticsProperties()
) : AutoCloseable {
    val solver: UtSolver by symbolicState::solver

    val memory: Memory by symbolicState::memory

    private var outgoingEdges = 0

    fun isInNestedMethod() = executionStack.size > 1

    val localVariableMemory
        get() = executionStack.last().localVariableMemory

    val inputArguments
        get() = executionStack.last().inputArguments

    val parameters
        get() = executionStack.last().parameters

    /**
     * Retrieves MUT parameters.
     */
    val methodUnderTestParameters
        get() = executionStack.firstOrNull()?.parameters?.map { it.value }
            ?: error("Cannot retrieve MUT parameters from empty execution stack")

    val isThrowException: Boolean
        get() = (lastEdge?.decisionNum ?: 0) < CALL_DECISION_NUM

    val isInsideStaticInitializer
        get() = executionStack.any { it.method.isStaticInitializer }

    fun createExceptionState(
        exception: SymbolicFailure,
        update: SymbolicStateUpdate
    ): ExecutionState {
        val last = executionStack.last()
        // go to negative indexing below CALL_DECISION_NUM for exceptions
        val edge = Edge(stmt, stmt, CALL_DECISION_NUM - (++outgoingEdges))
        return ExecutionState(
            stmt = stmt,
            symbolicState = symbolicState + update,
            executionStack = executionStack.set(executionStack.lastIndex, last.update(update.localMemoryUpdates)),
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

    fun pop(methodResult: MethodResult): ExecutionState {
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

    fun push(
        stmt: Stmt,
        inputArguments: ArrayDeque<SymbolicValue>,
        update: SymbolicStateUpdate,
        method: SootMethod
    ): ExecutionState {
        val edge = Edge(this.stmt, stmt, CALL_DECISION_NUM)
        val stackElement = ExecutionStackElement(
            this.stmt,
            localVariableMemory = localVariableMemory.memoryForNestedMethod().update(update.localMemoryUpdates),
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

    fun update(
        stateUpdate: SymbolicStateUpdate
    ): ExecutionState {
        val last = executionStack.last()
        val stackElement = last.update(stateUpdate.localMemoryUpdates)
        return copy(
            symbolicState = symbolicState + stateUpdate,
            executionStack = executionStack.set(executionStack.lastIndex, stackElement)
        )
    }

    fun update(
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

    /**
     * Tell to solver that states with status [UtSolverStatusUNDEFINED] can be created from current state.
     *
     * Note: Solver optimize cloning respect this flag.
     */
    fun expectUndefined() {
        solver.expectUndefined = true
    }

    fun withLabel(newLabel: StateLabel) = copy(label = newLabel)

    override fun close() {
        solver.close()
    }


    /**
     * Collects full statement path from method entry point to current statement, including current statement.
     *
     * Each step contains statement, call depth for nested calls (returns belong to called method) and decision.
     * Decision for current statement is zero.
     *
     * Note: calculates depth wrongly for thrown exception, check SAT-811, SAT-812
     * TODO: fix SAT-811, SAT-812
     */
    fun fullPath(): List<Step> {
        var depth = 0
        val path = path.zip(
            decisionPath.subList(1, decisionPath.size)
        ).map { (stmt, decision) ->
            val stepDepth = when (decision) {
                CALL_DECISION_NUM -> depth++
                RETURN_DECISION_NUM -> depth--
                else -> depth
            }
            Step(stmt, stepDepth, decision)
        }
        return path + Step(stmt, depth, 0)
    }

    /**
     * Prettifies full statement path for logging.
     *
     * Note: marks return statements with *depth-1* to pair with call statement.
     */
    fun prettifiedPathLog(): String {
        val path = fullPath()
        val prettifiedPath = prettifiedPath(path)
        return " MD5(path)=${md5(prettifiedPath)}\n$prettifiedPath"
    }

    private fun md5(prettifiedPath: String) = prettifiedPath.md5()

    fun md5() = prettifiedPath(fullPath()).md5()

    private fun prettifiedPath(path: List<Step>) =
        path.joinToString(separator = "\n") { (stmt, depth, decision) ->
            val prefix = when (decision) {
                CALL_DECISION_NUM -> "call[${depth}] - " + "".padEnd(2 * depth, ' ')
                RETURN_DECISION_NUM -> " ret[${depth - 1}] - " + "".padEnd(2 * depth, ' ')
                else -> "          " + "".padEnd(2 * depth, ' ')
            }
            "$prefix$stmt"
        }

    fun definitelyFork() {
        stateAnalyticsProperties.definitelyFork()
    }

    fun updateIsVisitedNew() {
        stateAnalyticsProperties.updateIsVisitedNew()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExecutionState

        if (stmt != other.stmt) return false
        if (symbolicState != other.symbolicState) return false
        if (executionStack != other.executionStack) return false
        if (path != other.path) return false
        if (visitedStatementsHashesToCountInPath != other.visitedStatementsHashesToCountInPath) return false
        if (decisionPath != other.decisionPath) return false
        if (edges != other.edges) return false
        if (stmts != other.stmts) return false
        if (pathLength != other.pathLength) return false
        if (lastEdge != other.lastEdge) return false
        if (lastMethod != other.lastMethod) return false
        if (methodResult != other.methodResult) return false
        if (exception != other.exception) return false

        return true
    }

    private val hashCode by lazy {
        Objects.hash(
            stmt, symbolicState, executionStack, path, visitedStatementsHashesToCountInPath, decisionPath,
            edges, stmts, pathLength, lastEdge, lastMethod, methodResult, exception
        )
    }

    override fun hashCode(): Int = hashCode

    var reward by stateAnalyticsProperties::reward
    val features by stateAnalyticsProperties::features
    var executingTime by stateAnalyticsProperties::executingTime
    val depth by stateAnalyticsProperties::depth
    var visitedBeforeLastFork by stateAnalyticsProperties::visitedBeforeLastFork
    var visitedAfterLastFork by stateAnalyticsProperties::visitedAfterLastFork
    var stmtsSinceLastCovered by stateAnalyticsProperties::stmtsSinceLastCovered
    val parent by stateAnalyticsProperties::parent
}