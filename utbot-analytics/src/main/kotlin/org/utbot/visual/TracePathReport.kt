package org.utbot.visual

import org.utbot.summary.tag.BasicTypeTag
import org.utbot.summary.tag.ExecutionTag
import org.utbot.summary.tag.UniquenessTag
import org.utbot.summary.tag.StatementTag
import org.utbot.summary.tag.TraceTag
import soot.jimple.JimpleBody

class TracePathReport : AbstractHtmlReport(bodyWidth = 1200) {
    private val tab = "&nbsp;&nbsp;&nbsp;&nbsp;"

    private val freqSymbols = mapOf(
        UniquenessTag.Common to "  ",
        UniquenessTag.Unique to "★ </mark>",
        UniquenessTag.Partly to "► "
    )

    private val basicTagColors = mapOf(
        BasicTypeTag.Initialization to "#aab7b8",
        BasicTypeTag.Condition to "black",
        BasicTypeTag.Return to "#28b463",
        BasicTypeTag.Assignment to "black",
        BasicTypeTag.Basic to "black",
        BasicTypeTag.ExceptionAssignment to "red",
        BasicTypeTag.ExceptionThrow to "red",
        BasicTypeTag.Invoke to "black",
        BasicTypeTag.IterationStart to "blue",
        BasicTypeTag.IterationEnd to "#2874a6"
    )

    private val executionPostfix = mapOf(
        ExecutionTag.True to " : ✓",
        ExecutionTag.False to " : ✗",
        ExecutionTag.Executed to ""
    )

    fun addJimpleBody(jimpleBody: JimpleBody?) {
        if (jimpleBody == null)
            return
        var result = "<h2>Jimple Body</h2>\n"
        jimpleBody.units.forEach {
            result += "${it.javaSourceStartLineNumber}:$tab  $it <br>\n"
        }

        builder.addRawHTML(result)
    }

    private fun buildStructViewStatementTag(statementTag: StatementTag?): String {
        if (statementTag == null)
            return ""
        var result = "<li> ${statementTag.step.stmt} \n" +
                "<br> Decision = <strong>${markDecision(statementTag.executionTag)}</strong>\n" +
                "<br> Line = <strong>${statementTag.line}</strong> \n" +
                "<br> Type = <strong>${statementTag.basicTypeTag}</strong> \n" +
                "<br> Frequency = <strong>${statementTag.uniquenessTag}</strong> \n" +
                "<br> Call times = <strong>${statementTag.callOrderTag}</strong> <br>\n" +
                "</li>"

        if (statementTag.invoke != null) {
            result += "\n<br><strong>Invocation:</strong> <br>\n" +
                    "<ul>${buildStructViewStatementTag(statementTag.invoke)}</ul>\n"
        }
        if (statementTag.iterations.size > 0) {
            result += "<strong>Iterations:</strong>\n"
            result += "<ul>"
            for (i in 0 until statementTag.iterations.size) {
                result += "<br><strong>Iteration $i</strong><br> ${buildStructViewStatementTag(statementTag.iterations[i])}\n"
            }
            result += "</ul>"
        }
        if (statementTag.next != null) {
            result += "<br>${buildStructViewStatementTag(statementTag.next)}\n"
        }
        return result
    }

    fun addStructViewTraces(tracesTags: Iterable<TraceTag>) {
        var table = "<h2> Struct View </h2>\n <table style=\"width:100%;  vertical-align:top;\">" +
                "<tr>\n"
        tracesTags.forEach {
            table += "<td style=\"vertical-align:top;\"> <ul> ${buildStructViewStatementTag(it.rootStatementTag)}</ul></td>\n"
        }
        table += "</tr>\n"
        builder.addRawHTML(table)
    }

    fun addTracesTable(tagsToKeywords: List<List<TraceTag>>, name: String) {
        var table = "<h2>$name</h2>\n" +
                "<table style=\"width:100%\">"

        table += "<th>№ </th>"
        table += "<th>Jimple code</th>"
        table += "<th>Source code</th>"
        table += "<th>Keywords</th> "
        table += "<th>Comment</th>"

        for ((clusterNum, clusterTraceTags) in tagsToKeywords.withIndex()) {

            for (traceTags in clusterTraceTags) {
                table += "<tr>"
                val traceTagsVisual = traceTags.rootStatementTag?.let { visualizeStatementTag(it) }
                table += "<td>$clusterNum</td>"

                table += "<td><pre>"
                table += traceTagsVisual ?: "None"
                table += "</pre></td>"

                table += "<td><pre>"
                table += "No source code yet"
                table += "</pre></td>"

                table += "<td><pre>"
                table += traceTags.summary
                table += "</pre></td>"
                table += "</tr>"
            }
        }
        table += "</table>"
        builder.addRawHTML(table)
    }

    private fun markDecision(executionTag: ExecutionTag): String {
        var result = when (executionTag) {
            ExecutionTag.True -> "<span style=\"color:green;\">"
            ExecutionTag.False -> "<span style=\"color:red;\">"
            else -> "<span style=\"color:black;\">"
        }
        result += "$executionTag </span>"
        return result
    }

    fun addExecutionVisualisation(rootTag: StatementTag, name: String){
        var visualization = "<h2>$name</h2>\n"
        visualization += "<pre>"
        visualization += visualizeStatementTag(rootTag)
        visualization += "</pre>"
        builder.addRawHTML(visualization)
    }

    private fun visualizeStatementTag(tag: StatementTag, tabPrefix: String = ""): String {
        var localTabPrefix = tabPrefix
        var visualization = ""

        visualization += "<font color=\"${basicTagColors[tag.basicTypeTag]}\">"
        visualization += tag.line.toString().padEnd(3, ' ') + ':'
        visualization += localTabPrefix
        visualization += freqSymbols[tag.uniquenessTag]
        if (tag.uniquenessTag == UniquenessTag.Unique) visualization += "<mark>"
        visualization += tag.step.stmt.toString()
        visualization += executionPostfix[tag.executionTag]
        if (tag.uniquenessTag == UniquenessTag.Unique) visualization += "</mark>"
        visualization += "</font>"
        visualization += "\n"

        if (tag.invoke != null) visualization += visualizeStatementTag(tag.invoke!!, localTabPrefix + "\t")
        if (tag.iterations.size > 0) {
            for (cycle in tag.iterations) visualization += visualizeStatementTag(cycle, localTabPrefix + "\t")
        }

        if (tag.basicTypeTag == BasicTypeTag.IterationEnd) localTabPrefix = localTabPrefix.removePrefix("\t")
        if (tag.next != null) visualization += visualizeStatementTag(tag.next!!, localTabPrefix)

        return visualization
    }

}