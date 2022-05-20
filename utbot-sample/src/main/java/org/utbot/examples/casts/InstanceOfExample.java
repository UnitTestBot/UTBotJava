package org.utbot.examples.casts;

import org.utbot.api.mock.UtMock;
import java.util.HashMap;
import java.util.Map;

public class InstanceOfExample {
    public CastClassFirstSucc simpleInstanceOf(CastClass objectExample) {
        if (objectExample instanceof CastClassFirstSucc) {
            return (CastClassFirstSucc) objectExample;
        }

        return null;
    }

    public int nullPointerCheck(CastClass objectExample) {
        if (objectExample instanceof CastClassFirstSucc) {
            return ((CastClassFirstSucc) objectExample).z;
        }

        return objectExample.x;
    }

    public int virtualCall(CastClass objectExample) {
        if (objectExample instanceof CastClassFirstSucc) {
            return objectExample.foo();
        }

        return -1;
    }

    public int virtualFunctionCallWithCast(Object objectExample) {
        if (objectExample instanceof CastClass) {
            return ((CastClassFirstSucc) objectExample).foo();
        }

        return -1;
    }

    public int virtualCallWithoutOneInheritor(CastClass objectExample) {
        if (objectExample instanceof CastClassFirstSucc) {
            return ((CastClassFirstSucc) objectExample).z;
        }
        return objectExample.foo();
    }

    public int virtualCallWithoutOneInheritorInverse(CastClass objectExample) {
        if (!(objectExample instanceof CastClassFirstSucc)) {
            return objectExample.foo();
        }

        return ((CastClassFirstSucc) objectExample).z;
    }

    public int withoutOneInheritorOnArray(Object objectExamples) {
        if (objectExamples instanceof CastClassFirstSucc[]) {
            return 0;
        } else {
            return 1;
        }
    }

    public int withoutOneInheritorOnArrayInverse(Object objectExamples) {
        if (!(objectExamples instanceof CastClassFirstSucc[])) {
            return 0;
        } else {
            return 1;
        }
    }

    public int instanceOfAsPartOfInternalExpressions(Object[] objectExample) {
        UtMock.assume(objectExample != null);
        UtMock.assume(objectExample.length == 2);

        boolean isElem0FirstSucc = objectExample[0] instanceof CastClassFirstSucc[];
        boolean isElem1SecondSucc = objectExample[1] instanceof CastClassSecondSucc[];
        boolean and = isElem0FirstSucc && isElem1SecondSucc;
        if (and) {
            return 1;
        }

        boolean isElem0SecondSucc = objectExample[0] instanceof CastClassSecondSucc[];
        boolean isElem1FirstSucc = objectExample[1] instanceof CastClassFirstSucc[];
        boolean or = isElem0SecondSucc || isElem1FirstSucc;
        if (or) {
            return 2;
        }

        return 3;
    }

    public int instanceOfAsPartOfInternalExpressionsCastClass(Object[] objectExample) {
        UtMock.assume(objectExample != null);
        UtMock.assume(objectExample.length == 2);

        boolean isElem0CastClass = objectExample[0] instanceof CastClass[];
        boolean isElem1CastClass = objectExample[1] instanceof CastClass[];
        boolean and = isElem0CastClass && isElem1CastClass;
        if (!and) {
            return 1;
        }

        boolean isElem0SecondSucc = objectExample[0] instanceof CastClassSecondSucc[];
        boolean isElem1FirstSucc = objectExample[1] instanceof CastClassFirstSucc[];
        boolean or = isElem0SecondSucc || isElem1FirstSucc;
        if (!or) {
            return 2;
        }

        return 3;
    }

    public int instanceOfAsPartOfInternalExpressionsXor(Object[] objectExample) {
        UtMock.assume(objectExample != null);
        UtMock.assume(objectExample.length == 2);

        boolean isElem0SecondSucc = objectExample[0] instanceof CastClassSecondSucc[];
        boolean isElem1FirstSucc = objectExample[1] instanceof CastClassFirstSucc[];
        boolean xor = isElem0SecondSucc ^ isElem1FirstSucc;

        if (xor) {
            if (isElem1FirstSucc) {
                return 1;
            } else {
                return 2;
            }
        } else {
            if (isElem1FirstSucc) {
                return 3;
            } else {
                return 4;
            }
        }
    }

    public int instanceOfAsPartOfInternalExpressionsXorInverse(Object[] objectExample) {
        UtMock.assume(objectExample != null);
        UtMock.assume(objectExample.length == 2);

        boolean isElem0SecondSucc = objectExample[0] instanceof CastClassSecondSucc[];
        boolean isElem1FirstSucc = objectExample[1] instanceof CastClassFirstSucc[];
        boolean xor = isElem0SecondSucc ^ isElem1FirstSucc;

        if (!xor) {
            if (isElem0SecondSucc) {
                return 1;
            } else {
                return 2;
            }
        } else {
            if (isElem0SecondSucc) {
                return 3;
            } else {
                return 4;
            }
        }
    }

