package fuzzer

import framework.api.js.JsClassId
import framework.api.js.util.isClass
import org.utbot.framework.plugin.api.UtTimeoutException
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.UtFuzzedExecution
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
    val concreteValues: Collection<JsFuzzedConcreteValue>,
    val thisInstance: JsClassId? = null,
    val tracer: Trie<JsStatement, *>
) : Description<JsClassId>(parameters) {

    constructor(
        name: String,
        parameters: List<JsClassId>,
        classId: JsClassId,
        concreteValues: Collection<JsFuzzedConcreteValue>,
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
) : Feedback<JsClassId, JsFuzzedValue> {

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

data class JsFuzzedValue(
    val model: UtModel,
    var summary: String? = null,
)

data class JsFuzzedConcreteValue(
    val classId: ClassId,
    val value: Any,
    val fuzzedContext: JsFuzzedContext = JsFuzzedContext.Unknown,
)

enum class JsFuzzedContext {
    EQ,
    NE,
    GT,
    GE,
    LT,
    LE,
    Unknown;

    fun reverse(): JsFuzzedContext = when (this) {
        EQ -> NE
        NE -> EQ
        GT -> LE
        LT -> GE
        LE -> GT
        GE -> LT
        Unknown -> Unknown
    }
}

fun UtModel.fuzzed(block: JsFuzzedValue.() -> Unit = {}): JsFuzzedValue = JsFuzzedValue(this).apply(block)

object JsIdProvider {
    private var _id = AtomicInteger(0)

    fun get() = _id.incrementAndGet()
}
