package org.utbot.framework.process

import org.utbot.framework.plugin.services.JdkInfoService

object OpenModulesContainer {
    private val modulesContainer: List<String>
    val javaVersionSpecificArguments: List<String>
        get() = modulesContainer
            .takeIf { JdkInfoService.provide().version > 8 }
            ?: emptyList()

    init {
        modulesContainer = buildList {
            openPackage("java.base", "jdk.internal.misc")
            openPackage("java.base", "java.lang")
            openPackage("java.base", "java.lang.reflect")
            openPackage("java.base", "sun.security.provider")
            add("--illegal-access=warn")
        }
    }

    private fun MutableList<String>.openPackage(module: String, pakage: String) {
        add("--add-opens")
        add("$module/$pakage=ALL-UNNAMED")
    }
}