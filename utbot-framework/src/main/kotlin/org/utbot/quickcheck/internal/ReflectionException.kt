

package org.utbot.quickcheck.internal;

public class ReflectionException extends RuntimeException {
    private static final long serialVersionUID = Long.MIN_VALUE;

    public ReflectionException(String message) {
        super(message);
    }

    public ReflectionException(Throwable cause) {
        super(cause.toString());
    }
}
