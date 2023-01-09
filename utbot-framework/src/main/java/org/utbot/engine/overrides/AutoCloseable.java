package org.utbot.engine.overrides;

import org.utbot.api.annotation.UtClassMock;

@UtClassMock(target = java.lang.AutoCloseable.class, internalUsage = true)
public interface AutoCloseable {
    default void close() throws Exception {
        // Do nothing
    }
}
