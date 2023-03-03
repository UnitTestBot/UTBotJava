package org.utbot.rd.loggers

import com.jetbrains.rd.util.*
import org.utbot.common.timeFormatter
import java.io.PrintStream
import java.time.LocalDateTime

class UtRdConsoleLogger(
    private val loggersLevel: LogLevel,
    private val streamToWrite: PrintStream,
    private val category: String
) : Logger {
    override fun isEnabled(level: LogLevel): Boolean {
        return level >= loggersLevel
    }

    private fun format(category: String, level: LogLevel, message: Any?, throwable: Throwable?) : String {
        val throwableToPrint = if (level < LogLevel.Error) throwable  else throwable ?: Exception() //to print stacktrace
        val rdCategory = if (category.isNotEmpty()) "RdCategory: ${category.substringAfterLast('.').padEnd(25)} | " else ""
        return "${LocalDateTime.now().format(timeFormatter)} | ${level.toString().uppercase().padEnd(5)} | $rdCategory${message?.toString()?:""} ${throwableToPrint?.getThrowableText()?.let { "| $it" }?:""}"
    }

    override fun log(level: LogLevel, message: Any?, throwable: Throwable?) {
        if (!isEnabled(level))
            return

        val msg = format(category, level, message, throwable)

        streamToWrite.println(msg)
    }
}