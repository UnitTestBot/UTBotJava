package org.utbot.intellij.plugin.settings

import org.utbot.framework.plugin.api.TestCaseGenerator
import org.utbot.framework.plugin.api.TestGeneratorService
import org.utbot.framework.plugin.api.UtService
import java.util.ServiceLoader

object TestGeneratorServiceLoader : UtServiceLoader<TestCaseGenerator>() {
    override val services: List<UtService<TestCaseGenerator>>
    override val defaultService: UtService<TestCaseGenerator>

    init {
        services = withLocalClassLoader {
            ServiceLoader.load(TestGeneratorService::class.java).toList()
        }
        services.forEach {
            serviceByName[it.displayName] = it
        }
        // TODO: process case when no generator is available
        defaultService = services.first()
    }
}