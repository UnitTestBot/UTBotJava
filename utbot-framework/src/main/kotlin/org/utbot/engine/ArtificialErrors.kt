package org.utbot.engine

/**
 * Represents an error that may be detected or not
 * during analysis in accordance with custom settings.
 *
 * Usually execution may be continued somehow after such error,
 * but the result may be different from basic expectations.
 */
sealed class ArtificialError(message: String): Error(message)

/**
 * Represents overflow detection errors in symbolic engine,
 * if a mode to detect them is turned on.
 *
 * See [TraversalContext.intOverflowCheck] for more details.
 */
class OverflowDetectionError(message: String): ArtificialError(message)