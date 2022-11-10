

package org.utbot.quickcheck.generator.java.lang;

import org.utbot.quickcheck.generator.GeneratorConfiguration;
import org.utbot.quickcheck.generator.java.lang.strings.CodePoints;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.charset.Charset;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>Produces {@link String}s whose code points correspond to code points in
 * a given {@link Charset}
 * ({@link Charset#defaultCharset() by default}).</p>
 */
public class Encoded extends AbstractStringGenerator {
    private CodePoints charsetPoints;

    public Encoded() {
        initialize(Charset.defaultCharset());
    }

    /**
     * Tells this generator to emit strings in the given charset.
     *
     * @param charset a charset to use as the source for characters of
     * generated strings
     */
    public void configure(InCharset charset) {
        initialize(Charset.forName(charset.value()));
    }

    private void initialize(Charset charset) {
        charsetPoints = CodePoints.forCharset(charset);
    }

    @Override protected int nextCodePoint(SourceOfRandomness random) {
        return charsetPoints.at(random.nextInt(0, charsetPoints.size() - 1));
    }

    /**
     * Names a {@link Charset}.
     */
    @Target({ PARAMETER, FIELD, ANNOTATION_TYPE, TYPE_USE })
    @Retention(RUNTIME)
    @GeneratorConfiguration
    public @interface InCharset {
        String value();
    }
}
