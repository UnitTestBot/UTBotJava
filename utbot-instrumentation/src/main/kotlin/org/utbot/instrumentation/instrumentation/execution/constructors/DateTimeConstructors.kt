package org.utbot.instrumentation.instrumentation.execution.constructors

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.*

internal class InstantConstructor : UtAssembleModelConstructorBase() {

    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface, value: Any, classId: ClassId
    ): UtExecutableCallModel {

        checkClassCast(classId.jClass, value::class.java)
        value as java.time.Instant
        val seconds = value.epochSecond
        val nanos = value.nano.toLong()

        val secondsModel = internalConstructor.construct(seconds, longClassId)
        val nanosModel = internalConstructor.construct(nanos, longClassId)


        return UtExecutableCallModel(
            instance = null,
            methodId(classId, "ofEpochSecond", classId, longClassId, longClassId),
            listOf(secondsModel, nanosModel),
            )
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface, value: Any
    ): List<UtStatementModel> = emptyList()
}

internal class DurationConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface, value: Any, classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)
        value as java.time.Duration
        val seconds = value.seconds
        val nanos = value.nano.toLong()

        val secondsModel = internalConstructor.construct(seconds, longClassId)
        val nanosModel = internalConstructor.construct(nanos, longClassId)


        return UtExecutableCallModel(
            instance = null,
            methodId(classId, "ofSeconds", classId, longClassId, longClassId),
            listOf(secondsModel, nanosModel),

            )
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface, value: Any
    ): List<UtStatementModel> = emptyList()

}

internal class LocalDateConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface, value: Any, classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)
        value as java.time.LocalDate

        with(internalConstructor) {
            return UtExecutableCallModel(
                instance = null,
                methodId(classId, "of", classId, intClassId, intClassId, intClassId),
                listOf(
                    construct(value.year, intClassId),
                    construct(value.monthValue, intClassId),
                    construct(value.dayOfMonth, intClassId)
                ),

                )
        }
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface, value: Any
    ): List<UtStatementModel> = emptyList()

}

internal class LocalTimeConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface, value: Any, classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)
        value as java.time.LocalTime

        with(internalConstructor) {
            return UtExecutableCallModel(
                instance = null,
                methodId(classId, "of", classId, intClassId, intClassId, intClassId, intClassId),
                listOf(
                    construct(value.hour, intClassId),
                    construct(value.minute, intClassId),
                    construct(value.second, intClassId),
                    construct(value.nano, intClassId)
                ),

                )
        }
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface, value: Any
    ): List<UtStatementModel> = emptyList()

}

internal class LocalDateTimeConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface, value: Any, classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)
        value as java.time.LocalDateTime
        val timeClassId = java.time.LocalTime::class.java.id
        val dateClassId = java.time.LocalTime::class.java.id

        with(internalConstructor) {
            return UtExecutableCallModel(
                instance = null,
                methodId(classId, "of", classId, dateClassId, timeClassId),
                listOf(
                    construct(value.toLocalDate(), dateClassId),
                    construct(value.toLocalTime(), timeClassId),
                ),

                )
        }
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface, value: Any
    ): List<UtStatementModel> = emptyList()

}

internal class ZoneIdConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface, value: Any, classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)
        value as java.time.ZoneId
        val id = value.id

        val idModel = internalConstructor.construct(id, stringClassId)


        return UtExecutableCallModel(
            instance = null, methodId(classId, "of", classId, stringClassId), listOf(idModel)
        )
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface, value: Any
    ): List<UtStatementModel> = emptyList()

}

internal class MonthDayConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface, value: Any, classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)
        value as java.time.MonthDay

        with(internalConstructor) {
            return UtExecutableCallModel(
                instance = null,
                methodId(classId, "of", classId, intClassId, intClassId),
                listOf(
                    construct(value.monthValue, intClassId),
                    construct(value.dayOfMonth, intClassId),
                ),

                )
        }
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface, value: Any
    ): List<UtStatementModel> = emptyList()

}

internal class YearConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface, value: Any, classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)
        value as java.time.Year

        with(internalConstructor) {
            return UtExecutableCallModel(
                instance = null,
                methodId(classId, "of", classId, intClassId),
                listOf(
                    construct(value.value, intClassId),
                ),

                )
        }
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface, value: Any
    ): List<UtStatementModel> = emptyList()

}

