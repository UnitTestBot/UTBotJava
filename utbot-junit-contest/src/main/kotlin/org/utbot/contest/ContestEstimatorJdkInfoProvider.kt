package org.utbot.contest

import org.utbot.common.PathUtil.toPath
import org.utbot.framework.plugin.services.JdkInfo
import org.utbot.framework.plugin.services.JdkInfoDefaultProvider

/**
 * This class is used to provide a path to jdk and its version in [ContestEstimator]
 * into the child process for concrete execution.
 */
class ContestEstimatorJdkInfoProvider(private val path: String) : JdkInfoDefaultProvider() {
    override val info: JdkInfo
        get() = JdkInfo(path.toPath(), super.info.version) // TODO: retrieve the correct version
}