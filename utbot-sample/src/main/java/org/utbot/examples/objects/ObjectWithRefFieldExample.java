package org.utbot.examples.objects;

public class ObjectWithRefFieldExample {
    public ObjectWithRefFieldClass defaultValue() {
        ObjectWithRefFieldClass obj = new ObjectWithRefFieldClass();
        //noinspection IfStatementWithIdenticalBranches
        if (obj.x != 0 || obj.y != 0 || obj.weight != 0.0 || obj.refField != null || obj.arrayField != null) {
            return obj;
        }
        return obj;
    }

    public ObjectWithRefFieldClass writeToRefTypeField(ObjectWithRefFieldClass objectExample, int value) {
        if (value != 42 || objectExample.refField != null) {
            throw new IllegalArgumentException();
        }

        SimpleDataClass simpleDataClass = new SimpleDataClass();
        simpleDataClass.a = value;
        simpleDataClass.b = value * 2;

        objectExample.refField = simpleDataClass;

        return objectExample;
    }

    public ObjectWithRefFieldClass defaultFieldValues() {
        return new ObjectWithRefFieldClass();
    }

    public int readFromRefTypeField(ObjectWithRefFieldClass objectExample) {
        if (objectExample.refField.a <= 0) {
            return -1;
        }

        return objectExample.refField.a;
    }

    public ObjectWithRefFieldClass writeToArrayField(ObjectWithRefFieldClass objectExample, int length) {
        if (length < 3) {
            throw new IllegalArgumentException();
        }

        int[] array = new int[length];
        for (int i = 0; i < length; i++) {
            array[i] = i + 1;
        }

        objectExample.arrayField = array;

        objectExample.arrayField[length - 1] = 100;

        return objectExample;
    }

    public int readFromArrayField(ObjectWithRefFieldClass objectExample, int value) {
        if (objectExample.arrayField[2] == value) {
            return 1;
        }
        return 2;
    }

    @SuppressWarnings("ConstantConditions")
    public int compareTwoDifferentObjectsFromArguments(ObjectWithRefFieldClass fst, ObjectWithRefFieldClass snd) {
        if (fst.x > 0 && snd.x < 0) {
            if (fst == snd) {
                throw new RuntimeException();
            } else {
                return 1;
            }
        }

        fst.x = snd.x;
        fst.y = snd.y;
        fst.weight = snd.weight;

        if (fst == snd) {
            return 2;
        }

        return 3;
    }

    @SuppressWarnings("ConstantConditions")
    public int compareTwoObjectsWithNullRefField(ObjectWithRefFieldClass fst, ObjectWithRefFieldClass snd) {
        fst.refField = null;
        snd.refField = new SimpleDataClass();
        if (fst == snd) {
            return 1;
        }
        return 2;
    }

    public int compareTwoObjectsWithDifferentRefField(ObjectWithRefFieldClass fst, ObjectWithRefFieldClass snd, int value) {
        fst.refField = new SimpleDataClass();
        fst.refField.a = value;

        snd.refField = new SimpleDataClass();
        snd.refField.a = fst.refField.a + 1;

        fst.refField.b = snd.refField.b;

        if (fst == snd) {
            return 1;
        }

        return 2;
    }

    public boolean compareTwoObjectsWithDifferentRefField(ObjectWithRefFieldClass fst, ObjectWithRefFieldClass snd) {
        return fst.refField == snd.refField;
    }

    public int compareTwoObjectsWithTheSameRefField(ObjectWithRefFieldClass fst, ObjectWithRefFieldClass snd) {
        SimpleDataClass simpleDataClass = new SimpleDataClass();

        fst.refField = simpleDataClass;
        snd.refField = simpleDataClass;
        fst.x = snd.x;
        fst.y = snd.y;
        fst.weight = snd.weight;

        if (fst == snd) {
            return 1;
        }

        return 2;
    }
}
