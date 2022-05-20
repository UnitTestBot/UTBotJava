package org.utbot.examples.casts;

import org.utbot.api.mock.UtMock;

public class CastExample {
    public CastClassFirstSucc simpleCast(CastClass objectExample) {
        if (objectExample == null) {
            return (CastClassFirstSucc) null;
        }
        return (CastClassFirstSucc) objectExample;
    }

    public CastClassSecondSucc castClassException(CastClass objectExample) {
        ((CastClassFirstSucc) objectExample).foo();
        return (CastClassSecondSucc) objectExample;
    }

    public CastClass castUp(CastClassFirstSucc objectExample) {
        return (CastClass) objectExample;
    }

    private CastClass getNull() {
        return null;
    }

    public CastClass[] castNullToDifferentTypes() {
        CastClass cc = getNull();
        CastClassFirstSucc a = (CastClassFirstSucc) cc;
        CastClassSecondSucc b = (CastClassSecondSucc) cc;
        CastClass[] result = new CastClass[2];
        result[0] = a;
        result[1] = b;
        return result;
    }

    public int fromObjectToPrimitive(Object obj) {
        return (int) obj;
    }

    public Colorable castFromObjectToInterface(Object object) {
        UtMock.assume(object != null);

        return (Colorable) object;
    }

    public CastClass complicatedCast(int index, Object[] array) {
        UtMock.assume(index == 0);
        UtMock.assume(array != null && array.length > index && array[index] != null);

        return (CastClassFirstSucc) array[index];
    }
}
