package org.utbot.examples.arrays;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class ArrayStoreExceptionExamples {

    public boolean correctAssignmentSamePrimitiveType(int[] data) {
        if (data == null || data.length == 0) return false;
        data[0] = 1;
        return true;
    }

    public boolean correctAssignmentIntToIntegerArray(Integer[] data) {
        if (data == null || data.length == 0) return false;
        data[0] = 1;
        return true;
    }

    public boolean correctAssignmentSubtype(Number[] data) {
        if (data == null || data.length == 0) return false;
        data[0] = 15;
        return true;
    }

    public boolean correctAssignmentToObjectArray(Object[] data) {
        if (data == null || data.length < 2) return false;
        data[0] = 1;
        data[1] = new ArrayList<Integer>();
        return true;
    }

    public void wrongAssignmentUnrelatedType(Integer[] data) {
        if (data == null || data.length == 0) return;
        ((Object[]) data)[0] = "x";
    }

    public void wrongAssignmentSupertype(Integer[] data) {
        if (data == null || data.length == 0) return;
        Number x = 1.2;
        ((Number[]) data)[0] = x;
    }

    public void checkGenericAssignmentWithCorrectCast() {
        Number[] data = new Number[3];
        genericAssignmentWithCast(data, 5);
    }

    public void checkGenericAssignmentWithWrongCast() {
        Number[] data = new Number[3];
        genericAssignmentWithCast(data, "x");
    }

    public void checkGenericAssignmentWithExtendsSubtype() {
        Number[] data = new Number[3];
        genericAssignmentWithExtends(data, 7);
    }

    public void checkGenericAssignmentWithExtendsUnrelated() {
        Number[] data = new Number[3];
        genericAssignmentWithExtends(data, "x");
    }

    public void checkObjectAssignment() {
        Object[] data = new Object[3];
        data[0] = "x";
    }

    public void checkAssignmentToObjectArray() {
        Object[] data = new Object[3];
        data[0] = 1;
        data[1] = "a";
        data[2] = data;
    }

    public void checkWrongAssignmentOfItself() {
        Number[] data = new Number[2];
        genericAssignmentWithCast(data, data);
    }

    public void checkGoodAssignmentOfItself() {
        Object[] data = new Object[2];
        genericAssignmentWithCast(data, data);
    }

    public int[] arrayCopyForIncompatiblePrimitiveTypes(long[] data) {
        if (data == null)
            return null;

        int[] result = new int[data.length];
        if (data.length != 0) {
            System.arraycopy(data, 0, result, 0, data.length);
        }

        return result;
    }

    public int[][] fill2DPrimitiveArray() {
        int[][] data = new int[2][3];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                data[i][j] = i * 3 + j;
            }
        }
        return data;
    }

    public Object[] fillObjectArrayWithList(List<Integer> list) {
        if (list == null)
            return null;

        Object[] result = new Object[1];
        result[0] = list;
        return result;
    }

    public Object[] fillWithTreeSet(TreeSet<Integer> treeSet) {
        if (treeSet == null)
            return null;

        Object[] result = new Object[1];
        result[0] = treeSet;
        return result;
    }

    public SomeInterface[] fillSomeInterfaceArrayWithSomeInterface(SomeInterface impl) {
        if (impl == null)
            return null;

        SomeInterface[] result = new SomeInterface[1];
        result[0] = impl;
        return result;
    }

    public Object[] fillObjectArrayWithSomeInterface(SomeInterface impl) {
        if (impl == null)
            return null;

        Object[] result = new Object[1];
        result[0] = impl;
        return result;
    }

    public Object[] fillWithSomeImplementation(SomeImplementation impl) {
        if (impl == null)
            return null;

        Object[] result = new Object[1];
        result[0] = impl;
        return result;
    }

    private <T, E> void genericAssignmentWithCast(T[] data, E element) {
        if (data == null || data.length == 0) return;
        data[0] = (T) element;
    }

    private <T, E extends T> void genericAssignmentWithExtends(T[] data, E element) {
        if (data == null || data.length == 0) return;
        data[0] = element;
    }
}
