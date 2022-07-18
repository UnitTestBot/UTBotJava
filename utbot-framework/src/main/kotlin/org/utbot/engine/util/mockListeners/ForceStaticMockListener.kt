package org.utbot.engine.util.mockListeners

import org.utbot.engine.EngineController
import org.utbot.engine.MockStrategy
import org.utbot.engine.UtMockInfo
import org.utbot.engine.UtNewInstanceMockInfo
import org.utbot.engine.UtStaticMethodMockInfo
import org.utbot.engine.UtStaticObjectMockInfo

/**
 * Listener for mocker events in [org.utbot.engine.UtBotSymbolicEngine].
 * If forced static mock happened, cancels the engine job.
 *
 * Supposed to be created only if Mockito inline is not installed.
 */
class ForceStaticMockListener: MockListener {
    var forceStaticMockHappened = false
        private set

    override fun onShouldMock(controller: EngineController, strategy: MockStrategy, mockInfo: UtMockInfo) {
        if (mockInfo is UtNewInstanceMockInfo
            || mockInfo is UtStaticMethodMockInfo
            || mockInfo is UtStaticObjectMockInfo) {
            // If force static mocking happened -- сancel engine job
            controller.job?.cancel(ForceStaticMockCancellationException())
            forceStaticMockHappened = true
        }
    }
}