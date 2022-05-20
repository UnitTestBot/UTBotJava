package org.utbot.examples.controlflow;

public class CycleDependedCondition {

    public int oneCondition(int x) {
        for (int i = 0; i < x; i++) {
            if (i == 2) {
                return 1;
            }
        }
        return 0;
    }

    public int twoCondition(int x) {
        for (int i = 0; i < x; i++) {
            if (i > 2 && x == 4) {
                return 1;
            }
        }
        return 0;
    }

    public int threeCondition(int x) {
        for (int i = 0; i < x; i++) {
            if (i > 4 && i < 6 && x != 7) {
                return 1;
            }
        }
        return 0;
    }

    public int oneConditionHigherNumber(int x) {
        for (int i = 0; i < x; i++) {
            if (i == 100) {
                return 1;
            }
        }
        return 0;
    }
}
