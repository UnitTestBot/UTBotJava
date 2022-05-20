package org.utbot.examples.objects;

public class ObjectWithFinalStatic {
    public final static Integer keyValue = 420;

    public int parameterEqualsFinalStatic(Integer key, int value) {
        if (key == keyValue) {
            return value;
        }
        return -420;
    }
}
