package org.utbot.examples.casts;

import org.utbot.api.mock.UtMock;
import java.util.Collection;
import java.util.List;

public class ArrayCastExample {
    @SuppressWarnings("RedundantCast")
    public CastClass[] castToAncestor(CastClassFirstSucc[] array) {
        if (array == null) {
            return new CastClass[0];
        }
        return (CastClass[]) array;
    }

    public CastClassFirstSucc[] classCastException(CastClass[] array) {
        if (array == null) {
            return new CastClassFirstSucc[0];
        }
        return (CastClassFirstSucc[]) array;
    }

    @SuppressWarnings("ConstantConditions")
    public CastClassFirstSucc[] nullCast(CastClass[] array) {
        if (array != null) {
            array = null;
        }

        return (CastClassFirstSucc[]) array;
    }

    public CastClassFirstSucc[] castFromObject(Object array) {
        if (array == null) {
            return new CastClassFirstSucc[0];
        }
        return (CastClassFirstSucc[]) array;
    }

    public int[][][] nullArray() {
        return null;
    }

    public ColoredPoint[] successfulExampleFromJLS() {
        Point[] pa = new ColoredPoint[4];
        pa[0] = new ColoredPoint(2, 2, 12);
        pa[1] = new ColoredPoint(4, 5, 24);
        return (ColoredPoint[]) pa;
    }

    public ColoredPoint[] castAfterStore(Point[] pa) {
        pa[0] = new ColoredPoint(1, 2, 12);
        ColoredPoint[] cpa = (ColoredPoint[]) pa;
        cpa[1] = new ColoredPoint(2, 3, 14);
        return cpa;
    }

    public int[] castFromObjectToPrimitivesArray(Object array) {
        UtMock.assume(array != null);

        int[] intArray = (int[]) array;
        UtMock.assume(intArray.length > 0);

        return intArray;
    }

    public ColoredPoint[] castsChainFromObject(Object array) {
        if (array == null) {
            return null;
        }

        Point[] pa = (Point[]) array;
        if (pa.length < 1) {
            return null;
        }

        // two returns because we want avoid pathSelector optimization
        //noinspection IfStatementWithIdenticalBranches
        if (pa[0].x != 1) {
            pa[0].x = 5;
            return (ColoredPoint[]) pa;
        } else {
            pa[0].x = 10;
            return (ColoredPoint[]) pa;
        }
    }

    public List<ColoredPoint> castFromCollections(Collection<ColoredPoint> collection) {
        if (collection == null) {
            return null;
        }

        return (List<ColoredPoint>) collection;
    }

    public List<ColoredPoint> castFromIterable(Iterable<ColoredPoint> iterable) {
        if (iterable == null) {
            return null;
        }

        return (List<ColoredPoint>) iterable;
    }

    public Collection<ColoredPoint> castFromIterableToCollection(Iterable<ColoredPoint> iterable) {
        if (iterable == null) {
            return null;
        }

        return (Collection<ColoredPoint>) iterable;
    }
}
