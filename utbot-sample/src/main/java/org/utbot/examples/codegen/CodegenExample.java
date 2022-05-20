package org.utbot.examples.codegen;

import org.utbot.examples.codegen.nested.ClassFromAnotherPackage;
import java.util.List;
import java.util.Random;

@SuppressWarnings({"unused", "FieldMayBeFinal"})
public class CodegenExample {
    // return multidimensional array of ref types
    public Object[][] firstExample(
            ClassFromTheSamePackage a, ClassFromAnotherPackage b,
            List<Integer> c, List<ClassFromAnotherPackage> d,
            ClassFromAnotherPackage[] e, ClassFromAnotherPackage[][] f,
            Random fstRandom, Random sndRandom
    ) {
        int randomInt = new Random().nextInt();
        int anotherRandomInt = new Random().nextInt();

        ClassFromAnotherPackage bb = new ClassFromAnotherPackage(randomInt, anotherRandomInt);

        if (bb.c + d.get(0).getD() + ClassFromAnotherPackage.a + ClassFromAnotherPackage.b + ClassFromAnotherPackage.constValue() == 3) {
            int yetAnotherRandomInt = fstRandom.nextInt() + sndRandom.nextInt();

            if (yetAnotherRandomInt == 4 && ClassFromTheSamePackage.foo(c.get(0)) == 3 && ClassFromTheSamePackage.b == 1) {
                bb.c = yetAnotherRandomInt;
            }
        }

        Object[][] result = new Object[2][10];

        result[0] = new Object[]{a, b, c, d, e, f, fstRandom, sndRandom, bb};

        return result;
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    private class InnerClass {
        private int x;
        String s;

        InnerClass(int x) {
            this.x = 10 / x;
            s = String.valueOf(x);
        }

        int foo(int y) {
            if (x < y) {
                return s.charAt(1);
            }

            return s.charAt(2) / y;
        }
    }

    private static class StaticClass {
        private static int x = foo();

        private static int foo() {
            return 25 / (new Random()).nextInt();
        }

        int bar(int y) {
            if ((new Random()).nextInt() > 10) {
                return y / (x + (new Random()).nextInt());
            }

            return x / (new Random()).nextInt();
        }
    }
}
