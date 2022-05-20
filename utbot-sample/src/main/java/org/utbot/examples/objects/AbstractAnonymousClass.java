package org.utbot.examples.objects;

abstract class AbstractAnonymousClass {
    abstract int constValue();
    abstract int add(int x);

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
}
