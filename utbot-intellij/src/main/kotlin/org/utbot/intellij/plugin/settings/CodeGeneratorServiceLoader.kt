package org.utbot.intellij.plugin.settings

import org.utbot.framework.codegen.CodeGeneratorService
import org.utbot.framework.codegen.TestCodeGenerator
import org.utbot.framework.plugin.api.UtService
import java.util.ServiceLoader

object CodeGeneratorServiceLoader : UtServiceLoader<TestCodeGenerator>() {
    override val services: List<UtService<TestCodeGenerator>>
    override val defaultService: UtService<TestCodeGenerator>

    init {
        services = withLocalClassLoader {
            ServiceLoader.load(CodeGeneratorService::class.java).toList()
        }
        services.forEach {
            serviceByName[it.displayName] = it
        }
        // TODO: process case when no generator is available
        defaultService = services.first()
    }
}