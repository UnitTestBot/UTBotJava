package org.utbot.summary.tag

import org.utbot.framework.plugin.api.Step
import org.utbot.summary.clustering.SplitSteps

/**
 * @param traceSteps - a list of execution steps.
 * @param splitSteps - contain the information about common, unique and partly unique steps
 * A class builds an initial TAGGED representation from a list of steps
 */
class StatementTreeBuilder(private val splitSteps: SplitSteps, private val traceSteps: List<Step>) {
    /**
     * @param stmtCallCounter saves the information about number of execution of each step.
     * some of the statements and their exact decisions are multiple times executed.
     * It is counted here (step's depth are not taken into account).
     * @see Step#equals - there is no equality check on depth
     */
    private val stmtCallCounter = mutableMapOf<Step, Int>()

    /**
     * This function defines root tag which is the first step in the trace.
     * The rest structure is built by buildStatementTree and attached to the root tag.
     */
    fun build(): StatementTag? {
        val rootStatementTag = createStatementTag(0)
        if (rootStatementTag != null) {
            buildStatementTree(
                1,
                rootStatementTag,
                rootStatementTag.step.depth
            )
        }
        return rootStatementTag
    }

    /**
     * Recursively builds a basic tree tag structure from the list of steps, given
     * @param startIndex next step's id
     * @param statementTag previous tag,
     * Take a note this function can not identify such structures as
     * loops, missed loops, recursion and etc.
     * These structures are identified in
     * @see ExecutionStructureAnalysis
     * as they require initial tree tag structure. Otherwise it would be unreadable code here.
     */
    private fun buildStatementTree(startIndex: Int = 0, statementTag: StatementTag? = null, depth: Int = 0, isInvokedRecursively: Boolean = false): Int {
        var currentIndex = startIndex
        var previousStatementTag = statementTag
        while (currentIndex < traceSteps.size) {
            val currentStatement = createStatementTag(currentIndex)
            if (currentStatement?.basicTypeTag == BasicTypeTag.Invoke) {
                currentIndex++
                val nextStatementTag = createStatementTag(currentIndex)
                nextStatementTag?.let {
                    when {
                        it.step.depth < depth -> {
                            // When analyzing a steps' path, we can accidentally return before we process the whole path,
                            // this leads to missing info in summaries.
                            // In order to solve it, we track whether we called the method from [StatementTreeBuilder.build()]
                            // or called it here recursively.
                            // In the first case we do not return,
                            // in the second case we return an index.
                            currentIndex--
                            if (isInvokedRecursively) return currentIndex
                            else return@let
                        }
                        it.step.depth > depth -> {
                            currentStatement.invoke = nextStatementTag

                            // We save the nextStatementTag in .next as well at the end of the step path
                            // in order to recover info from it
                            if (currentIndex == traceSteps.lastIndex) currentStatement.next = nextStatementTag

                            currentIndex = buildStatementTree(
                                currentIndex + 1,
                                nextStatementTag,
                                nextStatementTag.step.depth,
                                isInvokedRecursively = true
                            )
                            currentIndex--
                        }
                        else -> {
                            stmtCallCounter[it.step] = stmtCallCounter[it.step]?.minus(1) ?: 0
                            currentIndex--
                        }
                    }
                }
            }
            previousStatementTag?.let {
                it.next = currentStatement
            }
            previousStatementTag = currentStatement
            currentIndex++
        }
        return currentIndex
    }

    /**
     * @param index - step's index in the list of executed steps
     * Creates a statement tag from given step index
     * Returns statement tag or null element if the index is out of range
     */
    private fun createStatementTag(index: Int): StatementTag? {
        if (index >= traceSteps.size) return null
        val currentStep = traceSteps[index]
        val executionFrequency = traceSteps.filter { currentStep == it }.size
        stmtCallCounter.merge(currentStep, 1, Int::plus)
        val callOrderTag = getCallOrderTag(stmtCallCounter[currentStep])
        return StatementTag(
            currentStep,
            statementFrequencyTag(currentStep, splitSteps),
            callOrderTag,
            executionFrequency,
            index
        )
    }
}