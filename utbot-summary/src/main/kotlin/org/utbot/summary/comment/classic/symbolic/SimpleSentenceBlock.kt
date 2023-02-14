package org.utbot.summary.comment.classic.symbolic

import org.utbot.framework.plugin.api.DocCodeStmt
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.summary.SummarySentenceConstants.AT_CODE
import org.utbot.summary.SummarySentenceConstants.CARRIAGE_RETURN
import org.utbot.summary.SummarySentenceConstants.CLOSE_BRACKET
import org.utbot.summary.SummarySentenceConstants.COMMA_SYMBOL
import org.utbot.summary.SummarySentenceConstants.DOT_SYMBOL
import org.utbot.summary.SummarySentenceConstants.NEW_LINE
import org.utbot.summary.SummarySentenceConstants.OPEN_BRACKET
import org.utbot.summary.SummarySentenceConstants.SENTENCE_SEPARATION
import org.utbot.summary.SummarySentenceConstants.TAB
import org.utbot.summary.comment.*

class SimpleSentenceBlock(val stringTemplates: StringsTemplatesInterface) {
    val iterationSentenceBlocks = mutableListOf<Pair<String, List<SimpleSentenceBlock>>>()
    var notExecutedIterations: List<IterationDescription>? = null
    var recursion: Pair<String, SimpleSentenceBlock>? = null
    var exceptionThrow: String? = null
    var detectedError: String? = null
    var nextBlock: SimpleSentenceBlock? = null

    val stmtTexts = mutableListOf<StmtDescription>()
    var squashedStmtTexts = mutableSetOf<SquashedStmtTexts>()
    var invokeSentenceBlock: Pair<String, SimpleSentenceBlock>? = null
    var tabCounter: Int = 0


    fun toSentence(): String {
        var result = ""
        var putSentenceDot: Boolean
        var restartSentence = false


        squashedStmtTexts.forEach {
            result += it.toSentence()
        }

        if (invokeSentenceBlock != null) {
            val invokeDescription = invokeSentenceBlock?.first
            val invokeSentence = invokeSentenceBlock?.second?.toSentence()
            if (!invokeDescription.isNullOrEmpty() && !invokeSentence.isNullOrEmpty()) {
                val dotAddedAndShiftedInvokeSentence =
                    lastCommaToDot(invokeSentence).trim().replace(NEW_LINE, NEW_LINE + TAB)
                result += stringTemplates.invokeSentence.format(invokeDescription, dotAddedAndShiftedInvokeSentence)
                result += NEW_LINE
                restartSentence = true
            }
        }

        iterationSentenceBlocks.forEach { (loopDesc, sentenceBlocks) ->
            result += stringTemplates.iterationSentence.format(
                stringTemplates.codeSentence.format(loopDesc),
                numberOccurrencesToText(
                    sentenceBlocks.size
                )
            )
            putSentenceDot = true
            restartSentence = true
            val textSentenceBlocks = mutableListOf<String>()
            sentenceBlocks.forEach { sentenceBlock ->
                sentenceBlock.tabCounter += 1
                textSentenceBlocks += sentenceBlock.toSentence()
            }
            val insideSentenceStructure =
                squash(textSentenceBlocks.asSequence().filter { it.isNotEmpty() }.toMutableList())

            if (insideSentenceStructure.isNotEmpty()) {
                putSentenceDot = false
                result += "$COMMA_SYMBOL$NEW_LINE"
                result += TAB.repeat(tabCounter + 1)
                result += stringTemplates.insideIterationSentence.format(insideSentenceStructure)
            }
            if (putSentenceDot) result += "$DOT_SYMBOL "
        }

        if (recursion != null) {
            result += stringTemplates.recursionSentence.format(recursion?.first)
            putSentenceDot = true
            restartSentence = true
            val insideRecursionSentenceStruct = recursion?.second?.toSentence()
            if (!insideRecursionSentenceStruct.isNullOrEmpty()) {
                result += stringTemplates.insideRecursionSentence.format(insideRecursionSentenceStruct)
                putSentenceDot = false
            }
            if (putSentenceDot) result += "$DOT_SYMBOL "
        }

        val nextSentenceBlock = nextBlock?.toSentence()
        if (!nextSentenceBlock.isNullOrEmpty()) {
            if (restartSentence) {
                result += stringTemplates.sentenceBeginning + " "
                restartSentence = false
            }
            result += "${nextSynonyms.random()} $nextSentenceBlock"

            if (nextSentenceBlock.trim().endsWith(DOT_SYMBOL)) {
                restartSentence = true
            }
        }

        if (notExecutedIterations.isNullOrEmpty().not()) {
            if (restartSentence) {
                result += stringTemplates.sentenceBeginning + " "
                restartSentence = false
            }
            var notIterated = stringTemplates.noIteration + " "
            notExecutedIterations?.forEach {
                notIterated += stringTemplates.codeSentence.format(it.description) + "$COMMA_SYMBOL "
            }
            result = notIterated + result
        }


        exceptionThrow?.let {
            if (restartSentence) {
                result += stringTemplates.sentenceBeginning + " "
                restartSentence = false
            }
            result += stringTemplates.throwSentence.format(it)
            result += NEW_LINE
        }

        detectedError?.let {
            if (restartSentence) {
                result += stringTemplates.sentenceBeginning + " "
                restartSentence = false
            }
            result += stringTemplates.suspiciousBehaviorDetectedSentence.format(it)
            result += NEW_LINE
        }

        return result
    }

