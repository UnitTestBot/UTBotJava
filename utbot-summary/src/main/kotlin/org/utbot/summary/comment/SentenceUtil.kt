package org.utbot.summary.comment

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.ThrowStmt
import com.github.javaparser.ast.stmt.WhileStmt
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.DocTagStatement
import org.utbot.summary.SummarySentenceConstants.AT_CODE
import org.utbot.summary.SummarySentenceConstants.COMMA_SYMBOL
import org.utbot.summary.SummarySentenceConstants.DOT_SYMBOL
import org.utbot.summary.SummarySentenceConstants.NEW_LINE
import org.utbot.summary.SummarySentenceConstants.TAB
import org.utbot.summary.comment.classic.symbolic.SquashedStmtTexts
import org.utbot.summary.comment.classic.symbolic.StmtType

fun numberWithSuffix(number: Int) = when (number % 10) {
    1 -> "${number}st"
    2 -> "${number}nd"
    3 -> "${number}rd"
    else -> "${number}th"
}

/**
 * Returns reason for given ThrowStmt
 * It can be parent ast node
 * or grandparent node if parent is BlockStmt class,
 */
fun getExceptionReason(throwStmt: ThrowStmt): Node? {
    val parent = throwStmt.parentNode.orElse(null)
    if (parent != null) {
        return if (parent is BlockStmt) {
            parent.parentNode.orElse(null)
        } else {
            parent
        }
    }
    return null
}

fun numberOccurrencesToText(n: Int): String = when (n) {
    0 -> ""
    1 -> "once"
    2 -> "twice"
    else -> "$n times"
}

fun getTextIterationDescription(node: Node): String = when (node) {
    is WhileStmt -> whileDescription(node)
    is ForStmt -> forDescription(node)
    is ForEachStmt -> forEachDescription(node)
    else -> ""
}

fun getTextTypeIterationDescription(node: Node): String = when (node) {
    is WhileStmt -> "WhileLoop"
    is ForStmt -> "ForLoop"
    is ForEachStmt -> "ForEachLoop"
    else -> ""
}

fun isLoopStatement(node: Node) = when (node) {
    is WhileStmt -> true
    is ForStmt -> true
    is ForEachStmt -> true
    else -> false
}

fun prepareString(str: String) = str.substring(1, str.length - 1)

fun whileDescription(whileStmt: WhileStmt) = "while(${whileStmt.condition})"

fun forDescription(forStmt: ForStmt): String {
    val init = if (forStmt.initialization.isNonEmpty) prepareString(forStmt.initialization.toString()) else ""
    val compare = if (forStmt.compare.isPresent) forStmt.compare.get().toString() else ""
    val update = if (forStmt.update.isNonEmpty) prepareString(forStmt.update.toString()) else ""

    return "for($init; $compare; $update)"
}

fun forEachDescription(forEachStmt: ForEachStmt) = "for(${forEachStmt.variable}: ${forEachStmt.iterable})"

val nextSynonyms = arrayOf(
    "afterwards",
    "later",
    "further",
    "next",
    "then"
)

val forbiddenMethodInvokes = arrayOf(
    "<init>",
    "<clinit>",
    "valueOf",
    "getClass",
)

val forbiddenClassInvokes = arrayOf(
    "soot.dummy.InvokeDynamic"
)

/**
 * Filters out
 * ```soot.dummy.InvokeDynamic#makeConcat```,
 * ```soot.dummy.InvokeDynamic#makeConcatWithConstants```,
 * constructor calls (```<init>```), ```bootstrap$```
 * and other unwanted things from name and comment text.
 */
fun shouldSkipInvoke(className: String, methodName: String) =
    className in forbiddenClassInvokes || methodName in forbiddenMethodInvokes || methodName.endsWith("$")

fun squashDocRegularStatements(sentences: List<DocStatement>): List<DocStatement> {
    val newStatements = mutableListOf<DocStatement>()
    var str = ""
    for (stmt in sentences) {
        when (stmt) {
            is DocRegularStmt -> str += stmt.stmt
            is DocTagStatement -> newStatements += squashDocRegularStatements(stmt.content)
            else -> {
                if (str.isNotEmpty()) {
                    newStatements += DocRegularStmt(str)
                    str = ""
                }
                newStatements += stmt
            }
        }
    }

    return newStatements
}

fun squashIdenticalRegularStatements(sentences: List<DocStatement>): List<DocStatement> {
    val regularsReversed = sentences.indices.filter { sentences[it] is DocRegularStmt }.reversed()
    val mutableSentences = sentences.toMutableList()
    for (i in regularsReversed.indices) {
        if (i == regularsReversed.size - 1) break
        val curr = sentences[regularsReversed[i]]
        val prev = sentences[regularsReversed[i + 1]]
        curr as DocRegularStmt
        prev as DocRegularStmt

        if (curr.stmt.trim() in prev.stmt.trim()) {
            mutableSentences[regularsReversed[i]] =
                DocRegularStmt(curr.stmt.replace(curr.stmt.trim(), "").replace("\n\n", "\n"))
        }
    }
    return mutableSentences
}

/**
 * This code may be have to be replaced in the future.
 * It is used only for iteration sentence blocks. Loops can be iterated
 * multiple times, in each iteration it can trigger multiple
 * unique statements. And each iteration is one sentence block!!!
 * Before sentence is built each block is squashed:
 * @see SimpleSentenceBlock#squashStmtText
 * then string is built for each such block.
 * Now, remember one loop can be multiple squashed sentence strings.
 * We can actually concat them and built sentence. However, one particular case with
 * multiple squashed sentences of one loop iterations can be:
 *      iteration 1, squashed txt: triggers condition: c1, c2
 *      iteration 2, squashed txt: triggers condition: c3, c4, c5
 *      iteration 3, squashed txt: invokes: i1
 * This function squashes above structure into:
 * multiple squashed sentences of one loop:
 *      iteration 1, squashed txt: triggers condition: c1, c2, c3, c4, c5
 *      iteration 3, squashed txt: invokes: i1
 */
