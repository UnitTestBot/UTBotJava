package org.utbot.python.newtyping.inference

import org.utbot.python.newtyping.ast.visitor.hints.HintCollectorResult
import org.utbot.python.newtyping.general.UtType

abstract class TypeInferenceAlgorithm {
    abstract suspend fun run(
        hintCollectorResult: HintCollectorResult,
        isCancelled: () -> Boolean,
        annotationHandler: suspend (UtType) -> InferredTypeFeedback,
    ): Int
}

sealed interface InferredTypeFeedback

object SuccessFeedback : InferredTypeFeedback
object InvalidTypeFeedback : InferredTypeFeedback