    fun toDocStmt(): List<DocStatement> {
        var putSentenceDot: Boolean
        var restartSentence = false
        val docStmts = mutableListOf<DocStatement>()

        squashedStmtTexts.forEach {
            docStmts += it.toDocStmts()
        }

        if (invokeSentenceBlock != null) {
            val invokeDescription = invokeSentenceBlock?.first
            val invokeSentence = (invokeSentenceBlock?.second?.toDocStmt() ?: listOf()).toMutableList()
            if (!invokeDescription.isNullOrEmpty() && !invokeSentence.isNullOrEmpty()) {
                for (i in invokeSentence.indices) {
                    val stmt = invokeSentence[i]
                    if (stmt is DocRegularStmt && stmt.stmt.contains(NEW_LINE)) {
                        invokeSentence[i] = DocRegularStmt(stmt.stmt.replace(NEW_LINE, NEW_LINE + TAB))
                    }
                }
                docStmts += templateToDocStmt(
                    stringTemplates.invokeSentence,
                    DocRegularStmt(invokeDescription),
                    *invokeSentence.toTypedArray()
                )
                restartSentence = true
            }
        }

        iterationSentenceBlocks.forEach { (loopDesc, sentenceBlocks) ->
            docStmts += templateToDocStmt(
                stringTemplates.iterationSentence,
                DocCodeStmt(loopDesc),
                DocRegularStmt(
                    numberOccurrencesToText(
                        sentenceBlocks.size
                    )
                )
            )
            putSentenceDot = true
            restartSentence = true
            val textSentenceBlocks = mutableListOf<DocStatement>()
            sentenceBlocks.forEach { sentenceBlock ->
                sentenceBlock.tabCounter += 1
                textSentenceBlocks += sentenceBlock.toDocStmt()
            }

            val squashedRegularStatements =
                squashIdenticalRegularStatements(squashDocRegularStatements(textSentenceBlocks))

            if (squashedRegularStatements.isNotEmpty()) {
                putSentenceDot = false
                docStmts += DocRegularStmt("$COMMA_SYMBOL$NEW_LINE")
                docStmts += DocRegularStmt(TAB.repeat(tabCounter + 1))
                docStmts += templateToDocStmt(
                    stringTemplates.insideIterationSentence,
                    *squashedRegularStatements.toTypedArray()
                )
            }
            if (putSentenceDot) docStmts += DocRegularStmt("$DOT_SYMBOL ")
        }

        if (recursion != null) {
            docStmts += templateToDocStmt(
                stringTemplates.recursionSentence,
                DocRegularStmt(recursion?.first ?: "")
            )
            putSentenceDot = true
            restartSentence = true
            val insideRecursionSentenceStruct = recursion?.second?.toDocStmt()
            if (!insideRecursionSentenceStruct.isNullOrEmpty()) {
                docStmts += templateToDocStmt(
                    stringTemplates.insideRecursionSentence,
                    *insideRecursionSentenceStruct.toTypedArray()
                )

                putSentenceDot = false
            }
            if (putSentenceDot) docStmts += DocRegularStmt("$DOT_SYMBOL ")
        }

        val nextSentenceBlock = nextBlock?.toDocStmt()
        if (!nextSentenceBlock.isNullOrEmpty()) {
            if (restartSentence) {
                docStmts += DocRegularStmt("\n" + stringTemplates.sentenceBeginning + " ")
                restartSentence = false
            }
            docStmts += DocRegularStmt("${nextSynonyms.random()} ")
            docStmts += nextSentenceBlock
        }

        if (notExecutedIterations.isNullOrEmpty().not()) {
            if (restartSentence) {
                docStmts += DocRegularStmt(stringTemplates.sentenceBeginning + " ")
                restartSentence = false
            }
            val notIterated = mutableListOf<DocStatement>(DocRegularStmt(stringTemplates.noIteration + " "))
            notExecutedIterations?.forEach {
                notIterated += DocCodeStmt(it.description)
                notIterated += DocRegularStmt("$COMMA_SYMBOL ")
            }
            docStmts.addAll(0, notIterated)
        }

        exceptionThrow?.let {
            docStmts += DocRegularStmt(NEW_LINE)
            if (restartSentence) {
                docStmts += DocRegularStmt(stringTemplates.sentenceBeginning + " ")
                restartSentence = false
            }
            docStmts += DocRegularStmt(stringTemplates.throwSentence.format(it)) //TODO SAT-1310
            docStmts += DocRegularStmt(NEW_LINE)
        }

        detectedError?.let {
            docStmts += DocRegularStmt(NEW_LINE)
            if (restartSentence) {
                docStmts += DocRegularStmt(stringTemplates.sentenceBeginning + " ")
                restartSentence = false
            }
            docStmts += DocRegularStmt(stringTemplates.suspiciousBehaviorDetectedSentence.format(it))
            docStmts += DocRegularStmt(NEW_LINE)
        }

        return docStmts
    }

