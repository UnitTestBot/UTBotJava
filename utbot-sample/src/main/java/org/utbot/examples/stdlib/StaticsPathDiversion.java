package org.utbot.examples.stdlib;

import org.utbot.api.mock.UtMock;

import java.io.File;

public class StaticsPathDiversion {
    @SuppressWarnings({"IfStatementWithIdenticalBranches"})
    // In this test we check that the symbolic engine does not change the static field `File.separator`
    public String separatorEquality(String s) {
        // Ignore this case to make sure we will have not more than 2 executions even without minimization
        UtMock.assume(s != null);

        // We use if-else here instead of a simple return to get executions for both return values
        if (File.separator.equals(s)) {
            return File.separator;
        } else {
            return File.separator;
        }
    }
}