fun squash(sentences: MutableList<String>): String {

    for (i in sentences.size - 2 downTo 0) {
        val sentence = sentences[i]
        val prevSentence = sentences[i + 1]

        val prevCommandStructs = prevSentence.split(NEW_LINE).filter {
            it.isNotEmpty() && !it.contains(AT_CODE) && !it.contains(TAB)
        }

        val commandStructs = sentence.split(NEW_LINE).filter {
            it.isNotEmpty() && !it.contains(AT_CODE) && !it.contains(TAB)
        }
        val prevCommand = prevCommandStructs.firstOrNull()
        val command = commandStructs.lastOrNull()

        if (!prevCommand.isNullOrEmpty() && prevCommand == command) {
            val prevSubSentence = prevSentence.substringAfter(prevCommand + NEW_LINE)
            sentences[i] += prevSubSentence
            sentences.removeAt(i + 1)
        }
    }

    return sentences.joinToString()
}

/**
 * This one is a prittification function which doesn't affect on the structure of the sentence.
 * The whole comment is multiple lines. In one line a sentence can end and another one can start.
 * This function looks at each line, if there are more than 2 sentences in one line, and
 * one of them has more than 100 chars, then that sentence is moved new line.
 */
fun splitLongSentence(sentence: String): String {
    var result = ""
    val subSentences = sentence.split(NEW_LINE) //split by line
    for (subSentence in subSentences) {
        val simpleSentences = subSentence.split("$DOT_SYMBOL ") // split by dot, Now we have 'subsentences'
        //if one of the subsentences have more than 100 chars, it is moved to new line.
        var countCharacterPerLine = 0
        for (i in 0 until simpleSentences.size - 1) {
            val simpleSentence = simpleSentences[i]
            when {
                countCharacterPerLine + simpleSentence.length <= 100 -> {//
                    countCharacterPerLine += simpleSentence.length
                    result += "$simpleSentence. "
                }
                i != 0 -> {
                    countCharacterPerLine = simpleSentence.length
                    result += "\n$simpleSentence. "
                }
                else -> {
                    countCharacterPerLine = simpleSentence.length
                    result += "$simpleSentence. "
                }
            }
        }
        // the same as above only for the last line
        val simpleSentence = simpleSentences[simpleSentences.size - 1]
        result += when {
            countCharacterPerLine + simpleSentence.length <= 100 -> simpleSentence
            simpleSentences.size != 1 -> NEW_LINE + simpleSentence
            else -> simpleSentence
        }
        result += NEW_LINE
    }
    return result.substring(0, result.length - 1)
}

fun lastCommaToDot(sentence: String): String {
    val result = sentence.trimEnd()
    if (result.lastOrNull() == COMMA_SYMBOL) return result.substring(0, result.length - 1) + DOT_SYMBOL + NEW_LINE
    return sentence
}


fun <T> merge(first: List<T>, second: List<T>): MutableList<T> {
    val list: MutableList<T> = ArrayList(first)
    list += second
    return list
}

val criteriaRepetition: (
    SquashedStmtTexts, SquashedStmtTexts
) -> Boolean = { previousSquashedStmts, currentSquashedStmts ->
    if (previousSquashedStmts.stmtType == currentSquashedStmts.stmtType) {
        currentSquashedStmts.stmtTexts = merge(previousSquashedStmts.stmtTexts, currentSquashedStmts.stmtTexts)
        true
    } else {
        false
    }
}


/*
  If there is invoke that
      has the same name as in condition,
      condition and invoke located in one line,
      condition and invoke are consecutively described
  then we can remove invoke description
*/
val criteriaInvokeCalledInUniqueCondition: (SquashedStmtTexts, SquashedStmtTexts) -> Boolean =
    { previousSquashedStmts, currentSquashedStmts ->
        var result = false
        if (previousSquashedStmts.stmtType == StmtType.Invoke && currentSquashedStmts.stmtType == StmtType.Condition) {
            val stmtText = previousSquashedStmts.stmtTexts.firstOrNull()
            if (stmtText != null) {
                val methodName = stmtText.description.substringAfter("::")
                val repetition = currentSquashedStmts.stmtTexts.firstOrNull()?.description?.contains(methodName)
                if (repetition == true) result = true
            }
        }
        result
    }


fun filterSquashedText(
    squashedStmtTexts: MutableSet<SquashedStmtTexts>,
    criteria: (
        previousSquashedStmts: SquashedStmtTexts,
        currentSquashedStmts: SquashedStmtTexts
    ) -> Boolean
) {
    if (squashedStmtTexts.size < 2)
        return
    val toRemoveSquashedStmtTexts = mutableSetOf<SquashedStmtTexts>()
    var prevSquashedStmtText: SquashedStmtTexts? = null
    // apply filter
    for (squashedStmtText in squashedStmtTexts) {
        prevSquashedStmtText?.let { if (criteria(it, squashedStmtText)) toRemoveSquashedStmtTexts += it }
        prevSquashedStmtText = squashedStmtText
    }
    // remove
    toRemoveSquashedStmtTexts.forEach { squashedStmtTexts.remove(it) }
}
