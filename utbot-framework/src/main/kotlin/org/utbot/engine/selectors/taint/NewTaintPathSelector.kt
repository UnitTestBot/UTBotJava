package org.utbot.engine.selectors.taint

import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.head
import org.utbot.engine.jimpleBody
import org.utbot.engine.selectors.BasePathSelector
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy
import org.utbot.engine.state.ExecutionStackElement
import org.utbot.engine.state.ExecutionState
import org.utbot.engine.taint.TaintSinkData
import org.utbot.engine.taint.TaintSourceData
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.util.graph
import org.utbot.framework.util.sootMethod
import soot.Scene
import soot.SootMethod
import soot.jimple.Stmt
import soot.jimple.toolkits.callgraph.CallGraph
import soot.toolkits.graph.ExceptionalUnitGraph
import java.util.PriorityQueue

class NewTaintPathSelector(
    val graph: ExceptionalUnitGraph,
    private val taintsToBeFound: Map<TaintSourceData, Set<TaintSinkData>>,
    choosingStrategy: ChoosingStrategy,
    stoppingStrategy: StoppingStrategy
) : BasePathSelector(choosingStrategy, stoppingStrategy) {
    private val executionQueue: PriorityQueue<ExecutionState> = PriorityQueue(Comparator.comparingInt { it.weight })
    private val taintSources: Set<TaintSourceData> = taintsToBeFound.keys
    private val callGraph: CallGraph = Scene.v().callGraph
    private val globalGraph: InterProceduralUnitGraph = InterProceduralUnitGraph(graph)

    override fun removeImpl(state: ExecutionState): Boolean = executionQueue.remove(state)

    override fun pollImpl(): ExecutionState? = executionQueue.peek()?.also { executionQueue.remove(it) }

    override fun peekImpl(): ExecutionState? = if (executionQueue.isEmpty()) null else executionQueue.peek()

    override fun offerImpl(state: ExecutionState) {
        executionQueue += state
    }

    private val ExecutionState.weight: Int
        get() {
            val distance = run {
                var previousStates: List<ExecutionStackElement> = executionStack
                val visitedTaintSources = visitedTaintSources(taintSources)

                val targetPoints = if (visitedTaintSources.isEmpty()) {
                    // Need to reach any of taint sources (or the closest?)
                    taintSources.map { StmtWithOuterMethod(it.stmt, it.outerMethod) }
                } else {
                    // Need to reach the closest corresponding sink
                    taintSinksBySources(visitedTaintSources).map { StmtWithOuterMethod(it.stmt, it.outerMethod) }
                }

                val targetMethods = targetPoints.map { it.method.sootMethod }

                // Contains all stack elements before the one, that can reach pointsToReach
                var previousStackElements = mutableListOf<ExecutionStackElement>()
                var methodPaths: List<MethodsPath>
                var stateMethod: SootMethod
                do {
                    if (previousStates.isEmpty()) {
                        if (lastMethod !in targetMethods) {
                            // We didn't find a path to a source or sink
                            // TODO give an infinite weight to this state?
                            return@run INF
                        } else {
                            // We already in the target method
                            methodPaths = emptyList()
                            stateMethod = lastMethod!!
                            break
                        }
                    }

                    val stackElement = previousStates.last()
                    stateMethod = stackElement.method
                    methodPaths = runDijkstra(stateMethod, targetMethods)
                    previousStates = previousStates.subList(0, previousStates.lastIndex)

                    previousStackElements += stackElement
                } while (methodPaths.isEmpty())
                // Drop the last stack element since we don't need to reach its exit points
                previousStackElements = previousStackElements.subList(0, previousStackElements.lastIndex)

                // Check if we are already in the required method
                if (lastMethod in targetMethods) {
                    val methodGraph = lastMethod!!.jimpleBody().graph()

                    return@run calculateDistancesInProceduralGraphToSpecifiedStmts(
                        stmt,
                        methodGraph,
                        targetPoints.map { it.stmt }
                    ).minOfOrNull { it.value } ?: INF
                }

                val theShortestInterProceduralPath = methodPaths.minByOrNull { it.size } ?: emptyList()
                val sourceStmtsAlongTheShortestPath =
                    retrieveSourceStmtsByPath(stateMethod, theShortestInterProceduralPath)

                // If the current state has no paths to reach required points but methods before have,
                // we need to find return statements of all states before the state with stateMethod
                val distancesToReturnStmts = previousStackElements.map {
                    val methodGraph = it.method.jimpleBody().graph()
                    val returnStmts = methodGraph.tails.map { tail -> tail as Stmt }

                    calculateDistancesInProceduralGraphToSpecifiedStmts(methodGraph.head, methodGraph, returnStmts)
                }

                // For the first method we need to calculate distances to source stmts from the current stmt
                val currentStackStmt = previousStackElements.lastOrNull()?.caller ?: stmt
                val firstMethodInInterProceduralPath = sourceStmtsAlongTheShortestPath.firstOrNull()
                val distanceFromCurrentStmtToFirstNextStmts = firstMethodInInterProceduralPath?.let {
                    val methodGraph = it.first.jimpleBody().graph()

                    calculateDistancesInProceduralGraphToSpecifiedStmts(
                        currentStackStmt,
                        methodGraph,
                        firstMethodInInterProceduralPath.second
                    )
                } ?: emptyMap()

                val nextStmtsToNextMethods = if (sourceStmtsAlongTheShortestPath.isEmpty()) {
                    emptyList()
                } else {
                    sourceStmtsAlongTheShortestPath.subList(1, sourceStmtsAlongTheShortestPath.lastIndex)
                }
                val distancesFromNextStmtsToNextMethods = nextStmtsToNextMethods.map {
                    val methodGraph = it.first.jimpleBody().graph()

                    calculateDistancesInProceduralGraphToSpecifiedStmts(methodGraph.head, methodGraph, it.second)
                }

                val innerProceduralDistance = distancesToReturnStmts.sumOf { it.values.min() } +
                        (distanceFromCurrentStmtToFirstNextStmts.minOfOrNull { it.value } ?: 0) +
                        distancesFromNextStmtsToNextMethods.sumOf { it.values.min() }

                val interProceduralDistance = theShortestInterProceduralPath.size

                innerProceduralDistance * INNER_DISTANCE_COEFFICIENT + interProceduralDistance * INTER_DISTANCE_COEFFICIENT
            }

            // The bigger distance means the less weight
            return -distance
        }

    override fun isEmpty(): Boolean = executionQueue.isEmpty()

    override val name: String = "TaintPathSelector"

    override fun close() {
        executionQueue.forEach {
            it.close()
        }
    }

    private fun ExecutionState.pathsToMethods(targetMethods: List<SootMethod>): List<MethodsPath> {
        var previousStateElements: List<ExecutionStackElement> = executionStack

        while (previousStateElements.isNotEmpty()) {
            val currentStackElement = previousStateElements.last()
            val methodsPaths = currentStackElement.pathsToMethods(targetMethods)

            if (methodsPaths.isNotEmpty()) {
                return methodsPaths
            }

            previousStateElements = previousStateElements.subList(0, previousStateElements.lastIndex)
        }

        return emptyList()
    }

    private fun ExecutionStackElement.pathsToMethods(targetMethods: List<SootMethod>): List<MethodsPath> =
        runDijkstra(method, targetMethods)

    // TODO seems we do not need dijkstra to find the shortest paths in the call graph, BFS should be enough
    private fun runDijkstra(stateMethod: SootMethod, methodsToReach: List<SootMethod>): List<MethodsPath> {
        val parents = mutableMapOf<SootMethod, SootMethod>()
        val distances = mutableMapOf<SootMethod, Int>()
        val queue = PriorityQueue<Pair<Int, SootMethod>>(Comparator.comparingInt { it.first })

        queue += 0 to stateMethod
        distances[stateMethod] = 0
        while (queue.isNotEmpty()) {
            val (d, srcMethod) = queue.poll()
            val distanceFrom = distances.getOrPut(srcMethod) { INF }
            if (d > distanceFrom) {
                continue
            }

            for (edge in callGraph.edgesOutOf(srcMethod)) {
                val targetMethod = edge.tgt.method()
                val distanceTo = distances.getOrPut(targetMethod) { INF }
                if (distanceFrom + 1 < distanceTo) {
                    distances[targetMethod] = distanceFrom + 1
                    parents[targetMethod] = srcMethod
                    queue += -(distanceFrom + 1) to targetMethod
                }
            }
        }

        // It is more convenient to think that we have reached the stateMethod from itself
        parents[stateMethod] = stateMethod

        return methodsToReach.mapNotNull {
            recoverPath(it, stateMethod, parents).ifEmpty { null }
        }
    }

    private fun recoverPath(
        methodToReach: SootMethod,
        startMethod: SootMethod,
        parents: MutableMap<SootMethod, SootMethod>
    ): MethodsPath =
        generateSequence(parents[methodToReach]) { parentMethod ->
            parents[parentMethod]?.let {
                if (it == startMethod) null else it
            }
        }.toList().asReversed()

    private fun retrieveSourceStmtsByPath(
        startMethod: SootMethod,
        path: MethodsPath
    ): List<Pair<SootMethod, List<Stmt>>> {
        var curMethod = startMethod

        val stmtsToNextMethods = mutableListOf<Pair<SootMethod, List<Stmt>>>()
        for (nextMethod in path) {
            val stmtsToNextMethod = curMethod.activeBody.units.filter { unit ->
                val edgesOutOfStmt = callGraph.edgesOutOf(unit)
                val targetMethods = edgesOutOfStmt.asSequence().toList().map { it.tgt.method() }

                nextMethod in targetMethods
            }.map { it as Stmt }

            curMethod = nextMethod
            stmtsToNextMethods += curMethod to stmtsToNextMethod

            stmtsToNextMethod.forEach {
                globalGraph.join(it, nextMethod.jimpleBody().graph(), registerEdges = false/*TODO register or not?*/)
            }
        }

        return stmtsToNextMethods
    }

    private fun calculateDistancesInProceduralGraphToSpecifiedStmts(
        start: Stmt,
        graph: ExceptionalUnitGraph,
        targets: List<Stmt>
    ): Map<Stmt, Int> {
        val distances = calculateDistancesInProceduralGraphFromStmtWithBfs(start, graph)

        return targets.associateWith {
            distances[it] ?: error("Distance from $start to $it was not calculated")
        }
    }

    private fun calculateDistancesInProceduralGraphFromStmtWithBfs(
        start: Stmt,
        graph: ExceptionalUnitGraph
    ): Map<Stmt, Int> {
        val used = mutableMapOf<Stmt, Boolean>()
        val distances = mutableMapOf<Stmt, Int>()
        val parents = mutableMapOf<Stmt, Stmt>()
        val queue = ArrayDeque<Stmt>()

        queue += start
        distances += start to 0
        used[start] = true
        while (!queue.isEmpty()) {
            val stmtFrom = queue.removeFirst()
            val distanceFrom = distances.getOrDefault(stmtFrom, INF)

            for (stmtTo in graph.getSuccsOf(stmtFrom)) {
                stmtTo as Stmt
                if (!used.getOrDefault(stmtTo, false)) {
                    used[stmtTo] = true
                    queue += stmtTo
                    distances[stmtTo] = distanceFrom + 1
                    parents[stmtTo] = stmtFrom
                }
            }
        }

        return distances
    }

    private fun taintSinksBySources(sources: Set<TaintSourceData>): Set<TaintSinkData> =
        sources.flatMapTo(mutableSetOf()) { taintsToBeFound[it] ?: emptySet() }

    private fun taintSinksBySource(source: TaintSourceData): Set<TaintSinkData> = taintSinksBySources(setOf(source))

    private data class StmtWithOuterMethod(val stmt: Stmt, val method: ExecutableId)

    companion object {
        const val INF: Int = Int.MAX_VALUE
        const val INNER_DISTANCE_COEFFICIENT: Int = 1
        const val INTER_DISTANCE_COEFFICIENT: Int = 3
    }
}

private typealias MethodsPath = List<SootMethod>

private fun ExecutionState.visitedTaintSources(taintSources: Set<TaintSourceData>): Set<TaintSourceData> =
    path.toSet().let { pathStmts ->
        taintSources.filterTo(mutableSetOf()) { it.stmt in pathStmts }
    }
