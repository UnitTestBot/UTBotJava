

package org.utbot.quickcheck.generator.java.time;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.InRange;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.internal.Reflection.defaultValueOf;

/**
 * Produces values of type {@link LocalDateTime}.
 */
public class LocalDateTimeGenerator extends Generator<LocalDateTime> {
    private static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

    private LocalDateTime min = LocalDateTime.MIN;
    private LocalDateTime max = LocalDateTime.MAX;

    public LocalDateTimeGenerator() {
        super(LocalDateTime.class);
    }

    /**
     * <p>Tells this generator to produce values within a specified
     * {@linkplain InRange#min() minimum} and/or {@linkplain InRange#max()
     * maximum}, inclusive, with uniform distribution, down to the
     * nanosecond.</p>
     *
     * <p>If an endpoint of the range is not specified, the generator will use
     * dates with values of either {@link LocalDateTime#MIN} or
     * {@link LocalDateTime#MAX} as appropriate.</p>
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
            min = LocalDateTime.parse(range.min(), formatter);
        if (!defaultValueOf(InRange.class, "max").equals(range.max()))
            max = LocalDateTime.parse(range.max(), formatter);

        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                String.format("bad range, %s > %s", min, max));
        }
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        /* Project the LocalDateTime to an Instant for easy long-based generation.
           Any zone id will do as long as we use the same one throughout. */
        return UtModelGenerator.getUtModelConstructor().construct(LocalDateTime.ofInstant(
            random.nextInstant(
                min.atZone(UTC_ZONE_ID).toInstant(),
                max.atZone(UTC_ZONE_ID).toInstant()),
            UTC_ZONE_ID), classIdForType(LocalDateTime.class));
    }
}
