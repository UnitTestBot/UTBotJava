package org.utbot.examples.arrays;

public class ObjectWithPrimitivesClassSucc extends ObjectWithPrimitivesClass {
    public int anotherX; //, y; TODO: hidden field disabled, we do not support it yet

    public ObjectWithPrimitivesClassSucc() {
    }

    public ObjectWithPrimitivesClassSucc(int x, int y, double weight, int anotherX) {
        super(x, y, weight);
        this.anotherX = anotherX;
    }
}
