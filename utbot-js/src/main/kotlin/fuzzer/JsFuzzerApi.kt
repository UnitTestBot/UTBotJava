package fuzzer

import framework.api.js.JsClassId
import framework.api.js.JsUtFuzzedExecution
import framework.api.js.util.isClass
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtTimeoutException
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.Description
import org.utbot.fuzzing.Feedback
import org.utbot.fuzzing.utils.Trie
import java.util.concurrent.atomic.AtomicInteger

sealed interface JsFuzzingExecutionFeedback
class JsValidExecution(val utFuzzedExecution: JsUtFuzzedExecution) : JsFuzzingExecutionFeedback

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

data class JsFeedback(
    override val control: Control = Control.CONTINUE,
    val result: Trie.Node<JsStatement> = Trie.emptyNode()
) : Feedback<JsClassId, UtModel>

data class JsStatement(
    val number: Int
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

object JsIdProvider {
    private var id = AtomicInteger(0)

    fun createId() = id.incrementAndGet()
}
