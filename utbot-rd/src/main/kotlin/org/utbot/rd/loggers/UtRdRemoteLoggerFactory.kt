package org.utbot.rd.loggers

import com.jetbrains.rd.util.ILoggerFactory
import com.jetbrains.rd.util.Logger
import org.utbot.rd.generated.LoggerModel

/**
 * Creates loggers that are mapped to the remote counter-part.
 * Category is added to message
*/
class UtRdRemoteLoggerFactory(
    private val loggerModel: LoggerModel
) : ILoggerFactory {
    override fun getLogger(category: String): Logger {
        return UtRdRemoteLogger(loggerModel, category)
    }
}