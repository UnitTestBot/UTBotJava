package org.utbot.taint.parser.yaml

/**
 * See "docs/TaintAnalysis.md" for configuration format.
 */
data class DtoTaintConfiguration(
    val sources: List<DtoTaintSource>,
    val passes: List<DtoTaintPass>,
    val cleaners: List<DtoTaintCleaner>,
    val sinks: List<DtoTaintSink>
)

data class DtoTaintSource(
    val methodFqn: DtoMethodFqn,
    val addTo: DtoTaintEntities,
    val marks: DtoTaintMarks,
    val signature: DtoTaintSignature = DtoTaintSignatureAny,
    val conditions: DtoTaintConditions = DtoNoTaintConditions,
)

data class DtoTaintPass(
    val methodFqn: DtoMethodFqn,
    val getFrom: DtoTaintEntities,
    val addTo: DtoTaintEntities,
    val marks: DtoTaintMarks,
    val signature: DtoTaintSignature = DtoTaintSignatureAny,
    val conditions: DtoTaintConditions = DtoNoTaintConditions,
)

data class DtoTaintCleaner(
    val methodFqn: DtoMethodFqn,
    val removeFrom: DtoTaintEntities,
    val marks: DtoTaintMarks,
    val signature: DtoTaintSignature = DtoTaintSignatureAny,
    val conditions: DtoTaintConditions = DtoNoTaintConditions,
)

data class DtoTaintSink(
    val methodFqn: DtoMethodFqn,
    val check: DtoTaintEntities,
    val marks: DtoTaintMarks,
    val signature: DtoTaintSignature = DtoTaintSignatureAny,
    val conditions: DtoTaintConditions = DtoNoTaintConditions,
)

data class DtoMethodFqn(
    val packageNames: List<String>,
    val className: String,
    val methodName: String
)

sealed interface DtoTaintEntities
data class DtoTaintEntitiesSet(val entities: Set<DtoTaintEntity>) : DtoTaintEntities

sealed interface DtoTaintMarks
object DtoTaintMarksAll : DtoTaintMarks
data class DtoTaintMarksSet(val marks: Set<DtoTaintMark>) : DtoTaintMarks

sealed interface DtoTaintSignature
object DtoTaintSignatureAny : DtoTaintSignature
data class DtoTaintSignatureList(val argumentTypes: List<DtoArgumentType>) : DtoTaintSignature

sealed interface DtoTaintConditions
object DtoNoTaintConditions : DtoTaintConditions
data class DtoTaintConditionsMap(val entityToCondition: Map<DtoTaintEntity, DtoTaintCondition>) : DtoTaintConditions

sealed interface DtoTaintEntity
object DtoTaintEntityThis : DtoTaintEntity
data class DtoTaintEntityArgument(/** one-based */ val index: UInt) : DtoTaintEntity
object DtoTaintEntityReturn : DtoTaintEntity

data class DtoTaintMark(val name: String)

sealed interface DtoTaintCondition
data class DtoTaintConditionEqualValue(val argumentValue: DtoArgumentValue) : DtoTaintCondition
data class DtoTaintConditionIsType(val argumentType: DtoArgumentType) : DtoTaintCondition
data class DtoTaintConditionNot(val inner: DtoTaintCondition) : DtoTaintCondition
data class DtoTaintConditionOr(val inners: List<DtoTaintCondition>) : DtoTaintCondition

sealed interface DtoArgumentValue
object DtoArgumentValueNull : DtoArgumentValue
data class DtoArgumentValueBoolean(val value: Boolean) : DtoArgumentValue
data class DtoArgumentValueLong(val value: Long) : DtoArgumentValue
data class DtoArgumentValueDouble(val value: Double) : DtoArgumentValue
data class DtoArgumentValueString(val value: String) : DtoArgumentValue

sealed interface DtoArgumentType
object DtoArgumentTypeAny : DtoArgumentType
data class DtoArgumentTypeString(val typeFqn: String) : DtoArgumentType
