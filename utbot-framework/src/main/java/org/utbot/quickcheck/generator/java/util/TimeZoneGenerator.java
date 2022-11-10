

package org.utbot.quickcheck.generator.java.util;
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;

import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.TimeZone;

import static java.util.TimeZone.getAvailableIDs;
import static java.util.TimeZone.getTimeZone;
import static org.utbot.external.api.UtModelFactoryKt.classIdForType;

/**
 * Produces values of type {@link TimeZone}.
 */
public class TimeZoneGenerator extends Generator<TimeZone> {
    private static final String[] AVAILABLE_IDS = getAvailableIDs();

    public TimeZoneGenerator() {
        super(TimeZone.class);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(getTimeZone(random.choose(AVAILABLE_IDS)), classIdForType(TimeZone.class));
    }
}
