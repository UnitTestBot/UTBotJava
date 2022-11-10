

package org.utbot.quickcheck.generator.java.time;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.InRange;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.time.ZoneOffset;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.internal.Reflection.defaultValueOf;

/**
 * Produces values of type {@link ZoneOffset}.
 */
public class ZoneOffsetGenerator extends Generator<ZoneOffset> {
    /* The way ZoneOffsets work, ZoneOffset.MAX (-18:00) is actually
       the lower end of the seconds range, whereas ZoneOffset.MIN (+18:00)
       is the upper end. */
    private ZoneOffset min = ZoneOffset.MAX;
    private ZoneOffset max = ZoneOffset.MIN;

    public ZoneOffsetGenerator() {
        super(ZoneOffset.class);
    }

    /**
     * <p>Tells this generator to produce values within a specified
     * {@linkplain InRange#min() minimum} and/or {@linkplain InRange#max()
     * maximum}, inclusive, with uniform distribution.</p>
     *
     * <p>If an endpoint of the range is not specified, the generator will use
     * ZoneOffsets with values of either {@code ZoneOffset#MIN} or
     * {@code ZoneOffset#MAX} as appropriate.</p>
     *
     * <p>{@linkplain InRange#format()} is ignored. ZoneOffsets are always
     * parsed using their zone id.</p>
     *
     * @see ZoneOffset#of(String)
     * @param range annotation that gives the range's constraints
     */
    public void configure(InRange range) {
        if (!defaultValueOf(InRange.class, "min").equals(range.min()))
            min = ZoneOffset.of(range.min());
        if (!defaultValueOf(InRange.class, "max").equals(range.max()))
            max = ZoneOffset.of(range.max());

        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                String.format("bad range, %s > %s", min, max));
        }
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        int minSeconds = min.getTotalSeconds();
        int maxSeconds = max.getTotalSeconds();

        return UtModelGenerator.getUtModelConstructor().construct(ZoneOffset.ofTotalSeconds(
            (minSeconds <= maxSeconds)
                ? random.nextInt(minSeconds, maxSeconds)
                : random.nextInt(maxSeconds, minSeconds)), classIdForType(ZoneOffset.class));
    }
}
