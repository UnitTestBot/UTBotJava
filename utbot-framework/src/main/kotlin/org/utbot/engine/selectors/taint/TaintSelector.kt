package org.utbot.engine.selectors.taint

import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.canRetrieveBody
import org.utbot.engine.head
import org.utbot.engine.jimpleBody
import org.utbot.engine.retrieveMethod
import org.utbot.engine.selectors.BasePathSelector
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy
import org.utbot.engine.state.Edge
import org.utbot.engine.state.ExecutionState
import org.utbot.framework.util.graph
import soot.Scene
import soot.SootMethodRef
import soot.jimple.DefinitionStmt
import soot.jimple.Expr
import soot.jimple.IdentityRef
import soot.jimple.InvokeExpr
import soot.jimple.MonitorStmt
import soot.jimple.ParameterRef
import soot.jimple.Stmt
import soot.jimple.SwitchStmt
import soot.jimple.ThisRef
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JBreakpointStmt
import soot.jimple.internal.JCaughtExceptionRef
import soot.jimple.internal.JDynamicInvokeExpr
import soot.jimple.internal.JGotoStmt
import soot.jimple.internal.JIdentityStmt
import soot.jimple.internal.JIfStmt
import soot.jimple.internal.JInterfaceInvokeExpr
import soot.jimple.internal.JInvokeStmt
import soot.jimple.internal.JLookupSwitchStmt
import soot.jimple.internal.JNopStmt
import soot.jimple.internal.JRetStmt
import soot.jimple.internal.JReturnStmt
import soot.jimple.internal.JReturnVoidStmt
import soot.jimple.internal.JSpecialInvokeExpr
import soot.jimple.internal.JStaticInvokeExpr
import soot.jimple.internal.JTableSwitchStmt
import soot.jimple.internal.JThrowStmt
import soot.jimple.internal.JVirtualInvokeExpr
import soot.toolkits.graph.ExceptionalUnitGraph
import java.util.PriorityQueue

