package org.utbot.framework.plugin.api

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

/**
 * An artificial error that could be implicitly thrown by the symbolic engine during taint sink processing.
 */
class TaintAnalysisError(
    sinkFqn: String,
    taintedVar: String,
    taintMark: String,
    message: String = "'$taintedVar' marked '$taintMark' was passed into '$sinkFqn' method"
) : ArtificialError(message)
