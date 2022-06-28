package org.utbot.engine.util.mockListeners
import org.utbot.engine.EngineController
import org.utbot.engine.MockStrategy
import org.utbot.engine.util.mockListeners.exceptions.ForceMockCancellationException

/**
 * Listener for mocker events in [org.utbot.engine.UtBotSymbolicEngine].
 * If forced mock happened, cancels the engine job.
 *
 * Supposed to be created only if Mockito is not installed.
 */
class ForceMockListener: MockListener {
    var forceMockHappened = false
        private set

    override fun onShouldMock(controller: EngineController, strategy: MockStrategy) {
        // If force mocking happened -- сancel engine job
        controller.job?.cancel(ForceMockCancellationException())
        forceMockHappened = true
    }
}
