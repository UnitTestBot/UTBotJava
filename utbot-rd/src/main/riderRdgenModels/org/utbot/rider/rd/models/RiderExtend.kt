@file:Suppress("unused")

package org.utbot.rider.rd.models

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rider.model.nova.ide.SolutionModel

object UtBotRiderModel : Ext(SolutionModel.Solution) {
    val startPublishArgs = structdef {
        field("fileName", PredefinedType.string)
        field("arguments", PredefinedType.string)
        field("workingDirectory", PredefinedType.string)
    }

    init {
        signal("startPublish", startPublishArgs).async
        signal("logPublishOutput", PredefinedType.string).async
        signal("logPublishError", PredefinedType.string).async
        signal("stopPublish", PredefinedType.int).async

        signal("startVSharp", PredefinedType.void).async
        signal("logVSharp", PredefinedType.string).async
        signal("stopVSharp", PredefinedType.int).async
    }
}