package org.utbot.usvm.jc

import org.utbot.common.JarUtils

object JcJars {
    val approximationsJar by lazy { extractUsvmJar("approximations.jar") }
    val approximationsApiJar by lazy { extractUsvmJar("usvm-jvm-api.jar") }
    val collectorsJar by lazy { extractUsvmJar("usvm-jvm-instrumentation-collectors.jar") }
    val runnerJar by lazy { extractUsvmJar("usvm-jvm-instrumentation-runner.jar") }

    private fun extractUsvmJar(jarFileName: String) = JarUtils.extractJarFileFromResources(
        jarFileName = jarFileName,
        jarResourcePath = "lib/$jarFileName",
        targetDirectoryName = "usvm"
    )
}