    public int instanceOfAsPartOfInternalExpressionsIntValue(Object objectExamples) {
        boolean isArrayOfSecondSucc = objectExamples instanceof CastClassSecondSucc[];
        boolean isArrayOfCastClass = objectExamples instanceof CastClass[];
        int sum = (isArrayOfSecondSucc ? 1 : 0) + (isArrayOfCastClass ? 1 : 0);
        if (sum == 1) {
            return 1;
        } else if (sum == 2) {
            return 2;
        }
        return 3;
    }

    public int instanceOfAsInternalExpressionsMap(Object objectExamples) {
        Map<Boolean, Map<Boolean, Integer>> mp = new HashMap<Boolean, Map<Boolean, Integer>>() {{ // (bool, bool) -> int
            HashMap<Boolean, Integer> mp0 = new HashMap<>();
            mp0.put(false, 0);
            mp0.put(true, 1);

            HashMap<Boolean, Integer> mp1 = new HashMap<>();
            mp1.put(false, 2);
            mp1.put(true, 3);

            put(false, mp0);
            put(true, mp1);

        }};

        boolean first = objectExamples instanceof CastClassFirstSucc[];
        boolean second = objectExamples instanceof CastClassSecondSucc[];

        int value = mp.get(first).get(second);

        if (value == 0) { // !first && !second
            return 0;
        } else if (value == 1) { // !first && second
            return 1;
        } else if (value == 2) { // first && !second
            return 2;
        } else { // first && second (unreachable)
            return 3;
        }
    }

    public CastClass symbolicInstanceOf(CastClass[] objects, int index) {
        if (index < 1 || index > 3) {
            return null;
        }

        if (objects[index] instanceof CastClassFirstSucc) {
            return objects[index];
        }

        objects[index] = new CastClassSecondSucc();

        return objects[index];
    }

    public CastClass[] complicatedInstanceOf(CastClass[] objects, int index, CastClass objectExample) {
        if (index < 0 || index > 2 || objects == null || objects.length < index + 2) {
            return null;
        }

        if (objectExample instanceof CastClassFirstSucc) {
            objects[index] = objectExample;
            objects[index + 1] = new CastClassSecondSucc();
        } else {
            objects[index] = objectExample;
            objects[index + 1] = new CastClassFirstSucc();
        }

        if (objects[index + 1] instanceof CastClassSecondSucc) {
            objects[index].x = ((CastClassFirstSucc) objects[index]).z;
        } else if (objects[index + 1] instanceof CastClassFirstSucc) {
            objects[index].x = objects[index].foo(); // virtual call
        }

        return objects;
    }

    public CastClass[] instanceOfFromArray(CastClass[] array) {
        if (array.length != 3) {
            return null;
        }

        if (array[0] instanceof CastClassFirstSucc) {
            return array;
        }

        if (array[0] instanceof CastClassSecondSucc) {
            array[0] = null;
            return array;
        }

        return array;
    }

    public CastClass instanceOfFromArrayWithReadingAnotherElement(CastClass[] array) {
        if (array.length < 2) {
            return null;
        }

        if (array[0] instanceof CastClassFirstSucc) {
            CastClass elem = array[1];
            return array[0];
        } else {
            return null;
        }
    }

    public CastClass instanceOfFromArrayWithReadingSameElement(CastClass[] array) {
        if (array.length < 2) {
            return null;
        }

        if (array[0] instanceof CastClassFirstSucc) {
            CastClass elem = array[0];
            return array[0];
        } else {
            return null;
        }
    }

    @SuppressWarnings("ConstantConditions")
    public int isNull(Number[] a) {
        if (a instanceof Object) {
            return 1;
        } else {
            return 2;
        }
    }

    @SuppressWarnings({"IfStatementWithIdenticalBranches", "ConstantConditions"})
    public Number[] arrayInstanceOfArray(Number[] a) {
        if (a == null) {
            return a;
        }
        if (a instanceof Integer[]) {
            return a;
        } else if (a instanceof Double[]) {
            return a;
        } else {
            return a;
        }
    }

    public Object objectInstanceOfArray(Object a) {
        if (a instanceof int[]) {
            return a;
        } else if (a instanceof boolean[]) {
            return a;
        }
        return a;
    }

    @SuppressWarnings("RedundantIfStatement")
    public Object[] instanceOfObjectArray(Object[] array) {
        if (array instanceof int[][][]) {
            return array;
        }

        if (array == null) {
            return null;
        }

        return array;
    }
}
