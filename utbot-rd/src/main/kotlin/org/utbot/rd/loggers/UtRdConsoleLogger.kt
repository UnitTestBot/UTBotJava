package org.utbot.rd.loggers

import com.jetbrains.rd.util.LogLevel
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.defaultLogFormat
import org.utbot.common.dateFormatter
import java.io.PrintStream
import java.time.LocalDateTime

class UtRdConsoleLogger(
    private val loggersLevel: LogLevel,
    private val streamToWrite: PrintStream,
    private val category: String = ""
) : Logger {
    override fun isEnabled(level: LogLevel): Boolean {
        return level >= loggersLevel
    }

    override fun log(level: LogLevel, message: Any?, throwable: Throwable?) {
        if (!isEnabled(level))
            return

        val msg = LocalDateTime.now().format(dateFormatter) + " | ${
            defaultLogFormat(
                category,
                level,
                message,
                throwable
            )
        }"
        streamToWrite.println(msg)
    }
}