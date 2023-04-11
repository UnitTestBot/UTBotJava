package org.utbot.rd.loggers

import com.jetbrains.rd.util.LogLevel
import com.jetbrains.rd.util.Logger
import com.jetbrains.rd.util.collections.CountingSet
import com.jetbrains.rd.util.getThrowableText
import com.jetbrains.rd.util.reactive.valueOrThrow
import com.jetbrains.rd.util.threadLocalWithInitial
import org.utbot.rd.generated.LogArguments
import org.utbot.rd.generated.LoggerModel
import org.utbot.rd.startBlocking

class UtRdRemoteLogger(
    private val loggerModel: LoggerModel,
    private val category: String
) : Logger {
    private val logLevel: LogLevel by lazy { logLevelValues[loggerModel.getCategoryMinimalLogLevel.valueOrThrow] }

    companion object {
        val logLevelValues = LogLevel.values()
        private val threadLocalExecutingBackingFiled: ThreadLocal<CountingSet<UtRdRemoteLogger>> =
            threadLocalWithInitial { CountingSet() }

        internal val threadLocalExecuting get() = threadLocalExecutingBackingFiled.get()
    }

    override fun isEnabled(level: LogLevel): Boolean {
        // On every protocol sends/receives event RD to its own loggers.
        // They will be redirected here, and then sent via RD to another process,
        // producing new log event again thus causing infinite recursion.
        // The solution is to prohibit writing any logs from inside logger.
        // This is implemented via thread local counter per logger,
        // which will be incremented when this logger fires event to another process,
        // and deny all following log events until previous log event is delivered.
        if (threadLocalExecuting[this] > 0)
            return false

        return level >= logLevel
    }

    private fun format(message: Any?, throwable: Throwable?): String {
        val rdCategory = if (category.isNotEmpty()) "RdCategory: ${category.substringAfterLast('.').padEnd(25)} | " else ""
        return "$rdCategory${message?.toString() ?: ""}${
            throwable?.getThrowableText()?.let { "${message?.let { " | " } ?: ""}$it" } ?: ""
        }"
    }

    override fun log(level: LogLevel, message: Any?, throwable: Throwable?) {
        if (!isEnabled(level) || message == null && throwable == null)
            return

        threadLocalExecuting.add(this, +1)
        try {
            val renderedMsg = format(message, throwable)
            val args = LogArguments(category, level.ordinal, renderedMsg)

            loggerModel.log.fire(args)
        } finally {
            threadLocalExecuting.add(this, -1)
        }
    }

}