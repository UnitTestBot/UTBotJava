package org.utbot.contest.usvm.log

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Appender
import org.apache.logging.log4j.core.Core
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginFactory
import org.apache.logging.log4j.core.layout.PatternLayout
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import org.apache.logging.log4j.core.config.plugins.PluginAttribute
import org.apache.logging.log4j.core.impl.ThrowableProxy
import org.apache.logging.log4j.message.Message
import org.apache.logging.log4j.message.SimpleMessage
import java.util.concurrent.ConcurrentHashMap

@Plugin(
    name = "ErrorCountingAppender",
    category = Core.CATEGORY_NAME,
    elementType = Appender.ELEMENT_TYPE,
    printObject = true
)
class ErrorCountingLoggerAppender(name: String, private val delegateAppenderName: String) : AbstractAppender(
    name,
    null,
    PatternLayout.createDefaultLayout(),
    false,
    Property.EMPTY_ARRAY
) {

    private val delegate: Appender by lazy {
        (LogManager.getContext(false) as LoggerContext)
            .configuration
            .getAppender(delegateAppenderName)
    }

    companion object {
        private val occurrenceCountsToLogAt =
            generateSequence(1) { it * 10 }.take(10).toList() + listOf(2, 5, 50, 500)
        private val errorOccurrenceCounts: MutableMap<Any, Int> = ConcurrentHashMap<Any, Int>()

        @PluginFactory
        @JvmStatic
        fun createAppender(
            @PluginAttribute("name") name: String,
            @PluginAttribute("delegateAppender") delegateAppenderName: String
        ) = ErrorCountingLoggerAppender(name, delegateAppenderName)

        fun resetOccurrenceCounter() {
            errorOccurrenceCounts.keys.toList().forEach { errorOccurrenceCounts[it] = 0 }
        }
    }

    override fun append(event: LogEvent) {
        if (event.level >= Level.INFO) {
            delegate.append(event)
            return
        }

        val key = event.thrown?.stackTrace?.firstOrNull() ?: event.message.formattedMessage
        val alreadyLogged = key in errorOccurrenceCounts
        val count = (errorOccurrenceCounts[key] ?: 0) + 1
        errorOccurrenceCounts[key] = count

        if (!alreadyLogged) {
            delegate.append(event)
        } else if (count in occurrenceCountsToLogAt) {
            val modifiedMessage = "(x$count) ${event.message.formattedMessage}${
                event.thrown?.let { e ->
                    "\n${e.javaClass.name}: ${e.message}\n\tat ${e.stackTrace.firstOrNull()}" 
                }.orEmpty()
            }"
            delegate.append(object : LogEvent by event {
                override fun getMessage(): Message = SimpleMessage(modifiedMessage)
                override fun getThrown(): Throwable? = null
                override fun getThrownProxy(): ThrowableProxy? = null
            })
        }
    }
}
