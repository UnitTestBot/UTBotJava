package analyzer;

import org.springframework.context.annotation.PropertySource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public class ConfigurationManager {

    private final ClassLoader classLoader;
    private final Class userConfigurationClass;

    public ConfigurationManager(ClassLoader classLoader, Class userConfigurationClass) {
        this.classLoader = classLoader;
        this.userConfigurationClass = userConfigurationClass;
    }

    public void patchPropertySourceAnnotation() throws Exception {

        Class<?> proxyClass = classLoader.loadClass("java.lang.reflect.Proxy");
        Field hField = proxyClass.getDeclaredField("h");
        hField.setAccessible(true);

        //Annotation annotationProxy = userConfigurationClass.getAnnotations()[2];
        Optional<Annotation> propertySourceAnnotation =
                Arrays.stream(userConfigurationClass.getAnnotations())
                        .filter(el -> el.annotationType() == PropertySource.class)
                        .findFirst();
        if (propertySourceAnnotation.isPresent()) {
            InvocationHandler annotationInvocationHandler = (InvocationHandler) (hField.get(propertySourceAnnotation.get()));

            Class<?> annotationInvocationHandlerClass =
                    classLoader.loadClass("sun.reflect.annotation.AnnotationInvocationHandler");
            Field memberValuesField = annotationInvocationHandlerClass.getDeclaredField("memberValues");
            memberValuesField.setAccessible(true);

            Map<String, Object> memberValues = (Map<String, Object>) (memberValuesField.get(annotationInvocationHandler));
            memberValues.put("value", "classpath:fakeapplication.properties");
        }
    }
}
