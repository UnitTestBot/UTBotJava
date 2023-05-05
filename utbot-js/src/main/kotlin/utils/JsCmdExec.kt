package utils

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
    ): Pair<String, String> {
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
        return Pair(
            process.inputStream.bufferedReader().use { it.readText() },
            process.errorStream.bufferedReader().use { it.readText() }
        )
    }
}
