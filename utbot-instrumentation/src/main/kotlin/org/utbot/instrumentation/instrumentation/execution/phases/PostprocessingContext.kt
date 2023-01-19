package org.utbot.instrumentation.instrumentation.execution.phases

import org.utbot.common.withAccessibility
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.util.jField

class PostprocessingPhaseError(cause: Throwable) : PhaseError(
    message = "Error during postprocessing phase",
    cause
)

/**
 * The responsibility of this phase is resetting environment to the initial state.
 */
class PostprocessingContext : PhaseContext<PostprocessingPhaseError> {

    override fun wrapError(error: Throwable): PostprocessingPhaseError =
        PostprocessingPhaseError(error)

    fun resetStaticFields(staticFields: Map<FieldId, Any?>) {
        staticFields.forEach { (fieldId, value) ->
            fieldId.jField.run {
                withAccessibility {
                    set(null, value)
                }
            }
        }
    }

}