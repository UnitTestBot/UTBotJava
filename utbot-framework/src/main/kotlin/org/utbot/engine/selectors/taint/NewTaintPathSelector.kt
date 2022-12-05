package org.utbot.engine.selectors.taint

import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.head
import org.utbot.engine.isReturn
import org.utbot.engine.jimpleBody
import org.utbot.engine.pathLogger
import org.utbot.engine.selectors.BasePathSelector
import org.utbot.engine.selectors.nurs.InheritorsSelector
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy
import org.utbot.engine.state.ExecutionStackElement
import org.utbot.engine.state.ExecutionState
import org.utbot.engine.taint.TaintSinkData
import org.utbot.engine.taint.TaintSourceData
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.util.executableId
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
    private val executionQueue: PriorityQueue<StateWithWeight> = PriorityQueue(Comparator.comparingLong { it.weight!! })
    private val taintSources: Set<TaintSourceData> = taintsToBeFound.keys
    private val callGraph: CallGraph = Scene.v().callGraph
    private val globalGraph: InterProceduralUnitGraph = InterProceduralUnitGraph(graph)
    private val alreadyPushedStateHashes: MutableSet<Int> = mutableSetOf()

    private var counter: Int = 0

    override fun removeImpl(state: ExecutionState): Boolean = executionQueue.remove(StateWithWeight(state))

    override fun pollImpl(): ExecutionState? = if (executionQueue.isEmpty()) null else executionQueue.poll().executionState

    override fun peekImpl(): ExecutionState? = if (executionQueue.isEmpty()) null else executionQueue.peek().executionState

    override fun offerImpl(state: ExecutionState) {
        if (counter++ % 5 == 0) {
//            logQueue()
        }

        val hashCode = state.hashCode()
        if (hashCode in alreadyPushedStateHashes) {
            return
        }

        val weight = state.weight
//        if (weight > MAX_DISTANCE) {
//            return
//        }

        val weightWithLoopCoefficient = with(state) {
            val numberOfStmtOccurrencesInPath = visitedStatementsHashesToCountInPath[stmt.hashCode()] ?: 0

            // Drop loop state if it exceeds loop steps limit
            UtSettings.loopStepsLimit
                .takeIf { it > 0 }
                ?.let {
                    if (numberOfStmtOccurrencesInPath > it) {
                        return
                    }
                }

            weight + numberOfStmtOccurrencesInPath * InheritorsSelector.REPEATED_STMT_COEFFICIENT
        }

        executionQueue += StateWithWeight(state, weight)
        alreadyPushedStateHashes += hashCode
    }

    private fun logQueue() {
        queue().forEach {
            pathLogger.warn {
                "Method - (${it.first.lastMethod}), stmt - (${it.first.stmt}), weight - (${it.second})"
            }
        }
    }

    private val ExecutionState.weight: Long
        get() {
            val distance = run {
                val visitedTaintSources = visitedTaintSources(taintSources)

                val targetPoints = if (visitedTaintSources.isEmpty()) {
                    // Need to reach any of taint sources (or the closest?)
                    taintSources.map { StmtWithOuterMethod(it.stmt, it.outerMethod) }
                } else {
                    // Need to reach the closest corresponding sink
                    val correspondingSinks =
                        taintSinksBySources(visitedTaintSources).map { StmtWithOuterMethod(it.stmt, it.outerMethod) }

                    // But do not reach already visited sinks
                    val visitedStmts = path.toSet()
                    correspondingSinks.filterNot { it.stmt in visitedStmts }
                }

                val targetMethods = targetPoints.mapTo(mutableSetOf()) { it.method.sootMethod }

                val (methodsPaths, stackElementsToReturn) = pathsToMethods(targetMethods)

                if (methodsPaths.isEmpty()) {
                    // Cannot reach target methods from the current state
                    return@run MAX_DISTANCE
                }

//                // Check if we are already in the required method
//                if (lastMethod in targetMethods) {
//                    val methodGraph = lastMethod!!.jimpleBody().graph()
//
//                    return@run calculateDistancesInProceduralGraphToSpecifiedStmts(
//                        stmt,
//                        methodGraph,
//                        targetPoints.map { it.stmt }
//                    ).minOfOrNull { it.value } ?: INF
//                }

                val theShortestInterProceduralPath = methodsPaths.minByOrNull { it.size } ?: emptyList()
                val sourceStmtsAlongTheShortestPath =
                    retrieveStmtsToReachMethodsAlongPath(theShortestInterProceduralPath)

                // If the current state has no paths to reach required points but methods before have,
                // we need to find return statements of all states before the state with stateMethod.
                // For that we need to find firstly current stmts in all state before the target state
                val currentStmtsInStateToReturn = if (stackElementsToReturn.isEmpty()) {
                    emptyList()
                } else {
                    val result = mutableListOf<Pair<ExecutionStackElement, Stmt>>()
                    var curStateCurStmt: Stmt? = stmt

                    for (i in stackElementsToReturn.lastIndex downTo 0) {
                        val curStackElement = stackElementsToReturn[i]
                        result += curStackElement to curStateCurStmt!!
                        curStateCurStmt = curStackElement.caller
                    }

                    result
                }

                val distancesToReturnStmts = currentStmtsInStateToReturn.map {
                    val (stackElement, curStmt) = it

                    val methodGraph = stackElement.method.jimpleBody().graph()
                    val returnStmts = methodGraph.tails.filterIsInstance<Stmt>().filter { tail -> tail.isReturn }

                    calculateDistancesInProceduralGraphToSpecifiedStmts(curStmt, methodGraph, returnStmts)
                }

                // For the first method we need to calculate distances to source stmts from the current stmt
                val stmtToStartToReachMethods = stackElementsToReturn.firstOrNull()?.caller ?: stmt
                val firstMethodInInterProceduralPath = sourceStmtsAlongTheShortestPath.firstOrNull()
                val distanceFromCurrentStmtToFirstNextStmts = firstMethodInInterProceduralPath?.let {
                    val methodGraph = it.first.jimpleBody().graph()

                    calculateDistancesInProceduralGraphToSpecifiedStmts(
                        stmtToStartToReachMethods,
                        methodGraph,
                        firstMethodInInterProceduralPath.second
                    )
                } ?: emptyMap()

                // For the last method we need to calculate distances to target stmts from the head
                // (or from the current stmt, if we will be already in the target method after returning by stack)
                val targetMethod = theShortestInterProceduralPath.last()
                val targetMethodGraph = targetMethod.jimpleBody().graph()
                val startStmtInTheTargetMethod = if (theShortestInterProceduralPath.size == 1) {
                    stmtToStartToReachMethods
                } else {
                    targetMethodGraph.head
                }
                val distancesToTargetStmts = calculateDistancesInProceduralGraphToSpecifiedStmts(
                    startStmtInTheTargetMethod,
                    targetMethodGraph,
                    targetPoints.filter { it.method == targetMethod.executableId }.map { it.stmt }
                )

                val nextStmtsToNextMethods = if (sourceStmtsAlongTheShortestPath.size < 2) {
                    emptyList()
                } else {
                    // We need to skip the first and the last methods, as they have already been counted
                    sourceStmtsAlongTheShortestPath.subList(1, sourceStmtsAlongTheShortestPath.lastIndex)
                }
                val distancesFromNextStmtsToNextMethods = nextStmtsToNextMethods.map {
                    val methodGraph = it.first.jimpleBody().graph()

                    calculateDistancesInProceduralGraphToSpecifiedStmts(methodGraph.head, methodGraph, it.second)
                }

                val innerProceduralDistance = (distanceFromCurrentStmtToFirstNextStmts.minOfOrNull { it.value } ?: 0) +
                        distancesFromNextStmtsToNextMethods.sumOf { it.values.min() } +
                        (distancesToTargetStmts.minOfOrNull { it.value } ?: 0)

                // We don't need to count the target method
                val interProceduralDistance = theShortestInterProceduralPath.size - 1

                val returnDistances = distancesToReturnStmts.sumOf { it.values.min() }

                innerProceduralDistance * INNER_DISTANCE_COEFFICIENT + returnDistances * RETURN_DISTANCE_COEFFICIENT + interProceduralDistance * INTER_DISTANCE_COEFFICIENT
            }

//             The bigger distance means the less weight
//            return -distance

            return distance
        }

    override fun isEmpty(): Boolean = executionQueue.isEmpty()

    override val name: String = "TaintPathSelector"

    override fun close() {
        executionQueue.forEach {
            it.executionState.close()
        }
    }

    override fun queue(): List<Pair<ExecutionState, Double>> =
        executionQueue.map { it.executionState to it.weight!!.toDouble() }.sortedBy { it.second }

    private fun ExecutionState.pathsToMethods(targetMethods: Set<SootMethod>): MethodsPathWithStackElementsToReturn {
        var previousStateElements: List<ExecutionStackElement> = executionStack

        while (previousStateElements.isNotEmpty()) {
            val currentStackElement = previousStateElements.last()
            val methodsPaths = currentStackElement.pathsToMethods(targetMethods)

            if (methodsPaths.isNotEmpty()) {
                return MethodsPathWithStackElementsToReturn(
                    methodsPaths,
                    executionStack.subList(previousStateElements.size, executionStack.size)
                )
            }

            previousStateElements = previousStateElements.subList(0, previousStateElements.lastIndex)
        }

        return MethodsPathWithStackElementsToReturn(emptyList(), executionStack)
    }

    private data class MethodsPathWithStackElementsToReturn(
        val methodsPath: List<MethodsPath>,
        val stackElementsToReturn: List<ExecutionStackElement>
    )

    private fun ExecutionStackElement.pathsToMethods(targetMethods: Set<SootMethod>): List<MethodsPath> =
