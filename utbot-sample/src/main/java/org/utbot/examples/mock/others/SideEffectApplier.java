package org.utbot.examples.mock.others;

import org.utbot.examples.mock.service.impl.ExampleClass;

public class SideEffectApplier {
    public void applySideEffect(ExampleClass a) {
        a.field += 1;
    }
}
