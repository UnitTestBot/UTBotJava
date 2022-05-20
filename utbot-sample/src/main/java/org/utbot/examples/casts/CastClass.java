package org.utbot.examples.casts;

import org.utbot.api.mock.UtMock;

public class CastClass {
    int x;
    int defaultValue = 5;

    int foo() {
        return defaultValue;
    }

    CastClass castToInheritor() {
        UtMock.assume(this instanceof CastClassFirstSucc);

        return this;
    }
}
