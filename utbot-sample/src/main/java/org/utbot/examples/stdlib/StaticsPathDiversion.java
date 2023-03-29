package org.utbot.examples.stdlib;

import org.utbot.api.mock.UtMock;

import java.io.File;

public class StaticsPathDiversion {
    @SuppressWarnings("RedundantIfStatement")
    public boolean separatorEquality(String s) {
        // Ignore this case to make sure we will have not more than 2 executions even without minimization
        UtMock.assume(s != null);

        // We use if-else here instead of a simple return to get executions for both return values - true and false
        if (File.separator.equals(s)) {
            return true;
        } else {
            return false;
        }
    }
}
