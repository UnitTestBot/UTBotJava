package org.utbot.sarif

import kotlinx.coroutines.runBlocking
import org.utbot.framework.process.generated.RdSourceFindingStrategy
import org.utbot.framework.process.generated.SourceStrategyMethodArgs
import java.io.File

class RdSourceFindingStrategyFacade(
    private val testSetsId: Long,
    private val realStrategy: RdSourceFindingStrategy
) : SourceFindingStrategy() {
    override val testsRelativePath: String
        get() = runBlocking { realStrategy.testsRelativePath.startSuspending(testSetsId) }

    override fun getSourceRelativePath(classFqn: String, extension: String?): String = runBlocking {
        realStrategy.getSourceRelativePath.startSuspending(SourceStrategyMethodArgs(testSetsId, classFqn, extension))
    }

    override fun getSourceFile(classFqn: String, extension: String?): File? = runBlocking {
        realStrategy.getSourceFile.startSuspending(SourceStrategyMethodArgs(testSetsId, classFqn, extension))?.let {
            File(it)
        }
    }
}