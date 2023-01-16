package fuzzer.new

import framework.api.js.JsClassId
import org.utbot.fuzzer.FuzzedContext
import org.utbot.fuzzer.FuzzedValue
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

class JsFeedback(
    override val control: Control = Control.CONTINUE,
    val values: List<FuzzedValue>
) : Feedback<JsClassId, FuzzedValue> {

    override fun equals(other: Any?): Boolean {
        val castOther = other as? JsFeedback
        return control == castOther?.control
    }

    override fun hashCode(): Int {
        return control.hashCode()
    }
}