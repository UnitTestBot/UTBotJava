package org.utbot.rd.loggers

import com.jetbrains.rd.util.ILoggerFactory
import com.jetbrains.rd.util.LogLevel
import com.jetbrains.rd.util.Logger
import java.io.PrintStream

class UtRdConsoleLoggerFactory(
    private val loggersLevel: LogLevel,
    private val streamToWrite: PrintStream
) : ILoggerFactory {
    override fun getLogger(category: String): Logger {
        return UtRdConsoleLogger(loggersLevel, streamToWrite, category)
    }
}