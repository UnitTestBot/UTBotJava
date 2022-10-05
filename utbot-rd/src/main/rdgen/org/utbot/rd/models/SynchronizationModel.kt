package org.utbot.rd.models

import com.jetbrains.rd.generator.nova.*

object SynchronizationModelRoot: Root()

object SynchronizationModel: Ext(SynchronizationModelRoot) {
    init {
        signal("synchronizationSignal", PredefinedType.string).async
    }
}