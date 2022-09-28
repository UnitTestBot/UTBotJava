package org.utbot.rd.models

import com.jetbrains.rd.generator.nova.*

object SettingsProtocolRoot: Root()

object SettingsModel : Ext(SettingsProtocolRoot) {
    val settingForArgument = structdef {
        field("key", PredefinedType.string)
        field("propertyName", PredefinedType.string)
    }
    val settingForResult = structdef {
        field("value", PredefinedType.string.nullable)
    }
    init {
        call("settingFor", settingForArgument, settingForResult).async
    }
}