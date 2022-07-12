package org.utbot.framework

import org.utbot.common.FileUtil
import org.utbot.framework.util.runSoot
import java.nio.file.Path
import kotlin.reflect.KClass

object SootUtils {

    /**
     * Runs Soot in tests if it hasn't already been done.
     */
    fun runSoot(clazz: KClass<*>) {
        val buildDir = FileUtil.locateClassPath(clazz) ?: FileUtil.isolateClassFiles(clazz)
        val buildDirPath = buildDir.toPath()

        if (buildDirPath != previousBuildDir) {
            runSoot(buildDirPath, null)
            previousBuildDir = buildDirPath
        }
    }

    private var previousBuildDir: Path? = null
}
