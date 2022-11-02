package org.utbot.fuzzing.samples;

import org.utbot.fuzzing.samples.data.Recursive;
import org.utbot.fuzzing.samples.data.UserDefinedClass;

@SuppressWarnings({"unused", "RedundantIfStatement"})
public class Objects {

    public int test(UserDefinedClass userDefinedClass) {
        if (userDefinedClass.getX() > 5 && userDefinedClass.getY() < 10) {
            return 1;
        } else if (userDefinedClass.getZ() < 20) {
            return 2;
        } else {
            return 3;
        }
    }

    public boolean testMe(Recursive r) {
        if (r.val() > 10) {
            return true;
        }
        return false;
    }

}
