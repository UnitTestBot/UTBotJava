package org.utbot.intellij.plugin.inspection

import com.intellij.codeInspection.ex.*
import com.intellij.codeInspection.ui.InspectionToolPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.ui.content.ContentManager
import org.utbot.sarif.Sarif
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Overrides some methods of [GlobalInspectionContextImpl] to satisfy the logic of [UTBotInspectionTool].
 */
class UTBotInspectionContext(
    project: Project,
    contentManager: NotNullLazyValue<out ContentManager>,
    val srcClassPathToSarifReport: MutableMap<Path, Sarif>
) : GlobalInspectionContextImpl(project, contentManager) {

    /**
     * See [GlobalInspectionContextImpl.myPresentationMap] for more details.
     */
    private val myPresentationMap: ConcurrentMap<InspectionToolWrapper<*, *>, InspectionToolPresentation> =
        ConcurrentHashMap()

    private val globalInspectionToolWrapper by lazy {
        val utbotInspectionTool = UTBotInspectionTool.getInstance(srcClassPathToSarifReport)
        GlobalInspectionToolWrapper(utbotInspectionTool).also {
            it.initialize(/* context = */ this)
        }
    }

    /**
     * Returns [InspectionProfileImpl] with only one inspection tool - [UTBotInspectionTool].
     */
    override fun getCurrentProfile(): InspectionProfileImpl {
        val supplier = InspectionToolsSupplier.Simple(listOf(globalInspectionToolWrapper))
        return InspectionProfileImpl("UTBotInspectionToolProfile", supplier, BASE_PROFILE)
    }

    override fun close(noSuspiciousCodeFound: Boolean) {
        myPresentationMap.clear()
        super.close(noSuspiciousCodeFound)
    }

    override fun cleanup() {
        myPresentationMap.clear()
        super.cleanup()
    }

    /**
     * Overriding is needed to provide [UTBotInspectionToolPresentation]
     * instead of the standard implementation of the [InspectionToolPresentation].
     */
    override fun getPresentation(toolWrapper: InspectionToolWrapper<*, *>): InspectionToolPresentation {
        return myPresentationMap.computeIfAbsent(toolWrapper) {
            UTBotInspectionToolPresentation(globalInspectionToolWrapper, context = this)
        }
    }
}
