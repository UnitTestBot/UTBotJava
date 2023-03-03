package org.utbot.examples.types;

import org.utbot.api.mock.UtMock;

import java.util.List;

public class PathDependentGenericsExample {
    public int pathDependentGenerics(GenericParent element) {
        if (element instanceof ClassWithOneGeneric) {
            functionWithOneGeneric((ClassWithOneGeneric<?>) element);
            return 1;
        }

        if (element instanceof ClassWithTwoGenerics) {
            functionWithTwoGenerics((ClassWithTwoGenerics<?, ?>) element);
            return 2;
        }

        return 3;
    }

    public int functionWithSeveralTypeConstraintsForTheSameObject(Object element) {
        if (element instanceof List<?>) {
            functionWithSeveralGenerics((List<? extends Number>) element, (List<?>) element);

            UtMock.assume(!((List<?>) element).isEmpty());
            Object value = ((List<?>) element).get(0);
            UtMock.assume(value != null);

            if (value instanceof Number) {
                return 1;
            } else {
                return 2; // unreachable
            }
        }

        return 3;
    }

    private <T, K extends Number> void functionWithSeveralGenerics(List<K> firstValue, List<T> anotherValue) {
    }

    private <T> void functionWithOneGeneric(ClassWithOneGeneric<T> value) {
        System.out.println();
    }

    private <T, K> void functionWithTwoGenerics(ClassWithTwoGenerics<T, K> value) {
        System.out.println();
    }
}

abstract class GenericParent {
}

class ClassWithOneGeneric<T> extends GenericParent {
}

class ClassWithTwoGenerics<T, A> extends GenericParent {
}
