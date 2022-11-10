

package org.utbot.quickcheck.generator.java.time;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.concrete.UtModelConstructor;
import org.utbot.framework.plugin.api.*;
import org.utbot.framework.plugin.api.util.IdUtilKt;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.InRange;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.internal.Reflection.defaultValueOf;

/**
 * Produces values of type {@link Clock}.
 */
public class ClockGenerator extends Generator<Clock> {
    private static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

    private Instant min = Instant.MIN;
    private Instant max = Instant.MAX;

    public ClockGenerator() {
        super(Clock.class);
    }

    /**
     * <p>Tells this generator to produce values within a specified
     * {@linkplain InRange#min() minimum} and/or {@linkplain InRange#max()
     * maximum}, inclusive, with uniform distribution, down to the
     * nanosecond.</p>
     *
     * <p>Instances of this class are configured using {@link Instant}
     * strings.</p>
     *
     * <p>If an endpoint of the range is not specified, the generator will use
     * instants with values of either {@link Instant#MIN} or
     * {@link Instant#MAX} as appropriate.</p>
     *
     * <p>{@linkplain InRange#format()} is ignored. Instants are always
     * parsed using {@link java.time.format.DateTimeFormatter#ISO_INSTANT}.</p>
     *
     * @param range annotation that gives the range's constraints
     */
    public void configure(InRange range) {
        if (!defaultValueOf(InRange.class, "min").equals(range.min()))
            min = Instant.parse(range.min());
        if (!defaultValueOf(InRange.class, "max").equals(range.max()))
            max = Instant.parse(range.max());

        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                    String.format("bad range, %s > %s", min, max));
        }
    }

    @Override
    public UtModel generate(
            SourceOfRandomness random,
            GenerationStatus status) {

        final Instant instant = random.nextInstant(min, max);
        final ZoneId zoneId = UTC_ZONE_ID;

        final Method clockFixed;
        try {
            clockFixed = Clock.class.getMethod("fixed", Instant.class, ZoneId.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        final UtModelConstructor modelConstructor = UtModelGenerator.getUtModelConstructor();
        final UtModel instantModel = modelConstructor.construct(instant, classIdForType(Instant.class));
        final UtModel zoneIdModel = modelConstructor.construct(zoneId, classIdForType(ZoneId.class));

        final ClassId classId = classIdForType(Clock.class);
        final ExecutableId constructorId = IdUtilKt.getExecutableId(clockFixed);

        final int modelId = modelConstructor.computeUnusedIdAndUpdate();

        return new UtAssembleModel(
                modelId,
                classId,
                constructorId.getName() + "#" + modelId,
                new UtExecutableCallModel(null, constructorId, List.of(instantModel, zoneIdModel)),
                null,
                (a) -> List.of()
        );
    }
}