internal class YearMonthConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface, value: Any, classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)
        value as java.time.YearMonth

        with(internalConstructor) {
            return UtExecutableCallModel(
                instance = null,
                methodId(classId, "of", classId, intClassId, intClassId),
                listOf(
                    construct(value.year, intClassId),
                    construct(value.monthValue, intClassId),
                ),

                )
        }
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface, value: Any
    ): List<UtStatementModel> = emptyList()

}

internal class PeriodConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface, value: Any, classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)
        value as java.time.Period

        with(internalConstructor) {
            return UtExecutableCallModel(
                instance = null,
                methodId(classId, "of", classId, intClassId, intClassId, intClassId),
                listOf(
                    construct(value.years, intClassId),
                    construct(value.months, intClassId),
                    construct(value.days, intClassId),
                ),

                )
        }
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface, value: Any
    ): List<UtStatementModel> = emptyList()

}

internal class ZoneOffsetConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface, value: Any, classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)
        value as java.time.ZoneOffset

        with(internalConstructor) {
            return UtExecutableCallModel(
                instance = null,
                methodId(classId, "ofTotalSeconds", classId, intClassId),
                listOf(
                    construct(value.totalSeconds, intClassId),
                ),

                )
        }
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface, value: Any
    ): List<UtStatementModel> = emptyList()

}

internal class OffsetTimeConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface, value: Any, classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)
        value as java.time.OffsetTime
        val timeClassId = java.time.LocalTime::class.java.id
        val offsetClassId = java.time.ZoneOffset::class.java.id

        with(internalConstructor) {
            return UtExecutableCallModel(
                instance = null,
                methodId(classId, "of", classId, timeClassId, offsetClassId),
                listOf(
                    construct(value.toLocalTime(), timeClassId),
                    construct(value.offset, offsetClassId),
                ),

                )
        }
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface, value: Any
    ): List<UtStatementModel> = emptyList()

}

internal class OffsetDateTimeConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface, value: Any, classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)
        value as java.time.OffsetDateTime
        val dateTimeClassId = java.time.LocalDateTime::class.java.id
        val offsetClassId = java.time.ZoneOffset::class.java.id

        with(internalConstructor) {
            return UtExecutableCallModel(
                instance = null,
                methodId(classId, "of", classId, dateTimeClassId, offsetClassId),
                listOf(
                    construct(value.toLocalDateTime(), dateTimeClassId),
                    construct(value.offset, offsetClassId),
                ),

                )
        }
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface, value: Any
    ): List<UtStatementModel> = emptyList()

}

internal class ZonedDateTimeConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface, value: Any, classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)
        value as java.time.ZonedDateTime
        val dateTimeClassId = java.time.LocalDateTime::class.java.id
        val offsetClassId = java.time.ZoneOffset::class.java.id
        val zoneClassId = java.time.ZoneId::class.java.id

        with(internalConstructor) {
            return UtExecutableCallModel(
                instance = null,
                methodId(classId, "ofLenient", classId, dateTimeClassId, offsetClassId, zoneClassId),
                listOf(
                    construct(value.toLocalDateTime(), dateTimeClassId),
                    construct(value.offset, offsetClassId),
                    construct(value.zone, zoneClassId),
                ),

                )
        }
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface, value: Any
    ): List<UtStatementModel> = emptyList()

}

internal class DateConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface, value: Any, classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)
        value as java.util.Date
        val instantClassId = java.time.Instant::class.java.id

        with(internalConstructor) {
            return UtExecutableCallModel(
                instance = null,
                methodId(classId, "from", classId, instantClassId),
                listOf(
                    construct(value.toInstant(), instantClassId),
                ),

                )
        }
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface, value: Any
    ): List<UtStatementModel> = emptyList()

}

internal class TimeZoneConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface, value: Any, classId: ClassId
    ): UtExecutableCallModel {
        checkClassCast(classId.jClass, value::class.java)
        value as java.util.TimeZone
        val zoneClassId = java.time.ZoneId::class.java.id

        with(internalConstructor) {
            return UtExecutableCallModel(
                instance = null,
                methodId(classId, "getTimeZone", classId, zoneClassId),
                listOf(
                    construct(value.toZoneId(), zoneClassId),
                ),

                )
        }
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface, value: Any
    ): List<UtStatementModel> = emptyList()

}
