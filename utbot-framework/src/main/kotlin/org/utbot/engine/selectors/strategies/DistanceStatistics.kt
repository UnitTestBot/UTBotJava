package org.utbot.engine.selectors.strategies

import org.utbot.engine.Edge
import org.utbot.engine.ExecutionState
import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.isReturn
import org.utbot.engine.pathLogger
import org.utbot.engine.stmts
import kotlin.math.min
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import soot.jimple.Stmt
import soot.toolkits.graph.ExceptionalUnitGraph

/**
 * calculates distances between stmts
 * - on joins of new graphs recalculates distances between stmts
 * - on traversed updates distances to closest uncovered stmts
 */
class DistanceStatistics(
    graph: InterProceduralUnitGraph
) : TraverseGraphStatistics(graph), ChoosingStrategy {
    private val sortedDistances = mutableMapOf<Stmt, List<Distance>>()
    private val distancePointers = mutableMapOf<Stmt, Int>()
    private val calls: MutableMap<Stmt, MutableSet<Stmt>> = mutableMapOf()

    /**
     * Map represents join relation graph1 in ancestors(graph2) iff exists stmt in graph1 such that graph2 joined to stmt
     */
    private val ancestors = mutableMapOf<ExceptionalUnitGraph, MutableSet<ExceptionalUnitGraph>>()

    /**
     * Map of distances to closest uncovered statements
     */
    private val distanceToClosestUncovered: MutableMap<Stmt, Int> =
        graph.stmts.associateWithTo(mutableMapOf()) { 0 }

    /**
     * Drops executionState if all the edges on path are covered (with respect to uncovered
     * throw statements of the methods they belong to) and there is no reachable and uncovered statement.
     */
    override fun shouldDrop(state: ExecutionState): Boolean {
        val shouldDrop = state.edges.all { graph.isCoveredWithAllThrowStatements(it) } && distanceToUncovered(state) == Int.MAX_VALUE

        if (shouldDrop) {
            pathLogger.debug {
                "Dropping state (lastStatus=${state.solver.lastStatus}) by the distance statistics. MD5: ${state.md5()}"
            }
        }

        return shouldDrop
    }

    fun isCovered(edge: Edge): Boolean = graph.isCovered(edge)

    private val closestToReturn = mutableMapOf<Stmt, Int>()

    init {
        recomputeDistances(graph.graphs)
        updateDistanceToUncovered()
    }

    private fun recomputeDistances(graphs: List<ExceptionalUnitGraph>) {
        val used = mutableSetOf<ExceptionalUnitGraph>()
        val queue = ArrayDeque<ExceptionalUnitGraph>().apply { addAll(graphs) }
        while (queue.isNotEmpty()) {
            val u = queue.removeFirst()
            used += u
            u.stmts.forEach { recomputeDistancesBFS(it) }
            queue += (ancestors[u] ?: emptyList()).filterNot { it in used }
        }
    }

    private fun recomputeDistancesBFS(stmt: Stmt) {
        val queue = ArrayDeque<Pair<Stmt, PersistentList<Stmt>>>()
        val distances = mutableListOf(mutableMapOf(stmt to 0))
        val minDistances = mutableMapOf<Stmt, Int>()
        queue += stmt to persistentListOf()
        while (queue.isNotEmpty()) {
            val (v, stack) = queue.removeFirst()
            val callees = calls[v]
            val i = stack.size
            val dist = distances[i][v]!!
            when {
                callees != null -> callees.forEach { callee ->
                    if (v in stack) {
                        return@forEach
                    } else {
                        if (distances.size <= i + 1) {
                            distances.add(mutableMapOf())
                        }
                        if (distances[i + 1].getOrDefault(callee, Int.MAX_VALUE) > dist + 1) {
                            distances[i + 1][callee] = dist + 1
                            queue += callee to stack.add(v)
                        }
                    }
                }
                else -> {
                    for (u in graph.succStmts(v)) {
                        if (distances[i].getOrDefault(u, Int.MAX_VALUE) > dist + 1) {
                            distances[i][u] = dist + 1
                            queue += u to stack
                        }
                    }
                    if (v.isReturn) {
                        if (stack.isEmpty()) {
                            minDistances.merge(stmt, dist) { a, b ->
                                min(a, b)
                            }
                        } else {
                            for (u in graph.succStmts(stack.last())) {
                                if (distances[i - 1].getOrDefault(u, Int.MAX_VALUE) > dist + 1) {
                                    distances[i - 1][u] = dist + 1
                                    queue += u to stack.removeAt(stack.lastIndex)
                                }
                            }
                        }
                    }
                }
            }
        }
        val mergedMap = distances.fold(mutableMapOf<Stmt, Int>()) { map1, map2 ->
            for (entry in map2) {
                map1.merge(entry.key, entry.value) { a, b ->
                    min(a, b)
                }
            }
            map1
        }
        sortedDistances[stmt] = mergedMap.map { Distance(it.key, it.value) }.sortedBy { it.distance }
        distancePointers[stmt] = sortedDistances[stmt]!!.indexOfFirst { !graph.isCovered(it.to) }.let {
            if (it < 0) sortedDistances[stmt]!!.size else it
        }
        closestToReturn[stmt] = minDistances.minOfOrNull { it.value } ?: Int.MAX_VALUE
    }

    /**
     * minimal distance to closest uncovered statement in interprocedural graph for execution.
     */
    fun distanceToUncovered(state: ExecutionState): Int {
        var calc = 0
        var stmt: Stmt = state.stmt
        val distances = mutableListOf<Int>()
        if (state.lastEdge != null && state.lastEdge in graph.implicitEdges) {
            return if (state.lastEdge in graph.coveredImplicitEdges) {
                Int.MAX_VALUE
            } else {
                0
            }
        }

        for (stackElement in state.executionStack.asReversed()) {
            val caller = stackElement.caller
            val distance = distanceToClosestUncovered[stmt] ?: Int.MAX_VALUE
            val distanceToRet = closestToReturn[stmt] ?: error("$stmt is not in graph")
            if (distance != Int.MAX_VALUE) {
                distances += calc + distance
            }
            if (caller == null) {
                break
            }
            if (distanceToRet != Int.MAX_VALUE) {
                calc += distanceToRet
            } else {
                break
            }
            stmt = caller
        }
        return distances.minOrNull() ?: Int.MAX_VALUE
    }

    /**
     * Distance to closest uncovered statement in the same method and inner (currently joined) methods
     */
    private val Stmt.distanceToUncovered: Int
        get() {
            val i = distancePointers.computeIfPresent(this) { _, i ->
                var j = i
                val distances = sortedDistances[this]!!
                while (j < distances.size && graph.isCovered(distances[j].to)) {
                    j++
                }
                if (distances.size == j) {
                    null
                } else {
                    j
                }
            }
            return if (i == null) {
                Int.MAX_VALUE
            } else {
                sortedDistances.getValue(this)[i].distance
            }
        }

    private fun updateDistanceToUncovered() =
        distanceToClosestUncovered.replaceAll { u, value ->
            if (value == Int.MAX_VALUE) {
                value
            } else {
                u.distanceToUncovered
            }
        }

    /**
     * Recompute distances for methods that can be changed after this join and update
     * correspondingly closestToUncovered
     */
    override fun onJoin(stmt: Stmt, graph: ExceptionalUnitGraph, shouldRegister: Boolean) {
        val stmts = graph.stmts.toSet()

        recomputeDistances(listOf(graph))
        updateDistanceToUncovered()
        if (shouldRegister) {
            if (stmts.first() !in distanceToClosestUncovered) {
                stmts.forEach { u ->
                    distanceToClosestUncovered.computeIfAbsent(u) { it.distanceToUncovered }
                }
            }
        }
        notifyObservers()
    }

    override fun onTraversed(executionState: ExecutionState) {
        updateDistanceToUncovered()
        notifyObservers()
    }
}

private data class Distance(val to: Stmt, val distance: Int)