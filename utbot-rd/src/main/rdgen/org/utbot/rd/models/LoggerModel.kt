@file:Suppress("unused")

package org.utbot.rd.models

import com.jetbrains.rd.generator.nova.*

object LoggerRoot : Root()
object LoggerModel : Ext(LoggerRoot) {
    val logArguments = structdef {
        field("category", PredefinedType.string)
        field("logLevelOrdinal", PredefinedType.int).doc("Integer value for com.jetbrains.rd.util.LogLevel")
        field("message", PredefinedType.string)
    }

    init {
        signal("initRemoteLogging", PredefinedType.void).async
        signal("log", logArguments).async
        property(
            "getCategoryMinimalLogLevel",
            PredefinedType.int
        ).async.doc("Property value - integer for com.jetbrains.rd.util.LogLevel.")
    }
}