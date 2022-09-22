package org.utbot.intellij.plugin.inspection

import com.intellij.codeInspection.ex.GlobalInspectionContextImpl
import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.ui.content.ContentManager
import org.utbot.sarif.Sarif
import java.nio.file.Path

/**
 * Overrides some methods of [InspectionManagerEx] to satisfy the logic of [UTBotInspectionTool].
 */
class UTBotInspectionManager(project: Project) : InspectionManagerEx(project) {

    private var srcClassPathToSarifReport: MutableMap<Path, Sarif> = mutableMapOf()

    companion object {
        fun getInstance(project: Project, srcClassPathToSarifReport: MutableMap<Path, Sarif>) =
            UTBotInspectionManager(project).also {
                it.srcClassPathToSarifReport = srcClassPathToSarifReport
            }
    }

    /**
     * See [InspectionManagerEx.myContentManager] for more details.
     */
    private val myContentManager: NotNullLazyValue<ContentManager> by lazy {
        NotNullLazyValue.createValue {
            getProblemsViewContentManager(project)
        }
    }

    /**
     * Overriding is needed to provide [UTBotInspectionContext] instead of [GlobalInspectionContextImpl].
     */
    override fun createNewGlobalContext(): GlobalInspectionContextImpl =
        UTBotInspectionContext(project, myContentManager, srcClassPathToSarifReport)
}
