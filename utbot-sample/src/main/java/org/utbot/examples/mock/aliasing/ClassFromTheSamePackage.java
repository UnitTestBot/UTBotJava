package org.utbot.examples.mock.aliasing;

import org.utbot.examples.mock.aliasing.parent.InterfaceFromAnotherPackage;

public class ClassFromTheSamePackage implements InterfaceFromAnotherPackage  {
    @Override
    public int foo(int x) {
        return x;
    }
}
