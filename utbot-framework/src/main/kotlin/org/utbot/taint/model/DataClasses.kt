package org.utbot.taint.model

import kotlinx.serialization.Serializable

// Data classes corresponding to the parsed data transfer objects.
// See [org.utbot.taint.parser.model] for more details.

@Serializable
data class MethodFqn(
    val packageNames: List<String>,
    val className: String,
    val methodName: String
)

@Serializable
sealed interface TaintEntities {
    val entities: Collection<TaintEntity>
}
@Serializable data class TaintEntitiesSet(override val entities: Set<TaintEntity>) : TaintEntities

@Serializable sealed interface TaintMarks
@Serializable object TaintMarksAll : TaintMarks
@Serializable data class TaintMarksSet(val marks: Set<TaintMark>) : TaintMarks

@Serializable sealed interface TaintEntity
@Serializable object TaintEntityThis : TaintEntity
@Serializable object TaintEntityReturn : TaintEntity
@Serializable data class TaintEntityArgument(/** one-based */ val index: UInt) : TaintEntity

@Serializable  data class TaintMark(val name: String)

@Serializable sealed interface ArgumentValue
@Serializable object ArgumentValueNull : ArgumentValue
@Serializable data class ArgumentValueBoolean(val value: Boolean) : ArgumentValue
@Serializable data class ArgumentValueLong(val value: Long) : ArgumentValue
@Serializable data class ArgumentValueDouble(val value: Double) : ArgumentValue
@Serializable data class ArgumentValueString(val value: String) : ArgumentValue

@Serializable sealed interface ArgumentType
@Serializable object ArgumentTypeAny : ArgumentType
@Serializable data class ArgumentTypeString(val typeFqn: String) : ArgumentType

@Serializable sealed interface TaintSignature
@Serializable object TaintSignatureAny : TaintSignature
@Serializable data class TaintSignatureList(val argumentTypes: List<ArgumentType>) : TaintSignature
