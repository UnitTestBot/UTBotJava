package org.utbot.framework.plugin.api

class SymbolicEngineTestGeneratorService : TestGeneratorService {
    override val displayName: String = "Symbolic engine"
    override val serviceProvider: TestCaseGenerator = UtBotTestCaseGenerator
}
