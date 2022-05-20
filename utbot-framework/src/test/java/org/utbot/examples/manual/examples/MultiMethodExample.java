package org.utbot.examples.manual.examples;

public class MultiMethodExample {
    public void firstMethod() {

    }

    public String secondMethod() {
        return "Nothing";
    }

    public int thirdMethod(String param) {
        return  param == null ? 0 : param.length();
    }
}
