package org.utbot.engine

import org.utbot.engine.selectors.strategies.TraverseGraphStatistics
import soot.SootClass
import soot.SootMethod
import soot.jimple.Stmt
import soot.jimple.SwitchStmt
import soot.jimple.internal.JReturnStmt
import soot.jimple.internal.JReturnVoidStmt
import soot.jimple.internal.JThrowStmt
import soot.toolkits.graph.ExceptionalUnitGraph

private const val EXCEPTION_DECISION_SHIFT = 100000

/**
 * InterProcedural mutable graph that represents unit graph with multiple methods.
 *
 * Maintains currently covered edges and stmts during traverse.
 *
 * Can attach [TraverseGraphStatistics]
 */
class InterProceduralUnitGraph(graph: ExceptionalUnitGraph) {
    private var attachAllowed = true
    val graphs = mutableListOf(graph)
    private val statistics = mutableListOf<TraverseGraphStatistics>()
    // All the methods in the InterProceduralUnitGraph
    private val methods: MutableSet<SootMethod> = mutableSetOf(graph.body.method)
    // Methods with registered edges
    private val registeredMethods: MutableSet<SootMethod> = mutableSetOf(graph.body.method)

    // We use this field for exceptional statements of the methods from the [registeredMethods] map.
    // Points of interest are statements of the graph that must be covered during the exploration.
    // Therefore, there is no need to put here statements from registered methods since
    // we try to cover all of their edges.
    private val methodToUncoveredThrowStatements: MutableMap<SootMethod, MutableSet<Stmt>> = mutableMapOf()

    private val unexceptionalSuccsCache = mutableMapOf<ExceptionalUnitGraph, Map<Stmt, Set<Stmt>>>()
    private val exceptionalSuccsCache = mutableMapOf<ExceptionalUnitGraph, Map<Stmt, Set<Stmt>>>()
    private val edgesCache = mutableMapOf<ExceptionalUnitGraph, List<Edge>>()

    val stmts: MutableSet<Stmt> = graph.stmts.toMutableSet()
    private val registeredEdges: MutableSet<Edge> = graph.edges.toMutableSet()
    // this field is required for visualization
    private val allExplicitEdges: MutableSet<Edge> = graph.edges.toMutableSet()

    val allEdges: Set<Edge> get() = allExplicitEdges + implicitEdges

    /**
     * Used in [org.utbot.engine.selectors.nurs.InheritorsSelector] for a fast search of Virtual invoke successors.
     */
    val invokeSuccessors: MutableMap<Stmt, MutableSet<Stmt>> = mutableMapOf()
    val traps: MutableList<Pair<Stmt, SootClass>> = graph.traps
    val implicitEdges: MutableSet<Edge> = mutableSetOf()

    private val exceptionalSuccs: MutableMap<Stmt, Set<Stmt>> = graph.exceptionalSuccs.toMutableMap()
    private val unexceptionalSuccs: MutableMap<Stmt, Set<Stmt>> = graph.unexceptionalSuccs.toMutableMap()

    private val stmtToGraph: MutableMap<Stmt, ExceptionalUnitGraph> = stmts.associateWithTo(mutableMapOf()) { graph }
    private val edgeToGraph: MutableMap<Edge, ExceptionalUnitGraph> = graph.edges.associateWithTo(mutableMapOf()) { graph }

    private val registeredEdgesCount: MutableMap<Stmt, Int> = graph.outgoingEdgesCount
    private val allEdgesCount: MutableMap<Stmt, Int> = graph.outgoingEdgesCount
    private val outgoingEdgesCount: MutableMap<Stmt, Int> = graph.outgoingEdgesCount

    fun method(stmt: Stmt): SootMethod = stmtToGraph[stmt]?.body?.method ?: error("$stmt not in graph.")

    /**
     * @return all unexceptional successors of stmt
     */
    fun succStmts(stmt: Stmt): List<Stmt> =
        (exceptionalSuccs[stmt] ?: emptyList()) + (unexceptionalSuccs[stmt] ?: emptyList())

    /**
     * @return single edge to unexceptional successor of stmt
     * @throws error if successor of stmt is not single
     */
    fun succ(stmt: Stmt): Edge = unexceptionalSuccs[stmt]
        ?.firstOrNull()
        ?.let { Edge(stmt, it, 0) }
        ?: error("$stmt not in graph.")

    /**
     * @return all edges to unexceptional successors of stmt
     */
    fun succs(stmt: Stmt): List<Edge> = unexceptionalSuccs[stmt]
        ?.mapIndexed { i, v -> Edge(stmt, v, i) }
        ?: error("$stmt not in graph.")

    /**
     * @return all edges to exceptional successors of stmt
     */
    fun exceptionalSuccs(stmt: Stmt): List<Edge> = exceptionalSuccs[stmt]
        ?.mapIndexed { i, v -> Edge(stmt, v, EXCEPTION_DECISION_SHIFT + i) }
        ?: error("$stmt not in graph.")

