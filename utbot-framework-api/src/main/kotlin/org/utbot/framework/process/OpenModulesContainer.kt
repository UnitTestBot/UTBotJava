package org.utbot.framework.process

import org.utbot.framework.plugin.services.JdkInfoService

object OpenModulesContainer {
    private val modulesContainer: List<String>
    val javaVersionSpecificArguments: List<String>
        get() = modulesContainer
            .takeIf { JdkInfoService.provide().version > 8 } ?: emptyList()

    init {
        modulesContainer = buildList {
            openPackage("java.base", "sun.security.util")
            openPackage("java.base", "java.text")
            openPackage("java.base", "java.lang.invoke")
            openPackage("java.base", "jdk.internal.misc")
            openPackage("java.base", "sun.reflect.generics.repository")
            openPackage("java.base", "java.io")
            openPackage("java.base", "java.nio")
            openPackage("java.base", "java.nio.file")
            openPackage("java.base", "java.net")
            openPackage("java.base", "java.lang")
            openPackage("java.base", "java.security")
            openPackage("java.base", "java.util")
            openPackage("java.base", "java.util.stream")
            openPackage("java.base", "java.util.concurrent")
            openPackage("java.base", "java.util.concurrent.locks")
            openPackage("java.base", "java.math")
            openPackage("java.base", "java.lang.ref")
            openPackage("java.base", "java.lang.reflect")
            openPackage("java.base", "sun.security.provider")
            openPackage("java.base", "sun.net.util")
            openPackage("java.base", "sun.nio.fs")
            openPackage("java.base", "jdk.internal.event")
            openPackage("java.base", "jdk.internal.jimage")
            openPackage("java.base", "jdk.internal.jimage.decompressor")
            openPackage("java.base", "jdk.internal.jmod")
            openPackage("java.base", "jdk.internal.jtrfs")
            openPackage("java.base", "jdk.internal.loader")
            openPackage("java.base", "jdk.internal.logger")
            openPackage("java.base", "jdk.internal.math")
            openPackage("java.base", "jdk.internal.misc")
            openPackage("java.base", "jdk.internal.module")
            openPackage("java.base", "jdk.internal.org.objectweb.asm.commons")
            openPackage("java.base", "jdk.internal.org.objectweb.asm.signature")
            openPackage("java.base", "jdk.internal.org.objectweb.asm.tree")
            openPackage("java.base", "jdk.internal.org.objectweb.asm.tree.analysis")
            openPackage("java.base", "jdk.internal.org.objectweb.asm.util")
            openPackage("java.base", "jdk.internal.org.xml.sax")
            openPackage("java.base", "jdk.internal.org.xml.sax.helpers")
            openPackage("java.base", "jdk.internal.perf")
            openPackage("java.base", "jdk.internal.platform")
            openPackage("java.base", "jdk.internal.ref")
            openPackage("java.base", "jdk.internal.reflect")
            openPackage("java.base", "jdk.internal.util")
            openPackage("java.base", "jdk.internal.util.jar")
            openPackage("java.base", "jdk.internal.util.xml")
            openPackage("java.base", "jdk.internal.util.xml.impl")
            openPackage("java.base", "jdk.internal.vm")
            openPackage("java.base", "jdk.internal.vm.annotation")
            add("--illegal-access=warn")
        }
    }

    private fun MutableList<String>.openPackage(module: String, pakage: String) {
        add("--add-opens")
        add("$module/$pakage=ALL-UNNAMED")
    }
}