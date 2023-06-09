package org.utbot.taint

import org.utbot.taint.model.*
import org.utbot.taint.parser.yaml.*

/**
 * Converts objects from .yaml config to model data classes.
 */
object YamlTaintConfigurationAdapter {

    fun convert(configuration: YamlTaintConfiguration) =
        TaintConfiguration(
            configuration.sources.map { it.convert() },
            configuration.passes.map { it.convert() },
            configuration.cleaners.map { it.convert() },
            configuration.sinks.map { it.convert() },
        )

    // internal

    private fun YamlTaintSource.convert() =
        TaintSource(
            methodFqn.convert(),
            addTo.convert(),
            marks.convert(),
            signature.convert(),
            conditions.convert()
        )

    private fun YamlTaintPass.convert() =
        TaintPass(
            methodFqn.convert(),
            getFrom.convert(),
            addTo.convert(),
            marks.convert(),
            signature.convert(),
            conditions.convert()
        )

    private fun YamlTaintCleaner.convert() =
        TaintCleaner(
            methodFqn.convert(),
            removeFrom.convert(),
            marks.convert(),
            signature.convert(),
            conditions.convert()
        )

    private fun YamlTaintSink.convert() =
        TaintSink(
            methodFqn.convert(),
            check.convert(),
            marks.convert(),
            signature.convert(),
            conditions.convert()
        )

    private fun YamlTaintSignature.convert(): TaintSignature =
        when (this) {
            YamlTaintSignatureAny -> TaintSignatureAny
            is YamlTaintSignatureList -> TaintSignatureList(
                argumentTypes.map { it.convert() }
            )
        }

    private fun YamlTaintConditions.convert(): TaintCondition =
        when (this) {
            YamlNoTaintConditions -> ConditionTrue
            is YamlTaintConditionsMap -> ConditionAnd(
                entityToCondition.map { (entity, condition) ->
                    condition.convert(entity)
                }
            )
        }

    private fun YamlTaintCondition.convert(entity: YamlTaintEntity): TaintCondition =
        when (this) {
            is YamlTaintConditionEqualValue -> {
                ConditionEqualValue(entity.convert(), argumentValue.convert())
            }
            is YamlTaintConditionIsType -> {
                ConditionIsType(entity.convert(), argumentType.convert())
            }
            is YamlTaintConditionNot -> {
                ConditionNot(inner.convert(entity))
            }
            is YamlTaintConditionOr -> {
                ConditionOr(inners.map { it.convert(entity) })
            }
        }

    private fun YamlMethodFqn.convert() =
        MethodFqnValue(packageNames, className, methodName)

    private fun YamlTaintEntities.convert(): TaintEntities =
        when (this) {
            is YamlTaintEntitiesSet -> {
                TaintEntitiesSet(entities.map { it.convert() }.toSet())
            }
        }

    private fun YamlTaintMarks.convert(): TaintMarks =
        when (this) {
            YamlTaintMarksAll -> TaintMarksAll
            is YamlTaintMarksSet -> TaintMarksSet(marks.map { it.convert() }.toSet())
        }

    private fun YamlTaintEntity.convert(): TaintEntity =
        when (this) {
            is YamlTaintEntityArgument -> TaintEntityArgument(index)
            YamlTaintEntityReturn -> TaintEntityReturn
            YamlTaintEntityThis -> TaintEntityThis
        }

    private fun YamlTaintMark.convert() =
        TaintMark(name)

    private fun YamlArgumentType.convert(): ArgumentType =
        when (this) {
            YamlArgumentTypeAny -> ArgumentTypeAny
            is YamlArgumentTypeString -> ArgumentTypeString(typeFqn)
        }

    private fun YamlArgumentValue.convert(): ArgumentValue =
        when (this) {
            YamlArgumentValueNull -> ArgumentValueNull
            is YamlArgumentValueBoolean -> ArgumentValueBoolean(value)
            is YamlArgumentValueLong -> ArgumentValueLong(value)
            is YamlArgumentValueDouble -> ArgumentValueDouble(value)
            is YamlArgumentValueString -> ArgumentValueString(value)
        }
}