    /**
     * During sentence block generation, block are filled with statement description (StmtDescription).
     * A list of stmtDescription1, stmtDescription2, ..., stmtDescriptionN.
     * Consequently before sentence generation these statements have to be orderly squashed, to list of objects:
     * squashedStmtTexts1, squashedStmtTexts2, ...,squashedStmtTextsK
     * @see orderedSquashStmtText
     * This function triggers squashing operations of this block and its child blocks.
     * Also some of the invokes sequentially multiple types are described, because
     * they may appear inside of condition structure. Then, soot will give us multiple descriptions
     * of one actual call. The remove of abundant description is triggered here also.
     * @see removeInvokeCalledInUniqueCondition
     * It is not removed in orderedSquashStmtText as these invokes have different types of statements (Ex.: one
     * will be JInvokeStmt another as JIfStmt).
     */
    fun squashStmtText() {
        squashedStmtTexts = orderedSquashStmtText()
        removeInvokeCalledInUniqueCondition()
        recursion?.second?.squashStmtText()
        nextBlock?.squashStmtText()
        iterationSentenceBlocks.forEach { (_, iteration) ->
            iteration.forEach { block -> block.squashStmtText() }
        }
    }

    /**
     * Each statement has its own description. Sentence block contains a list of
     * statement descriptions. They are may be of different types. Ex.:
     *      invoke:m1
     *      invoke: m2
     *      activates condition: f1
     *      activates condition: f2
     *      etc
     * This function orderly squashes such statement descriptions into:
     *      invoke:m1, m2
     *      activates condition: f1, f2
     *      etc
     * @return a list of squashed descriptions of statements
     * @see SquashedStmtTexts
     */
    private fun orderedSquashStmtText(): MutableSet<SquashedStmtTexts> {
        val result = mutableSetOf<SquashedStmtTexts>()
        if (stmtTexts.size <= 1) {
            stmtTexts.forEach { result += (SquashedStmtTexts(it.stmtType, it, stringTemplates)) }
        } else {
            var i = 0
            var prevSquash = SquashedStmtTexts(stmtTexts[i].stmtType, stmtTexts[i], stringTemplates)
            result += (prevSquash)
            while (i < stmtTexts.size - 1) {
                i++
                val currentStmtText = stmtTexts[i]
                if (prevSquash.stmtType == currentStmtText.stmtType) {
                    prevSquash.stmtTexts.add(currentStmtText)
                } else {
                    prevSquash = SquashedStmtTexts(currentStmtText.stmtType, currentStmtText, stringTemplates)
                    result += (prevSquash)
                }
            }
        }
        return result
    }

    /*
      If the condition invokes method call then in some cases this invoke will have
      two separate descriptions: its own description and description inside condition.
      So, if there is invoke that meets all these requirements at once:
          has the same name as in condition,
          condition and invoke located in one line,
          condition and invoke are consecutively described
      then first invoke description is removed
     */
    private fun removeInvokeCalledInUniqueCondition() {
        filterSquashedText(squashedStmtTexts, criteriaInvokeCalledInUniqueCondition)
        filterSquashedText(squashedStmtTexts, criteriaRepetition)
    }

    fun isEmpty(): Boolean {
        if (squashedStmtTexts.isNotEmpty()) return false
        if (iterationSentenceBlocks.isNotEmpty()) return false
        if (recursion != null) return false
        if (nextBlock?.isEmpty() == false) return false
        if (notExecutedIterations.isNullOrEmpty().not()) return false
        exceptionThrow?.let { return false }
        detectedError?.let { return false }
        if (invokeSentenceBlock != null) return false
        return true
    }

}

