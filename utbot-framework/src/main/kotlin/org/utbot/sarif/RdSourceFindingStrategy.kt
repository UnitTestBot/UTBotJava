package org.utbot.sarif

import kotlinx.coroutines.runBlocking
import org.utbot.engine.logger
import org.utbot.framework.process.generated.RdSourceFindingStrategy
import org.utbot.framework.process.generated.SourceStrategeMethodArgs
import java.io.File

class RdSourceFindingStrategyFacade(private val realStrategy: RdSourceFindingStrategy): SourceFindingStrategy() {
    override val testsRelativePath: String
        get() = runBlocking { realStrategy.testsRelativePath.startSuspending(Unit) }

    override fun getSourceRelativePath(classFqn: String, extension: String?): String = runBlocking {
        logger.error("getSourceRelativePath for $classFqn, $extension")
        realStrategy.getSourceRelativePath.startSuspending(SourceStrategeMethodArgs(classFqn, extension))
    }

    override fun getSourceFile(classFqn: String, extension: String?): File? = runBlocking {
        logger.error("getSourceFile for $classFqn, $extension")
        realStrategy.getSourceFile.startSuspending(SourceStrategeMethodArgs(classFqn, extension))?.let {
            logger.error("it: $it")
            File(it)
        }
    }
}