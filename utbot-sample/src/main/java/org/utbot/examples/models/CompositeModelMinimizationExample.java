package org.utbot.examples.models;

import org.utbot.api.mock.UtMock;
import org.utbot.examples.objects.WrappedInt;

public class CompositeModelMinimizationExample {
    public boolean singleNotNullArgumentInitializationRequired(WrappedInt a) {
        UtMock.assume(a != null);
        return a.getValue() == 1;
    }

    public boolean sameArgumentsInitializationRequired(WrappedInt a, WrappedInt b) {
        UtMock.assume(a == b);
        return b.getValue() == 1;
    }

    public boolean distinctNotNullArgumentsSecondInitializationNotExpected(WrappedInt a, WrappedInt b) {
        UtMock.assume(a != null && b != null && a != b);
        return a.getValue() == 1;
    }

    public boolean distinctNotNullArgumentsInitializationRequired(WrappedInt a, WrappedInt b) {
        UtMock.assume(a != null && b != null && a != b);
        return a.getValue() + 1 != b.getValue();
    }
}
