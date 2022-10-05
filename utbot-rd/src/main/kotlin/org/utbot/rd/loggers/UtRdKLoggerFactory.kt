package org.utbot.rd.loggers

import com.jetbrains.rd.util.ILoggerFactory
import com.jetbrains.rd.util.Logger
import mu.KLogger

class UtRdKLoggerFactory(private val realLogger: KLogger) : ILoggerFactory {
    override fun getLogger(category: String): Logger {
        return UtRdKLogger(realLogger, category)
    }
}