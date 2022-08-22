package org.utbot.examples.arrays;

import org.utbot.api.mock.UtMock;

import java.util.Arrays;

public class IntArrayBasics {
    public int[] intArrayWithAssumeOrExecuteConcretely(int x, int n) {
        UtMock.assumeOrExecuteConcretely(n > 30);

        if (x > 0) {
            if (n < 20) {
                return new int[2];
            } else {
                return new int[4];
            }
        } else {
            if (n < 20) {
                return new int[10];
            } else {
                return new int[20];
            }
        }
    }

    public int[] initAnArray(int n) {
        int[] a = new int[n];
        a[n - 1] = n - 1;
        a[n - 2] = n - 2;
        return a;
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean isValid(int[] a, int n) {
        if ((a[n] == 9) && (n == 5)) {
            return true;
        }
        if ((a[n] > 9) && (n == 5))
            return true;
        else
            return false;
    }

    public int getValue(int[] a, int n) {
        if (a.length > 6) {
            return a[n];
        }
        return -1;
    }

    public int setValue(int[] a, int x) {
        if (x > 0 && a.length > 0) {
            a[0] = x;
            if (a[0] > 2) {
                return 2;
            } else {
                return 1;
            }
        }
        return 0;
    }

    public int checkFour(int[] a) {
        if (a.length < 4) {
            return -1;
        }
        if (a[0] == 1 && a[1] == 2 && a[2] == 3 && a[3] == 4) {
            return a[0] + a[1] + a[2] + a[3];
        }
        return 0;
    }

    public int nullability(int[] a) {
        if (a == null) {
            return 1;
        }
        if (a.length > 1) {
            return 2;
        }
        return 3;
    }

    public int equality(int[] a, int[] b) {
        if (a == null || null == b) {
            return 1;
        }
        if (a.length == b.length) {
            return 2;
        }
        return 3;
    }

    // overwrite
    public int overwrite(int[] a, int b) {
        if (a.length != 1) {
            return 0;
        }
        if (a[0] > 0) {
            a[0] = b;
            if (a[0] < 0) {
                return 1;
            }
            return 2;
        }
        return 3;
    }

    public int[] mergeArrays(int[] fst, int[] snd) {

        int fstLength = fst.length;
        int sndLength = snd.length;

        if (fstLength < 2 || sndLength < 2) {
            return null;
        }

        int fstIndex = 0;
        int sndIndex = 0;
        int length = fstLength + sndLength;

        int[] result = new int[length];

        for (int i = 0; i < length; i++) {
            if (fstIndex == fstLength) {
                result[fstIndex + sndIndex] = snd[sndIndex++];
            } else if (sndIndex == sndLength) {
                result[fstIndex + sndIndex] = fst[fstIndex++];
            } else if (fst[fstIndex] < snd[sndIndex]) {
                result[fstIndex + sndIndex] = fst[fstIndex++];
            } else {
                result[fstIndex + sndIndex] = snd[sndIndex++];
            }
        }

        return result;
    }

    public int[] newArrayInTheMiddle(int[] array) {
        array[0] = 1;
        array[1] = 2;
        array[2] = 3;

        int[] resultArray = new int[3];
        resultArray[0] = array[0];
        resultArray[1] = array[1];
        resultArray[2] = array[2];

        return resultArray;
    }

    public int[] reversed(int[] array) {
        if (array.length != 3) {
            throw new IllegalArgumentException();
        }

        if (array[0] <= array[1] || array[1] <= array[2]) {
            return null;
        }

        int[] copiedArray = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            copiedArray[array.length - i - 1] = array[i];
        }

        return copiedArray;
    }

    public int[] updateCloned(int[] a) {
        if (a.length != 3) {
            throw new IllegalArgumentException();
        }

        int[] b = a.clone();
        for (int i = 0; i < b.length; i++) {
            b[i] *= 2;
        }
        return b;
    }

    public int arrayEqualsExample(int[] arr) {
        boolean a = Arrays.equals(arr, new int[]{1, 2, 3});
        if (a) {
            return 1;
        } else {
            return 2;
        }
    }

}