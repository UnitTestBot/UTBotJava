package org.utbot.python.newtyping.inference.baseline

import mu.KotlinLogging
import org.utbot.python.PythonMethod
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.ast.visitor.hints.*
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.inference.*
import org.utbot.python.newtyping.runmypy.checkSuggestedSignatureWithDMypy
import org.utbot.python.utils.TemporaryFileManager
import java.io.File

private val EDGES_TO_LINK = listOf(
    EdgeSource.Identification,
    EdgeSource.Assign,
    EdgeSource.OpAssign,
    EdgeSource.Operation,
    EdgeSource.Comparison
)

private val logger = KotlinLogging.logger {}

class BaselineAlgorithm(
    private val storage: PythonTypeStorage,
    private val pythonPath: String,
    private val pythonMethodCopy: PythonMethod,
    private val directoriesForSysPath: Set<String>,
    private val moduleToImport: String,
    private val namesInModule: Collection<String>,
    private val initialErrorNumber: Int,
    private val configFile: File
) : TypeInferenceAlgorithm() {
    override fun run(hintCollectorResult: HintCollectorResult, isCancelled: () -> Boolean): Sequence<Type> = sequence {
        val generalRating = createGeneralTypeRating(hintCollectorResult, storage)
        val initialState = getInitialState(hintCollectorResult, generalRating)
        val states: MutableSet<BaselineAlgorithmState> = mutableSetOf(initialState)
        val fileForMypyRuns = TemporaryFileManager.assignTemporaryFile(tag = "mypy.py")

        while (states.isNotEmpty()) {
            if (isCancelled())
                break
            logger.debug("State number: ${states.size}")
            val state = chooseState(states)
            val newState = expandState(state, storage)
            if (newState != null) {
                logger.debug("Checking ${newState.signature.pythonTypeRepresentation()}")
                if (checkSignature(newState.signature as FunctionType, fileForMypyRuns, configFile)) {
                    logger.debug("Found new state!")
                    yield(newState.signature)
                    states.add(newState)
                }
            } else {
                states.remove(state)
            }
        }
    }

    private fun checkSignature(signature: FunctionType, fileForMypyRuns: File, configFile: File): Boolean {
        pythonMethodCopy.type = signature
        return checkSuggestedSignatureWithDMypy(
            pythonMethodCopy,
            directoriesForSysPath,
            moduleToImport,
            namesInModule,
            fileForMypyRuns,
            pythonPath,
            configFile,
            initialErrorNumber
        )
    }

    // TODO: something smarter?
    private fun chooseState(states: Set<BaselineAlgorithmState>): BaselineAlgorithmState {
        return states.random()
    }

    private fun getInitialState(
        hintCollectorResult: HintCollectorResult,
        generalRating: List<Type>
    ): BaselineAlgorithmState {
        val signatureDescription =
            hintCollectorResult.initialSignature.pythonDescription() as PythonCallableTypeDescription
        val root = PartialTypeNode(hintCollectorResult.initialSignature, true)
        val allNodes: MutableSet<BaselineAlgorithmNode> = mutableSetOf(root)
        val argumentRootNodes = signatureDescription.argumentNames.map { hintCollectorResult.parameterToNode[it]!! }
        argumentRootNodes.forEachIndexed { index, node ->
            val visited: MutableSet<HintCollectorNode> = mutableSetOf()
            visitNodesByReverseEdges(node, visited) { it is HintEdgeWithBound && EDGES_TO_LINK.contains(it.source) }
            val (lowerBounds, upperBounds) = collectBoundsFromComponent(visited)
            val decomposed = decompose(node.partialType, lowerBounds, upperBounds, 1)
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
}