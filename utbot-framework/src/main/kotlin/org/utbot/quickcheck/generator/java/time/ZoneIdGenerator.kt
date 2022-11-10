

package org.utbot.quickcheck.generator.java.time;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.time.ZoneId;

import static java.time.ZoneId.getAvailableZoneIds;
import static org.utbot.external.api.UtModelFactoryKt.classIdForType;

/**
 * Produces values of type {@link ZoneId}.
 */
public class ZoneIdGenerator extends Generator<ZoneId> {
    public ZoneIdGenerator() {
        super(ZoneId.class);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(ZoneId.of(random.choose(getAvailableZoneIds())), classIdForType(ZoneId.class));
    }
}
