package org.utbot.intellij.plugin.inspection

import com.intellij.codeInspection.ex.GlobalInspectionContextImpl
import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.ui.content.ContentManager
import org.utbot.sarif.Sarif
import java.nio.file.Path

/**
 * Creates [UTBotInspectionContext] with right arguments.
 *
 * Inheritance is needed to provide [UTBotInspectionContext] instead of [GlobalInspectionContextImpl].
 *
 * See [com.intellij.codeInspection.ex.InspectionManagerEx] for details.
 */
class UTBotInspectionManager(project: Project) : InspectionManagerEx(project) {

    companion object {
        fun getInstance(project: Project, sarifReports: MutableMap<Path, Sarif>) =
            UTBotInspectionManager(project).also {
                it.sarifReports = sarifReports
            }
    }

    private val myContentManager: NotNullLazyValue<ContentManager> by lazy {
        NotNullLazyValue.createValue {
            getProblemsViewContentManager(project)
        }
    }

    private var sarifReports: MutableMap<Path, Sarif> = mutableMapOf()

    override fun createNewGlobalContext(): GlobalInspectionContextImpl =
        UTBotInspectionContext(project, myContentManager, sarifReports)
}
