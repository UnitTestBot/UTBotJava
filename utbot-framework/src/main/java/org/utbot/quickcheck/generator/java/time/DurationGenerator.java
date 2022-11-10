

package org.utbot.quickcheck.generator.java.time;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.InRange;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.time.Duration;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.internal.Reflection.defaultValueOf;

/**
 * Produces values of type {@link Duration}.
 */
public class DurationGenerator extends Generator<Duration> {
    private Duration min = Duration.ofSeconds(Long.MIN_VALUE, 0);
    private Duration max = Duration.ofSeconds(Long.MAX_VALUE, 999_999_999);

    public DurationGenerator() {
        super(Duration.class);
    }

    /**
     * <p>Tells this generator to produce values within a specified
     * {@linkplain InRange#min() minimum} and/or {@linkplain InRange#max()
     * maximum}, inclusive, with uniform distribution, down to the
     * nanosecond.</p>
     *
     * <p>If an endpoint of the range is not specified, the generator will use
     * durations with second values of either {@link Long#MIN_VALUE} or
     * {@link Long#MAX_VALUE} (with nanoseconds set to 999,999,999) as
     * appropriate.</p>
     *
     * <p>{@linkplain InRange#format()} is ignored. Durations are always
     * parsed using formats based on the ISO-8601 duration format
     * {@code PnDTnHnMn.nS} with days considered to be exactly 24 hours.
     *
     * @see Duration#parse(CharSequence)
     * @param range annotation that gives the range's constraints
     */
    public void configure(InRange range) {
        if (!defaultValueOf(InRange.class, "min").equals(range.min()))
            min = Duration.parse(range.min());
        if (!defaultValueOf(InRange.class, "max").equals(range.max()))
            max = Duration.parse(range.max());

        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                String.format("bad range, %s > %s", min, max));
        }
    }

    @Override public UtModel generate(SourceOfRandomness random, GenerationStatus status) {
        return UtModelGenerator.getUtModelConstructor().construct(random.nextDuration(min, max), classIdForType(Duration.class));
    }
}
