

package org.utbot.quickcheck.internal.generator;

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator;
import org.utbot.framework.plugin.api.UtModel;
import org.utbot.quickcheck.generator.GenerationStatus;
import org.utbot.quickcheck.generator.Generator;
import org.utbot.quickcheck.internal.DefaultMethodHandleMaker;
import org.utbot.quickcheck.random.SourceOfRandomness;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static java.lang.System.identityHashCode;
import static java.lang.reflect.Proxy.newProxyInstance;
import static org.utbot.external.api.UtModelFactoryKt.classIdForType;

public class MarkerInterfaceGenerator<T> extends Generator<T> {
    private final Class<T> markerType;

    MarkerInterfaceGenerator(Class<T> markerType) {
        super(markerType);

        this.markerType = markerType;
    }

    @Override public UtModel generate(
        SourceOfRandomness random,
        GenerationStatus status) {
        return UtModelGenerator.getUtModelConstructor().construct(markerType.cast(
                newProxyInstance(
                        markerType.getClassLoader(),
                        new Class<?>[] { markerType },
                        new MarkerInvocationHandler<>(markerType))), classIdForType(markerType));
//        return markerType.cast(
//            newProxyInstance(
//                markerType.getClassLoader(),
//                new Class<?>[] { markerType },
//                new MarkerInvocationHandler<>(markerType)));
    }

    private static class MarkerInvocationHandler<T>
        implements InvocationHandler {

        private final Class<T> markerType;
        private final DefaultMethodHandleMaker methodHandleMaker =
            new DefaultMethodHandleMaker();

        MarkerInvocationHandler(Class<T> markerType) {
            this.markerType = markerType;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {

            if (Object.class.equals(method.getDeclaringClass()))
                return handleObjectMethod(proxy, method, args);
            if (method.isDefault())
                return handleDefaultMethod(proxy, method, args);

            return null;
        }

        private Object handleObjectMethod(
            Object proxy,
            Method method,
            Object[] args) {

            if ("equals".equals(method.getName()))
                return proxy == args[0];
            if ("hashCode".equals(method.getName()))
                return identityHashCode(proxy);

            return handleToString();
        }

        private Object handleDefaultMethod(
            Object proxy,
            Method method,
            Object[] args)
            throws Throwable {

            MethodHandle handle =
                methodHandleMaker.handleForSpecialMethod(method);
            return handle.bindTo(proxy).invokeWithArguments(args);
        }

        private String handleToString() {
            return "a synthetic instance of " + markerType;
        }
    }
}
