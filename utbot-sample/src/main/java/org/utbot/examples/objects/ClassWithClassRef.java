package org.utbot.examples.objects;

import java.util.List;

public class ClassWithClassRef {
    protected Class<? extends List<?>> someListClass;

    public String classRefName() {
        return someListClass.getName();
    }
}