    private val ExceptionalUnitGraph.outgoingEdgesCount: MutableMap<Stmt, Int>
        get() = stmts.associateWithTo(mutableMapOf()) { succ(it).count() }

    /**
     * Returns a method that the [edge] is belongs to. If the [edge] does not
     * have such method, null will be returned.
     *
     * Note: for example, implicit edges and edges produced by an invocation
     * does not have a method they could belong to.
     */
    private fun methodByEdge(edge: Edge): SootMethod? = edgeToGraph[edge]?.body?.method

    /**
     * Joins a new [graph] to the [stmt]. Depending on the [registerEdges], edges of the [graph]
     * might be either registered in the global graph or not.
     *
     * If they are registered, path selector tries to cover all edges from the [graph].
     */
    fun join(stmt: Stmt, graph: ExceptionalUnitGraph, registerEdges: Boolean) {
        attachAllowed = false

        val invokeEdge = Edge(stmt, graph.head, CALL_DECISION_NUM)
        val method = graph.body.method
        val alreadyJoined = method in methods

        if (!alreadyJoined) {
            graphs += graph
            methods += method
            traps += graph.traps
            exceptionalSuccs += graph.exceptionalSuccs
            unexceptionalSuccs += graph.unexceptionalSuccs

            graph.edges.forEach { edgeToGraph[it] = graph }

            val joinedStmts = graph.stmts.onEach { stmtToGraph[it] = graph }
            outgoingEdgesCount += graph.outgoingEdgesCount
            stmts += joinedStmts

            allExplicitEdges += graph.edges
            allEdgesCount += graph.outgoingEdgesCount
            allEdgesCount.computeIfPresent(stmt) { _, value -> value + 1 }

            if (registerEdges) {
                registeredMethods += method
                registeredEdgesCount += graph.outgoingEdgesCount
                registeredEdges += graph.edges
                registeredEdgesCount.computeIfPresent(stmt) { _, value ->
                    value + 1
                }
            }

            // it is important to have this method call after we register the given method
            // because we find uncoveredPoints for unregistered methods only
            method.uncoveredThrowStatements()
        }

        invokeSuccessors.compute(stmt) { _, value ->
            value?.apply { add(graph.head) } ?: mutableSetOf(graph.head)
        }

        registeredEdges += invokeEdge
        allExplicitEdges += invokeEdge

        outgoingEdgesCount.computeIfPresent(stmt) { _, value ->
            value + 1
        }

        statistics.forEach {
            it.onJoin(stmt, graph, registerEdges)
        }
    }

    /**
     * mark that executionState is successfully and completely traversed
     */
    fun traversed(executionState: ExecutionState) {
        attachAllowed = false
        executionState.close()
        coveredOutgoingEdges.putIfAbsent(executionState.stmt, 0)

        for (edge in executionState.edges) {
            markAsCoveredStmt(edge.src)
            markAsCoveredStmt(edge.dst)

            when (edge) {
                in implicitEdges -> coveredImplicitEdges += edge
                !in registeredEdges, !in coveredEdges-> {
                    coveredOutgoingEdges.putIfAbsent(edge.src, 0)
                    coveredOutgoingEdges.compute(edge.src) { _, value -> (value ?: 0) + 1 }
                    coveredEdges += edge
                }
            }
        }
        statistics.forEach {
            it.onTraversed(executionState)
        }
    }

    /**
     * Returns uncovered throw statements for [this] method.
     */
    private fun SootMethod.uncoveredThrowStatements(): Set<Stmt> {
        val throwStatements = methodToUncoveredThrowStatements[this]

        if (throwStatements != null) {
            return throwStatements
        }

        // We don't have to additionally specify throw statements
        // since for the registeredMethods we try to cover all of their edges.
        if (this in registeredMethods) {
            return emptySet()
        }

        // We don't want to cover all the exceptions in not overridden library classes
        if (declaringClass.isLibraryNonOverriddenClass) {
            return emptySet()
        }

        val uncoveredThrowStatements = methodToUncoveredThrowStatements.getOrPut(this) { mutableSetOf() }

        if (!canRetrieveBody()) {
            return uncoveredThrowStatements
        }

        uncoveredThrowStatements += jimpleBody().units.filterIsInstance<JThrowStmt>()

        return uncoveredThrowStatements
    }

    private fun markAsCoveredStmt(stmt: Stmt) {
        val method = methodByStmt(stmt)
        methodToUncoveredThrowStatements[method]?.remove(stmt)
    }

    /**
     * register new implicit edge, that is not represented in graph
     */
    fun registerImplicitEdge(edge: Edge) {
        attachAllowed = false
        implicitEdges.add(edge)
    }

