package org.utbot.framework.concrete.phases

import org.utbot.common.withAccessibility
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.util.jField

class PostprocessingPhaseError(cause: Throwable) : PhaseError(
    message = "Error during postprocessing phase",
    cause
)

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