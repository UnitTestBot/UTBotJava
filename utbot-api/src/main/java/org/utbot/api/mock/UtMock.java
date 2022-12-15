package org.utbot.api.mock;

import org.utbot.api.exception.UtMockAssumptionViolatedException;

public class UtMock {
    public static <T> T makeSymbolic() {
        return makeSymbolic(false);
    }

    @SuppressWarnings("unused")
    public static <T> T makeSymbolic(boolean isNullable) {
        return null;
    }

    @SuppressWarnings("unused")
    public static void assume(boolean predicate) {
        // to use compilers checks, i.e. for possible NPE
        if (!predicate) {
            throw new UtMockAssumptionViolatedException();
        }
    }

    @SuppressWarnings("unused")
    public static void assumeOrExecuteConcretely(boolean predicate) {
        // In oppose to assume, we don't have predicate check here
        // to avoid UtMockAssumptionViolatedException during concrete execution
    }

    @SuppressWarnings("unused")
    public static void disableClassCastExceptionCheck(Object object) {}
}