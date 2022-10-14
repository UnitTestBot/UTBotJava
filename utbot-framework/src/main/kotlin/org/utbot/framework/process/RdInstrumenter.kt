package org.utbot.framework.process

import com.jetbrains.rd.framework.IProtocol
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.utbot.framework.process.generated.ComputeSourceFileByClassArguments
import org.utbot.framework.process.generated.rdInstrumenterAdapter
import org.utbot.instrumentation.instrumentation.instrumenter.InstrumenterAdapter
import java.io.File
import java.nio.file.Path

private val logger = KotlinLogging.logger { }

class RdInstrumenter(private val protocol: IProtocol): InstrumenterAdapter() {
    override fun computeSourceFileByClass(
        className: String,
        packageName: String?,
        directoryToSearchRecursively: Path
    ): File? = runBlocking {
        logger.debug { "starting computeSourceFileByClass with classname - $className" }
        val result = try {
             protocol.rdInstrumenterAdapter.computeSourceFileByClass.startSuspending(
                ComputeSourceFileByClassArguments(
                    className,
                    packageName
                )
            )
        }
        catch(e: Exception) {
            logger.error(e) { "error during computeSourceFileByClass" }
            throw e
        }
        logger.debug { "computeSourceFileByClass result for $className from idea: $result"}
        return@runBlocking result?.let { File(it) }
    }
}