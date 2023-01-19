package utils

import org.utbot.framework.plugin.api.TimeoutException
import settings.JsTestGenerationSettings.defaultTimeout
import java.io.BufferedReader
import java.io.File
import java.util.concurrent.TimeUnit

object JsCmdExec {

    fun runCommand(
        dir: String? = null,
        shouldWait: Boolean = false,
        timeout: Long = defaultTimeout,
        vararg cmd: String,
    ): Pair<BufferedReader, BufferedReader> {
        val builder = ProcessBuilder(*cmd)
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
        return Pair(process.inputStream.bufferedReader(), process.errorStream.bufferedReader())
    }
}
