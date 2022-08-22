package org.utbot.engine.util.mockListeners

import org.utbot.engine.EngineController
import org.utbot.engine.MockStrategy
import org.utbot.engine.UtMockInfo

/**
 * Controller that allows to attach listeners to mocker in [org.utbot.engine.UtBotSymbolicEngine].
 */
class MockListenerController(private val controller: EngineController) {
    val listeners = mutableListOf<MockListener>()

    fun attach(listener: MockListener) {
        listeners += listener
    }

    fun detach(listener: MockListener) {
        listeners -= listener
    }

    fun onShouldMock(strategy: MockStrategy, mockInfo: UtMockInfo) {
        listeners.map { it.onShouldMock(controller, strategy, mockInfo) }
    }
}
