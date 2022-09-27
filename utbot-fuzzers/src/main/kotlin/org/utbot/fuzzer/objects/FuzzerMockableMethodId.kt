package org.utbot.fuzzer.objects

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtModel

/**
 * Implements [MethodId] but also can supply a mock for this execution.
 *
 * Simplest example: setter and getter,
 * when this methodId is a setter, getter can be used for a mock to supply correct value.
 */
internal class FuzzerMockableMethodId(
    classId: ClassId,
    name: String,
    returnType: ClassId,
    parameters: List<ClassId>,
    val mock: () -> Map<ExecutableId, List<UtModel>> = { emptyMap() },
) : MethodId(classId, name, returnType, parameters) {

    constructor(copyOf: MethodId, mock: () -> Map<ExecutableId, List<UtModel>> = { emptyMap() }) : this(
        copyOf.classId, copyOf.name, copyOf.returnType, copyOf.parameters, mock
    )

}

internal fun MethodId.toFuzzerMockable(block: suspend SequenceScope<Pair<MethodId, List<UtModel>>>.() -> Unit): FuzzerMockableMethodId {
    return FuzzerMockableMethodId(this) {
        sequence { block() }.toMap()
    }
}