package org.utbot.instrumentation.instrumentation.execution.phases

import org.utbot.common.withAccessibility
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.framework.plugin.api.util.jField
import org.utbot.instrumentation.instrumentation.et.TraceHandler


/**
 * The responsibility of this phase is environment preparation before execution.
 */
class PreparationPhase(
    private val traceHandler: TraceHandler
) : ExecutionPhase {

    override fun wrapError(e: Throwable): ExecutionPhaseException =
        ExecutionPhaseError(this.javaClass.simpleName, e)

    fun setStaticFields(staticFieldsValues: Map<FieldId, UtConcreteValue<*>>): Map<FieldId, Any?> {
        val savedStaticFields = mutableMapOf<FieldId, Any?>()
        staticFieldsValues.forEach { (fieldId, value) ->
            fieldId.jField.run {
                withAccessibility {
                    savedStaticFields[fieldId] = get(null)
                    set(null, value.value)
                }
            }
        }
        return savedStaticFields
    }

    fun resetTrace() {
        traceHandler.resetTrace()
    }

}