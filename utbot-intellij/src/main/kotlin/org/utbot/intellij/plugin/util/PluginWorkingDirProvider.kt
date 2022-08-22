package org.utbot.intellij.plugin.util

import com.intellij.openapi.project.Project
import org.utbot.common.PathUtil.toPath
import org.utbot.framework.plugin.services.WorkingDirDefaultProvider
import java.nio.file.Path

class PluginWorkingDirProvider(
    project: Project,
) : WorkingDirDefaultProvider() {

    /**
     * We believe that in most cases the test runner working dir is the project root, otherwise we need to parse test
     * configuration, but it's not easy.
     */
    override val workingDir: Path =
        project.basePath?.toPath()
            ?: super.workingDir
}