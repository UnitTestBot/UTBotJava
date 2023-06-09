package org.utbot.taint.parser.yaml

/**
 * See "docs/TaintAnalysis.md" for configuration format.
 */
data class YamlTaintConfiguration(
    val sources: List<YamlTaintSource>,
    val passes: List<YamlTaintPass>,
    val cleaners: List<YamlTaintCleaner>,
    val sinks: List<YamlTaintSink>
)

data class YamlTaintSource(
    val methodFqn: YamlMethodFqn,
    val addTo: YamlTaintEntities,
    val marks: YamlTaintMarks,
    val signature: YamlTaintSignature = YamlTaintSignatureAny,
    val conditions: YamlTaintConditions = YamlNoTaintConditions,
)

data class YamlTaintPass(
    val methodFqn: YamlMethodFqn,
    val getFrom: YamlTaintEntities,
    val addTo: YamlTaintEntities,
    val marks: YamlTaintMarks,
    val signature: YamlTaintSignature = YamlTaintSignatureAny,
    val conditions: YamlTaintConditions = YamlNoTaintConditions,
)

data class YamlTaintCleaner(
    val methodFqn: YamlMethodFqn,
    val removeFrom: YamlTaintEntities,
    val marks: YamlTaintMarks,
    val signature: YamlTaintSignature = YamlTaintSignatureAny,
    val conditions: YamlTaintConditions = YamlNoTaintConditions,
)

data class YamlTaintSink(
    val methodFqn: YamlMethodFqn,
    val check: YamlTaintEntities,
    val marks: YamlTaintMarks,
    val signature: YamlTaintSignature = YamlTaintSignatureAny,
    val conditions: YamlTaintConditions = YamlNoTaintConditions,
)

data class YamlMethodFqn(
    val packageNames: List<String>,
    val className: String,
    val methodName: String
)

sealed interface YamlTaintEntities
data class YamlTaintEntitiesSet(val entities: Set<YamlTaintEntity>) : YamlTaintEntities

sealed interface YamlTaintMarks
object YamlTaintMarksAll : YamlTaintMarks
data class YamlTaintMarksSet(val marks: Set<YamlTaintMark>) : YamlTaintMarks

sealed interface YamlTaintSignature
object YamlTaintSignatureAny : YamlTaintSignature
data class YamlTaintSignatureList(val argumentTypes: List<YamlArgumentType>) : YamlTaintSignature

sealed interface YamlTaintConditions
object YamlNoTaintConditions : YamlTaintConditions
data class YamlTaintConditionsMap(val entityToCondition: Map<YamlTaintEntity, YamlTaintCondition>) : YamlTaintConditions

sealed interface YamlTaintEntity
object YamlTaintEntityThis : YamlTaintEntity
data class YamlTaintEntityArgument(/** one-based */ val index: UInt) : YamlTaintEntity

object YamlTaintEntityReturn : YamlTaintEntity

data class YamlTaintMark(val name: String)

sealed interface YamlTaintCondition
data class YamlTaintConditionEqualValue(val argumentValue: YamlArgumentValue) : YamlTaintCondition
data class YamlTaintConditionIsType(val argumentType: YamlArgumentType) : YamlTaintCondition
data class YamlTaintConditionNot(val inner: YamlTaintCondition) : YamlTaintCondition
data class YamlTaintConditionOr(val inners: List<YamlTaintCondition>) : YamlTaintCondition

sealed interface YamlArgumentValue
object YamlArgumentValueNull : YamlArgumentValue
data class YamlArgumentValueBoolean(val value: Boolean) : YamlArgumentValue
data class YamlArgumentValueLong(val value: Long) : YamlArgumentValue
data class YamlArgumentValueDouble(val value: Double) : YamlArgumentValue
data class YamlArgumentValueString(val value: String) : YamlArgumentValue

sealed interface YamlArgumentType
object YamlArgumentTypeAny : YamlArgumentType
data class YamlArgumentTypeString(val typeFqn: String) : YamlArgumentType
