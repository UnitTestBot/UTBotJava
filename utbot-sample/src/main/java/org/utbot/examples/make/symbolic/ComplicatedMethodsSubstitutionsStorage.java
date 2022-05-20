package org.utbot.examples.make.symbolic;

import org.utbot.api.annotation.UtClassMock;
import org.utbot.api.annotation.UtConstructorMock;
import org.utbot.api.mock.UtMock;

@UtClassMock(target = ClassWithComplicatedMethods.class)
public class ComplicatedMethodsSubstitutionsStorage {
    int a;

    @UtConstructorMock
    public ComplicatedMethodsSubstitutionsStorage(double a, double b) {
        UtMock.assume(a < 0);
        UtMock.assume(b < 0);

        this.a = (int) (a + b);
    }

    public ComplicatedMethodsSubstitutionsStorage constructComplicatedMethod(int x) {
        UtMock.assume(x > 100);
        UtMock.assume(x != a);

        this.a = x;
        return this;
    }

    public void methodWithSideEffect(int x) {
        UtMock.assume(a == 15);
        UtMock.assume(x > 0);

        double result = Math.sqrt(x);

        if (result == x) {
            a = 2821;
        } else {
            a = 2822;
        }
    }
}