//        runDijkstraInInterProceduralGraph(method, targetMethods)
        runBfsInInterProceduralGraph(method, targetMethods)

    private fun runBfsInInterProceduralGraph(start: SootMethod, targetMethods: Set<SootMethod>): List<MethodsPath> {
        val used = mutableMapOf<SootMethod, Boolean>()
        val distances = mutableMapOf<SootMethod, Long>()
        val parents = mutableMapOf<SootMethod, SootMethod>()
        val queue = ArrayDeque<SootMethod>()

        queue += start
        distances += start to 0L
        used[start] = true
        while (!queue.isEmpty()) {
            val srcMethod = queue.removeFirst()
            val distanceFrom = distances.getOrDefault(srcMethod, MAX_DISTANCE)

            for (edge in callGraph.edgesOutOf(srcMethod)) {
                val targetMethod = edge.tgt.method()

                if (!used.getOrDefault(targetMethod, false)) {
                    used[targetMethod] = true
                    queue += targetMethod
                    distances[targetMethod] = distanceFrom + 1
                    parents[targetMethod] = srcMethod
                }
            }
        }

        return targetMethods.mapNotNull {
            if (!used.getOrDefault(it, false)) {
                null
            } else {
                recoverPath(it, parents)
            }
        }
    }

    // TODO seems we do not need dijkstra to find the shortest paths in the call graph, BFS should be enough
    private fun runDijkstraInInterProceduralGraph(start: SootMethod, targetMethods: Set<SootMethod>): List<MethodsPath> {
        val parents = mutableMapOf<SootMethod, SootMethod>()
        val distances = mutableMapOf<SootMethod, Long>()
        val queue = PriorityQueue<Pair<Long, SootMethod>>(Comparator.comparingLong { it.first })

        queue += 0L to start
        distances[start] = 0L
        while (queue.isNotEmpty()) {
            val (d, srcMethod) = queue.poll()
            val distanceFrom = distances.getOrPut(srcMethod) { MAX_DISTANCE }
            if (d > distanceFrom) {
                continue
            }

            for (edge in callGraph.edgesOutOf(srcMethod)) {
                val targetMethod = edge.tgt.method()
                val distanceTo = distances.getOrPut(targetMethod) { MAX_DISTANCE }
                if (distanceFrom + 1 < distanceTo) {
                    distances[targetMethod] = distanceFrom + 1
                    parents[targetMethod] = srcMethod
                    queue += -(distanceFrom + 1) to targetMethod
                }
            }
        }

        return targetMethods.mapNotNull {
            if (distances.getOrDefault(it, MAX_DISTANCE) == MAX_DISTANCE) {
                null
            } else {
                recoverPath(it, parents)
            }
        }
    }

    private fun recoverPath(
        targetMethod: SootMethod,
        parents: Map<SootMethod, SootMethod>
    ): MethodsPath =
        generateSequence(targetMethod) { parents[it] }
            .toList()
            .asReversed()

    private fun retrieveStmtsToReachMethodsAlongPath(
        path: MethodsPath
    ): List<Pair<SootMethod, List<Stmt>>> {
        if (path.isEmpty()) {
            return emptyList()
        }

        var curMethod = path.first()

        val stmtsToNextMethods = mutableListOf<Pair<SootMethod, List<Stmt>>>()
        for (nextMethod in path.subList(1, path.size)) {
            val stmtsToNextMethod = curMethod.activeBody.units.filter { unit ->
                val edgesOutOfStmt = callGraph.edgesOutOf(unit)
                val targetMethods = edgesOutOfStmt.asSequence().toList().map { it.tgt.method() }

                nextMethod in targetMethods
            }.map { it as Stmt }

            stmtsToNextMethods += curMethod to stmtsToNextMethod
            curMethod = nextMethod

            stmtsToNextMethod.forEach {
                globalGraph.join(it, nextMethod.jimpleBody().graph(), registerEdges = true/*TODO register or not?*/)
            }
        }

        return stmtsToNextMethods
    }

    private fun calculateDistancesInProceduralGraphToSpecifiedStmts(
        start: Stmt,
        graph: ExceptionalUnitGraph,
        targets: List<Stmt>
    ): Map<Stmt, Long> {
        val distances = calculateDistancesInProceduralGraphFromStmtWithBfs(start, graph)

        return targets.associateWith {
            val i = distances[it]
            if (i != null) {
                i
            } else {
                // It means we have already skipped the target stmt, return INF
//                org.utbot.engine.pathLogger.warn { ("$it was already skipped: now at $start") }
                MAX_DISTANCE
            }
        }
    }

    private fun calculateDistancesInProceduralGraphFromStmtWithBfs(
        start: Stmt,
        graph: ExceptionalUnitGraph
    ): Map<Stmt, Long> {
        val used = mutableMapOf<Stmt, Boolean>()
        val distances = mutableMapOf<Stmt, Long>()
        val parents = mutableMapOf<Stmt, Stmt>()
        val queue = ArrayDeque<Stmt>()

        queue += start
        distances += start to 0L
        used[start] = true
        while (!queue.isEmpty()) {
            val stmtFrom = queue.removeFirst()
            val distanceFrom = distances.getOrDefault(stmtFrom, MAX_DISTANCE)

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

    private class StateWithWeight(val executionState: ExecutionState, val weight: Long? = null) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as StateWithWeight

            if (executionState != other.executionState) return false

            return true
        }

        override fun hashCode(): Int = executionState.hashCode()
    }

    companion object {
        const val MAX_DISTANCE: Long = Int.MAX_VALUE.toLong()
        const val MIN_DISTANCE: Long = -MAX_DISTANCE

        const val INNER_DISTANCE_COEFFICIENT: Long = 1L
        const val RETURN_DISTANCE_COEFFICIENT: Long = 2L
        const val INTER_DISTANCE_COEFFICIENT: Long = 10L
    }
}

private typealias MethodsPath = List<SootMethod>

class NeverDroppingStrategy(override val graph: InterProceduralUnitGraph) : ChoosingStrategy {
    override fun shouldDrop(state: ExecutionState): Boolean = false
}

private fun ExecutionState.visitedTaintSources(taintSources: Set<TaintSourceData>): Set<TaintSourceData> =
    path.toSet().let { pathStmts ->
        taintSources.filterTo(mutableSetOf()) { it.stmt in pathStmts }
    }
