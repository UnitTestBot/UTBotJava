

package org.utbot.quickcheck.generator.java.util;
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;

import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.util.Locale;

import static java.util.Locale.getAvailableLocales;
import static org.utbot.external.api.UtModelFactoryKt.classIdForType;

/**
 * Produces values of type {@link Locale}.
 */
public class LocaleGenerator extends Generator<Locale> {
    private static final Locale[] AVAILABLE_LOCALES = getAvailableLocales();

    public LocaleGenerator() {
        super(Locale.class);
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {

        return UtModelGenerator.getUtModelConstructor().construct(random.choose(AVAILABLE_LOCALES), classIdForType(Locale.class));
    }
}
