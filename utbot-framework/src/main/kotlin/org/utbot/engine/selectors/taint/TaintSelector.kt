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

class TaintSelector(
    val graph: ExceptionalUnitGraph,
    choosingStrategy: ChoosingStrategy,
    stoppingStrategy: StoppingStrategy
) : BasePathSelector(choosingStrategy, stoppingStrategy) {
    val start = graph.head

    private val globalGraph = InterProceduralUnitGraph(graph)
    private val stmtsQueue = ArrayDeque<Stmt>()
    private lateinit var currentStmt: Stmt

    init {
        joinAllNonAbstractMethodGraphs()
    }

    private fun joinAllNonAbstractMethodGraphs() {
        stmtsQueue += start

        while (!stmtsQueue.isEmpty()) {
            currentStmt = stmtsQueue.removeFirst()
            traverseStmt(currentStmt)
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
            stmtsQueue += globalGraph.succ(currentStmt).dst
        }
    }

    private fun virtualAndInterfaceInvoke(methodRef: SootMethodRef) {
        val method = Scene.v().getMethod(methodRef.signature)
        if (method.canRetrieveBody()) {
            globalGraph.join(currentStmt, method.jimpleBody().graph(), registerEdges = true/*TODO what should be passed?*/)
            stmtsQueue += globalGraph.succ(currentStmt).dst
        }
    }

    private fun staticInvoke(invokeExpr: JStaticInvokeExpr) {
        val method = invokeExpr.retrieveMethod()
        if (method.canRetrieveBody()) {
            globalGraph.join(currentStmt, method.jimpleBody().graph(), registerEdges = true/*TODO what should be passed?*/)
            stmtsQueue += globalGraph.succ(currentStmt).dst
        }
    }

    private fun traverseIfStmt(current: JIfStmt) {
        val (negativeCaseEdge, positiveCaseEdge) = globalGraph.succs(current).let { it[0] to it.getOrNull(1) }

        stmtsQueue += negativeCaseEdge.dst
        positiveCaseEdge?.let { stmtsQueue += it.dst }
    }

    private fun traverseIdentityStmt(current: JIdentityStmt) {
        when (val identityRef = current.rightOp as IdentityRef) {
            is ParameterRef, is ThisRef, is JCaughtExceptionRef -> {
                // TODO strange error occurred
                stmtsQueue += globalGraph.succ(currentStmt).dst
            }
            else -> error("Unsupported $identityRef")
        }
    }

    private fun traverseAssignStmt(current: JAssignStmt) {
        val rightValue = current.rightOp

        if (rightValue is InvokeExpr) {
            invokeResult(rightValue)
        }

        stmtsQueue += globalGraph.succ(currentStmt).dst
    }

    override fun offer(state: ExecutionState) {
        super.offer(state)
    }

    override fun peek(): ExecutionState? {
        return super.peek()
    }

    override fun poll(): ExecutionState? {
        return super.poll()
    }

    override fun remove(state: ExecutionState): Boolean {
        return super.remove(state)
    }

    override fun queue(): List<Pair<ExecutionState, Double>> {
        return super.queue()
    }

    override fun removeImpl(state: ExecutionState): Boolean {
        TODO("Not yet implemented")
    }

    override fun pollImpl(): ExecutionState? {
        TODO("Not yet implemented")
    }

    override fun peekImpl(): ExecutionState? {
        TODO("Not yet implemented")
    }

    override fun offerImpl(state: ExecutionState) {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override val name: String
        get() = TODO("Not yet implemented")

    override fun close() {
        TODO("Not yet implemented")
    }
}