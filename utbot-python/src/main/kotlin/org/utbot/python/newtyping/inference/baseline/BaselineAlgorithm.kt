package org.utbot.python.newtyping.inference.baseline

import mu.KotlinLogging
import org.utbot.python.PythonMethod
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.ast.visitor.hints.*
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.inference.*
import org.utbot.python.newtyping.inference.constructors.FakeClassStorage
import org.utbot.python.newtyping.mypy.checkSuggestedSignatureWithDMypy
import org.utbot.python.newtyping.utils.weightedRandom
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
    override suspend fun run(
        hintCollectorResult: HintCollectorResult,
        isCancelled: () -> Boolean,
        annotationHandler: suspend (Type) -> InferredTypeFeedback,
    ) {
        val generalRating = createGeneralTypeRating(hintCollectorResult, storage)
        val initialStates = getInitialStates(hintCollectorResult, generalRating)
        val states: MutableSet<BaselineAlgorithmState> = initialStates.toMutableSet()
        val fileForMypyRuns = TemporaryFileManager.assignTemporaryFile(tag = "mypy.py")
        val checkedSignatures = mutableSetOf(PythonTypeWrapperForEqualityCheck(hintCollectorResult.initialSignature))

        run breaking@{
            while (states.isNotEmpty()) {
                if (isCancelled())
                    return@breaking
                logger.debug("State number: ${states.size}")
                val state = chooseState(states.toList())
                val expanded = expandState(state, storage)
                if (expanded == null) {
                    states.remove(state)
                    continue
                }
                val (newStates, newType, fakeClassStorage) = expanded
                logger.info("Checking ${newType.pythonTypeRepresentation()}")
                checkedSignatures.add(PythonTypeWrapperForEqualityCheck(newType))
                if (checkSignature(newType, fileForMypyRuns, configFile, fakeClassStorage)) {
                    annotationHandler(newType)
                    /*
                    when (annotationHandler(newState.signature)) {
                        SuccessFeedback -> {
                            states.add(newState)
                        }
                        InvalidTypeFeedback -> {}
                    }
                    */
                    state.children += 1
                    newStates.forEach { newState ->
                        states.add(newState)
                    }
                }
            }
        }
    }

    private fun checkSignature(
        signature: FunctionType,
        fileForMypyRuns: File,
        configFile: File,
        fakeClassStorage: FakeClassStorage
    ): Boolean {
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
            fakeClassStorage
        )
    }

    private fun chooseState(states: List<BaselineAlgorithmState>): BaselineAlgorithmState {
        val weights = states.map { 1.0 / (it.children * it.children + 1) }
        return weightedRandom(states, weights)
    }

    private fun getInitialStates(
        hintCollectorResult: HintCollectorResult,
        generalRating: List<Type>
    ): List<BaselineAlgorithmState> {
        val paramNames = pythonMethodCopy.arguments.map { it.name }
        val root = PartialTypeNode(hintCollectorResult.initialSignature, true)
        val allNodes: MutableList<BaselineAlgorithmNode> = mutableListOf(root)
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
        return listOf(
            BaselineAlgorithmState(
                allNodes,
                generalRating,
                storage,
                FakeClassStorage(),
                allNodes.mapNotNull { it as? AnyTypeNode })
        )
    }
}