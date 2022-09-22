package org.utbot.intellij.plugin.inspection

import com.intellij.codeInspection.CommonProblemDescriptor
import com.intellij.codeInspection.ex.InspectionToolWrapper
import com.intellij.codeInspection.ui.DefaultInspectionToolPresentation

/**
 * Overrides [resolveProblem] to avoid suppressing the button [ViewGeneratedTestFix].
 */
class UTBotInspectionToolPresentation(
    toolWrapper: InspectionToolWrapper<*, *>,
    context: UTBotInspectionContext
) : DefaultInspectionToolPresentation(toolWrapper, context) {

    /**
     * This method is called when the user clicks on the quick fix button.
     * In the case of [UTBotInspectionTool] we do not want to remove the button after applying the fix.
     *
     * See [DefaultInspectionToolPresentation.resolveProblem] for more details.
     */
    override fun resolveProblem(descriptor: CommonProblemDescriptor) {
        // nothing
    }
}