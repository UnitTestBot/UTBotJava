package org.utbot.examples.arrays;

import org.utbot.api.mock.UtMock;
import org.utbot.examples.casts.ColoredPoint;
import org.utbot.examples.casts.Point;

public class ArrayOfArrays {
    public int[][][] defaultValues() {
        int[][][] array = new int[1][][];
        //noinspection IfStatementWithIdenticalBranches
        if (array[0] == null) {
            return array;
        }

        return array;
    }

    public int[][] sizesWithoutTouchingTheElements() {
        int[][] array = new int[10][3];
        for (int i = 0; i < 2; i++) {
            int a = array[i][0];
        }
        return array;
    }

    public int[][][][] defaultValuesWithoutLastDimension() {
        int[][][][] array = new int[4][4][4][];

        //noinspection IfStatementWithIdenticalBranches
        if (array[0][0][0] != null) {
            return array;
        }

        return array;
    }

    public int[][][][] createNewMultiDimensionalArray(int i, int j) {
        return new int[2][i][j][3];
    }

    public int[][][][] defaultValuesWithoutTwoDimensions(int i) {
        if (i < 2) {
            return null;
        }

        int[][][][] array = new int[i][i][][];

        //noinspection IfStatementWithIdenticalBranches
        if (array[0][0] != null) {
            return array;
        }

        return array;
    }

    public int[][][] defaultValuesNewMultiArray() {
        int[][][] array = new int[1][1][1];
        //noinspection IfStatementWithIdenticalBranches
        if (array[0] == null) {
            return array;
        }

        return array;
    }

    public int[][] simpleExample(int[][] matrix) {
        UtMock.assume(matrix != null && matrix.length > 2);
        UtMock.assume(matrix[0] != null && matrix[1] != null && matrix[2] != null);

        // TODO("This condition could be removed, but we'll get matrix with the same arrays inside")
        if (matrix[1] == matrix[2] || matrix[0] == matrix[2]) {
            return null;
        }
        if (matrix[1][1] == 1) {
            matrix[2][2] = 2;
        } else {
            matrix[2][2] = -2;
        }

        return matrix;
    }

    public boolean isIdentityMatrix(int[][] matrix) {
        if (matrix.length < 3) {
            throw new IllegalArgumentException("matrix.length < 3");
        }

        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i].length != matrix.length) {
                return false;
            }
            for (int j = 0; j < matrix[i].length; j++) {
                if (i == j && matrix[i][j] != 1) {
                    return false;
                }

                if (i != j && matrix[i][j] != 0) {
                    return false;
                }
            }
        }

        return true;
    }

    public int[][][] createNewThreeDimensionalArray(int length, int constValue) {
        if (length != 2) {
            return new int[0][][];
        }
        int[][][] matrix = new int[length][length][length];
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                for (int k = 0; k < length; k++) {
                    matrix[i][j][k] = constValue + 1;
                }
            }
        }

        return matrix;
    }

    public int[][][] reallyMultiDimensionalArray(int[][][] array) {
        if (array[1][2][3] != 12345) {
            array[1][2][3] = 12345;
        } else {
            array[1][2][3] -= 12345 * 2;
        }
        return array;
    }

    public Point[][] multiDimensionalObjectsArray(Point[][] array) {
        array[0] = new ColoredPoint[2];
        array[1] = new Point[1];
        return array;
    }

    public int[][] fillMultiArrayWithArray(int[] value) {
        if (value.length < 2) {
            return new int[0][];
        }

        for (int i = 0; i < value.length; i++) {
            value[i] += i;
        }

        int length = 3;
        int[][] array = new int[length][value.length];

        for (int i = 0; i < length; i++) {
            array[i] = value;
        }

        return array;
    }

    public Object[] arrayWithItselfAnAsElement(Object[] array) {
        UtMock.assume(array != null && array.length > 0);

        if (array[0] == array) {
            return array;
        }

        return null;
    }
}
