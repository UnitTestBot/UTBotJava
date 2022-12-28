package org.utbot.greyboxfuzzer.generator


import org.utbot.greyboxfuzzer.quickcheck.NonTrackingGenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.*
import org.utbot.greyboxfuzzer.quickcheck.generator.java.time.*
import org.utbot.greyboxfuzzer.quickcheck.generator.java.util.*
import org.utbot.greyboxfuzzer.quickcheck.generator.java.lang.*
import org.utbot.greyboxfuzzer.quickcheck.generator.java.math.*
import org.utbot.greyboxfuzzer.quickcheck.generator.java.nio.charset.CharsetGenerator
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

object GreyBoxFuzzerGeneratorsAndSettings {

    const val seed = 42L
    val maxDepthOfGeneration = AtomicInteger(5)
    val sourceOfRandomness = SourceOfRandomness(Random(seed))
    val genStatus = NonTrackingGenerationStatus(sourceOfRandomness)

    val generatorRepository =
        UTGeneratorRepository(sourceOfRandomness).also {
            it.register(DurationGenerator())
            it.register(MonthDayGenerator())
            it.register(LocalDateTimeGenerator())
            it.register(YearMonthGenerator())
            it.register(ClockGenerator())
            it.register(ZonedDateTimeGenerator())
            it.register(LocalDateGenerator())
            it.register(ZoneIdGenerator())
            it.register(YearGenerator())
            it.register(OffsetTimeGenerator())
            it.register(InstantGenerator())
            it.register(ZoneOffsetGenerator())
            it.register(LocalTimeGenerator())
            it.register(OffsetDateTimeGenerator())
            it.register(PeriodGenerator())
            it.register(BigDecimalGenerator())
            it.register(BigIntegerGenerator())
            it.register(CharsetGenerator())
            it.register(ShortGenerator())
            it.register(BooleanGenerator())
            it.register(IntegerGenerator())
            it.register(ByteGenerator())
            it.register(StringGenerator())
            it.register(LongGenerator())
            it.register(DoubleGenerator())
            it.register(CharacterGenerator())
            it.register(FloatGenerator())
            it.register(OptionalIntGenerator())
            it.register(OptionalDoubleGenerator())
            it.register(LinkedListGenerator())
            it.register(LinkedHashSetGenerator())
            it.register(HashMapGenerator())
            it.register(LocaleGenerator())
            it.register(BitSetGenerator())
            //it.register(TimeZoneGenerator())
            it.register(HashSetGenerator())
            it.register(ArrayListGenerator())
            it.register(VectorGenerator())
            it.register(LinkedHashMapGenerator())
            it.register(HashtableGenerator())
            it.register(OptionalLongGenerator())
            it.register(PropertiesGenerator())
            it.register(OptionalGenerator())
            it.register(DateGenerator())
            it.register(StackGenerator())
            it.register(VoidGenerator())
            it.register(PrimitiveCharGenerator())
            it.register(PrimitiveBooleanGenerator())
            it.register(PrimitiveByteGenerator())
            it.register(PrimitiveDoubleGenerator())
            it.register(PrimitiveFloatGenerator())
            it.register(PrimitiveIntGenerator())
            it.register(PrimitiveLongGenerator())
            it.register(PrimitiveShortGenerator())
            it.register(RFC4122.Version3())
            it.register(RFC4122.Version4())
            it.register(RFC4122.Version5())
        }
}