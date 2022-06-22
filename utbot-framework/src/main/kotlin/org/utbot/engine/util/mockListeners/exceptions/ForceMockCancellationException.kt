package org.utbot.engine.util.mockListeners.exceptions

import kotlinx.coroutines.CancellationException

/**
 * Exception used in [org.utbot.engine.util.mockListeners.ForceMockListener].
 */
class ForceMockCancellationException: CancellationException("Forced mocks without Mockito")
