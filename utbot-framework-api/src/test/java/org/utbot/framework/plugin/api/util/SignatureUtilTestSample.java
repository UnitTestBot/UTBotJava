package org.utbot.framework.plugin.api.util;

import java.util.List;

@SuppressWarnings("unused")
public class SignatureUtilTestSample {
    public SignatureUtilTestSample() {
    }

    public SignatureUtilTestSample(int i, long l) {
    }

    public SignatureUtilTestSample(int[] f, long[][] d) {
    }

    public SignatureUtilTestSample(Object o, String s, List<Integer> list) {
    }

    public void oneByte(byte b) {
    }

    public void allPrimitives(byte b, short s, char c, int i, long l, float f, double d, boolean bool) {
    }

    public void allPrimitiveArrays(byte[] b, short[] s, char[] c, int[] i, long[] l, float[] f, double[] d, boolean[] bool) {
    }

    public void multiDimensional(byte[][] b, int[][][] i, long[][][][] l) {
    }

    public void stringArray(String[] a) {
    }

    public void oneObject(Object o) {
    }

    public void objectsAndCollections(Object o, String s, List<Integer> list) {
    }

    public long returnsPrimitive() {
        return 1L;
    }

    public float[][] returnsArray() {
        return new float[0][0];
    }

    public String returnsString() {
        return "";
    }
}