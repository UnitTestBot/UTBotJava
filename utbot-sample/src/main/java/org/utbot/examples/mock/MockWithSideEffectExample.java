package org.utbot.examples.mock;

import org.utbot.examples.mock.others.SideEffectApplier;
import org.utbot.examples.mock.service.impl.ExampleClass;
import javax.validation.constraints.NotNull;

public class MockWithSideEffectExample {
    SideEffectApplier applier;

    @SuppressWarnings({"RedundantIfStatement"})
    boolean checkSideEffect(int x) {
        ExampleClass a = new ExampleClass(x);
        applier.applySideEffect(a);
        if (a.field != x) {
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings({"RedundantIfStatement"})
    boolean checkSideEffectElimination(@NotNull ExampleClass a) {
        a.field = 1;
        a.incrementField();
        if (a.field == 1) {
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings({"RedundantIfStatement"})
    boolean checkStaticMethodSideEffectElimination(@NotNull ExampleClass a) {
        a.field = 1;
        ExampleClass.staticIncrementField(a);
        if (a.field == 1) {
            return true;
        } else {
            return false;
        }
    }
}
