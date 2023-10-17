package org.utbot.api.java;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.utbot.common.PathUtil;
import org.utbot.external.api.TestMethodInfo;
import org.utbot.external.api.UtModelFactory;
import org.utbot.framework.plugin.api.*;
import org.utbot.framework.plugin.api.util.UtContext;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.utbot.framework.plugin.api.util.IdUtilKt.getExecutableId;

/**
 * Extracted to share between modules
 */
public class AbstractUtBotJavaApiTest {

    public static final String GENERATED_TEST_CLASS_NAME = "GeneratedTest";

    public static Method getMethodByName(Class<?> clazz, String name, Class<?>... parameters) {
        try {
            return clazz.getDeclaredMethod(name, parameters);
        } catch (NoSuchMethodException ignored) {
            Assertions.fail();
        }
        throw new RuntimeException();
    }

    private AutoCloseable context;
    // Helpful hand to create  models for classes
    protected UtModelFactory modelFactory;

    @BeforeEach
    public void setUp() {
        context = UtContext.Companion.setUtContext(new UtContext(UtContext.class.getClassLoader()));
        modelFactory = new UtModelFactory();
    }

    @AfterEach
    public void tearDown() {
        try {
            context.close();
            modelFactory = null;
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    protected static TestMethodInfo buildTestMethodInfo(
            Method methodUnderTest,
            UtCompositeModel classUnderTestModel,
            List<UtModel> parametersModels,
            Map<FieldId, UtModel> staticsModels
    ) {

        ExecutableId methodExecutableId = getExecutableId(methodUnderTest);
        EnvironmentModels methodState = new EnvironmentModels(
                classUnderTestModel,
                parametersModels,
                staticsModels,
                methodExecutableId
        );

        return new TestMethodInfo(methodUnderTest, methodState);
    }

    /**
     * Utility method to get class path of the class
     */
    @NotNull
    protected static String getClassPath(Class<?> clazz) {
        try {
            return normalizePath(clazz.getProtectionDomain().getCodeSource().getLocation());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    protected static String normalizePath(URL url) throws URISyntaxException {
        return new File(url.toURI()).getPath();
    }

    /**
     * @return classpath of the current thread. For testing environment only.
     */
    @NotNull
    protected static String getDependencyClassPath() {

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        URL[] urls = PathUtil.getUrlsFromClassLoader(contextClassLoader);

        return Arrays.stream(urls).map(url ->
        {
            try {
                return new File(url.toURI()).toString();
            } catch (URISyntaxException e) {
                Assertions.fail(e);
            }
            throw new RuntimeException();
        }).collect(Collectors.joining(File.pathSeparator));
    }
}
