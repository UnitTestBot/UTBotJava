package org.utbot.fuzzer

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId

/**
 * Method traverser is an object,
 * that helps to collect information about a method.
 *
 * @param name pretty name of the method
 * @param returnType type of returning value
 * @param parameters method parameters types
 * @param concreteValues any concrete values to be processed by fuzzer
 *
 */
class FuzzedMethodDescription(
    val name: String,
    val returnType: ClassId,
    val parameters: List<ClassId>,
    val concreteValues: Collection<FuzzedConcreteValue> = emptyList()
) {

    /**
     * Name that can be used to generate test names
     */
    var compilableName: String? = null

    /**
     * Class Name
     */
    var className: String? = null

    /**
     * Package Name
     */
    var packageName: String? = null

    /**
     * Returns parameter name by its index in the signature
     */
    var parameterNameMap: (Int) -> String? = { null }

    /**
     * Map class id to indices of this class in parameters list.
     */
    val parametersMap: Map<ClassId, List<Int>> by lazy {
        val result = mutableMapOf<ClassId, MutableList<Int>>()
        parameters.forEachIndexed { index, classId ->
            result.computeIfAbsent(classId) { mutableListOf() }.add(index)
        }
        result
    }

    constructor(executableId: ExecutableId, concreteValues: Collection<FuzzedConcreteValue> = emptyList()) : this(
        executableId.classId.simpleName + "." + executableId.name,
        executableId.returnType,
        executableId.parameters,
        concreteValues
    )
}

/**
 * Object to pass concrete values to fuzzer
 */
data class FuzzedConcreteValue(
    val classId: ClassId,
    val value: Any,
    val relativeOp: FuzzedOp = FuzzedOp.NONE,
)

enum class FuzzedOp(val sign: String?) {
    NONE(null),
    EQ("=="),
    NE("!="),
    GT(">"),
    GE(">="),
    LT("<"),
    LE("<="),
    CH(null), // changed or called
    ;

    fun isComparisonOp() = this == EQ || this == NE || this == GT || this == GE || this == LT || this == LE

    fun reverseOrNull() : FuzzedOp? = when(this) {
        EQ -> NE
        NE -> EQ
        GT -> LE
        LT -> GE
        LE -> GT
        GE -> LT
        else -> null
    }

    fun reverseOrElse(another: (FuzzedOp) -> FuzzedOp): FuzzedOp =
        reverseOrNull() ?: another(this)
}