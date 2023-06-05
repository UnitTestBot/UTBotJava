package org.utbot.fuzzing.providers

import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.*
import org.utbot.fuzzing.utils.hex
import java.text.SimpleDateFormat
import java.util.*

abstract class ClassValueProvider(
    val classId: ClassId
) : ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription> {
    override fun accept(type: FuzzedType) = type.classId == classId
}

object NumberValueProvider : ClassValueProvider(Number::class.id) {
    override fun generate(description: FuzzedDescription, type: FuzzedType) = sequence<Seed<FuzzedType, FuzzedValue>> {
        listOf(
            byteClassId, shortClassId, intClassId, longClassId, floatClassId, doubleClassId
        ).forEach { numberPrimitiveType ->
            yield(Seed.Recursive(
                construct = Routine.Create(listOf(FuzzedType(numberPrimitiveType))) { v -> v.first() },
                empty = Routine.Empty { UtNullModel(type.classId).fuzzed() }
            ))
        }
    }
}

class DateValueProvider(
    private val idGenerator: IdGenerator<Int>
) : ClassValueProvider(Date::class.id) {
    override fun generate(description: FuzzedDescription, type: FuzzedType) = sequence<Seed<FuzzedType, FuzzedValue>> {
        // now date
        val nowDateModel = UtAssembleModel(
            id = idGenerator.createId(),
            classId = type.classId,
            modelName = "Date::now",
            instantiationCall = UtStatementCallModel(
                instance = null,
                statement = type.classId.allConstructors.firstOrNull { it.parameters.isEmpty() }
                    ?: error("Cannot find default constructor of ${type.classId}"),
                params = emptyList())
        ).fuzzed { }
        yield(Seed.Simple(nowDateModel))

        // from string dates
        val strings = description.constants
            .filter {
                it.classId == stringClassId
            }
            .map { it.value }
            .filterIsInstance<String>()
            .distinct()
        val dateFormats = strings
            .filter { it.isDateFormat() } + defaultDateFormat
        val formatToDates = dateFormats.associateWith { format -> strings.filter { it.isDate(format) } }
        formatToDates.forEach { (format, dates) ->
            dates.forEach { date ->
                yield(Seed.Simple(
                    assembleDateFromString(idGenerator.createId(), format, date)
                ))
            }
        }

        // from numbers
        type.classId.allConstructors
            .filter {
                it.parameters.isNotEmpty() && it.parameters.all { p -> p == intClassId || p == longClassId }
            }
            .forEach {  constructor ->
                yield(Seed.Recursive(
                    construct = Routine.Create(constructor.parameters.map { FuzzedType(it) }) { values ->
                        UtAssembleModel(
                            id = idGenerator.createId(),
                            classId = type.classId,
                            modelName = "Date(${values.map { it.model.classId }})",
                            instantiationCall = UtStatementCallModel(null, constructor, values.map { it.model })
                        ).fuzzed {  }
                    },
                    empty = Routine.Empty { nowDateModel }
                ))
            }
    }

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

    private fun assembleDateFromString(id: Int, formatString: String, dateString: String): FuzzedValue {
        val simpleDateFormatModel = assembleSimpleDateFormat(idGenerator.createId(), formatString)
        val dateFormatParse = simpleDateFormatModel.classId.jClass
            .getMethod("parse", String::class.java).executableId
        val instantiationCall = UtStatementCallModel(
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

        val instantiationCall = UtStatementCallModel(instance = null, formatStringConstructor, listOf(formatModel))
        return UtAssembleModel(
            id,
            simpleDateFormatId,
            "$simpleDateFormatId[$stringClassId]#" + id.hex(),
            instantiationCall
        ) {
            listOf(UtStatementCallModel(instance = this, formatSetLenient, listOf(UtPrimitiveModel(false))))
        }
    }
}