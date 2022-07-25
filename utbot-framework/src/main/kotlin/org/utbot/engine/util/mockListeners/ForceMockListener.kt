package org.utbot.engine.util.mockListeners
import org.utbot.engine.EngineController
import org.utbot.engine.MockStrategy
import org.utbot.engine.UtMockInfo
import org.utbot.framework.plugin.api.TestCaseGenerator
import org.utbot.framework.util.Conflict
import org.utbot.framework.util.ConflictTriggers

/**
 * Listener for mocker events in [org.utbot.engine.UtBotSymbolicEngine].
 * If forced mock happened, cancels the engine job.
 *
 * Supposed to be created only if Mockito is not installed.
 */
class ForceMockListener(triggers: ConflictTriggers): MockListener(triggers) {
    override fun onShouldMock(controller: EngineController, strategy: MockStrategy, mockInfo: UtMockInfo) {
        // If force mocking happened -- сancel engine job
        controller.job?.cancel(ForceMockCancellationException())

        triggers[Conflict.ForceMockHappened] = true
    }

    companion object {
        fun create(testCaseGenerator: TestCaseGenerator, conflictTriggers: ConflictTriggers) : ForceMockListener {
            val listener = ForceMockListener(conflictTriggers)
            testCaseGenerator.engineActions.add { engine -> engine.attachMockListener(listener) }

            return listener
        }
    }
}
