package org.utbot.intellij.plugin.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.unscramble.AnalyzeStacktraceUtil
import javax.swing.Icon

/**
 * Button that launches the built-in "Analyze Stack Trace" action. Displayed as a quick fix.
 *
 * @param exceptionMessage short description of the detected exception.
 * @param stackTraceLines list of strings of the form "className.methodName(fileName:lineNumber)".
 */
class AnalyzeStackTraceFix(
    private val exceptionMessage: String,
    private val stackTraceLines: List<String>
) : LocalQuickFix, Iconable {

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

    /**
     * This text is displayed on the quick fix button.
     */
    override fun getName() = "Analyze stack trace"

    override fun getFamilyName() = name

    override fun getIcon(flags: Int): Icon = AllIcons.Actions.Lightning
}