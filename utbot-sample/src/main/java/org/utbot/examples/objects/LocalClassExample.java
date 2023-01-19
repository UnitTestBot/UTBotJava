package org.utbot.examples.objects;

public class LocalClassExample {
    int localClassFieldExample(int y) {
        class LocalClass {
            final int x;

            public LocalClass(int x) {
                this.x = x;
            }
        }

        LocalClass localClass = new LocalClass(42);

        return localClass.x + y;
    }
}
