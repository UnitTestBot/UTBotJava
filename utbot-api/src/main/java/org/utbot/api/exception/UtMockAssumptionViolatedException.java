package org.utbot.api.exception;

public class UtMockAssumptionViolatedException extends RuntimeException {
    @Override
    public String getMessage() {
        return "UtMock assumption violated";
    }
}
