package org.utbot.examples.annotations.lombok;


public enum EnumWithoutAnnotations {
    ENUM_CONSTANT("Constant_1");
    private final String constant;

    EnumWithoutAnnotations(String constant) {
        this.constant = constant;
    }

    public String getConstant() {
        return constant;
    }
}