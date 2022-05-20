package org.utbot.examples.manual.examples;

import org.utbot.examples.assemble.DirectAccess;

public class DirectAccessExample {
    int foo(DirectAccess directAccess) {
        directAccess.setA(42);
        return 15;
    }
}
