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
        className: String,
        packageName: String?,
        directoryToSearchRecursively: Path
    ): File? {
        logger.debug { "starting computeSourceFileByClass with classname - $className" }
        val result = logger.logException {
            val arguments = ComputeSourceFileByClassArguments(className, packageName)

            rdInstrumenterAdapter.computeSourceFileByClass.startBlocking(arguments)
        }
        logger.debug { "computeSourceFileByClass result for $className from idea: $result" }
        return result?.let { File(it) }
    }
}