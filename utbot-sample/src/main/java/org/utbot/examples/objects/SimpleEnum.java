package org.utbot.examples.objects;

public enum SimpleEnum {
    FIRST(1),
    SECOND(2);

    private final int code;

    SimpleEnum(int code) {
        this.code = code;
    }

    static SimpleEnum fromCode(int code) {
        for (SimpleEnum value : values()) {
            if (value.getCode() == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("No enum corresponding to given code: " + code);
    }

    private int getCode() {
        return code;
    }

    static SimpleEnum fromIsFirst(boolean isFirst) {
        return isFirst ? FIRST : SECOND;
    }

    int publicGetCode() {
        return this == FIRST ? 1 : 2;
    }
}