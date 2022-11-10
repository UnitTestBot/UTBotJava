

package org.utbot.quickcheck.generator.java.time;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.InRange;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.internal.Reflection.defaultValueOf;

/**
 * Produces values of type {@link YearMonth}.
 */
public class YearMonthGenerator extends Generator<YearMonth> {
    private YearMonth min = YearMonth.of(Year.MIN_VALUE, 1);
    private YearMonth max = YearMonth.of(Year.MAX_VALUE, 12);

    public YearMonthGenerator() {
        super(YearMonth.class);
    }

    /**
     * <p>Tells this generator to produce values within a specified
     * {@linkplain InRange#min() minimum} and/or {@linkplain InRange#max()
     * maximum}, inclusive, with uniform distribution.</p>
     *
     * <p>If an endpoint of the range is not specified, the generator will use
     * dates with values of either {@code YearMonth(Year#MIN_VALUE, 1)} or
     * {@code YearMonth(Year#MAX_VALUE, 12)} as appropriate.</p>
     *
     * <p>{@link InRange#format()} describes
     * {@linkplain DateTimeFormatter#ofPattern(String) how the generator is to
     * interpret the range's endpoints}.</p>
     *
     * @param range annotation that gives the range's constraints
     */
    public void configure(InRange range) {
        DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern(range.format());

        if (!defaultValueOf(InRange.class, "min").equals(range.min()))
            min = YearMonth.parse(range.min(), formatter);
        if (!defaultValueOf(InRange.class, "max").equals(range.max()))
            max = YearMonth.parse(range.max(), formatter);

        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                String.format("bad range, %s > %s", min, max));
        }
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        long generated =
            random.nextLong(
                min.getYear() * 12L + min.getMonthValue() - 1,
                max.getYear() * 12L + max.getMonthValue() - 1);

        return UtModelGenerator.getUtModelConstructor().construct(YearMonth.of(
            (int) (generated / 12),
            (int) Math.abs(generated % 12) + 1), classIdForType(YearMonth.class));
    }
}
