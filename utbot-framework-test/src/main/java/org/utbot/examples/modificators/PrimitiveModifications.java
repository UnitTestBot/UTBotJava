package org.utbot.examples.modificators;

import java.util.Arrays;

/**
 * A class with primitive field modification scenarios:
 * - constructor
 * - public method
 * - private method with public ancestor
 * - static and std call result
 * - method with exception
 */
public class PrimitiveModifications {
    protected int x, y, z, t;

    public PrimitiveModifications(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public PrimitiveModifications(int x, int y, int z) {
        this(x, y);
        this.z = z;
    }

    public void setOne() {
        z = 1;
    }

    public void setSeveral() {
        z = 1;
        x = 1;
    }

    public void setWithPrivateCall() {
        x = 1;
        setPrivateFromPublic();
    }

    public void setWithPrivateCallsHierarchy() {
        x = 1;
        setPrivateCallsPrivatesFromPublic();
    }

    public void setWithStdCall() {
        int[] arr = new int[]{x, y, z, t};
        Arrays.sort(arr);

        x = arr[0];
    }

    public void setCallResult() {
        int[] nums = {x, y, z, t};
        x = run(nums);
    }

    public void setStaticCallResult() {
        x = 1;
        y = runStatic();
    }

    public void setAndThrow(int a) {
        if (a < 0) {
            throw new IllegalArgumentException();
        }
        z = 1;
    }

    private void setPrivateCallsPrivatesFromPublic() {
        setPrivateFromPublic();
        setPrivateFromPrivate();
    }

    private void setPrivateFromPrivate() {
        z = 1;
    }

    private void setPrivateFromPublic() {
        t = 1;
    }

    private void setPrivately() {
        t = 1;
    }

    private static int runStatic() {
        return 0;
    }

    private int run(int[] numbers) {
        return numbers[0];
    }
}

