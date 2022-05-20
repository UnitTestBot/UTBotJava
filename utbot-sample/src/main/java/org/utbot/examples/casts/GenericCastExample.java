package org.utbot.examples.casts;

public class GenericCastExample {
    public <R extends Comparable<R>> R max(R fst, ClassWithGenerics<Object, R> snd) {
        int result = fst.compareTo(snd.getComparableGenericField());
        if (result >= 0) {
            return fst;
        } else {
            return snd.getComparableGenericField();
        }
    }

    public int compareTwoNumbers(Integer a, ClassWithGenerics<Object, Integer> b) {
        if (max(a, b).equals(a)) {
            return 1;
        } else {
            return -1;
        }
    }

    public int getGenericFieldValue(ClassWithGenerics<Integer, Double> generic) {
        return generic.getGenericField();
    }

    public int compareGenericField(ClassWithGenerics<String, Integer> generic, Integer value) {
        if (value.equals(generic.getComparableGenericField())) {
            return 1;
        }
        return -1;
    }

    public int createNewGenericObject() {
        ClassWithGenerics<Object, Integer> genericObject = new ClassWithGenerics<>(null, 10);
        return genericObject.getComparableGenericField();
    }

    public int sumFromArrayOfGenerics(ClassWithGenerics<Integer, Character>.InnerClassWithGeneric generic) {
        int a = generic.getGenericArray()[0];
        int b = generic.getGenericArray()[1];

        return a + b;
    }
}
