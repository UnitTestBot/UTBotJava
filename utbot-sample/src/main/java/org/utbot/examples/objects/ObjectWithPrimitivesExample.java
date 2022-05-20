package org.utbot.examples.objects;

public class ObjectWithPrimitivesExample {
    public ObjectWithPrimitivesClass max(ObjectWithPrimitivesClass fst, ObjectWithPrimitivesClass snd) {
        if (fst.x > snd.x && fst.y > snd.y) {
            return fst;
        } else if (fst.x < snd.x && fst.y < snd.y) {
            return snd;
        }
        return fst;
    }

    @SuppressWarnings("unused")
    public ObjectWithPrimitivesClass ignoredInputParameters(ObjectWithPrimitivesClass fst, ObjectWithPrimitivesClass snd) {
        return new ObjectWithPrimitivesClass();
    }

    public ObjectWithPrimitivesClass example(ObjectWithPrimitivesClass value) {
        if (value.x == 1) {
            return value;
        }
        value.x = 1;
        return value;
    }

    @SuppressWarnings("IfStatementWithIdenticalBranches")
    public ObjectWithPrimitivesClassSucc defaultValueForSuperclassFields() {
        ObjectWithPrimitivesClassSucc obj = new ObjectWithPrimitivesClassSucc();

        if (obj.x != 0) {
            return obj;
        }

        return obj;
    }

    public ObjectWithPrimitivesClass createObject(int a, int b, ObjectWithPrimitivesClass objectExample) {
        ObjectWithPrimitivesClass object = new ObjectWithPrimitivesClass();
        object.x = a + 5;
        object.y = b + 6;
        object.weight = objectExample.weight;
        if (object.weight < 0) {
            throw new IllegalArgumentException();
        }
        return object;
    }

    public ObjectWithPrimitivesClass memory(ObjectWithPrimitivesClass objectExample, int value) {
        if (value > 0) {
            objectExample.x = 1;
            objectExample.y = 2;
            objectExample.weight = 1.2;
        } else {
            objectExample.x = -1;
            objectExample.y = -2;
            objectExample.weight = -1.2;
        }
        return objectExample;
    }

    public ObjectWithPrimitivesClass nullExample(ObjectWithPrimitivesClass o) {
        if (o.x == 0 && o.y == 0) {
            o = null;
        }
        return o;
    }

    @SuppressWarnings("ConstantConditions")
    public int compareWithNull(ObjectWithPrimitivesClass fst, ObjectWithPrimitivesClass snd) {
        if (fst == null) {
            return 1;
        }
        if (null == snd) {
            return 2;
        }

        if (null == null) {
            return 3;
        }

        throw new RuntimeException();
    }

    public int compareTwoNullObjects(int value) {
        ObjectWithPrimitivesClass fst = new ObjectWithPrimitivesClass();
        ObjectWithPrimitivesClass snd = new ObjectWithPrimitivesClass();

        fst.x = value + 1;
        snd.x = value + 2;

        // Avoid Jimple return optimization
        if (fst.x == value + 1) {
            fst = null;
        }
        if (snd.x == value + 2) {
            snd = null;
        }

        if (fst == snd) {
            return 1;
        }

        throw new RuntimeException();
    }

    public boolean compareTwoOuterObjects(ObjectWithPrimitivesClass fst, ObjectWithPrimitivesClass snd) {
        if (fst == null || snd == null) throw new NullPointerException();

        return fst == snd;
    }

    public int compareTwoDifferentObjects(int a) {
        ObjectWithPrimitivesClass fst = new ObjectWithPrimitivesClass();
        ObjectWithPrimitivesClass snd = new ObjectWithPrimitivesClass();

        fst.x = a;
        snd.x = a;

        if (fst == snd) {
            throw new RuntimeException();
        }

        return 1;
    }

    @SuppressWarnings({"UnusedAssignment", "ConstantConditions"})
    public int compareTwoRefEqualObjects(int a) {
        ObjectWithPrimitivesClass fst = new ObjectWithPrimitivesClass();
        ObjectWithPrimitivesClass snd = new ObjectWithPrimitivesClass();

        snd = fst;
        fst.x = a;

        if (fst == snd) {
            return 1;
        }

        throw new RuntimeException();
    }

    @SuppressWarnings("NewObjectEquality")
    public int compareObjectWithArgument(ObjectWithPrimitivesClass fst) {
        ObjectWithPrimitivesClass snd = new ObjectWithPrimitivesClass();

        if (snd == fst) {
            throw new RuntimeException();
        }

        return 1;
    }

    public int compareTwoIdenticalObjectsFromArguments(ObjectWithPrimitivesClass fst, ObjectWithPrimitivesClass snd) {
        fst.x = snd.x;
        fst.y = snd.y;
        fst.weight = snd.weight;

        if (fst == snd) {
            return 1;
        }
        return 2;
    }


    public ObjectWithPrimitivesClass getOrDefault(ObjectWithPrimitivesClass objectExample, ObjectWithPrimitivesClass defaultValue) {
        if (defaultValue.x == 0 && defaultValue.y == 0) {
            throw new IllegalArgumentException();
        }

        if (objectExample == null) {
            return defaultValue;
        }

        return objectExample;
    }

    public int inheritorsFields(ObjectWithPrimitivesClassSucc fst, ObjectWithPrimitivesClass snd) {
        fst.x = 1; // ObjectWithPrimitivesClass_x
        fst.anotherX = 2; // ObjectWithPrimitivesClassSucc_anotherX
        fst.y = 3; // ObjectWithPrimitivesClass_y - we use y field of parent class and don't support field hiding
        fst.weight = 4.5; // ObjectWithPrimitivesClass_weight

        snd.x = 1; // ObjectWithPrimitivesClass_x
        snd.y = 2; // ObjectWithPrimitivesClass_y
        snd.weight = 3.4; // ObjectWithPrimitivesClass_weight

        return 1;
    }

    public ObjectWithPrimitivesClass createWithConstructor(int x, int y) {
        return new ObjectWithPrimitivesClass(x + 1, y + 2, 3.3);
    }

    public ObjectWithPrimitivesClassSucc createWithSuperConstructor(int x, int y, int anotherX) {
        return new ObjectWithPrimitivesClassSucc(x + 1, y + 2, 3.3, anotherX + 4);
    }

    public ObjectWithPrimitivesClass fieldWithDefaultValue(int x, int y) {
        return new ObjectWithPrimitivesClass(x, y, 3.3);
    }

    public int valueByDefault() {
        ObjectWithPrimitivesClass objectExample = new ObjectWithPrimitivesClass();
        return objectExample.valueByDefault;
    }
}

