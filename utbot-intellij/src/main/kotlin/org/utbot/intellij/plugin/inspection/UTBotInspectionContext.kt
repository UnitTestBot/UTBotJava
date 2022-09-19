package org.utbot.intellij.plugin.inspection

import com.intellij.codeInspection.ex.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.ui.content.ContentManager
import org.utbot.sarif.Sarif
import java.nio.file.Path

/**
 * Provides [InspectionProfileImpl] with only one inspection tool - [UTBotInspectionTool].
 *
 * @see GlobalInspectionContextImpl
 */
class UTBotInspectionContext(
    project: Project,
    contentManager: NotNullLazyValue<out ContentManager>,
    val sarifReports: MutableMap<Path, Sarif>
) : GlobalInspectionContextImpl(project, contentManager) {

    override fun getCurrentProfile(): InspectionProfileImpl {
        val utbotInspectionTool = UTBotInspectionTool.getInstance(sarifReports)
        val globalInspectionToolWrapper = GlobalInspectionToolWrapper(utbotInspectionTool)
        globalInspectionToolWrapper.initialize(this)
        val supplier = InspectionToolsSupplier.Simple(listOf(globalInspectionToolWrapper))
        return InspectionProfileImpl("UTBotInspectionToolProfile", supplier, BASE_PROFILE)
    }
}
