package analyzer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.util.Map;

public class ConfigurationManager {

    private final ClassLoader classLoader;
    private final Class userConfigurationClass;

    public ConfigurationManager(ClassLoader classLoader, Class userConfigurationClass) {
        this.classLoader = classLoader;
        this.userConfigurationClass = userConfigurationClass;
    }

    public void patchPropertySourceAnnotation() throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException {

        Class<?> proxyClass = classLoader.loadClass("java.lang.reflect.Proxy");
        Field hField = proxyClass.getDeclaredField("h");
        hField.setAccessible(true);

        InvocationHandler o = (InvocationHandler) (hField.get(userConfigurationClass.getAnnotations()[2]));

        Class<?> annotationInvocationHandlerClass = classLoader.loadClass("sun.reflect.annotation.AnnotationInvocationHandler");
        Field memberValuesField = annotationInvocationHandlerClass.getDeclaredField("memberValues");
        memberValuesField.setAccessible(true);
        Map<String, Object> memberValues = (Map<String, Object>) (memberValuesField.get(o));
        memberValues.put("value", "classpath:fakeapplication.properties");
    }
}
