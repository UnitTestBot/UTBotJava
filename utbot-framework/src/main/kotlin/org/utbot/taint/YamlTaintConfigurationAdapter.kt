package org.utbot.taint

import org.utbot.taint.model.*
import org.utbot.taint.parser.yaml.*

/**
 * Converts data transfer objects from .yaml config to model data classes.
 */
object YamlTaintConfigurationAdapter {

    fun convert(configuration: DtoTaintConfiguration) =
        TaintConfiguration(
            configuration.sources.map { it.convert() },
            configuration.passes.map { it.convert() },
            configuration.cleaners.map { it.convert() },
            configuration.sinks.map { it.convert() },
        )

    // internal

    private fun DtoTaintSource.convert() =
        TaintSource(
            methodFqn.convert(),
            addTo.convert(),
            marks.convert(),
            signature.convert(),
            conditions.convert()
        )

    private fun DtoTaintPass.convert() =
        TaintPass(
            methodFqn.convert(),
            getFrom.convert(),
            addTo.convert(),
            marks.convert(),
            signature.convert(),
            conditions.convert()
        )

    private fun DtoTaintCleaner.convert() =
        TaintCleaner(
            methodFqn.convert(),
            removeFrom.convert(),
            marks.convert(),
            signature.convert(),
            conditions.convert()
        )

    private fun DtoTaintSink.convert() =
        TaintSink(
            methodFqn.convert(),
            check.convert(),
            marks.convert(),
            signature.convert(),
            conditions.convert()
        )

    private fun DtoTaintSignature.convert(): TaintSignature =
        when (this) {
            DtoTaintSignatureAny -> TaintSignatureAny
            is DtoTaintSignatureList -> TaintSignatureList(
                argumentTypes.map { it.convert() }
            )
        }

    private fun DtoTaintConditions.convert(): TaintCondition =
        when (this) {
            DtoNoTaintConditions -> ConditionTrue
            is DtoTaintConditionsMap -> ConditionAnd(
                entityToCondition.map { (entity, condition) ->
                    condition.convert(entity)
                }
            )
        }

    private fun DtoTaintCondition.convert(entity: DtoTaintEntity): TaintCondition =
        when (this) {
            is DtoTaintConditionEqualValue -> {
                ConditionEqualValue(entity.convert(), argumentValue.convert())
            }
            is DtoTaintConditionIsType -> {
                ConditionIsType(entity.convert(), argumentType.convert())
            }
            is DtoTaintConditionNot -> {
                ConditionNot(inner.convert(entity))
            }
            is DtoTaintConditionOr -> {
                ConditionOr(inners.map { it.convert(entity) })
            }
        }

    private fun DtoMethodFqn.convert() =
        MethodFqnValue(packageNames, className, methodName)

    private fun DtoTaintEntities.convert(): TaintEntities =
        when (this) {
            is DtoTaintEntitiesSet -> {
                TaintEntitiesSet(entities.map { it.convert() }.toSet())
            }
        }

    private fun DtoTaintMarks.convert(): TaintMarks =
        when (this) {
            DtoTaintMarksAll -> TaintMarksAll
            is DtoTaintMarksSet -> TaintMarksSet(marks.map { it.convert() }.toSet())
        }

    private fun DtoTaintEntity.convert(): TaintEntity =
        when (this) {
            is DtoTaintEntityArgument -> TaintEntityArgument(index)
            DtoTaintEntityReturn -> TaintEntityReturn
            DtoTaintEntityThis -> TaintEntityThis
        }

    private fun DtoTaintMark.convert() =
        TaintMark(name)

    private fun DtoArgumentType.convert(): ArgumentType =
        when (this) {
            DtoArgumentTypeAny -> ArgumentTypeAny
            is DtoArgumentTypeString -> ArgumentTypeString(typeFqn)
        }

    private fun DtoArgumentValue.convert(): ArgumentValue =
        when (this) {
            DtoArgumentValueNull -> ArgumentValueNull
            is DtoArgumentValueBoolean -> ArgumentValueBoolean(value)
            is DtoArgumentValueLong -> ArgumentValueLong(value)
            is DtoArgumentValueDouble -> ArgumentValueDouble(value)
            is DtoArgumentValueString -> ArgumentValueString(value)
        }
}
