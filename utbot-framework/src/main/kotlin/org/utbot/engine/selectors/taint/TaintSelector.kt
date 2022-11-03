/*
package org.utbot.engine.selectors.taint

import org.utbot.engine.Eq
import org.utbot.engine.Gt
import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.Lt
import org.utbot.engine.MethodResult
import org.utbot.engine.ObjectValue
import org.utbot.engine.SymbolicSuccess
import org.utbot.engine.TraversalContext
import org.utbot.engine.fold
import org.utbot.engine.head
import org.utbot.engine.jimpleBody
import org.utbot.engine.pc.mkNot
import org.utbot.engine.pc.mkOr
import org.utbot.engine.retrieveMethod
import org.utbot.engine.selectors.BasePathSelector
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy
import org.utbot.engine.state.CALL_DECISION_NUM
import org.utbot.engine.state.Edge
import org.utbot.engine.state.ExecutionState
import org.utbot.engine.state.createExceptionState
import org.utbot.engine.voidValue
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.id
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.util.graph
import soot.SootMethodRef
import soot.Value
import soot.jimple.DefinitionStmt
import soot.jimple.Expr
import soot.jimple.MonitorStmt
import soot.jimple.Stmt
import soot.jimple.SwitchStmt
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JBreakpointStmt
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

    val globalGraph = InterProceduralUnitGraph(graph)

    init {
        var invokeExpr: Expr? = null

        val method = when (invokeExpr) {
            is JInterfaceInvokeExpr -> invokeExpr.methodRef.resolve()
            is JVirtualInvokeExpr -> invokeExpr.methodRef.resolve()
            is JSpecialInvokeExpr -> invokeExpr.retrieveMethod()
            is JStaticInvokeExpr -> invokeExpr.retrieveMethod()
            is JDynamicInvokeExpr -> null// TODO should we retrieve its body?
            else -> error("Unknown class ${invokeExpr!!::class}")
        }

        val body = method!!.jimpleBody()
        var graph = body.graph()

        globalGraph.join()

        val newStmt = globalGraph.succ()
    }

    private fun joinAllNonAbstractMethodGraphs() {
        traverseStmt(start)
    }

    private fun traverseStmt(current: Stmt) {
        when (current) {
            is JAssignStmt -> traverseAssignStmt(current)
            is JIdentityStmt -> traverseIdentityStmt(current)
            is JIfStmt -> traverseIfStmt(current)
            is JInvokeStmt -> traverseInvokeStmt(current)
            is SwitchStmt -> traverseSwitchStmt(current)
            is JReturnStmt -> {}*/
/*processResult(symbolicSuccess(current))*//*

            is JReturnVoidStmt -> {}*/
/*processResult(SymbolicSuccess(voidValue))*//*

            is JRetStmt -> error("This one should be already removed by Soot: $current")
            is JThrowStmt -> traverseThrowStmt(current)
            is JBreakpointStmt -> traverseStmt(globalGraph.succ(current).dst)*/
/*offerState(updateQueued(globalGraph.succ(current)))*//*

            is JGotoStmt -> traverseStmt(globalGraph.succ(current).dst)*/
/*offerState(updateQueued(globalGraph.succ(current)))*//*

            is JNopStmt -> traverseStmt(globalGraph.succ(current).dst)*/
/*offerState(updateQueued(globalGraph.succ(current)))*//*

            is MonitorStmt -> traverseStmt(globalGraph.succ(current).dst)*/
/*offerState(updateQueued(globalGraph.succ(current)))*//*

            is DefinitionStmt -> TODO("$current")
            else -> error("Unsupported: ${current::class}")
        }
    }

    private fun traverseThrowStmt(current: JThrowStmt) {
        */
/*val edge = Edge(current, current, CALL_DECISION_NUM)

        globalGraph.registerImplicitEdge(edge)
        traverseStmt()*//*



        */
/*val classId = exception.fold(
            { it.javaClass.id },
            { (exception.symbolic as ObjectValue).type.id }*//*


        // TODO maybe ignore?
    }

    */
/*private fun findCatchBlock(current: Stmt, classId: ClassId): Edge? {
        val stmtToEdge = globalGraph.exceptionalSuccs(current).associateBy { it.dst }
        return globalGraph.traps.asSequence().mapNotNull { (stmt, exceptionClass) ->
            stmtToEdge[stmt]?.let { it to exceptionClass }
        }.firstOrNull { it.second in hierarchy.ancestors(classId) }?.first
    }*//*


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
        TODO("Not yet implemented")
    }

    private fun invokeResult(invokeExpr: Expr) {
        when (invokeExpr) {
            is JStaticInvokeExpr -> staticInvoke(invokeExpr)
            is JInterfaceInvokeExpr -> virtualAndInterfaceInvoke(invokeExpr.base, invokeExpr.methodRef, invokeExpr.args)
            is JVirtualInvokeExpr -> virtualAndInterfaceInvoke(invokeExpr.base, invokeExpr.methodRef, invokeExpr.args)
            is JSpecialInvokeExpr -> specialInvoke(invokeExpr)
            is JDynamicInvokeExpr -> {}
            else -> error("Unknown class ${invokeExpr::class}")
        }
    }

    private fun specialInvoke(invokeExpr: JSpecialInvokeExpr) {
        TODO("Not yet implemented")
    }

    private fun virtualAndInterfaceInvoke(base: Value?, methodRef: SootMethodRef?, args: List<Value>) {
        TODO("Not yet implemented")
    }

    private fun staticInvoke(invokeExpr: JStaticInvokeExpr) {
        TODO("Not yet implemented")
    }



    private fun traverseIfStmt(current: JIfStmt) {
        TODO("Not yet implemented")
    }

    private fun traverseIdentityStmt(current: JIdentityStmt) {
        TODO("Not yet implemented")
    }

    private fun traverseAssignStmt(current: JAssignStmt) {
        TODO("Not yet implemented")
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
}*/
