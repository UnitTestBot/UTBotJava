package fuzzer.new

import framework.api.js.JsClassId
import framework.api.js.JsUtModel
import org.utbot.fuzzer.FuzzedContext
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.Description
import org.utbot.fuzzing.Feedback

data class JsFuzzedConcreteValue(
    val type: JsClassId,
    val value: Any,
    val fuzzedContext: FuzzedContext = FuzzedContext.Unknown,
)

class JsMethodDescription(
    val name: String,
    parameters: List<JsClassId>,
    val concreteValues: Collection<JsFuzzedConcreteValue>
) : Description<JsClassId>(parameters)


// TODO: Currently summary is unused (always null), further explore its usage.
data class JsFuzzedValue(
    val model: JsUtModel,
    var summary: String? = null,
)

class JsFeedback(
    override val control: Control = Control.CONTINUE,
    val values: List<JsFuzzedValue>
) : Feedback<JsClassId, Any> {

    override fun equals(other: Any?): Boolean {
        val castOther = other as? JsFeedback
        return control == castOther?.control
    }

    override fun hashCode(): Int {
        return control.hashCode()
    }
}