package org.utbot.engine.selectors.taint

import org.utbot.common.doNotRun
import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.head
import org.utbot.engine.isOverridden
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
import soot.Unit
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
    private val taintSinks: Set<TaintSinkData> = taintsToBeFound.values.flatMapTo(mutableSetOf()) { it }
    private val callGraph: CallGraph = Scene.v().callGraph
    private val globalGraph: InterProceduralUnitGraph = InterProceduralUnitGraph(graph)
    private val alreadyPushedStateHashes: MutableSet<Int> = mutableSetOf()
    val visitedTaintPairs: MutableSet<TaintPair> = mutableSetOf()

    private val taintSourcesToVisit: Set<TaintSourceData>
        get() =
            taintsToBeFound.entries.filterNot {
                val sourceWithCorrespondingSinks = it.value.map { sink ->
                    TaintPair(it.key.stmt, sink.stmt)
                }

                visitedTaintPairs.containsAll(sourceWithCorrespondingSinks)
            }.mapTo(mutableSetOf()) { it.key }

    private var counter: Int = 0

    override fun removeImpl(state: ExecutionState): Boolean = executionQueue.remove(StateWithWeight(state))

    override fun pollImpl(): ExecutionState? = if (executionQueue.isEmpty()) null else executionQueue.poll().executionState

    override fun peekImpl(): ExecutionState? = if (executionQueue.isEmpty()) null else executionQueue.peek().executionState

    override fun offerImpl(state: ExecutionState) {
//        if (counter++ % 5 == 0) {
//            logQueue()
//        }

//        if (state.stmt.toString().let { "exchange" in it }) {
//            pathLogger.warn {  }
//        }

//        val hashCode = state.hashCode()
//        if (hashCode in alreadyPushedStateHashes) {
//            return
//        }

        val weight = state.weight
        if (weight >= MAX_DISTANCE) {
            return
        }

        if (weight == 0L) {
//            pathLogger.warn {  }
        }

        // TODO use it instead of a simple weight?
        val weightWithLoopCoefficient = doNotRun {
            with(state) {
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
        }
        executionQueue += StateWithWeight(state, weight)
//        alreadyPushedStateHashes += hashCode
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
            executionStack.last().method.let {
                val declaringClass = it.declaringClass

                if (declaringClass.isOverridden) {
                    // We need to exit from the wrappers ASAP so calculate a distance to the nearest return stmt
                    val methodGraph = it.jimpleBody().graph()
                    val returnStmts = methodGraph.tails.filterIsInstance<Stmt>().filter { tail -> tail.isReturn }

                    val distancesToReturn =
                        calculateDistancesInProceduralGraphToSpecifiedStmts(stmt, methodGraph, returnStmts)

                    return distancesToReturn.values.minOrNull()
                        ?: error("No reachable return stmts from $stmt in the wrapped method $it")
                }
            }

            val distance = run {
                val remainingTaintSourcesToVisit = taintSourcesToVisit
                val visitedTaintSources = visitedTaintSources(remainingTaintSourcesToVisit)

                val targetPoints = if (visitedTaintSources.isEmpty()) {
                    // Need to reach any of non fully visited (with all corresponding sinks) taint sources (or the closest?)
                    remainingTaintSourcesToVisit.map { StmtWithOuterMethod(it.stmt, it.outerMethod) }
                } else {
                    // Need to reach the closest corresponding sink
                    val sourcesAndCorrespondingSinks = visitedTaintSources.associateWith {
                        taintSinksBySource(it)
                    }.entries.flatMapTo(mutableSetOf()) {
                        it.value.map { sink -> it.key to sink }
                    }

                    // But do not reach already visited source/sink pairs
                    sourcesAndCorrespondingSinks
                        .filterNot { TaintPair(it.first.stmt, it.second.stmt) in visitedTaintPairs }
                        .map { StmtWithOuterMethod(it.second.stmt, it.second.outerMethod) }
                }

                val (methodsPaths, stackElementsToReturn) = pathsToMethods(targetPoints)

                if (methodsPaths.isEmpty()) {
                    // Cannot reach target methods from the current state
                    return@run MAX_DISTANCE
                }

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

    private fun ExecutionState.pathsToMethods(targetMethods: List<StmtWithOuterMethod>): MethodsPathWithStackElementsToReturn {
        var previousStateElements: List<ExecutionStackElement> = executionStack

        while (previousStateElements.isNotEmpty()) {
            val currentStackElement = previousStateElements.last()
            val startingStmtsInStackElement = if (previousStateElements.size == executionStack.size) {
                // If we did not rise up in stack, take the current stms
                listOf(stmt)
            } else {
                // Otherwise, take the NEXT stmts in the current stack element
                val currentStmtInTheStackElement = executionStack[previousStateElements.lastIndex + 1].caller!!
                val graph = currentStackElement.method.jimpleBody().graph()

                graph.getSuccsOf(currentStmtInTheStackElement)
            }
            val methodsPathsWithTargetStmts =
                currentStackElement.pathsToMethods(startingStmtsInStackElement, targetMethods)

            if (methodsPathsWithTargetStmts.isNotEmpty()) {
                val stackElementsToReturn = executionStack.subList(previousStateElements.size, executionStack.size)

                return MethodsPathWithStackElementsToReturn(
                    methodsPathsWithTargetStmts.map { it.first },
                    stackElementsToReturn
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

    private fun ExecutionStackElement.pathsToMethods(
        currentStmtsInStackElement: List<Unit>,
        targetPoints: List<StmtWithOuterMethod>
    ): List<Pair<MethodsPath, Stmt>> = runBfsInInterProceduralGraph(method, currentStmtsInStackElement, targetPoints)

    private fun runBfsInInterProceduralGraph(
        start: SootMethod,
        currentStmtsInStackElement: List<Unit>,
        targetPoints: List<StmtWithOuterMethod>
    ): List<Pair<MethodsPath, Stmt>> {
        val used = mutableMapOf<SootMethod, Boolean>()
        val distances = mutableMapOf<SootMethod, Long>()
        val parents = mutableMapOf<SootMethod, SootMethod>()
        val queue = ArrayDeque<SootMethod>()

        distances += start to 0L
        used[start] = true

        val nextStmtsAfterCurrentStmtIncluding = mutableListOf(currentStmtsInStackElement)
        val uniqueNextStmts = mutableSetOf(*nextStmtsAfterCurrentStmtIncluding.single().toTypedArray())
        while (true) {
            val nextStmts = nextStmtsAfterCurrentStmtIncluding
                .last()
                .flatMap { globalGraph.succStmts(it as Stmt) }
                .filter { it !in uniqueNextStmts }

            if (!uniqueNextStmts.addAll(nextStmts)) {
                break
            }

            nextStmtsAfterCurrentStmtIncluding += nextStmts
        }

        uniqueNextStmts.forEach { stmtInTheStartMethod ->
            val edgesOut = callGraph.edgesOutOf(stmtInTheStartMethod)

            edgesOut.forEach {
                val method = it.tgt()

                queue += method
                parents[method] = start
                distances += method to 1L
                used[method] = true
            }
        }

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
            /*if (srcMethod.canRetrieveBody()) {
                srcMethod.jimpleBody().units.forEach {
                    for (edge in callGraph.edgesOutOf(it)) {
                        val targetMethod = edge.tgt.method()

                        if (!used.getOrDefault(targetMethod, false)) {
                            used[targetMethod] = true
                            queue += targetMethod
                            distances[targetMethod] = distanceFrom + 1
                            parents[targetMethod] = srcMethod
                        }
                    }
                }
            }*/
        }

        return targetPoints.mapNotNull {
            val sootMethod = it.method.sootMethod

            if (!used.getOrDefault(sootMethod, false)) {
                null
            } else {
                recoverPath(sootMethod, parents) to it.stmt
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

    data class TaintPair(val taintSourceStmt: Stmt, val taintSinkStmt: Stmt)

    companion object {
        const val MAX_DISTANCE: Long = Int.MAX_VALUE.toLong()
        const val MIN_DISTANCE: Long = 0L

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
