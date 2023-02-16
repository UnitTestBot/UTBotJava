package org.utbot.taint.parser.model

/**
 * See "docs/TaintAnalysis.md" for configuration format.
 */
data class Configuration(
    val sources: List<Source>,
    val passes: List<Pass>,
    val cleaners: List<Cleaner>,
    val sinks: List<Sink>
)

data class Source(
    val methodFqn: MethodFqn,
    val addTo: TaintEntities,
    val marks: TaintMarks,
    val signature: Signature = AnySignature,
    val conditions: Conditions = NoConditions,
)

data class Pass(
    val methodFqn: MethodFqn,
    val getFrom: TaintEntities,
    val addTo: TaintEntities,
    val marks: TaintMarks,
    val signature: Signature = AnySignature,
    val conditions: Conditions = NoConditions,
)

data class Cleaner(
    val methodFqn: MethodFqn,
    val removeFrom: TaintEntities,
    val marks: TaintMarks,
    val signature: Signature = AnySignature,
    val conditions: Conditions = NoConditions,
)

data class Sink(
    val methodFqn: MethodFqn,
    val check: TaintEntities,
    val marks: TaintMarks,
    val signature: Signature = AnySignature,
    val conditions: Conditions = NoConditions,
)

data class MethodFqn(
    val packageNames: List<String>,
    val className: String,
    val methodName: String
)

sealed interface TaintEntities
data class TaintEntitiesSet(val entities: Set<TaintEntity>) : TaintEntities

sealed interface TaintMarks
object AllTaintMarks : TaintMarks
data class TaintMarksSet(val marks: Set<TaintMark>) : TaintMarks

sealed interface Signature
object AnySignature : Signature
data class SignatureList(val argumentTypes: List<ArgumentType>) : Signature

sealed interface Conditions
object NoConditions : Conditions
data class ConditionsMap(val entityToCondition: Map<TaintEntity, Condition>) : Conditions

sealed interface TaintEntity
object ThisObject : TaintEntity
object ReturnValue : TaintEntity
data class MethodArgument(/** one-based */ val index: UInt) : TaintEntity

data class TaintMark(val name: String)

sealed interface Condition
data class ValueCondition(val argumentValue: ArgumentValue) : Condition
data class TypeCondition(val argumentType: ArgumentType) : Condition
data class NotCondition(val inner: Condition) : Condition
data class OrCondition(val inners: List<Condition>) : Condition

sealed interface ArgumentValue
object ArgumentValueNull : ArgumentValue
data class ArgumentValueBoolean(val value: Boolean) : ArgumentValue
data class ArgumentValueLong(val value: Long) : ArgumentValue
data class ArgumentValueDouble(val value: Double) : ArgumentValue
data class ArgumentValueString(val value: String) : ArgumentValue

sealed interface ArgumentType
object ArgumentTypeAny : ArgumentType
data class ArgumentTypeString(val typeFqn: String) : ArgumentType
