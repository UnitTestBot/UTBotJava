package org.utbot.examples.strings;

public class GenericExamples<T> {
    public boolean containsOk(T obj) {
        return obj.toString().contains("ok");
    }

    public boolean containsOkExample() {
        return new GenericExamples<String>().containsOk("Elders have spoken");
    }
}
