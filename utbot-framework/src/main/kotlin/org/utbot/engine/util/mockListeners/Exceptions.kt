package org.utbot.engine.util.mockListeners

import kotlinx.coroutines.CancellationException

/**
 * Exception used in [org.utbot.engine.util.mockListeners.ForceMockListener].
 */
class ForceMockCancellationException: CancellationException("Forced mocks without Mockito")

/**
 * Exception used in [org.utbot.engine.util.mockListeners.ForceStaticMockListener].
 */
class ForceStaticMockCancellationException: CancellationException("Forced static mocks without Mockito-inline")