class TaintSelector(
    val graph: ExceptionalUnitGraph,
    val taintSources: Set<Stmt>,
    val taintSinks: Set<Stmt>,
    choosingStrategy: ChoosingStrategy,
    stoppingStrategy: StoppingStrategy
) : BasePathSelector(choosingStrategy, stoppingStrategy) {
    val start = graph.head

    private val globalGraph = InterProceduralUnitGraph(graph)
    private val stmtsQueue = ArrayDeque<Stmt>()
    private lateinit var currentStmt: Stmt

    private val distancesBetweenStmts: MutableMap<Stmt, MutableMap<Stmt, Int>> = mutableMapOf()
    lateinit var adjacencyMatrix: Array<IntArray>
    private val stmtToIds: MutableMap<Stmt, Int> = mutableMapOf()

    private val executionQueue: PriorityQueue<ExecutionState> = PriorityQueue(Comparator { firstState, secondState ->
        val firstStmt = firstState.stmt
        val secondStmt = secondState.stmt

        // TODO check signs
        val firstId = stmtToIds[firstStmt] ?: return@Comparator 1
        val secondId = stmtToIds[secondStmt] ?: return@Comparator -1

        val firstDistance = if (firstState.path.any { it in taintSources }) {
            val sinkIds = taintSinks.mapNotNull { stmtToIds[it] }

            val toSinkDistances = sinkIds.map { adjacencyMatrix[firstId][it] }

            toSinkDistances.minOrNull() ?: Int.MAX_VALUE
        } else {
            val sourceIds = taintSources.mapNotNull { stmtToIds[it] }

            val toSourceDistances = sourceIds.map { adjacencyMatrix[firstId][it] }

            toSourceDistances.minOrNull() ?: Int.MAX_VALUE
        }

        val secondDistance = if (secondState.path.any { it in taintSources }) {
            val sinkIds = taintSinks.mapNotNull { stmtToIds[it] }

            val toSinkDistances = sinkIds.map { adjacencyMatrix[secondId][it] }

            toSinkDistances.minOrNull() ?: Int.MAX_VALUE
        } else {
            val sourceIds = taintSources.mapNotNull { stmtToIds[it] }

            val toSourceDistances = sourceIds.map { adjacencyMatrix[secondId][it] }

            toSourceDistances.minOrNull() ?: Int.MAX_VALUE
        }

        return@Comparator firstDistance.compareTo(secondDistance)
    })

    init {
        joinAllNonAbstractMethodGraphs()
        buildAdjacencyMatrix()
        runFLoydWarshall()
    }

    private fun joinAllNonAbstractMethodGraphs() {
        stmtsQueue += start

        while (!stmtsQueue.isEmpty()) {
            currentStmt = stmtsQueue.removeFirst()

            val initDistances = distancesBetweenStmts.getOrPut(currentStmt) { mutableMapOf() }
            initDistances[currentStmt] = 0

            traverseStmt(currentStmt)
        }
    }

    private fun buildAdjacencyMatrix() {
        val allStmts = globalGraph.stmts
        val n = allStmts.size
        adjacencyMatrix = Array(n) { IntArray(n) { Int.MAX_VALUE } }

        var id = 0
        for (from in allStmts) {
            val fromId = stmtToIds.getOrPut(from) { id++ }

            for (to in allStmts) {
                val toId = stmtToIds.getOrPut(to) { id++ }

                adjacencyMatrix[fromId][toId] = if (fromId == toId) {
                    0
                } else {
                    distancesBetweenStmts[from]?.let { it[to] } ?: Int.MAX_VALUE
                }
            }
        }
    }

    private fun runFLoydWarshall() {
        val n = adjacencyMatrix.size

        for(k in 0 until n) {
            for (i in 0 until n) {
                for (j in 0 until n) {
                    val anotherOption = adjacencyMatrix[i][k].let { first ->
                        if (first == Int.MAX_VALUE) {
                            Int.MAX_VALUE
                        } else {
                            adjacencyMatrix[k][j].let { second ->
                                if (second == Int.MAX_VALUE) {
                                    Int.MAX_VALUE
                                } else {
                                    first + second
                                }
                            }
                        }
                    }

                    adjacencyMatrix[i][j] = minOf(adjacencyMatrix[i][j], anotherOption)
                }
            }
        }
    }

    private fun traverseStmt(current: Stmt) {
        when (current) {
            is JAssignStmt -> traverseAssignStmt(current)
            is JIdentityStmt -> traverseIdentityStmt(current)
            is JIfStmt -> traverseIfStmt(current)
            is JInvokeStmt -> traverseInvokeStmt(current)
            is SwitchStmt -> traverseSwitchStmt(current)
            is JReturnStmt -> {}/*processResult(symbolicSuccess(current))*/
            is JReturnVoidStmt -> {}/*processResult(SymbolicSuccess(voidValue))*/
            is JRetStmt -> error("This one should be already removed by Soot: $current")
            is JThrowStmt -> traverseThrowStmt(current)
            is JBreakpointStmt -> traverseStmt(globalGraph.succ(current).dst)/*offerState(updateQueued(globalGraph.succ(current)))*/
            is JGotoStmt -> traverseStmt(globalGraph.succ(current).dst)/*offerState(updateQueued(globalGraph.succ(current)))*/
            is JNopStmt -> traverseStmt(globalGraph.succ(current).dst)/*offerState(updateQueued(globalGraph.succ(current)))*/
            is MonitorStmt -> traverseStmt(globalGraph.succ(current).dst)/*offerState(updateQueued(globalGraph.succ(current)))*/
            is DefinitionStmt -> TODO("$current")
            else -> error("Unsupported: ${current::class}")
        }
    }

    private fun traverseThrowStmt(current: JThrowStmt) {
        /*val edge = Edge(current, current, CALL_DECISION_NUM)

        globalGraph.registerImplicitEdge(edge)
        traverseStmt()*/


        /*val classId = exception.fold(
            { it.javaClass.id },
            { (exception.symbolic as ObjectValue).type.id }*/

        // TODO maybe ignore?
    }

    /*private fun findCatchBlock(current: Stmt, classId: ClassId): Edge? {
        val stmtToEdge = globalGraph.exceptionalSuccs(current).associateBy { it.dst }
        return globalGraph.traps.asSequence().mapNotNull { (stmt, exceptionClass) ->
            stmtToEdge[stmt]?.let { it to exceptionClass }
        }.firstOrNull { it.second in hierarchy.ancestors(classId) }?.first
    }*/

    private fun traverseSwitchStmt(current: SwitchStmt) {
        val successors = when (current) {
            is JTableSwitchStmt -> {
                val indexed = (current.lowIndex..current.highIndex).mapIndexed { i, _ ->
                    Edge(current, current.getTarget(i) as Stmt, i)
                }
                indexed + (Edge(current, current.defaultTarget as Stmt, indexed.size))
            }
            is JLookupSwitchStmt -> {
                val lookups = List(current.lookupValues.size) { i ->
                    Edge(current, current.getTarget(i) as Stmt, i)
                }
                lookups + (Edge(current, current.defaultTarget as Stmt, lookups.size))
            }
            else -> error("Unknown switch $current")
        }

        successors.forEach {
            traverseStmt(it.dst)
        }
    }

    private fun traverseInvokeStmt(current: JInvokeStmt) {
        invokeResult(current.invokeExpr)
    }

    private fun invokeResult(invokeExpr: Expr) {
        when (invokeExpr) {
            is JStaticInvokeExpr -> staticInvoke(invokeExpr)
            is JInterfaceInvokeExpr -> virtualAndInterfaceInvoke(invokeExpr.methodRef)
            is JVirtualInvokeExpr -> virtualAndInterfaceInvoke(invokeExpr.methodRef)
            is JSpecialInvokeExpr -> specialInvoke(invokeExpr)
            is JDynamicInvokeExpr -> {} // Dynamic invoke is not fully supported
            else -> error("Unknown class ${invokeExpr::class}")
        }
    }

    private fun specialInvoke(invokeExpr: JSpecialInvokeExpr) {
        val method = invokeExpr.retrieveMethod()
        if (method.canRetrieveBody()) {
            globalGraph.join(currentStmt, method.jimpleBody().graph(), registerEdges = true/*TODO what should be passed?*/)
            val nextStmt = globalGraph.succ(currentStmt).dst

            distancesBetweenStmts[currentStmt]?.let {
                it[nextStmt] = 1
            }
            stmtsQueue += nextStmt
        }
    }

    private fun virtualAndInterfaceInvoke(methodRef: SootMethodRef) {
        val method = Scene.v().getMethod(methodRef.signature)
        if (method.canRetrieveBody()) {
            globalGraph.join(currentStmt, method.jimpleBody().graph(), registerEdges = true/*TODO what should be passed?*/)
            val nextStmt = globalGraph.succ(currentStmt).dst

            distancesBetweenStmts[currentStmt]?.let {
                it[nextStmt] = 1
            }
            stmtsQueue += nextStmt
        }
    }

    private fun staticInvoke(invokeExpr: JStaticInvokeExpr) {
        val method = invokeExpr.retrieveMethod()
        if (method.canRetrieveBody()) {
            globalGraph.join(currentStmt, method.jimpleBody().graph(), registerEdges = true/*TODO what should be passed?*/)
            val nextStmt = globalGraph.succ(currentStmt).dst

            distancesBetweenStmts[currentStmt]?.let {
                it[nextStmt] = 1
            }
            stmtsQueue += nextStmt
        }
    }

    private fun traverseIfStmt(current: JIfStmt) {
        val (negativeCaseEdge, positiveCaseEdge) = globalGraph.succs(current).let { it[0] to it.getOrNull(1) }

        val negativeStmt = negativeCaseEdge.dst
        stmtsQueue += negativeStmt
        distancesBetweenStmts[currentStmt]?.let {
            it[negativeStmt] = 1
        }

        positiveCaseEdge?.let { positiveCaseEdgeNotNull ->
            val positiveStmt = positiveCaseEdgeNotNull.dst
            distancesBetweenStmts[currentStmt]?.let {
                it[positiveStmt] = 1
            }
            stmtsQueue += positiveStmt
        }
    }

    private fun traverseIdentityStmt(current: JIdentityStmt) {
        when (val identityRef = current.rightOp as IdentityRef) {
            is ParameterRef, is ThisRef, is JCaughtExceptionRef -> {
                // TODO strange error occurred
                val nextStmt = globalGraph.succ(currentStmt).dst

                distancesBetweenStmts[currentStmt]?.let {
                    it[nextStmt] = 1
                }
                stmtsQueue += nextStmt
            }
            else -> error("Unsupported $identityRef")
        }
    }

    private fun traverseAssignStmt(current: JAssignStmt) {
        val rightValue = current.rightOp

        if (rightValue is InvokeExpr) {
            invokeResult(rightValue)
        }

        val nextStmt = globalGraph.succ(currentStmt).dst

        distancesBetweenStmts[currentStmt]?.let {
            it[nextStmt] = 1
        }
        stmtsQueue += nextStmt
    }

    override fun offerImpl(state: ExecutionState) {
        executionQueue.add(state)
    }

    override fun peekImpl(): ExecutionState? {
        return executionQueue.peek()
    }

    override fun pollImpl(): ExecutionState? {
        return executionQueue.poll()
    }

    override fun removeImpl(state: ExecutionState): Boolean {
        return executionQueue.remove(state)
    }

    override fun queue(): List<Pair<ExecutionState, Double>> {
        // TODO real weight
        return executionQueue.map { it to 0.0 }
    }

    override fun isEmpty(): Boolean {
        return executionQueue.isEmpty()
    }

    override val name: String
        get() = "TaintPathSelector"

    override fun close() {
        executionQueue.forEach {
            it.close()
        }
    }
}
