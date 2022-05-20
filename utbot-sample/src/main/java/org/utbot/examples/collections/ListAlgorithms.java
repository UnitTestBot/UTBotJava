package org.utbot.examples.collections;

import java.util.List;

import static org.utbot.api.mock.UtMock.assume;

public class ListAlgorithms {

    public List<Integer> mergeListsInplace(List<Integer> a, List<Integer> b) {
        // invariant that lists are non-null and sorted
        assume(a != null);
        assume(b != null);
        assume(a.size() > 0 && a.size() < 3);
        assume(a.get(0) != null);
        for (int i = 0; i < a.size() - 1; i++) {
            assume(a.get(i + 1) != null);
            assume(a.get(i) < a.get(i + 1));
        }
        assume(b.size() > 0 && b.size() < 3);
        assume(b.get(0) != null);
        for (int i = 0; i < b.size() - 1; i++) {
            assume(b.get(i + 1) != null);
            assume(b.get(i) < b.get(i + 1));
        }
        int i = 0;
        int j = 0;
        while (i != a.size() || j != b.size()) {
            if (i == a.size()) {
                a.add(i++, b.get(j++));
            } else if (j == b.size() || a.get(i) < b.get(j)) {
                i++;
            } else {
                a.add(i++, b.get(j++));
            }
        }
        return a;
    }


}
