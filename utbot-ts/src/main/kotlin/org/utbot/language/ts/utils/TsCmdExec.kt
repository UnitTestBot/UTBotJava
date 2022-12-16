package org.utbot.language.ts.utils

import java.io.BufferedReader
import java.io.File
import java.util.concurrent.TimeUnit
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.language.ts.settings.TsTestGenerationSettings.defaultTimeout

object TsCmdExec {

    fun runCommand(
        dir: String? = null,
        shouldWait: Boolean = false,
        timeout: Long = defaultTimeout,
        vararg cmd: String,
    ): Pair<BufferedReader, BufferedReader> {
        val builder = ProcessBuilder(*TsOsProvider.getProviderByOs().getCmdPrefix(), *cmd)
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
        return process.inputStream.bufferedReader() to process.errorStream.bufferedReader()
    }
}
