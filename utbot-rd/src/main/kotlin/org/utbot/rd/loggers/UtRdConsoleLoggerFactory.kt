package org.utbot.rd.loggers

import com.jetbrains.rd.util.ILoggerFactory
import com.jetbrains.rd.util.LogLevel
import com.jetbrains.rd.util.Logger
import java.io.PrintStream

/**
 * Creates loggers with predefined log level, that writes to provided stream.
 * Create logger category is added to message.
 */
class UtRdConsoleLoggerFactory(
    private val loggersLevel: LogLevel,
    private val streamToWrite: PrintStream
) : ILoggerFactory {
    override fun getLogger(category: String): Logger {
        return UtRdConsoleLogger(loggersLevel, streamToWrite, category)
    }
}