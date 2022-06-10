package org.utbot.contest

import org.utbot.common.PathUtil.toPath
import org.utbot.framework.JdkPathDefaultProvider
import java.nio.file.Path

/**
 * This class is used to provide jdkPath set in [ContestEstimator]
 * into the child process for concrete execution.
 */
class ContestEstimatorJdkPathProvider(private val path: String) : JdkPathDefaultProvider() {
    override val jdkPath: Path
        get() = path.toPath()
}