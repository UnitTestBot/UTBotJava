package utils

import org.utbot.framework.plugin.api.TimeoutException
import settings.JsTestGenerationSettings.defaultTimeout
import java.io.BufferedReader
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

object JsCmdExec {

    private interface IOsPrefix {
        fun getCmdPrefix(): Array<String>
    }

    private class WindowsPrefix: IOsPrefix {
        override fun getCmdPrefix() = arrayOf("cmd.exe", "/c")
    }

    private class LinuxPrefix: IOsPrefix {
        override fun getCmdPrefix() = emptyArray<String>()
    }

    private fun getPrefixByOs(): Array<String> {
        val osData = System.getProperty("os.name").lowercase(Locale.getDefault())
        return when {
            osData.contains("windows") -> WindowsPrefix().getCmdPrefix()
            else -> LinuxPrefix().getCmdPrefix()
        }
    }

    fun runCommand(
        dir: String? = null,
        shouldWait: Boolean = false,
        timeout: Long = defaultTimeout,
        vararg cmd: String,
    ): Pair<BufferedReader, BufferedReader> {
        val builder = ProcessBuilder(*getPrefixByOs(), *cmd)
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
