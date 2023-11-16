package org.utbot.python.newtyping.inference.baseline

import mu.KotlinLogging
import org.utbot.python.PythonMethod
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.ast.visitor.hints.*
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.inference.*
import org.utbot.python.newtyping.mypy.checkSuggestedSignatureWithDMypy
import org.utbot.python.newtyping.utils.weightedRandom
import org.utbot.python.utils.TemporaryFileManager
import java.io.File
import kotlin.random.Random

private val EDGES_TO_LINK = listOf(
    EdgeSource.Identification,
    EdgeSource.Assign,
    EdgeSource.OpAssign,
    EdgeSource.Operation,
    EdgeSource.Comparison
)

private val logger = KotlinLogging.logger {}

class BaselineAlgorithm(
    private val storage: PythonTypeHintsStorage,
    private val hintCollectorResult: HintCollectorResult,
    private val pythonPath: String,
    private val pythonMethodCopy: PythonMethod,
    private val directoriesForSysPath: Set<String>,
    private val moduleToImport: String,
    private val namesInModule: Collection<String>,
    private val initialErrorNumber: Int,
    private val configFile: File,
    private val additionalVars: String,
    private val randomTypeFrequency: Int = 0,
    private val dMypyTimeout: Long?
) : TypeInferenceAlgorithm() {
    private val random = Random(0)

    private val generalRating = createGeneralTypeRating(hintCollectorResult, storage)
    private val initialState = getInitialState(hintCollectorResult, generalRating)
    private val states: MutableList<BaselineAlgorithmState> = mutableListOf(initialState)
    private val fileForMypyRuns = TemporaryFileManager.assignTemporaryFile(tag = "mypy.py")
    private var iterationCounter = 0
    private var randomTypeCounter = 0

    private val simpleTypes = simplestTypes(storage)
    private val mixtureType = createPythonUnionType(simpleTypes)

    private val expandedStates: MutableMap<UtType, Pair<BaselineAlgorithmState, BaselineAlgorithmState>> = mutableMapOf()
    private val statistic: MutableMap<UtType, Int> = mutableMapOf()

    private val checkedSignatures: MutableSet<UtType> = mutableSetOf()

    private fun getRandomType(): UtType? {
        val weights = states.map { 1.0 / (it.anyNodes.size * it.anyNodes.size + 1) }
        val state = weightedRandom(states, weights, random)
        val newState = expandState(state, storage, state.anyNodes.map { mixtureType })
        if (newState != null) {
            logger.info("Random type: ${newState.signature.pythonTypeRepresentation()}")
            expandedStates[newState.signature] = newState to state
            return newState.signature
        }
        return null
    }

    private fun getLaudedType(): UtType? {
        if (statistic.isEmpty()) return null
        val sum = statistic.values.sum()
        val weights = statistic.values.map { it.toDouble() / sum }
        val newType = weightedRandom(statistic.keys.toList(), weights, random)
        logger.info("Lauded type: ${newType.pythonTypeRepresentation()}")
        return newType
    }

    fun expandState(): UtType? {
        if (states.isEmpty()) return null

        logger.debug("State number: ${states.size}")
        iterationCounter++

        if (randomTypeFrequency > 0 && iterationCounter % randomTypeFrequency == 0) {
            randomTypeCounter++
            if (randomTypeCounter % 2 == 0) {
                val laudedType = getLaudedType()
                if (laudedType != null) return laudedType
            }
            val randomType = getRandomType()
            if (randomType != null) return randomType
        }

        val state = chooseState(states)
        val newState = expandState(state, storage)
        if (newState != null) {
            logger.info("Checking new state ${newState.signature.pythonTypeRepresentation()}")
            if (checkSignature(newState.signature as FunctionType, fileForMypyRuns, configFile)) {
                logger.debug("Found new state!")
                expandedStates[newState.signature] = newState to state
                return newState.signature
            }
        } else if (state.anyNodes.isEmpty()) {
            if (state.signature in checkedSignatures) {
                return state.signature
            }
            logger.info("Checking ${state.signature.pythonTypeRepresentation()}")
            if (checkSignature(state.signature as FunctionType, fileForMypyRuns, configFile)) {
                checkedSignatures.add(state.signature)
                return state.signature
            } else {
                states.remove(state)
            }
        } else {
            states.remove(state)
        }
        return expandState()
    }

    fun feedbackState(signature: UtType, feedback: InferredTypeFeedback) {
        val stateInfo = expandedStates[signature]
        if (stateInfo != null) {
            val (newState, parent) = stateInfo
            when (feedback) {
                SuccessFeedback -> {
                    states.add(newState)
                    parent.children += 1
                }

                InvalidTypeFeedback -> {
                    states.remove(newState)
                }
            }
            expandedStates.remove(signature)
        } else if (typesAreEqual(signature, initialState.signature)) {
            if (feedback is InvalidTypeFeedback) {
                states.remove(initialState)
            }
        }
    }


    override suspend fun run(
        isCancelled: () -> Boolean,
        annotationHandler: suspend (UtType) -> InferredTypeFeedback,
    ): Int {
        run breaking@ {
            while (states.isNotEmpty()) {
                if (isCancelled())
                    return@breaking
                logger.debug("State number: ${states.size}")
                iterationCounter++

                if (randomTypeFrequency > 0 && iterationCounter % randomTypeFrequency == 0) {
                    val weights = states.map { 1.0 / (it.anyNodes.size * it.anyNodes.size + 1) }
                    val state = weightedRandom(states, weights, random)
                    val newState = expandState(state, storage, state.anyNodes.map { mixtureType })
                    if (newState != null) {
                        logger.info("Random type: ${newState.signature.pythonTypeRepresentation()}")
                        annotationHandler(newState.signature)
                    }
                }

                val state = chooseState(states)
                val newState = expandState(state, storage)
                if (newState != null) {
                    if (iterationCounter == 1) {
                        annotationHandler(initialState.signature)
                    }
                    logger.info("Checking ${newState.signature.pythonTypeRepresentation()}")
                    if (checkSignature(newState.signature as FunctionType, fileForMypyRuns, configFile)) {
                        logger.debug("Found new state!")
                        when (annotationHandler(newState.signature)) {
                            SuccessFeedback -> {
                                states.add(newState)
                                state.children += 1
                            }
                            InvalidTypeFeedback -> {}
                        }
                    }
                } else {
                    states.remove(state)
                }
            }
        }
        return iterationCounter
    }

    private fun checkSignature(signature: FunctionType, fileForMypyRuns: File, configFile: File): Boolean {
        pythonMethodCopy.definition = PythonFunctionDefinition(
            pythonMethodCopy.definition.meta,
            signature
        )
        return checkSuggestedSignatureWithDMypy(
            pythonMethodCopy,
            directoriesForSysPath,
            moduleToImport,
            namesInModule,
            fileForMypyRuns,
            pythonPath,
            configFile,
            initialErrorNumber,
            additionalVars,
            timeout = dMypyTimeout
        )
    }

    private fun chooseState(states: List<BaselineAlgorithmState>): BaselineAlgorithmState {
        val weights = states.map { 1.0 / (it.children * it.children + 1) }
        return weightedRandom(states, weights, random)
    }

    private fun getInitialState(
        hintCollectorResult: HintCollectorResult,
        generalRating: List<UtType>
    ): BaselineAlgorithmState {
        val paramNames = pythonMethodCopy.arguments.map { it.name }
        val root = PartialTypeNode(pythonMethodCopy.definition.type, true)
        val allNodes: MutableSet<BaselineAlgorithmNode> = mutableSetOf(root)
        val argumentRootNodes = paramNames.map { hintCollectorResult.parameterToNode[it]!! }
        argumentRootNodes.forEachIndexed { index, node ->
            val visited: MutableSet<HintCollectorNode> = mutableSetOf()
            visitNodesByReverseEdges(node, visited) { it is HintEdgeWithBound && EDGES_TO_LINK.contains(it.source) }
            val (lowerBounds, upperBounds) = collectBoundsFromComponent(visited)
            val decomposed = decompose(node.partialType, lowerBounds, upperBounds, 1, storage)
            allNodes.addAll(decomposed.nodes)
            val edge = BaselineAlgorithmEdge(
                from = decomposed.root,
                to = root,
                annotationParameterId = index
            )
            addEdge(edge)
        }
        return BaselineAlgorithmState(allNodes, generalRating, storage)
    }

    fun laudType(type: FunctionType) {
        statistic[type] = statistic[type]?.plus(1) ?: 1
    }
}