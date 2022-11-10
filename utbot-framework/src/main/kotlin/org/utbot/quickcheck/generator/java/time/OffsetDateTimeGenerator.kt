

package org.utbot.quickcheck.generator.java.time;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.InRange;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.internal.Reflection.defaultValueOf;

/**
 * Produces values of type {@link OffsetDateTime}.
 */
public class OffsetDateTimeGenerator extends Generator<OffsetDateTime> {
    private static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

    private OffsetDateTime min = OffsetDateTime.MIN;
    private OffsetDateTime max = OffsetDateTime.MAX;

    public OffsetDateTimeGenerator() {
        super(OffsetDateTime.class);
    }

    /**
     * <p>Tells this generator to produce values within a specified
     * {@linkplain InRange#min() minimum} and/or {@linkplain InRange#max()
     * maximum}, inclusive, with uniform distribution, down to the
     * nanosecond.</p>
     *
     * <p>If an endpoint of the range is not specified, the generator will use
     * dates with values of either {@link OffsetDateTime#MIN} or
     * {@link OffsetDateTime#MAX} as appropriate.</p>
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
            min = OffsetDateTime.parse(range.min(), formatter);
        if (!defaultValueOf(InRange.class, "max").equals(range.max()))
            max = OffsetDateTime.parse(range.max(), formatter);

        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                String.format("bad range, %s > %s", min, max));
        }
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        // Project the OffsetDateTime to an Instant for easy long-based generation.
        return UtModelGenerator.getUtModelConstructor().construct(OffsetDateTime.ofInstant(
            random.nextInstant(min.toInstant(), max.toInstant()),
            UTC_ZONE_ID), classIdForType(OffsetDateTime.class));
    }
}
