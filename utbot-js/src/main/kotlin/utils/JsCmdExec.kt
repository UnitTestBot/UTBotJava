package utils

import java.io.BufferedReader
import java.io.File
import java.util.concurrent.TimeUnit
import org.utbot.framework.plugin.api.TimeoutException
import settings.JsTestGenerationSettings.defaultTimeout

object JsCmdExec {

    fun runCommand(
        dir: String? = null,
        shouldWait: Boolean = false,
        timeout: Long = defaultTimeout,
        vararg cmd: String,
    ): Triple<BufferedReader, BufferedReader, Int> {
        val builder = ProcessBuilder(*OsProvider.getProviderByOs().getCmdPrefix(), *cmd)
        dir?.let {
            builder.directory(File(it))
        }
        val process = builder.start()
        if (shouldWait) {
            if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                process.descendants().forEach {
                    it.destroy()
                }
                process.destroy()
                throw TimeoutException("")
            }
        }
        return Triple(process.inputStream.bufferedReader(), process.errorStream.bufferedReader(), process.exitValue())
    }
}
