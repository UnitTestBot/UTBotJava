package org.utbot.fuzzer.baseline.generator;

import org.utbot.framework.plugin.api.UtConcreteValue;
import org.utbot.framework.plugin.api.UtValueExecution;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

import static org.utbot.fuzzer.baseline.BaselineFuzzerKt.failedUtExecution;
import static org.utbot.fuzzer.baseline.BaselineFuzzerKt.successfulUtExecution;


/**
 *
 */
public class TestMethodGen {
    // TODO: add selection of test cases number to the plugin
    private final int TEST_CASES_NUMBER = 5;

    private final Method method;
    private final Object caller;

    private final TypeVariable<Method>[] typeParameters;
    private final Type[] typeValues;

    public TestMethodGen(Method method, Object caller) {
        this.method = method;
        this.caller = caller;
        typeParameters = method.getTypeParameters();
        typeValues = TypeChooser.chooseTypeParameterValues(typeParameters);
    }

    public List<UtValueExecution<?>> gen() {
        List<UtValueExecution<?>> executions = new ArrayList<>();
        for (int i = 0; i < TEST_CASES_NUMBER; i++) {
            UtValueExecution<?> execution = generateExecution();
            if (execution != null) {
                executions.add(generateExecution());
            }
        }
        return executions;
    }

    private UtValueExecution<?> generateExecution() {
        List<UtConcreteValue<?>> params = new ArrayList<>();
        for (int i = 0; i < method.getParameters().length; i++) {
            Type parType = method.getGenericParameterTypes()[i];
            ValueGen valueGen = new ValueGen(parType, typeParameters, typeValues);
            Object value = valueGen.generate();
            params.add(UtConcreteValue.invoke(value));
        }
        Object[] arguments = params.stream()
                .map(UtConcreteValue::getValue).toArray();

        UtValueExecution<?> execution;
        try {
            Object result = method.invoke(caller, arguments);
            execution = successfulUtExecution(params, result);
        } catch (IllegalAccessException | InvocationTargetException e) {
            // TODO: what if these exceptions were thrown by the method under test?
            return null;
        } catch (Exception e) {
            execution = failedUtExecution(params, e);
        }

        return execution;
    }
}
