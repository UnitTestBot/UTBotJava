package org.utbot.examples.synthesis;

import java.util.*;

public class SynthesisExamples {
    public void synthesizePoint(Point p) {
        if (p.getX() > 125) {
            if (p.getY() < 100000) {
                throw new IllegalArgumentException();
            }
        }
    }

    public void synthesizeInterface(SynthesisInterface i) {
        if (i.getX() == 10) {
            throw new IllegalArgumentException();
        }
    }

    public void synthesizeList(List<Point> points) {
        if (points.get(0).getX() >= 14) {
            throw new IllegalArgumentException();
        }
    }

    public void synthesizeSet(Set<Point> points) {
        for (Point p : points) {
            if (p.getX() >= 14) {
                throw new IllegalArgumentException();
            }
        }
    }

    public void synthesizeList2(int length, int index, int value) {
        ArrayList<Point> p = new ArrayList<>(Arrays.asList(new Point[length]));
        p.set(index, new Point(value));
        synthesizeList(p);
    }

    public void synthesizeObject(Object o) {
        if (o instanceof Point) {
            throw new IllegalArgumentException();
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void synthesizeDeepComplexObject(DeepComplexObject c) {
        if (c.getO().getA().getY() == 1) {
            throw new IllegalArgumentException();
        }
    }

    public void synthesizeComplexCounter(ComplexCounter c, ComplexObject b) {
        if (c.getA() == 5 && b.getA().getX() == 1000) {
            throw new IllegalArgumentException();
        }
    }

    public void synthesizeComplexObject(ComplexObject b) {
        if (b.getA().getX() == 1000) {
            throw new IllegalArgumentException();
        }
    }

    public void synthesizeComplexCounter2(ComplexCounter c, ComplexCounter d) {
        ComplexCounter f = new ComplexCounter();
        f.incrementA(5);
        int a = c.getA();
        int b = f.getB();
        if (c != d) {
            if (a == b) {
                throw new IllegalArgumentException();
            }
        }
    }

    public void synthesizeComplexCounter3(ComplexCounter c) {
        ComplexCounter f = new ComplexCounter();
        f.incrementA(4);
        if (c != f) {
            throw new IllegalArgumentException();
        }
    }

    public void synthesizeComplexObject2(ComplexObject a, ComplexObject b) {
        if (a.getA() == b.getA()) {
            throw new IllegalArgumentException();
        }
    }

    public void synthesizeInt(int a, int b) {
        if (a >= 2) {
            if (b <= 11) {
                throw new IllegalArgumentException();
            }
        }
    }

    public void synthesizeSimpleList(SimpleList a) {
        if (a.size() == 2) {
            if (a.get(0).getX() == 2) {
                if (a.get(1).getY() == 11) {
                    throw new IllegalArgumentException();
                }
            }
        }
    }

    public void synthesizeIntArray(int[] a) {
        if (a.length > 10) {
            throw new IllegalArgumentException();
        }
    }

    public void synthesizePointArray(Point[] a, int i) {
        if (a[i].getX() > 14) {
            throw new IllegalArgumentException();
        }
    }

    public void synthesizePointArray2(Point[] array, int x, int y) {
        if (array[x].getX() == y) {
            throw new IllegalArgumentException();
        }
    }

    public void synthesizeDoublePointArray(Point[][] a, int i, int j) {
        if (a[i][j].getX() > 14) {
            throw new IllegalArgumentException();
        }
    }

    public void synthesizeInterfaceArray(SynthesisInterface[] a, int i) {
        if (a[i].getX() == 10) {
            throw new IllegalArgumentException();
        }
    }
}
