package org.utbot.fuzzer.baseline.generator;

import org.utbot.framework.plugin.api.UtMethod;
import org.utbot.framework.plugin.api.UtValueExecution;
import org.utbot.framework.plugin.api.UtMethodValueTestSet;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KCallable;
import kotlin.reflect.KClass;
import kotlin.reflect.KFunction;
import kotlin.reflect.jvm.ReflectJvmMapping;

import static java.util.Collections.emptyMap;

public class Generator {
    public static List<UtValueExecution<?>> executions(UtMethod<?> utMethod, Class<?> clazz, Object caller) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> isSameMethod(utMethod, method))
                .filter(method -> !Modifier.isPrivate(method.getModifiers()))
                .map(method -> new TestMethodGen(method, caller).gen())
                .findFirst().orElseGet(Collections::emptyList);
    }

    private static boolean isSameMethod(UtMethod<?> utMethod, Method method) {
        KCallable<?> utCallable = utMethod.getCallable();
        if (!(utCallable instanceof KFunction<?>)) {
            return false;
        }
        KFunction<?> utKFunction = (KFunction<?>) utCallable;
        Method utJavaMethod = ReflectJvmMapping.getJavaMethod(utKFunction);
        if (utJavaMethod == null) {
            return false;
        }
        return utJavaMethod.equals(method);
    }

    public static UtMethodValueTestSet<?> generateTests(UtMethod<?> method) throws IllegalAccessException, InstantiationException {
        KClass<?> kClass = method.getClazz();
        Class<?> clazz = JvmClassMappingKt.getJavaClass(kClass);
        // TODO: newInstance() is deprecated, need to create an instance in another way
        Object object = clazz.newInstance();
        return new UtMethodValueTestSet<>(method, executions(method, clazz, object), emptyMap());
    }
}
