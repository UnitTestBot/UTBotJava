package org.utbot.fuzzing.samples;

import java.util.List;

@SuppressWarnings("unused")
public class FailToGenerateListGeneric {

    interface Something {}

    int func(List<Something> x) {
        return x.size();
    }

}
