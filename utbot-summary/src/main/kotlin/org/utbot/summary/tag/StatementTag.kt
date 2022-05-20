package org.utbot.summary.tag

import org.utbot.framework.plugin.api.Step
import org.utbot.summary.SummarySentenceConstants.NEW_LINE
import org.utbot.summary.SummarySentenceConstants.TAB
import soot.SootMethod
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JInvokeStmt

class StatementTag(
    val step: Step,
    val uniquenessTag: UniquenessTag,
    val callOrderTag: CallOrderTag,// the same statement order of execution
    val executionFrequency: Int, // how many times it is executed in this execution
    val index: Int, // index in execution sequence
    var codeEnvironment: CodeEnvironment = CodeEnvironment.MUT
) {
    var basicTypeTag = getBasicTypeTag(step.stmt)
    val executionTag = getExecutionTag(step)
    val line = step.stmt.javaSourceStartLineNumber //line of code
    var next: StatementTag? = null
    var invoke: StatementTag? = null
    var recursion: StatementTag? = null
    val iterations = mutableListOf<StatementTag>()

    fun invokeSootMethod(): SootMethod? {
        if (basicTypeTag == BasicTypeTag.Invoke) {
            val stmt = step.stmt
            if (stmt is JInvokeStmt || stmt is JAssignStmt) {
                return stmt.invokeExpr.method
            }
        }
        return null
    }

    /**
     * Recursively updates tags of the execution environment.
     * The execution environments: the method under test, recursion, invoke
     */
    fun updateExecutionEnvironmentTag(codeEnvironment: CodeEnvironment) {
        this.codeEnvironment = codeEnvironment
        invoke?.updateExecutionEnvironmentTag(CodeEnvironment.Invoke)
        recursion?.updateExecutionEnvironmentTag(CodeEnvironment.Recursion)
        iterations.forEach { it.updateExecutionEnvironmentTag(codeEnvironment) }
        next?.updateExecutionEnvironmentTag(codeEnvironment)
    }

    override fun toString(): String = buildString {
        append("$line: ${step.stmt}")
        invoke?.let {
            append(NEW_LINE)
            append("${TAB}Invocation:$NEW_LINE")
            append("$TAB$TAB")
            append(it.toString().replace(NEW_LINE, "$NEW_LINE${TAB}${TAB}"))
        }
        recursion?.let {
            append(NEW_LINE)
            append("${TAB}Invocation:$NEW_LINE")
            append("${TAB}${TAB}")
            append(invoke.toString().replace(NEW_LINE, "$NEW_LINE${TAB}${TAB}"))
        }
        if (iterations.isNotEmpty()) {
            append(NEW_LINE)
            append("Iterations: ${iterations.size}")
            for (i in 0 until iterations.size) {
                append(NEW_LINE)
                append("${TAB}Iteration $i$NEW_LINE")
                append("${TAB}${TAB}")
                append(iterations[i].toString().replace(NEW_LINE, "$NEW_LINE${TAB}${TAB}"))
            }
        }
        next?.let {
            append("$NEW_LINE$next")
        }
    }

    fun fullPrint(): String = buildString {
        append("$line, ${step.depth}: ${step.stmt} $basicTypeTag $executionTag ")
        append("UNIQ:$uniquenessTag ORDER:$callOrderTag FREQ::$executionFrequency ENV:$codeEnvironment")
        invoke?.let {
            append(NEW_LINE)
            append("${TAB}Invocation:$NEW_LINE")
            append("${TAB}${TAB}")
            append(it.fullPrint().replace(NEW_LINE, "$NEW_LINE${TAB}${TAB}"))
        }
        recursion?.let {
            append(NEW_LINE)
            append("${TAB}Recursion:$NEW_LINE")
            append("${TAB}${TAB}")
            append(it.fullPrint().replace(NEW_LINE, "$NEW_LINE${TAB}${TAB}"))
        }
        if (iterations.isNotEmpty()) {
            append(NEW_LINE)
            append("Iterations: ${iterations.size}")
            for (i in 0 until iterations.size) {
                append(NEW_LINE)
                append("${TAB}Iteration $i$NEW_LINE")
                append("${TAB}${TAB}")
                append(iterations[i].fullPrint().replace(NEW_LINE, "$NEW_LINE${TAB}${TAB}"))
            }
        }
        next?.let {
            append("$NEW_LINE${it.fullPrint()}")
        }
    }

    fun executionStepsStructure(): String = buildString {
        append("$line, ${step.decision}, ${step.depth}: ${step.stmt} ")
        invoke?.let {
            append(NEW_LINE)
            append("${TAB}Invocation:$NEW_LINE")
            append("${TAB}${TAB}")
            append(it.executionStepsStructure().replace(NEW_LINE, "$NEW_LINE${TAB}${TAB}"))
        }
        recursion?.let {
            append(NEW_LINE)
            append("${TAB}Recursion:$NEW_LINE")
            append("${TAB}${TAB}")
            append(it.executionStepsStructure().replace(NEW_LINE, "$NEW_LINE${TAB}${TAB}"))
        }
        if (iterations.isNotEmpty()) {
            append(NEW_LINE)
            append("Iterations: ${iterations.size}")
            for (i in 0 until iterations.size) {
                append(NEW_LINE)
                append("${TAB}Iteration $i$NEW_LINE")
                append("${TAB}${TAB}")
                append(iterations[i].executionStepsStructure().replace(NEW_LINE, "$NEW_LINE${TAB}${TAB}"))
            }
        }
        next?.let {
            append(NEW_LINE)
            append(it.executionStepsStructure())
        }
    }
}