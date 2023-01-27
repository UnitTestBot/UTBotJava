package org.utbot.instrumentation.instrumentation.execution.phases

import org.utbot.common.withAccessibility
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.util.jField


/**
 * The responsibility of this phase is resetting environment to the initial state.
 */
class PostprocessingPhase : ExecutionPhase {

    private var savedStaticsInstance: Map<FieldId, Any?>? = null

    var savedStatics: Map<FieldId, Any?>
        get() = savedStaticsInstance!!
        set(value) {
            savedStaticsInstance = value
        }

    override fun wrapError(e: Throwable): ExecutionPhaseException = ExecutionPhaseError(this.javaClass.simpleName, e)

    fun resetStaticFields() {
        savedStatics.forEach { (fieldId, value) ->
            fieldId.jField.run {
                withAccessibility {
                    set(null, value)
                }
            }
        }
    }

}