    /**
     * mark edge as visited during traverse
     */
    fun visitEdge(edge: Edge) {
        attachAllowed = false
        statistics.forEach {
            it.onVisit(edge)
        }
    }

    /**
     * mark node as visited during traverse
     */
    fun visitNode(executionState: ExecutionState) {
        statistics.forEach {
            it.onVisit(executionState)
        }
    }

    /**
     * attach new statistics to graph before any modifying operations
     */
    @Suppress("UNREACHABLE_CODE")
    fun attach(statistics: TraverseGraphStatistics) {
        if (!attachAllowed) {
            throw error("Cannot attach new statistics. Graph have modified.")
        }
        this.statistics += statistics
    }

    private val ExceptionalUnitGraph.exceptionalSuccs: Map<Stmt, Set<Stmt>>
        get() = exceptionalSuccsCache.getOrPut(this) {
            stmts.associateWithTo(mutableMapOf()) { stmt -> this.exceptionalSucc(stmt).toSet() }
        }

    private val ExceptionalUnitGraph.unexceptionalSuccs: Map<Stmt, Set<Stmt>>
        get() = unexceptionalSuccsCache.getOrPut(this) {
            stmts.associateWithTo(mutableMapOf()) { stmt -> this.unexceptionalSucc(stmt).toSet() }
        }

    private val ExceptionalUnitGraph.edges: List<Edge>
        get() = edgesCache.getOrPut(this) {
            stmts.flatMap { succ(it).mapIndexed { id, v -> Edge(it, v, id) } }
        }

    private val coveredEdges: MutableSet<Edge> = mutableSetOf()
    val coveredImplicitEdges: MutableSet<Edge> = mutableSetOf()
    private val coveredOutgoingEdges: MutableMap<Stmt, Int> = mutableMapOf()

    /**
     * Statement is covered if all the outgoing edges are covered.
     */
    fun isCovered(stmt: Stmt) =
        stmt !in registeredEdgesCount || isCoveredIgnoringRegistration(stmt)

    fun isCoveredIgnoringRegistration(stmt: Stmt) =
        stmt in coveredOutgoingEdges && coveredOutgoingEdges[stmt]!! >= allEdgesCount[stmt]!!

    /**
     * Edge is covered if we visited it in successfully completed execution at least once
     */
    fun isCovered(edge: Edge) =
        if (edge in implicitEdges) {
            edge in coveredImplicitEdges
        } else {
            (edge in coveredEdges || edge !in registeredEdges)
        }

    /**
     * If the [edge] does not have associated method, returns a result of [isCovered] call.
     * Otherwise, there are several cases:
     * * If the [edge] in [coveredEdges], returns true;
     * * If the [edge] belongs to a method of some overridden class and there is
     * uncovered throw statements in it, returns false;
     * * In all other cases, returns whether the [edge] absent in [registeredEdges].
     */
    fun isCoveredWithAllThrowStatements(edge: Edge): Boolean {
        val method = methodByEdge(edge) ?: return isCovered(edge)
        
        if (edge in coveredEdges) {
            return true
        }

        if (method.declaringClass.isOverridden && method.uncoveredThrowStatements().isNotEmpty()) {
            return false
        }

        return edge !in registeredEdges
    }

    /**
     * @return true if stmt has more than one outgoing edge
     */
    fun isFork(stmt: Stmt): Boolean = (outgoingEdgesCount[stmt] ?: 0) > 1

    private fun methodByStmt(stmt: Stmt) = stmtToGraph.getValue(stmt).body.method
}

private fun ExceptionalUnitGraph.succ(stmt: Stmt): Sequence<Stmt> = exceptionalSucc(stmt) + unexceptionalSucc(stmt)

private fun ExceptionalUnitGraph.exceptionalSucc(stmt: Stmt): Sequence<Stmt> =
    this.getExceptionalSuccsOf(stmt).asSequence().filterIsInstance<Stmt>()

private fun ExceptionalUnitGraph.unexceptionalSucc(stmt: Stmt): Sequence<Stmt> = when (stmt) {
    is SwitchStmt -> stmt.targets.asSequence().filterIsInstance<Stmt>() + stmt.defaultTarget as Stmt
    else -> this.getUnexceptionalSuccsOf(stmt).asSequence().filterIsInstance<Stmt>()
}

val ExceptionalUnitGraph.stmts
    get() = this.body.units.filterIsInstance<Stmt>()

private val ExceptionalUnitGraph.traps
    get() = this.body.traps.mapTo(mutableListOf()) { it.handlerUnit as Stmt to it.exception }

val ExceptionalUnitGraph.head: Stmt
    get() = this.heads.filterIsInstance<Stmt>().single { it !in traps.map { trap -> trap.first }}

val Stmt.isReturn
    get() = this is JReturnStmt ||
            this is JReturnVoidStmt