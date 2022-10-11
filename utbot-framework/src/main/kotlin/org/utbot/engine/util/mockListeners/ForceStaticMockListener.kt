package org.utbot.engine.util.mockListeners

import org.utbot.engine.EngineController
import org.utbot.engine.MockStrategy
import org.utbot.engine.UtMockInfo
import org.utbot.engine.UtNewInstanceMockInfo
import org.utbot.engine.UtStaticMethodMockInfo
import org.utbot.engine.UtStaticObjectMockInfo
import org.utbot.framework.plugin.api.TestCaseGenerator
import org.utbot.framework.util.Conflict
import org.utbot.framework.util.ConflictTriggers

/**
 * Listener for mocker events in [org.utbot.engine.UtBotSymbolicEngine].
 * If forced static mock happened, cancels the engine job.
 *
 * Supposed to be created only if Mockito inline is not installed.
 */
class ForceStaticMockListener private constructor(triggers: ConflictTriggers, private val cancelJob: Boolean): MockListener(triggers) {
    override fun onShouldMock(controller: EngineController, strategy: MockStrategy, mockInfo: UtMockInfo) {
        if (mockInfo is UtNewInstanceMockInfo
            || mockInfo is UtStaticMethodMockInfo
            || mockInfo is UtStaticObjectMockInfo) {
            // If force static mocking happened -- сancel engine job
            if (cancelJob) controller.job?.cancel(ForceStaticMockCancellationException())

            triggers[Conflict.ForceStaticMockHappened] = true
        }
    }

    companion object {
        fun create(testCaseGenerator: TestCaseGenerator, conflictTriggers: ConflictTriggers, cancelJob: Boolean = true) : ForceStaticMockListener {
            val listener = ForceStaticMockListener(conflictTriggers, cancelJob)
            testCaseGenerator.engineActions.add { engine -> engine.attachMockListener(listener) }

            return listener
        }
    }
}