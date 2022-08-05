package org.utbot.examples.enums;

public enum State {
    OPEN(255) {
        @Override
        public String toString() {
            return "<open>";
        }
    },
    CLOSED(127) {
        @Override
        public String toString() {
            return "<closed>";
        }
    },
    UNKNOWN(0) {
        @Override
        public String toString() {
            return "<unknown>";
        }
    };

    private final int code;

    State(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static State findStateByCode(int code) {
        for (State state: values()) {
            if (state.getCode() == code) {
                return state;
            }
        }
        return UNKNOWN;
    }
}
