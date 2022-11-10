

package org.utbot.quickcheck.generator.java.time;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.InRange;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.internal.Reflection.defaultValueOf;

/**
 * Produces values of type {@link LocalDate}.
 */
public class LocalDateGenerator extends Generator<LocalDate> {
    private LocalDate min = LocalDate.MIN;
    private LocalDate max = LocalDate.MAX;

    public LocalDateGenerator() {
        super(LocalDate.class);
    }

    /**
     * <p>Tells this generator to produce values within a specified
     * {@linkplain InRange#min() minimum} and/or {@linkplain InRange#max()
     * maximum}, inclusive, with uniform distribution.</p>
     *
     * <p>If an endpoint of the range is not specified, the generator will use
     * dates with values of either {@link LocalDate#MIN} or
     * {@link LocalDate#MAX} as appropriate.</p>
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
            min = LocalDate.parse(range.min(), formatter);
        if (!defaultValueOf(InRange.class, "max").equals(range.max()))
            max = LocalDate.parse(range.max(), formatter);

        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                String.format("bad range, %s > %s", min, max));
        }
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(LocalDate.ofEpochDay(
            random.nextLong(min.toEpochDay(), max.toEpochDay())), classIdForType(LocalDate.class));
    }
}
