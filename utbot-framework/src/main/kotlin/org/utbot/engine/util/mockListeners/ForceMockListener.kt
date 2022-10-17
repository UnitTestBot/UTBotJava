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
class ForceMockListener private constructor(triggers: ConflictTriggers, private val cancelJob: Boolean): MockListener(triggers) {
    override fun onShouldMock(controller: EngineController, strategy: MockStrategy, mockInfo: UtMockInfo) {
        // If force mocking happened -- сancel engine job
        if (cancelJob) controller.job?.cancel(ForceMockCancellationException())

        triggers[Conflict.ForceMockHappened] = true
    }

    companion object {
        fun create(testCaseGenerator: TestCaseGenerator, conflictTriggers: ConflictTriggers, cancelJob: Boolean = true) : ForceMockListener {
            val listener = ForceMockListener(conflictTriggers, cancelJob)
            testCaseGenerator.engineActions.add { engine -> engine.attachMockListener(listener) }

            return listener
        }
    }
}
