package org.utbot.examples.objects;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public class ClassRef {
    public Class<Boolean> takeBooleanClassRef() {
        return boolean.class;
    }

    public Class<ClassRef> takeClassRef() {
        return ClassRef.class;
    }

    public Class<?> takeClassRefFromParam(ClassRef classRef) {
        return classRef.getClass();
    }

    public Class<?> takeArrayClassRef() {
        return ClassRef[].class;
    }

    public Class<?> twoDimArrayClassRef() {
        return ClassRef[][].class;
    }

    public Class<?> twoDimArrayClassRefFromParam(ClassRef[][] classRef) {
        return classRef.getClass();
    }

    public Class<?> takeConstantClassRef() {
        return ClassRef.class;
    }

    @SuppressWarnings("ConstantConditions")
    public boolean equalityOnClassRef() {
        ClassRef ref1 = new ClassRef();
        ClassRef ref2 = new ClassRef();
        return ref1.getClass() == ref2.getClass();
    }

    public boolean equalityOnStringClassRef() {
        String a = "a";
        String b = "b";
        return a.getClass() == b.getClass();
    }

    public boolean equalityOnArrayClassRef() {
        int[] a = {1, 2};
        int[] b = {3, 4};
        return a.getClass() == b.getClass();
    }

    public boolean twoDimensionalArrayClassRef() {
        int[][] a = {{1, 2}, {3, 4}};
        int[][] b = {{4, 3}, {2, 1}};

        return a.getClass() == b.getClass();
    }

    public boolean equalityOnGenericClassRef() {
        List<Integer> ints = new ArrayList<>();
        List<Long> longs = new ArrayList<>();
        return ints.getClass() == longs.getClass();
    }
}