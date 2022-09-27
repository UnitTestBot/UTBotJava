package org.utbot.fuzzer.providers

import java.text.SimpleDateFormat
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.dateClassId
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldAllValues
import org.utbot.fuzzer.defaultModelProviders
import org.utbot.fuzzer.fuzz
import org.utbot.fuzzer.hex
import org.utbot.fuzzer.objects.assembleModel

class DateConstantModelProvider(
    private val idGenerator: IdentityPreservingIdGenerator<Int>,
) : ModelProvider {

    var totalLimit: Int = 20

    companion object {
        private const val defaultDateFormat = "dd-MM-yyyy HH:mm:ss:ms"
    }

    private fun String.isDate(format: String): Boolean {
        val formatter = SimpleDateFormat(format).apply {
            isLenient = false
        }
        return runCatching { formatter.parse(trim()) }.isSuccess
    }

    private fun String.isDateFormat(): Boolean {
        return none { it.isDigit() } && // fixes concrete date values
                runCatching { SimpleDateFormat(this) }.isSuccess
    }

    private fun generateFromNumbers(
        baseMethodDescription: FuzzedMethodDescription,
    ): Sequence<FuzzedValue> {
        val constructorsFromNumbers = dateClassId.allConstructors
            .filter { constructor ->
                constructor.parameters.isNotEmpty() &&
                        constructor.parameters.all { it == intClassId || it == longClassId }
            }.map { constructorId ->
                with(constructorId) {
                    ModelConstructor(parameters.map(::FuzzedType)) { assembleModel(idGenerator.createId(), constructorId, it) }
                }
            }.sortedBy { it.neededTypes.size }

        return sequence {
            constructorsFromNumbers.forEach { constructor ->
                yieldAll(
                    fuzzValues(
                        constructor.neededTypes.map(FuzzedType::classId),
                        baseMethodDescription,
                        defaultModelProviders(idGenerator)
                    ).map(constructor.createModel)
                )
            }
        }
    }

    private fun generateFromDates(
        baseMethodDescription: FuzzedMethodDescription,
    ): Sequence<FuzzedValue> {
        val strings = baseMethodDescription.concreteValues
            .asSequence()
            .filter { it.classId == stringClassId }
            .map { it.value as String }
            .distinct()
        val formats = strings.filter { it.isDateFormat() } + defaultDateFormat
        val formatToDates = formats.associateWith { format -> strings.filter { it.isDate(format) } }

        return sequence {
            formatToDates.forEach { (format, dates) ->
                dates.forEach { date ->
                    yield(assembleDateFromString(idGenerator.createId(), format, date))
                }
            }
        }
    }

    private fun generateNowDate(): Sequence<FuzzedValue> {
        val constructor = dateClassId.allConstructors.first { it.parameters.isEmpty() }
        return sequenceOf(assembleModel(idGenerator.createId(), constructor, emptyList()))
    }

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> {
        val parameters = description.parametersMap[dateClassId]
        if (parameters.isNullOrEmpty()) {
            return emptySequence()
        }

        return sequence {
            yieldAllValues(
                parameters,
                generateNowDate() + generateFromDates(description) +
                        generateFromNumbers(description).take(totalLimit)
            )
        }.take(totalLimit)
    }

    private fun fuzzValues(
        types: List<ClassId>,
        baseMethodDescription: FuzzedMethodDescription,
        modelProvider: ModelProvider,
    ): Sequence<List<FuzzedValue>> {
        if (types.isEmpty())
            return sequenceOf(listOf())
        val syntheticMethodDescription = FuzzedMethodDescription(
            "<synthetic method for DateModelProvider>", // TODO: maybe add more info here
            voidClassId,
            types,
            baseMethodDescription.concreteValues
        ).apply {
            packageName = baseMethodDescription.packageName
        }
        return fuzz(syntheticMethodDescription, modelProvider)
    }

    private fun assembleDateFromString(id: Int, formatString: String, dateString: String): FuzzedValue {
        val simpleDateFormatModel = assembleSimpleDateFormat(idGenerator.createId(), formatString)
        val dateFormatParse = simpleDateFormatModel.classId.jClass
            .getMethod("parse", String::class.java).executableId
        val instantiationCall = UtExecutableCallModel(
            simpleDateFormatModel, dateFormatParse, listOf(UtPrimitiveModel(dateString))
        )
        return UtAssembleModel(
            id,
            dateClassId,
            "$dateFormatParse#" + id.hex(),
            instantiationCall
        ).fuzzed {
            summary = "%var% = $dateFormatParse($stringClassId)"
        }
    }

    private fun assembleSimpleDateFormat(id: Int, formatString: String): UtAssembleModel {
        val simpleDateFormatId = SimpleDateFormat::class.java.id
        val formatStringConstructor = simpleDateFormatId.allConstructors.first {
            it.parameters.singleOrNull() == stringClassId
        }
        val formatSetLenient = SimpleDateFormat::setLenient.executableId
        val formatModel = UtPrimitiveModel(formatString)

        val instantiationCall = UtExecutableCallModel(instance = null, formatStringConstructor, listOf(formatModel))
        return UtAssembleModel(
            id,
            simpleDateFormatId,
            "$simpleDateFormatId[$stringClassId]#" + id.hex(),
            instantiationCall
        ) {
            listOf(UtExecutableCallModel(instance = this, formatSetLenient, listOf(UtPrimitiveModel(false))))
        }
    }

}
