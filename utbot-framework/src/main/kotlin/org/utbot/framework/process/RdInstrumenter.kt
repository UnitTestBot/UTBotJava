package org.utbot.framework.process

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.utbot.common.logException
import org.utbot.framework.process.generated.ComputeSourceFileByClassArguments
import org.utbot.framework.process.generated.RdInstrumenterAdapter
import org.utbot.instrumentation.instrumentation.instrumenter.InstrumenterAdapter
import org.utbot.rd.startBlocking
import java.io.File
import java.nio.file.Path

private val logger = KotlinLogging.logger { }

class RdInstrumenter(private val rdInstrumenterAdapter: RdInstrumenterAdapter) : InstrumenterAdapter() {
    override fun computeSourceFileByClass(
        clazz: Class<*>,
        directoryToSearchRecursively: Path
    ): File? {
        val canonicalClassName = clazz.canonicalName
        logger.debug { "starting computeSourceFileByClass for class - $canonicalClassName" }
        val result = logger.logException {
            val arguments = ComputeSourceFileByClassArguments(canonicalClassName)

            rdInstrumenterAdapter.computeSourceFileByClass.startBlocking(arguments)
        }
        logger.debug { "computeSourceFileByClass result for $canonicalClassName from idea: $result" }
        return result?.let { File(it) }
    }
}