package org.utbot.framework.plugin.api

import org.utbot.engine.UtBotSymbolicEngine

class EngineActionsController {
    private val actions: MutableList<(UtBotSymbolicEngine) -> Unit> = mutableListOf()

    fun add(action: (UtBotSymbolicEngine) -> Unit) {
        actions.add(action)
    }

    fun apply(symbolicEngine: UtBotSymbolicEngine) {
        actions.forEach { symbolicEngine.apply(it) }
        actions.clear()
    }
}