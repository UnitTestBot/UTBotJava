package org.utbot.examples.objects;

abstract class AbstractAnonymousClass {
    abstract int constValue();
    abstract int add(int x);

    public int methodWithoutOverrides(int x, int y) {
        return y + addFortyTwo(x);
    }

    public int methodWithOverride(int x, int y) {
        return y + addNumber(x);
    }

    public int addFortyTwo(int x) {
        return x + 42;
    }

    public int addNumber(int x) {
        return x + 27;
    }

    public static AbstractAnonymousClass getInstance(int x) {
        if (x % 2 == 0) {
            return new AnonymousClassAlternative();
        }

        return new AbstractAnonymousClass() {
            @Override
            int constValue() {
                return 42;
            }

            @Override
            int add(int x) {
                return x + 15;
            }

            @Override
            public int methodWithOverride(int x, int y) {
                return x + 37;
            }
        };
    }
}

class AnonymousClassAlternative extends AbstractAnonymousClass {
    @Override
    int constValue() {
        return 0;
    }

    @Override
    int add(int x) {
        return x + 1;
    }

    @Override
    public int methodWithOverride(int x, int y) {
        return x + 17;
    }
}
