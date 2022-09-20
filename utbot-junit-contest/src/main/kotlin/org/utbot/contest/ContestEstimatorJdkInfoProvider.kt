package org.utbot.contest

import org.utbot.common.PathUtil.toPath
import org.utbot.framework.plugin.services.JdkInfo
import org.utbot.framework.plugin.services.JdkInfoDefaultProvider
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * This class is used to provide a path to jdk and its version in [ContestEstimator]
 * into the child process for concrete execution.
 */
class ContestEstimatorJdkInfoProvider(private val path: String) : JdkInfoDefaultProvider() {
    override val info: JdkInfo
        get() = JdkInfo(path.toPath(), jdkVersionFrom(jdkPath = path)) // TODO: retrieve the correct version
}

private fun jdkVersionFrom(jdkPath: String) : Int {
    val processBuilder = ProcessBuilder(("$jdkPath${File.separatorChar}bin${File.separatorChar}java").toString(), "-version");
    val process = processBuilder.start()
    val errorStream = process.errorStream
    val bufferedReader = BufferedReader(InputStreamReader(errorStream))
    val javaVersionString = bufferedReader.use {
        bufferedReader.readLine()
    }
    val matcher = "\"(1\\.|)(\\d*)".toRegex()
    return Integer.parseInt(matcher.find(javaVersionString)?.groupValues?.getOrNull(2)!!)
}