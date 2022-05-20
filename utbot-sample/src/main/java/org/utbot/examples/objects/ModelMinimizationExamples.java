package org.utbot.examples.objects;

import org.utbot.api.mock.UtMock;

public class ModelMinimizationExamples {
    public boolean singleValueComparison(WrappedIntQuad quad) {
        WrappedIntQuad other = new WrappedIntQuad();
        return other.a.getValue() == quad.a.getValue();
    }

    public boolean singleValueComparisonNotNull(WrappedIntQuad quad) {
        UtMock.assume(quad != null && quad.a != null);
        WrappedIntQuad other = new WrappedIntQuad();
        return other.a.getValue() == quad.a.getValue();
    }

    public boolean singleFieldComparisonExample(WrappedIntQuad quad) {
        WrappedIntQuad other = new WrappedIntQuad();
        return other.a == quad.a;
    }

    public boolean conditionCheckANe(WrappedInt a, WrappedInt b) {
        UtMock.assume(a != null && b != null && a != b);
        int aValue = a.getValue();
        if (aValue == 42) {
            return true;
        }

        return aValue <= 0;
    }

    public boolean conditionCheckAEq(WrappedInt a, WrappedInt b) {
        UtMock.assume(a != null && a == b);
        int aValue = a.getValue();
        if (aValue == 42) {
            return true;
        }

        return aValue <= 0;
    }

    public boolean conditionCheckBNe(WrappedInt a, WrappedInt b) {
        UtMock.assume(a != null && b != null && a != b);
        int bValue = b.getValue();
        if (bValue == 42) {
            return true;
        }

        return bValue <= 0;
    }

    public boolean conditionCheckBEq(WrappedInt a, WrappedInt b) {
        UtMock.assume(a != null && a == b);
        int bValue = b.getValue();
        if (bValue == 42) {
            return true;
        }

        return bValue <= 0;
    }

    public boolean conditionCheckNoNullabilityConstraintExample(WrappedInt a, WrappedInt b) {
        int aValue = a.getValue();
        if (aValue == 42) {
            return true;
        }

        return aValue <= 0;
    }

    public int multipleConstraintsExample(WrappedInt a, WrappedInt b, WrappedInt c) {
        UtMock.assume(a != null && b != null && c != null);
        if (a.getValue() == 42) return 1;
        if (b.getValue() == 73) return 2;
        return 3;
    }

    public boolean firstArrayElementContainsSentinel(WrappedInt[] values) {
        UtMock.assume(values != null && values.length >= 3);
        for (WrappedInt value : values) {
            UtMock.assume(value != null);
        }

        return values[0].getValue() == 42;
    }
}
