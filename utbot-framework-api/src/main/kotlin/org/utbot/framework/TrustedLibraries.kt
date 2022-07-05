package org.utbot.framework

import mu.KotlinLogging
import org.utbot.common.PathUtil.toPath
import java.io.IOException

private val logger = KotlinLogging.logger {}

private val defaultUserTrustedLibrariesPath: String = "${utbotHomePath}/trustedLibraries.txt"
private const val userTrustedLibrariesKey: String = "utbot.settings.trusted.libraries.path"

object TrustedLibraries {
    /**
     * Always "trust" JDK.
     */
    private val defaultTrustedLibraries: List<String> = listOf(
        "java",
        "sun",
        "javax",
        "com.sun",
        "org.omg",
        "org.xml",
        "org.w3c.dom",
    )

    private val userTrustedLibraries: List<String>
        get() {
            val userTrustedLibrariesPath = System.getProperty(userTrustedLibrariesKey) ?: defaultUserTrustedLibrariesPath
            val userTrustedLibrariesFile = userTrustedLibrariesPath.toPath().toFile()

            if (!userTrustedLibrariesFile.exists()) {
                return emptyList()
            }

            return try {
                userTrustedLibrariesFile.readLines()
            } catch (e: IOException) {
                logger.info { e.message }

                emptyList()
            }
        }

    /**
     * Represents prefixes of packages for trusted libraries -
     * as the union of [defaultTrustedLibraries] and [userTrustedLibraries].
     */
    val trustedLibraries: Set<String> by lazy { (defaultTrustedLibraries + userTrustedLibraries).toSet() }
}