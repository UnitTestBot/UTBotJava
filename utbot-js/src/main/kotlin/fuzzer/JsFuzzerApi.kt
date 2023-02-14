package fuzzer

import framework.api.js.JsClassId
import framework.api.js.util.isClass
import org.utbot.framework.plugin.api.UtTimeoutException
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.UtFuzzedExecution
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.Description
import org.utbot.fuzzing.Feedback
import org.utbot.fuzzing.utils.Trie

sealed interface JsFuzzingExecutionFeedback
class JsValidExecution(val utFuzzedExecution: UtFuzzedExecution) : JsFuzzingExecutionFeedback

class JsTimeoutExecution(val utTimeout: UtTimeoutException) : JsFuzzingExecutionFeedback

class JsMethodDescription(
    val name: String,
    parameters: List<JsClassId>,
    val concreteValues: Collection<FuzzedConcreteValue>,
    val thisInstance: JsClassId? = null,
    val tracer: Trie<JsStatement, *>
) : Description<JsClassId>(parameters) {

    constructor(
        name: String,
        parameters: List<JsClassId>,
        classId: JsClassId,
        concreteValues: Collection<FuzzedConcreteValue>,
        tracer: Trie<JsStatement, *>
    ) : this(
        name,
        if (classId.isClass) listOf(classId) + parameters else parameters,
        concreteValues,
        classId.takeIf { it.isClass },
        tracer
    )
}

class JsFeedback(
    override val control: Control = Control.CONTINUE,
    val result: Trie.Node<JsStatement> = Trie.emptyNode()
) : Feedback<JsClassId, FuzzedValue> {

    override fun equals(other: Any?): Boolean {
        val castOther = other as? JsFeedback
        return control == castOther?.control
    }

    override fun hashCode(): Int {
        return control.hashCode()
    }
}

data class JsStatement(
    val number: Int
)
