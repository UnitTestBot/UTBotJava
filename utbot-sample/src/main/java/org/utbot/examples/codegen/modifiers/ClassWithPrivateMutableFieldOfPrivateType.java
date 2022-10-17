package org.utbot.examples.codegen.modifiers;

public class ClassWithPrivateMutableFieldOfPrivateType {
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private PrivateClass privateMutableField = null;

    public int changePrivateMutableFieldWithPrivateType() {
        privateMutableField = new PrivateClass();

        return privateMutableField.x;
    }

    private static class PrivateClass {
        int x = 0;
    }
}
