package org.utbot.examples.codegen.deepequals;

class FirstClass {
    SecondClass secondClass;

    FirstClass(SecondClass second) {
        this.secondClass = second;
    }
}

class SecondClass {
    FirstClass firstClass;

    SecondClass(FirstClass first) {
        this.firstClass = first;
    }
}

public class ClassWithCrossReferenceRelationship {
    public FirstClass returnFirstClass(int value) {
        if (value == 0) {
            return new FirstClass(new SecondClass(null));
        } else {
            FirstClass first = new FirstClass(null);
            first.secondClass = new SecondClass(first);

            return first;
        }
    }
}