data class StmtDescription(
    val stmtType: StmtType,
    val description: String,
    val frequency: Int = 0,
    val prefix: String = ""
)

// texts for these stmts types are automatically built
// while others require manual sentence build policy
enum class StmtType {
    Condition, Return, CountedReturn, Invoke, SwitchCase, CaughtException, RecursionAssignment
}

data class SquashedStmtTexts(
    val stmtType: StmtType,
    var stmtTexts: MutableList<StmtDescription> = mutableListOf(),
    val stringTemplates: StringsTemplatesInterface
) {
    constructor(
        stmtType: StmtType,
        stmtDescription: StmtDescription,
        stringTemplates: StringsTemplatesInterface
    ) : this(stmtType, stringTemplates = stringTemplates) {
        stmtTexts.add(stmtDescription)
        stmtTexts = stmtTexts.map {
            it.copy(description = it.description.replace(CARRIAGE_RETURN, ""))
        }.toMutableList()
    }

    fun toSentence(): String {
        var result = ""
        val separation = SENTENCE_SEPARATION
        var tab = TAB
        var end = NEW_LINE
        result += when (stmtType) {
            StmtType.Condition -> stringTemplates.conditionLine
            StmtType.CaughtException -> stringTemplates.caughtExceptionLine
            StmtType.CountedReturn -> stringTemplates.countedReturnLine
            StmtType.Invoke -> stringTemplates.invokeLine
            StmtType.Return -> {
                tab = ""
                stringTemplates.returnLine
            }
            StmtType.SwitchCase -> {
                tab = ""
                end = "$COMMA_SYMBOL "
                stringTemplates.switchCaseLine
            }
            StmtType.RecursionAssignment -> {
                tab = ""
                end = "$COMMA_SYMBOL "
                stringTemplates.recursionAssignmentLine
            }
        }
        if (stmtType != StmtType.Invoke && stmtType != StmtType.RecursionAssignment) {
            for (i in 0 until stmtTexts.size) {
                val stmtText = stmtTexts[i]
                result += tab + stmtText.prefix + stringTemplates.codeSentence.format(stmtText.description)
                result += if (i != stmtTexts.lastIndex) separation else end
            }
        } else {
            val frequencyOfExecution = stmtTexts.groupingBy { it }.eachCount()
            val invokes = frequencyOfExecution.keys.toList()
            for (index in invokes.indices) {
                val invoke = invokes[index]
                val numberSquashed = frequencyOfExecution[invoke] ?: 1
                result += "$tab${invoke.description} ${numberOccurrencesToText(numberSquashed + invoke.frequency - 1)}"
                result += if (index != invokes.lastIndex) separation else end
            }
        }

        return result
    }

    fun toDocStmts(): List<DocStatement> {
        val stmts = mutableListOf<DocStatement>()
        val separation = SENTENCE_SEPARATION
        var tab = TAB
        var end = NEW_LINE
        stmts += DocRegularStmt(
            when (stmtType) {
                StmtType.Condition -> stringTemplates.conditionLine
                StmtType.CaughtException -> stringTemplates.caughtExceptionLine
                StmtType.CountedReturn -> stringTemplates.countedReturnLine
                StmtType.Invoke -> stringTemplates.invokeLine
                StmtType.Return -> {
                    tab = ""
                    stringTemplates.returnLine
                }
                StmtType.SwitchCase -> {
                    tab = ""
                    end = "$COMMA_SYMBOL "
                    stringTemplates.switchCaseLine
                }
                StmtType.RecursionAssignment -> {
                    tab = ""
                    end = "$COMMA_SYMBOL "
                    stringTemplates.recursionAssignmentLine
                }
            }
        )
        if (stmtType != StmtType.Invoke && stmtType != StmtType.RecursionAssignment) {
            for (i in 0 until stmtTexts.size) {
                val stmtText = stmtTexts[i]
                if ((tab + stmtText.prefix).isNotEmpty()) stmts += DocRegularStmt(tab + stmtText.prefix)
                stmts += DocCodeStmt(stmtText.description.replace(CARRIAGE_RETURN, ""))

                if (i != stmtTexts.lastIndex) {
                    if (separation.isNotEmpty()) stmts += DocRegularStmt(separation)
                } else {
                    if (end.isNotEmpty()) stmts += DocRegularStmt(end)
                }
            }
        } else {
            val frequencyOfExecution = stmtTexts.groupingBy { it }.eachCount()
            val invokes = frequencyOfExecution.keys.toList()
            for (index in invokes.indices) {
                val invoke = invokes[index]
                val numberSquashed = frequencyOfExecution[invoke] ?: 1
                val numberOfOccurrences = numberOccurrencesToText(numberSquashed + invoke.frequency - 1)
                val line = "$tab${invoke.description} $numberOfOccurrences"
                stmts += DocRegularStmt(line)

                if (index != invokes.lastIndex) {
                    if (separation.isNotEmpty()) stmts += DocRegularStmt(separation)
                } else {
                    if (end.isNotEmpty()) stmts += DocRegularStmt(end)
                }
            }
        }

        return stmts
    }
}

