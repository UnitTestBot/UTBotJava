package org.utbot.examples.mixed;

import org.utbot.api.mock.UtMock;
import org.utbot.examples.objects.ObjectWithPrimitivesClass;

public class Simplifier {
    public ObjectWithPrimitivesClass simplifyAdditionWithZero(ObjectWithPrimitivesClass fst) {
        UtMock.assume(fst != null);

        fst.x = 0;

        fst.x += fst.shortValue;

        return fst;
    }
}
