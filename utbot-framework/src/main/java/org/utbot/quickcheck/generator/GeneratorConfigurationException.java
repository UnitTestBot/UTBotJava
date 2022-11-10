
package org.utbot.quickcheck.generator;

/**
 * Raised if a problem arises when attempting to configure a generator with
 * annotations from a property parameter.
 *
 * @see Generator#configure(java.lang.reflect.AnnotatedType)
 */
public class GeneratorConfigurationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public GeneratorConfigurationException(String message) {
        super(message);
    }

    public GeneratorConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
