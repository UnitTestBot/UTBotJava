package org.utbot.examples.objects;

public class SimpleClassMultiInstanceExample {
    public int singleObjectChange(SimpleDataClass first, SimpleDataClass second) {
        if (first.a < 5) {
            first.b = 3;
        }

        return first.b;
    }
}
