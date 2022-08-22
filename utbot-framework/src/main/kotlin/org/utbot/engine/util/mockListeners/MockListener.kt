package org.utbot.engine.util.mockListeners

import org.utbot.engine.EngineController
import org.utbot.engine.MockStrategy
import org.utbot.engine.UtMockInfo
import org.utbot.framework.plugin.api.TestCaseGenerator
import org.utbot.framework.util.ConflictTriggers

/**
 * Listener that can be attached using [MockListenerController] to mocker in [org.utbot.engine.UtBotSymbolicEngine].
 */
abstract class MockListener(
    val triggers: ConflictTriggers
) {
    abstract fun onShouldMock(controller: EngineController, strategy: MockStrategy, mockInfo: UtMockInfo)

    fun detach(testCaseGenerator: TestCaseGenerator, listener: MockListener) {
        testCaseGenerator.engineActions.add { engine -> engine.detachMockListener(listener) }
    }
}
