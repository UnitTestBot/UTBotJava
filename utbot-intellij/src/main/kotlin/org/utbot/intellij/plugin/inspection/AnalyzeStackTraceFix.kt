package org.utbot.intellij.plugin.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.unscramble.AnalyzeStacktraceUtil

class AnalyzeStackTraceFix(
    private val exceptionMessage: String,
    private val stackTraceLines: List<String>
) : LocalQuickFix {

    /**
     * Without `invokeLater` the [com.intellij.execution.impl.ConsoleViewImpl.myPredefinedFilters] will not be filled.
     *
     * See [com.intellij.execution.impl.ConsoleViewImpl.createCompositeFilter] for more details.
     */
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val stackTraceContent = stackTraceLines.joinToString("\n") { "at $it" }
        ApplicationManager.getApplication().invokeLater {
            AnalyzeStacktraceUtil.addConsole(
                /* project = */ project,
                /* consoleFactory = */ null,
                /* tabTitle = */ "StackTrace",
                /* text = */ "$exceptionMessage\n\n$stackTraceContent",
                /* icon = */ AllIcons.Actions.Lightning
            )
        }
    }

    override fun getName() = "Analyze stack trace"

    override fun getFamilyName() = name
}