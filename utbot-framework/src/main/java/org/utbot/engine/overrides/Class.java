package org.utbot.engine.overrides;

import org.utbot.api.annotation.UtClassMock;

@UtClassMock(target = java.lang.Class.class, internalUsage = true)
public class Class {
    public static boolean desiredAssertionStatus() {
        return true;
    }
}
