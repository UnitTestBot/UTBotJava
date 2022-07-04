package org.utbot.engine.util.mockListeners

import org.utbot.engine.EngineController
import org.utbot.engine.MockStrategy

/**
 * Listener that can be attached using [MockListenerController] to mocker in [org.utbot.engine.UtBotSymbolicEngine].
 */
interface MockListener {
    fun onShouldMock(controller: EngineController, strategy: MockStrategy)
}
