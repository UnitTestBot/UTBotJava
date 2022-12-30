package org.utbot.fuzzing.samples.data;

@SuppressWarnings("unused")
public class UserDefinedClass {
    public int x;
    private int y;
    public int z;

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public UserDefinedClass setY(int y) {
        this.y = y;
        return this;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public int getZ() {
        return z;
    }

    public UserDefinedClass() {
    }

    public UserDefinedClass(int x, int y) {
        this.x = x;
        this.y = y;
        this.z = 0;
    }

    public UserDefinedClass(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
