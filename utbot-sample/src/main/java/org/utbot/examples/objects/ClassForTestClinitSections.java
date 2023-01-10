package org.utbot.examples.objects;

public class ClassForTestClinitSections {
    private static int x = 5;

    public int resultDependingOnStaticSection() {
        if (x == 5) {
            return -1;
        }

        return 1;
    }
}
