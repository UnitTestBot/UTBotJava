package org.utbot.examples.collections;

import java.util.List;

public class ListWrapperReturnsVoidExample {
    public int runForEach(List<Object> list) {
        final int[] i = {0};
        list.forEach(o -> {
            if (o == null) i[0]++;
        });
        return i[0];
    }

    public int sumPositiveForEach(List<Integer> list) {
        final int[] sum = {0};
        list.forEach(i -> {
            if (i > 0) {
                sum[0] += i;
            }
        });
        if (sum[0] == 0) {
            return 0;
        } else {
            return sum[0];
        }
    }
}