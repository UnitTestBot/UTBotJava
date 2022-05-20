package org.utbot.external.api;

import org.utbot.framework.plugin.api.EnvironmentModels;
import org.utbot.framework.plugin.api.UtExecutionResult;
import java.lang.reflect.Method;

public class TestMethodInfo {
    private final UtExecutionResult utResult;

    public Method getMethodToBeTestedFromUserInput() {
        return methodToBeTestedFromUserInput;
    }

    public EnvironmentModels getInitialState() {
        return initialState;
    }

    final private Method methodToBeTestedFromUserInput;
    final private EnvironmentModels initialState;

    public TestMethodInfo(Method methodToBeTestedFromUserInput,
                          EnvironmentModels initialState,
                          UtExecutionResult utResult) {
        this.methodToBeTestedFromUserInput = methodToBeTestedFromUserInput;
        this.initialState = initialState;
        this.utResult = utResult;
    }

    public TestMethodInfo(Method methodToBeTestedFromUserInput,
                          EnvironmentModels initialState) {
        this(methodToBeTestedFromUserInput, initialState, null);
    }

    public UtExecutionResult getUtResult() {
        return utResult;
    }
}