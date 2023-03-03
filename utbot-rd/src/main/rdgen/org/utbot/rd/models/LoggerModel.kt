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
        signal("log", logArguments).async
        call(
            "getCategoryMinimalLogLevel",
            PredefinedType.string,
            PredefinedType.int
        ).async.doc("Parameter - log category.\nResult - integer value for com.jetbrains.rd.util.LogLevel.")
    }
}