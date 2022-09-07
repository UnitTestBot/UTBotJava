package org.utbot.framework

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.appender.AppenderLoggingException
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class JUnitSetup : BeforeAllCallback, AfterAllCallback {

    private var appender: ThrowingAppender? = null
    private val rootLogger = LogManager.getRootLogger() as Logger

    override fun beforeAll(context: ExtensionContext?) {
        appender = ThrowingAppender().apply { start() }
        rootLogger.addAppender(appender)
    }

    override fun afterAll(context: ExtensionContext?) {
        appender?.let {
            it.stop()
            rootLogger.removeAppender(it)
            appender = null
        }
    }

}

class ThrowingAppender : AbstractAppender(ThrowingAppender::class.simpleName, null, null, false, null) {
    override fun append(event: LogEvent) {
        if (event.level.isMoreSpecificThan(Level.ERROR))
            throw event.thrown ?: AppenderLoggingException(event.message.formattedMessage)
    }
}
