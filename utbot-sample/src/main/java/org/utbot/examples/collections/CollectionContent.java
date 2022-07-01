package org.utbot.examples.collections;

import org.jetbrains.annotations.NotNull;
import org.utbot.api.mock.UtMock;

import java.util.LinkedList;
import java.util.List;

public class CollectionContent {
    public List<Boolean> copyBooleans(@NotNull List<Boolean> source) {
        if (source.isEmpty())
            return new LinkedList<>();
        for (Boolean value : source) {
            UtMock.assume(value != null);
        }
        return source;
    }

    public List<Boolean> copyBooleansExplicitLoop(@NotNull List<Boolean> source) {
        if (source.isEmpty())
            return new LinkedList<>();
        for (int i = 0; i < source.size(); i++) {
            UtMock.assume(source.get(i) != null);
        }
        return source;
    }
}

class Static {
    private static int x = foo();

    static int i = 0;

    static int[] cache = new int[]{1, 2, 3};

    static int foo() {
        return cache[i];
    }
}
