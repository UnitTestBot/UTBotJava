



package org.utbot.quickcheck.generator.java.util;
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;

import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.generator.InRange;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.utbot.external.api.UtModelFactoryKt.classIdForType;
import static org.utbot.quickcheck.internal.Reflection.defaultValueOf;

/**
 * Produces values of type {@link Date}.
 */
public class DateGenerator extends Generator<Date> {
    private Date min = new Date(Integer.MIN_VALUE);
    private Date max = new Date(Long.MAX_VALUE);

    public DateGenerator() {
        super(Date.class);
    }

    /**
     * <p>Tells this generator to produce values within a specified
     * {@linkplain InRange#min() minimum} and/or {@linkplain InRange#max()
     * maximum}, inclusive, with uniform distribution, down to the
     * millisecond.</p>
     *
     * <p>If an endpoint of the range is not specified, the generator will use
     * dates with milliseconds-since-the-epoch values of either
     * {@link Integer#MIN_VALUE} or {@link Long#MAX_VALUE} as appropriate.</p>
     *
     * <p>{@link InRange#format()} describes
     * {@linkplain SimpleDateFormat#parse(String) how the generator is to
     * interpret the range's endpoints}.</p>
     *
     * @param range annotation that gives the range's constraints
     */
    public void configure(InRange range) {
        SimpleDateFormat formatter = new SimpleDateFormat(range.format());
        formatter.setLenient(false);

        try {
            if (!defaultValueOf(InRange.class, "min").equals(range.min()))
                min = formatter.parse(range.min());
            if (!defaultValueOf(InRange.class, "max").equals(range.max()))
                max = formatter.parse(range.max());
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }

        if (min.getTime() > max.getTime()) {
            throw new IllegalArgumentException(
                String.format("bad range, %s > %s", min, max));
        }
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(new Date(random.nextLong(min.getTime(), max.getTime())), classIdForType(Date.class));
    }
}
