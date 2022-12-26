package org.utbot.api.exception;

public class UtMockAssumptionViolatedException extends RuntimeException {

    public static final String errorMessage = "UtMock assumption violated";

    @Override
    public String getMessage() {
        return errorMessage;
    }
}
