package org.utbot.examples.enums;

import static org.utbot.examples.enums.ClassWithEnum.StatusEnum.*;

public class ClassWithEnum {
    public int useOrdinal(String s) {
        if (s != null) {
            return READY.ordinal();
        } else {
            return ERROR.ordinal();
        }
    }

    @SuppressWarnings("unused")
    public int useGetter(String s) {
        if (s != null) {
            return READY.getX();
        } else {
            return ERROR.getX();
        }
    }

    enum StatusEnum {
        READY(0),
        ERROR(-1);

        int x;

        StatusEnum(int x) {
            this.x = x;
        }

        public int getX() {
            return x;
        }
    }
}