/**
 * Enum classed used to define all string variables used
 * to build sentence block and in SquashedStmtTexts
 */
interface StringsTemplatesInterface {
    val invokeSentence: String
    val iterationSentence: String
    val insideIterationSentence: String
    val recursionSentence: String
    val insideRecursionSentence: String
    val sentenceBeginning: String
    val noIteration: String
    val codeSentence: String
    val throwSentence: String
    val suspiciousBehaviorDetectedSentence: String

    val conditionLine: String
    val returnLine: String
    val countedReturnLine: String
    val invokeLine: String
    val switchCaseLine: String
    val caughtExceptionLine: String
    val recursionAssignmentLine: String
}

open class StringsTemplatesSingular : StringsTemplatesInterface {
    override val invokeSentence: String = "calls %s$COMMA_SYMBOL$NEW_LINE${TAB}there it %s"
    override val iterationSentence: String = "iterates the loop %s %s"
    override val insideIterationSentence: String = "inside this loop$COMMA_SYMBOL the test %s"
    override val recursionSentence: String = "triggers the recursion of %s"
    override val insideRecursionSentence: String = "$COMMA_SYMBOL where the test %s"
    override val sentenceBeginning: String = "Test"
    override val noIteration: String = "does not iterate"
    override val codeSentence: String = "$OPEN_BRACKET$AT_CODE %s$CLOSE_BRACKET"
    override val throwSentence: String = "throws %s"
    override val suspiciousBehaviorDetectedSentence: String = "detects suspicious behavior %s"

    //used in Squashing
    override val conditionLine: String = "executes conditions:$NEW_LINE"
    override val returnLine: String = "returns from: "
    override val countedReturnLine: String = "returns from:$NEW_LINE"
    override val invokeLine: String = "invokes:$NEW_LINE"
    override val switchCaseLine: String = "activates switch case: "
    override val caughtExceptionLine: String = "catches exception:$NEW_LINE"
    override val recursionAssignmentLine: String = "triggers recursion of "
}

class StringsTemplatesPlural : StringsTemplatesSingular() {
    override val invokeSentence: String = "call %s$COMMA_SYMBOL$NEW_LINE${TAB}there it %s"
    override val iterationSentence: String = "iterate the loop %s %s"
    override val insideIterationSentence: String = "inside this loop$COMMA_SYMBOL the test %s"
    override val recursionSentence: String = "trigger the recursion of %s"
    override val insideRecursionSentence: String = "$COMMA_SYMBOL where the test %s"
    override val sentenceBeginning: String = "Tests"
    override val noIteration: String = "does not iterate"
    override val codeSentence: String = "$OPEN_BRACKET$AT_CODE %s$CLOSE_BRACKET"
    override val throwSentence: String = "throw %s"
    override val suspiciousBehaviorDetectedSentence: String = "detect suspicious behavior %s"

    //used in Squashing
    override val conditionLine: String = "execute conditions:$NEW_LINE"
    override val returnLine: String = "return from: "
    override val countedReturnLine: String = "return from:$NEW_LINE"
    override val invokeLine: String = "invoke:$NEW_LINE"
    override val switchCaseLine: String = "activate switch case: "
    override val caughtExceptionLine: String = "catches exception:$NEW_LINE"
    override val recursionAssignmentLine: String = "trigger recursion of "
}

/**
 * Replaces %s in template with docStmts in vararg
 * If vararg contains more elements than template contains %s then remaining statements are added in the end
 */
fun templateToDocStmt(template: String, vararg docStmts: DocStatement): List<DocStatement> {
    val stmts = mutableListOf<DocStatement>()
    val splitTemplate = template.split("%s")
    for (i in splitTemplate.indices) {
        stmts += DocRegularStmt(splitTemplate[i])
        if (i < docStmts.size) stmts += docStmts[i]
    }
    if (splitTemplate.size < docStmts.size) stmts +=
        docStmts.slice(splitTemplate.size until docStmts.size)
    return stmts
}
