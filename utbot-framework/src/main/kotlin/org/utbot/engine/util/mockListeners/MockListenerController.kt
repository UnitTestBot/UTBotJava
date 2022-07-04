package org.utbot.engine.util.mockListeners

import org.utbot.engine.EngineController
import org.utbot.engine.MockStrategy

/**
 * Controller that allows to attach listeners to mocker in [org.utbot.engine.UtBotSymbolicEngine].
 */
class MockListenerController(private val controller: EngineController) {
    val listeners = mutableListOf<MockListener>()

    fun attach(listener: MockListener) {
        listeners += listener
    }

    fun onShouldMock(strategy: MockStrategy) {
        listeners.map { it.onShouldMock(controller, strategy) }
    }
}
